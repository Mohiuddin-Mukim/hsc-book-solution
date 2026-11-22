package bd.hscsolution.catalog.service;

import bd.hscsolution.catalog.dto.*;
import bd.hscsolution.catalog.entity.*;
import bd.hscsolution.catalog.entity.AcademicEnums.ContentStatus;
import bd.hscsolution.catalog.repository.*;
import bd.hscsolution.media.*;
import bd.hscsolution.security.AuditService;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AuthoringService {
    private final QuestionRepository questions;
    private final QuestionVersionRepository qVersions;
    private final SolutionRepository solutions;
    private final SolutionVersionRepository sVersions;
    private final SolutionStepRepository steps;
    private final McqOptionRepository options;
    private final McqExplanationRepository explanations;
    private final QuestionMediaAttachmentRepository qMedia;
    private final SolutionStepMediaAttachmentRepository stepMedia;
    private final MediaAssetRepository media;
    private final MediaStorageRouter storage;
    private final JdbcTemplate db;
    private final AuditService audit;
    private final ObjectMapper mapper;

    public AuthoringService(QuestionRepository questions, QuestionVersionRepository qVersions,
                            SolutionRepository solutions, SolutionVersionRepository sVersions,
                            SolutionStepRepository steps, McqOptionRepository options,
                            McqExplanationRepository explanations,
                            QuestionMediaAttachmentRepository qMedia,
                            SolutionStepMediaAttachmentRepository stepMedia,
                            MediaAssetRepository media, MediaStorageRouter storage,
                            JdbcTemplate db, AuditService audit, ObjectMapper mapper) {
        this.questions = questions;
        this.qVersions = qVersions;
        this.solutions = solutions;
        this.sVersions = sVersions;
        this.steps = steps;
        this.options = options;
        this.explanations = explanations;
        this.qMedia = qMedia;
        this.stepMedia = stepMedia;
        this.media = media;
        this.storage = storage;
        this.db = db;
        this.audit = audit;
        this.mapper = mapper;
    }

    @Transactional
    public AcademicDtos.QuestionVersionDetail updateQuestionVersion(UUID qid, UUID versionId,
                                                                    AuthoringDtos.VersionEditRequest r, UUID actor) {
        QuestionVersion v = qv(qid, versionId);
        mutable(v);
        if (!Objects.equals(v.lockVersion, r.expectedLockVersion()))
            throw conflict("Draft changed by another user. Reload before saving.");
        v.languageCode = normalizeLanguage(r.languageCode());
        v.stem = r.stem().trim();
        v.marks = r.marks();
        try {
            v.metadata = mapper.writeValueAsString(Map.of("finalAnswer", r.finalAnswer() == null ? "" : r.finalAnswer()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid final answer metadata");
        }
        try {
            qVersions.saveAndFlush(v);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw conflict("Draft changed by another user. Reload before saving.");
        }
        audit.record(actor, "CONTENT_EDIT", "QUESTION_VERSION", versionId,
                null, Map.of("questionId", qid, "lockVersion", v.lockVersion));
        return detail(v);
    }

    public AcademicDtos.QuestionDetail detailForAdmin(UUID questionId) {
        Question q = q(questionId);
        QuestionVersion v = q.currentVersionId == null ? null : qVersions.findById(q.currentVersionId).orElse(null);
        Solution s = solutions.findByQuestionId(q.getId()).orElse(null);
        SolutionVersion sv = (s == null || s.currentVersionId == null) ? null : sVersions.findById(s.currentVersionId).orElse(null);

        AcademicDtos.QuestionVersionDetail qvd = (v == null) ? null : detail(v);
        AcademicDtos.SolutionDetail sd = (s == null) ? null : new AcademicDtos.SolutionDetail(
                s.getId(), s.status, s.currentVersionId, sv == null ? null : solutionDetail(sv)
        );

        return new AcademicDtos.QuestionDetail(
                q.getId(), q.getChapter().getId(), q.getChapter().getCode(), null,
                q.questionType, q.difficulty, q.status, qvd, sd
        );
    }

    @Transactional
    public AcademicDtos.SolutionVersionDetail updateSolutionVersion(UUID qid, UUID versionId,
                                                                    AuthoringDtos.SolutionEditRequest r, UUID actor) {
        SolutionVersion v = solutionVersion(qid, versionId);
        mutable(v);
        if (!Objects.equals(v.lockVersion, r.expectedLockVersion()))
            throw conflict("Draft changed by another user. Reload before saving.");
        v.languageCode = normalizeLanguage(r.languageCode());
        v.summary = r.summary();
        List<SolutionStep> old = steps.findAll().stream()
                .filter(x -> x.getSolutionVersion() != null && Objects.equals(x.getSolutionVersion().getId(), v.getId()))
                .toList();
        Map<Integer, SolutionStep> byNo = new HashMap<>();
        for (SolutionStep st : old) byNo.put(st.stepNo, st);
        Set<Integer> retained = new HashSet<>();
        for (var sr : r.steps() == null ? List.<AcademicDtos.SolutionStepRequest>of() : r.steps()) {
            if (sr.body() == null || sr.body().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solution step body is required");
            SolutionStep st = byNo.get(sr.stepNo());
            if (st == null) {
                st = new SolutionStep();
                st.solutionVersion = v;
            }
            st.stepNo = sr.stepNo();
            st.title = sr.title();
            st.body = sr.body().trim();
            steps.save(st);
            retained.add(sr.stepNo());
        }
        for (SolutionStep st : old) {
            if (!retained.contains(st.stepNo)) {
                stepMedia.deleteAll(stepMedia.findBySolutionStepIdOrderByDisplayOrderAsc(st.id));
                steps.delete(st);
            }
        }
        try {
            sVersions.saveAndFlush(v);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw conflict("Draft changed by another user. Reload before saving.");
        }
        audit.record(actor, "CONTENT_EDIT", "SOLUTION_VERSION", versionId, null, Map.of("questionId", qid, "lockVersion", v.lockVersion));
        return solutionDetail(v);
    }

    public AuthoringDtos.HistoryResponse history(UUID qid) {
        Question q = q(qid);
        List<AuthoringDtos.VersionHistoryItem> versions = qVersions.findAll().stream()
                .filter(v -> v.getQuestion() != null && Objects.equals(v.getQuestion().getId(), q.getId()))
                .sorted(Comparator.comparingInt(QuestionVersion::getVersionNo).reversed())
                .map(this::versionItem)
                .toList();

        List<AuthoringDtos.ReviewEventDto> events = db.query(
                "select id,question_id,actor_user_id,from_status,to_status,note,created_at from content_review_event where question_id=? order by created_at desc",
                (rs, n) -> new AuthoringDtos.ReviewEventDto(rs.getObject("id", UUID.class), rs.getObject("question_id", UUID.class),
                        rs.getObject("actor_user_id", UUID.class), status(rs.getString("from_status")), status(rs.getString("to_status")),
                        rs.getString("note"), rs.getObject("created_at", OffsetDateTime.class)), qid);
        List<AuthoringDtos.ReviewCommentDto> comments = db.query(
                "select id,question_id,version_id,actor_user_id,body,created_at from content_review_comment where question_id=? order by created_at asc",
                (rs, n) -> new AuthoringDtos.ReviewCommentDto(rs.getObject("id", UUID.class), rs.getObject("question_id", UUID.class),
                        rs.getObject("version_id", UUID.class), rs.getObject("actor_user_id", UUID.class),
                        rs.getString("body"), rs.getObject("created_at", OffsetDateTime.class)), qid);
        return new AuthoringDtos.HistoryResponse(qid, q.status, q.currentVersionId, q.publishedVersionId, versions, events, comments);
    }

    @Transactional
    public AuthoringDtos.ReviewCommentDto comment(UUID qid, AuthoringDtos.CommentRequest r, UUID actor) {
        q(qid);
        if (r.versionId() != null) qv(qid, r.versionId());
        UUID id = UUID.randomUUID();
        db.update("insert into content_review_comment(id,question_id,version_id,actor_user_id,body) values(?,?,?,?,?)",
                id, qid, r.versionId(), actor, r.body().trim());
        audit.record(actor, "CONTENT_COMMENT", "QUESTION", qid, null, Map.of("versionId", r.versionId() == null ? "" : r.versionId().toString()));
        return db.queryForObject("select id,question_id,version_id,actor_user_id,body,created_at from content_review_comment where id=?",
                (rs, n) -> new AuthoringDtos.ReviewCommentDto(rs.getObject("id", UUID.class), rs.getObject("question_id", UUID.class),
                        rs.getObject("version_id", UUID.class), rs.getObject("actor_user_id", UUID.class), rs.getString("body"),
                        rs.getObject("created_at", OffsetDateTime.class)), id);
    }

    public AuthoringDtos.DiffResponse diff(UUID qid, UUID fromId, UUID toId) {
        QuestionVersion a = qv(qid, fromId), b = qv(qid, toId);
        return new AuthoringDtos.DiffResponse(qid, versionItem(a), versionItem(b),
                !Objects.equals(a.stem, b.stem), !Objects.equals(a.marks, b.marks));
    }

    @Transactional
    public AcademicDtos.QuestionDetail transition(UUID qid, AuthoringDtos.ReviewActionRequest r, UUID actor) {
        Question q = q(qid);
        ContentStatus from = q.status, to = r.targetStatus();
        validateTransition(from, to);
        if (to == ContentStatus.PUBLISHED) validatePublish(q);
        if (to == ContentStatus.PUBLISHED) q.publishedVersionId = q.currentVersionId;
        q.status = to;
        questions.saveAndFlush(q);
        UUID event = UUID.randomUUID();
        db.update("insert into content_review_event(id,question_id,actor_user_id,from_status,to_status,note) values(?,?,?,?,?,?)",
                event, qid, actor, from.name(), to.name(), r.note());
        audit.record(actor, "CONTENT_" + to.name(), "QUESTION", qid,
                Map.of("status", from.name()), Map.of("status", to.name(), "note", r.note() == null ? "" : r.note()));
        QuestionVersion v = q.currentVersionId == null ? null : qVersions.findById(q.currentVersionId).orElse(null);
        return new AcademicDtos.QuestionDetail(q.getId(), q.getChapter().getId(), q.getChapter().getCode(), null, q.questionType, q.difficulty, q.status,
                v == null ? null : detail(v), null);
    }

    @Transactional
    public AcademicDtos.QuestionDetail rollback(UUID qid, AuthoringDtos.RollbackRequest r, UUID actor) {
        Question q = q(qid);
        QuestionVersion source = qv(qid, r.targetVersionId());
        int next = qVersions.countByQuestionIdAndLanguageCode(qid, source.languageCode) + 1;

        QuestionVersion v = new QuestionVersion();
        v.question = q;
        v.versionNo = next;
        v.languageCode = source.languageCode;
        v.stem = source.stem;
        v.marks = source.marks;
        v.metadata = source.metadata;
        v.createdBy = actor;
        v = qVersions.saveAndFlush(v);

        var sourceOptions = options.findAll().stream()
                .filter(x -> x.getQuestionVersion() != null && Objects.equals(x.getQuestionVersion().getId(), source.getId()))
                .toList();
        for (var o : sourceOptions) {
            McqOption n = new McqOption();
            n.questionVersion = v;
            n.optionKey = o.optionKey;
            n.optionText = o.optionText;
            n.correct = o.correct;
            n.displayOrder = o.displayOrder;
            options.save(n);
        }

        var sourceExplanations = explanations.findAll().stream()
                .filter(x -> x.getQuestionVersion() != null && Objects.equals(x.getQuestionVersion().getId(), source.getId()))
                .toList();
        for (var e : sourceExplanations) {
            McqExplanation n = new McqExplanation();
            n.questionVersion = v;
            n.languageCode = e.languageCode;
            n.explanation = e.explanation;
            explanations.save(n);
        }

        for (var a : qMedia.findByQuestionVersionIdOrderByDisplayOrderAsc(source.getId())) {
            if (a.getMediaAsset() == null) continue;
            MediaAsset asset = media.findById(a.getMediaAsset().getId()).orElseThrow();
            qMedia.save(new QuestionMediaAttachment(v, asset, a.displayOrder, a.attachmentRole));
        }

        q.currentVersionId = v.getId();
        q.status = ContentStatus.DRAFT;
        q = questions.saveAndFlush(q);

        audit.record(actor, "CONTENT_ROLLBACK", "QUESTION", qid,
                Map.of("fromVersionId", source.getId()), Map.of("newVersionId", v.getId()));

        return new AcademicDtos.QuestionDetail(
                q.getId(), q.getChapter().getId(), q.getChapter().getCode(), null,
                q.questionType, q.difficulty, q.status, detail(v), null
        );
    }


    private void validatePublish(Question q) {
        if (q.currentVersionId == null) throw conflict("Question needs a current version before publishing");
        QuestionVersion v = qVersions.findById(q.currentVersionId).orElseThrow();
        validateQuestionMedia(v.id);
        solutions.findByQuestionId(q.id).ifPresent(s -> {
            if (s.currentVersionId != null) validateSolutionMedia(s.currentVersionId);
        });
    }

    private void validateQuestionMedia(UUID versionId) {
        for (var a : qMedia.findByQuestionVersionIdOrderByDisplayOrderAsc(versionId)) {
            if (a.getMediaAsset() == null) continue;
            MediaAsset m = media.findById(a.getMediaAsset().getId())
                    .orElseThrow(() -> conflict("Referenced media asset is missing"));
            System.out.println("QMedia check -> id: " + m.getId()
                    + ", state: " + m.getLifecycleState()
                    + ", key: " + m.getStorageKey());
            if (m.getLifecycleState() != MediaEnums.LifecycleState.READY)
                throw conflict("Referenced media asset is not ready");
            try {
                boolean exists = storage.exists(m.getStorageKey());
                System.out.println("QMedia storage.exists(" + m.getStorageKey() + ") -> " + exists);
                if (!exists)
                    throw conflict("Referenced media object is not accessible");
            } catch (Exception e) {
                System.out.println("QMedia storage error -> " + e.getClass().getName() + ": " + e.getMessage());
                throw conflict("Referenced media object is not accessible");
            }
        }
    }

    private void validateSolutionMedia(UUID versionId) {
        var versionSteps = steps.findAll().stream()
                .filter(x -> x.getSolutionVersion() != null && Objects.equals(x.getSolutionVersion().getId(), versionId))
                .toList();

        for (var step : versionSteps) {
            for (var a : stepMedia.findBySolutionStepIdOrderByDisplayOrderAsc(step.getId())) {
                if (a.getMediaAsset() == null) continue;
                MediaAsset m = media.findById(a.getMediaAsset().getId())
                        .orElseThrow(() -> conflict("Referenced media asset is missing"));
                System.out.println("SMedia check -> step: " + step.getId()
                        + ", id: " + m.getId()
                        + ", state: " + m.getLifecycleState()
                        + ", key: " + m.getStorageKey());
                if (m.getLifecycleState() != MediaEnums.LifecycleState.READY)
                    throw conflict("Referenced media asset is not ready");
                try {
                    boolean exists = storage.exists(m.getStorageKey());
                    System.out.println("SMedia storage.exists(" + m.getStorageKey() + ") -> " + exists);
                    if (!exists)
                        throw conflict("Referenced media object is not accessible");
                } catch (Exception e) {
                    System.out.println("SMedia storage error -> " + e.getClass().getName() + ": " + e.getMessage());
                    throw conflict("Referenced media object is not accessible");
                }
            }
        }
    }


    private void validateTransition(ContentStatus from, ContentStatus to) {
        boolean ok = switch (from) {
            case DRAFT -> to == ContentStatus.SUBMITTED || to == ContentStatus.ARCHIVED;
            case SUBMITTED -> to == ContentStatus.CHANGES_REQUESTED || to == ContentStatus.APPROVED;
            case CHANGES_REQUESTED -> to == ContentStatus.SUBMITTED || to == ContentStatus.ARCHIVED;
            case APPROVED -> to == ContentStatus.PUBLISHED || to == ContentStatus.CHANGES_REQUESTED;
            case PUBLISHED -> to == ContentStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!ok) throw conflict("Invalid workflow transition: " + from + " -> " + to);
    }

    private QuestionVersion qv(UUID qid, UUID id) {
        QuestionVersion v = qVersions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question version not found"));
        if (v.getQuestion() == null || !Objects.equals(v.getQuestion().getId(), qid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to question");
        }
        return v;
    }

    private SolutionVersion solutionVersion(UUID qid, UUID id) {
        SolutionVersion v = sVersions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solution version not found"));
        if (v.getSolution() == null || v.getSolution().getQuestion() == null || !Objects.equals(v.getSolution().getQuestion().getId(), qid)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to question");
        }
        return v;
    }

    private Question q(UUID id) {
        return questions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    private void mutable(QuestionVersion v) {
        if (v.getQuestion() != null && Objects.equals(v.getQuestion().getPublishedVersionId(), v.getId())) {
            throw conflict("Published question version is immutable");
        }
    }

    private void mutable(SolutionVersion v) {
        if (v.getSolution() != null && Objects.equals(v.getSolution().getPublishedVersionId(), v.getId())) {
            throw conflict("Published solution version is immutable");
        }
    }

    private ResponseStatusException conflict(String m) {
        return new ResponseStatusException(HttpStatus.CONFLICT, m);
    }

    private String normalizeLanguage(String s) {
        return "en".equalsIgnoreCase(s) ? "en" : "bn";
    }

    private ContentStatus status(String s) {
        return s == null ? null : ContentStatus.valueOf(s);
    }

    private AuthoringDtos.VersionHistoryItem versionItem(QuestionVersion v) {
        return new AuthoringDtos.VersionHistoryItem(v.id, v.versionNo, v.languageCode, v.stem, v.marks, v.lockVersion);
    }

    private AcademicDtos.QuestionVersionDetail detail(QuestionVersion v) {
        var os = options.findAll().stream()
                .filter(x -> x.getQuestionVersion() != null && Objects.equals(x.getQuestionVersion().getId(), v.getId()))
                .sorted(Comparator.comparingInt(x -> x.displayOrder))
                .map(x -> new AcademicDtos.McqOptionDto(x.getId(), x.optionKey, x.optionText, x.correct, x.displayOrder))
                .toList();

        var es = explanations.findAll().stream()
                .filter(x -> x.getQuestionVersion() != null && Objects.equals(x.getQuestionVersion().getId(), v.getId()))
                .map(x -> new AcademicDtos.McqExplanationDto(x.getId(), x.languageCode, x.explanation))
                .toList();

        var ms = qMedia.findByQuestionVersionIdOrderByDisplayOrderAsc(v.getId()).stream()
                .map(a -> a.getMediaAsset() == null ? null : media.findById(a.getMediaAsset().getId())
                        .map(m -> new MediaDtos.MediaAssetResponse(
                                m.getId(), a.attachmentRole, a.displayOrder, m.caption, m.altText,
                                m.width == null ? 0 : m.width, m.height == null ? 0 : m.height, m.byteSize, m.mimeType,
                                m.checksumSha256,
                                m.getLifecycleState() != null ? m.getLifecycleState().name() : "PENDING",
                                "/api/v1/media/" + m.getId(),
                                media.questionUsage(m.getId()) + media.stepUsage(m.getId())))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();

        return new AcademicDtos.QuestionVersionDetail(v.getId(), v.versionNo, v.languageCode, v.stem, v.marks, v.lockVersion, os, es, ms);
    }


    private AcademicDtos.SolutionVersionDetail solutionDetail(SolutionVersion v) {
        var ss = steps.findAll().stream()
                .filter(x -> x.getSolutionVersion() != null && Objects.equals(x.getSolutionVersion().getId(), v.getId()))
                .sorted(Comparator.comparingInt(x -> x.stepNo))
                .map(x -> new AcademicDtos.SolutionStepDto(x.getId(), x.stepNo, x.title, x.body, List.of()))
                .toList();

        String finalAnswer = extractFinalAnswer(v.metadata);

        return new AcademicDtos.SolutionVersionDetail(
                v.getId(),
                v.versionNo,
                v.languageCode,
                v.summary,
                finalAnswer,
                v.lockVersion,
                ss
        );
    }

    private String extractFinalAnswer(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            var node = mapper.readTree(metadataJson);
            if (node != null && node.hasNonNull("finalAnswer")) {
                String value = node.get("finalAnswer").asText().trim();
                return value.isEmpty() ? null : value;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

}
