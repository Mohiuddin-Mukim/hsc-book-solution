package bd.hscsolution.catalog.service;

import bd.hscsolution.catalog.dto.AcademicDtos.*;
import bd.hscsolution.catalog.dto.PublicCatalogDtos;
import bd.hscsolution.catalog.entity.*;
import bd.hscsolution.catalog.repository.*;
import bd.hscsolution.security.AuditService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

@Service
public class AcademicCatalogService {
    private final SubjectRepository subjects;
    private final PaperRepository papers;
    private final BookRepository books;
    private final BookEditionRepository editions;
    private final ChapterRepository chapters;
    private final TopicRepository topics;
    private final QuestionRepository questions;
    private final QuestionVersionRepository qVersions;
    private final SolutionRepository solutions;
    private final SolutionVersionRepository sVersions;
    private final McqOptionRepository options;
    private final McqExplanationRepository explanations;
    private final SolutionStepRepository steps;
    private final AuditService audit;
    private final bd.hscsolution.catalog.repository.TagRepository tags;
    private final bd.hscsolution.catalog.repository.QuestionTagRepository questionTags;
    private final bd.hscsolution.media.QuestionMediaAttachmentRepository questionMedia;
    private final bd.hscsolution.media.SolutionStepMediaAttachmentRepository stepMedia;
    private final bd.hscsolution.media.MediaAssetRepository mediaAssets;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AcademicCatalogService(SubjectRepository subjects, PaperRepository papers, BookRepository books, BookEditionRepository editions, ChapterRepository chapters, TopicRepository topics, QuestionRepository questions, QuestionVersionRepository qVersions, SolutionRepository solutions, SolutionVersionRepository sVersions, McqOptionRepository options, McqExplanationRepository explanations, SolutionStepRepository steps, AuditService audit, bd.hscsolution.catalog.repository.TagRepository tags, bd.hscsolution.catalog.repository.QuestionTagRepository questionTags,
                                  bd.hscsolution.media.QuestionMediaAttachmentRepository questionMedia,
                                  bd.hscsolution.media.SolutionStepMediaAttachmentRepository stepMedia,
                                  bd.hscsolution.media.MediaAssetRepository mediaAssets) {
        this.subjects = subjects;
        this.papers = papers;
        this.books = books;
        this.editions = editions;
        this.chapters = chapters;
        this.topics = topics;
        this.questions = questions;
        this.qVersions = qVersions;
        this.solutions = solutions;
        this.sVersions = sVersions;
        this.options = options;
        this.explanations = explanations;
        this.steps = steps;
        this.audit = audit;
        this.tags = tags;
        this.questionTags = questionTags;
        this.questionMedia = questionMedia;
        this.stepMedia = stepMedia;
        this.mediaAssets = mediaAssets;
    }

    public Page<TagResponse> tags(int page, int size) {
        return tags.findAll(PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("code"))).map(t -> new TagResponse(t.id, t.code, t.label));
    }

    @Transactional
    public TagResponse createTag(TagRequest r, UUID actor) {
        Tag t = new Tag();
        t.code = norm(r.code());
        t.label = r.label().trim();
        tags.save(t);
        audit.record(actor, "CONTENT_CREATE", "TAG", t.id, null, Map.of("code", t.code));
        return new TagResponse(t.id, t.code, t.label);
    }

    @Transactional
    public void attachTag(UUID qid, UUID versionId, UUID tagId, UUID actor) {
        QuestionVersion v = or404(qVersions.findById(versionId), "Question version");
        if (v.getQuestion() == null || !java.util.Objects.equals(v.getQuestion().getId(), qid))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to question");
        Tag t = or404(tags.findById(tagId), "Tag");
        questionTags.save(new QuestionTag(new QuestionTag.Key(v.id, t.id)));
        audit.record(actor, "CONTENT_EDIT", "QUESTION_TAG", v.id, null, Map.of("tagId", tagId));
    }

    private <T> T or404(Optional<T> x, String what) {
        return x.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, what + " not found"));
    }

    public Page<SubjectResponse> subjects(int page, int size) {
        return subjects.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder").ascending())).map(s -> new SubjectResponse(s.id, s.code, s.name, s.displayOrder, s.active));
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest r, UUID actor) {
        Subject s = new Subject();
        s.code = norm(r.code());
        s.name = r.name().trim();
        s.displayOrder = r.displayOrder();
        s.active = r.active();
        subjects.save(s);
        audit.record(actor, "CONTENT_CREATE", "SUBJECT", s.id, null, Map.of("code", s.code));
        return new SubjectResponse(s.id, s.code, s.name, s.displayOrder, s.active);
    }

    @Transactional
    public SubjectResponse updateSubject(UUID id, SubjectRequest r, UUID actor) {
        Subject s = or404(subjects.findById(id), "Subject");
        Map<String, Object> before = Map.of("code", s.code, "name", s.name, "active", s.active);
        s.code = norm(r.code());
        s.name = r.name().trim();
        s.displayOrder = r.displayOrder();
        s.active = r.active();
        subjects.save(s);
        audit.record(actor, "CONTENT_EDIT", "SUBJECT", id, before, Map.of("code", s.code, "name", s.name, "active", s.active));
        return new SubjectResponse(s.id, s.code, s.name, s.displayOrder, s.active);
    }

    public Page<PaperResponse> papers(UUID subjectId, int page, int size) {
        return papers.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder").ascending()))
                .map(p -> new PaperResponse(p.getId(), p.getSubject().getId(), p.getSubject().getCode(), p.code, p.name, p.paperNumber, p.displayOrder));
    }


    @Transactional
    public PaperResponse createPaper(PaperRequest r, UUID actor) {
        Paper p = new Paper();
        p.subject = or404(subjects.findById(r.subjectId()), "Subject");
        p.code = norm(r.code());
        p.name = r.name().trim();
        p.paperNumber = r.paperNumber();
        p.displayOrder = r.displayOrder();
        p = papers.saveAndFlush(p);
        audit.record(actor, "CONTENT_CREATE", "PAPER", p.getId(), null, Map.of("subjectId", p.getSubject().getId(), "code", p.code));
        return to(p);
    }

    public Page<BookResponse> books(UUID subjectId, int page, int size) {
        Page<Book> p = subjectId == null ? books.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("title"))) : books.findAll(PageRequest.of(page, Math.min(size, 100)));
        return p.map(b -> new BookResponse(b.getId(), b.getSubject().getId(), b.title, b.publisher, b.isbn));
    }

    @Transactional
    public BookResponse createBook(BookRequest r, UUID actor) {
        Book b = new Book();
        b.subject = or404(subjects.findById(r.subjectId()), "Subject");
        b.title = r.title().trim();
        b.publisher = r.publisher();
        b.isbn = r.isbn();
        b = books.saveAndFlush(b);
        audit.record(actor, "CONTENT_CREATE", "BOOK", b.getId(), null, Map.of("title", b.title));
        return new BookResponse(b.getId(), b.getSubject().getId(), b.title, b.publisher, b.isbn);
    }

    @Transactional
    public EditionResponse createEdition(UUID bookId, EditionRequest r, UUID actor) {
        BookEdition e = new BookEdition();
        e.book = or404(books.findById(bookId), "Book");
        e.editionLabel = r.editionLabel().trim();
        e.publicationYear = r.publicationYear();
        e = editions.saveAndFlush(e);
        return new EditionResponse(e.getId(), e.getBook().getId(), e.editionLabel, e.publicationYear);
    }

    public Page<ChapterResponse> chapters(UUID paperId, int page, int size) {
        Page<Chapter> p = chapters.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder")));
        return p.map(c -> new ChapterResponse(c.getId(), c.getPaper().getId(), c.getPaper().getCode(), c.code, c.title, c.displayOrder));
    }

    @Transactional
    public ChapterResponse createChapter(ChapterRequest r, UUID actor) {
        Chapter c = new Chapter();
        c.paper = or404(papers.findById(r.paperId()), "Paper");
        c.code = norm(r.code());
        c.title = r.title().trim();
        c.displayOrder = r.displayOrder();
        c = chapters.saveAndFlush(c);
        audit.record(actor, "CONTENT_CREATE", "CHAPTER", c.getId(), null, Map.of("paperId", c.getPaper().getId(), "code", c.code));
        return new ChapterResponse(c.getId(), c.getPaper().getId(), c.getPaper().getCode(), c.code, c.title, c.displayOrder);
    }

    public Page<TopicResponse> topics(UUID chapterId, int page, int size) {
        Page<Topic> p = topics.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder")));
        return p.map(t -> new TopicResponse(t.getId(), t.getChapter().getId(), t.code, t.title, t.displayOrder));
    }

    @Transactional
    public TopicResponse createTopic(TopicRequest r, UUID actor) {
        Topic t = new Topic();
        t.chapter = or404(chapters.findById(r.chapterId()), "Chapter");
        t.code = norm(r.code());
        t.title = r.title().trim();
        t.displayOrder = r.displayOrder();
        t = topics.saveAndFlush(t);
        return new TopicResponse(t.getId(), t.getChapter().getId(), t.code, t.title, t.displayOrder);
    }

    @Transactional
    public QuestionDetail createQuestion(QuestionCreateRequest r, UUID actor) {
        Chapter c = or404(chapters.findById(r.chapterId()), "Chapter");

        Question q = new Question();
        q.chapter = c;
        q.questionType = r.questionType();
        q.difficulty = r.difficulty();
        q.createdBy = actor;
        q.status = ContentStatus.DRAFT;
        q = questions.saveAndFlush(q); // রিটার্ন ভ্যালু q-তে রাখুন

        QuestionVersion v = new QuestionVersion();
        v.question = q;
        v.versionNo = 1;
        v.languageCode = lang(r.languageCode());
        v.stem = r.stem().trim();
        v.marks = r.marks();
        v.createdBy = actor;
        v = qVersions.saveAndFlush(v); // রিটার্ন ভ্যালু v-তে রাখুন

        q.currentVersionId = v.getId(); // সরাসরি ফিল্ডের বদলে গেটার ব্যবহার করুন
        q = questions.saveAndFlush(q);

        audit.record(actor, "CONTENT_CREATE", "QUESTION", q.id, null, Map.of("versionId", v.getId()));

        return detail(q, v);
    }


    @Transactional
    public QuestionVersionDetail addQuestionVersion(UUID qid, QuestionVersionRequest r, UUID actor) {
        Question q = or404(questions.findById(qid), "Question");
        int next = qVersions.countByQuestionIdAndLanguageCode(qid, lang(r.languageCode())) + 1;
        QuestionVersion v = new QuestionVersion();
        v.question = q;
        v.versionNo = next;
        v.languageCode = lang(r.languageCode());
        v.stem = r.stem().trim();
        v.marks = r.marks();
        v.createdBy = actor;
        v = qVersions.saveAndFlush(v);

        q.currentVersionId = v.getId();
        if (q.status == ContentStatus.PUBLISHED) {
            q.status = ContentStatus.DRAFT;
        }
        questions.saveAndFlush(q);
        audit.record(actor, "CONTENT_EDIT", "QUESTION_VERSION", v.getId(), null, Map.of("questionId", qid, "versionNo", next, "language", v.languageCode));
        return versionDetail(v);
    }

    @Transactional
    public SolutionVersionDetail addSolutionVersion(UUID qid, SolutionVersionRequest r, UUID actor) {
        Question q = or404(questions.findById(qid), "Question");

        Solution s = solutions.findByQuestionId(qid).orElseGet(() -> {
            Solution x = new Solution();
            x.question = q;
            x.createdBy = actor;
            return solutions.saveAndFlush(x); // save এর বদলে saveAndFlush
        });

        int next = sVersions.countBySolutionIdAndLanguageCode(s.getId(), lang(r.languageCode())) + 1;

        SolutionVersion v = new SolutionVersion();
        v.solution = s;
        v.versionNo = next;
        v.languageCode = lang(r.languageCode());
        v.summary = r.summary();
        v.createdBy = actor;
        v = sVersions.saveAndFlush(v); // রিটার্ন করা ম্যানেজড অবজেক্টটি v-তে রাখুন

        for (SolutionStepRequest sr : r.steps() == null ? List.<SolutionStepRequest>of() : r.steps()) {
            SolutionStep st = new SolutionStep();
            st.solutionVersion = v;
            st.stepNo = sr.stepNo();
            st.title = sr.title();
            st.body = sr.body().trim();
            steps.save(st);
        }

        if (s.status != ContentStatus.PUBLISHED) {
            s.currentVersionId = v.getId();
            solutions.saveAndFlush(s);
        }

        audit.record(actor, "CONTENT_EDIT", "SOLUTION_VERSION", v.getId(), null,
                Map.of("questionId", qid, "versionNo", next, "language", v.languageCode));

        return solutionVersionDetail(v);
    }

    @Transactional
    public QuestionVersionDetail addMcqOptions(UUID qid, UUID versionId, List<McqOptionRequest> rs, UUID actor) {
        QuestionVersion v = or404(qVersions.findById(versionId), "Question version");
        if (v.getQuestion() == null || !java.util.Objects.equals(v.getQuestion().getId(), qid))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to question");
        for (McqOptionRequest r : rs) {
            McqOption o = new McqOption();
            o.questionVersion = v;
            o.optionKey = r.optionKey().trim().toUpperCase(Locale.ROOT);
            o.optionText = r.optionText().trim();
            o.correct = r.correct();
            o.displayOrder = r.displayOrder();
            options.save(o);
        }
        return versionDetail(v);
    }

    @Transactional
    public QuestionVersionDetail addMcqExplanation(UUID qid, UUID versionId, McqExplanationRequest r, UUID actor) {
        QuestionVersion v = or404(qVersions.findById(versionId), "Question version");
        if (v.getQuestion() == null || !java.util.Objects.equals(v.getQuestion().getId(), qid))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version does not belong to question");
        McqExplanation e = new McqExplanation();
        e.questionVersion = v;
        e.languageCode = lang(r.languageCode());
        e.explanation = r.explanation().trim();
        explanations.save(e);
        return versionDetail(v);
    }

    @Transactional
    public QuestionDetail workflow(UUID qid, WorkflowRequest r, UUID actor) {
        Question q = or404(questions.findById(qid), "Question");
        transition(q.status, r.targetStatus());
        if (r.targetStatus() == ContentStatus.PUBLISHED) {
            if (q.currentVersionId == null)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Question needs a current version before publishing");
            q.publishedVersionId = q.currentVersionId;
            q.status = ContentStatus.PUBLISHED;
        } else q.status = r.targetStatus();
        questions.save(q);
        audit.record(actor, "CONTENT_" + r.targetStatus(), "QUESTION", qid, null, Map.of("status", q.status.name(), "note", r.note() == null ? "" : r.note()));
        QuestionVersion v = q.currentVersionId == null ? null : qVersions.findById(q.currentVersionId).orElse(null);
        return detail(q, v);
    }

    @Transactional
    public SolutionDetail solutionWorkflow(UUID qid, WorkflowRequest r, UUID actor) {
        Solution s = or404(solutions.findByQuestionId(qid), "Solution");
        transition(s.status, r.targetStatus());
        if (r.targetStatus() == ContentStatus.PUBLISHED && s.currentVersionId == null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solution needs a current version before publishing");
        if (r.targetStatus() == ContentStatus.PUBLISHED) s.publishedVersionId = s.currentVersionId;
        s.status = r.targetStatus();
        solutions.save(s);
        audit.record(actor, "CONTENT_" + r.targetStatus(), "SOLUTION", s.id, null, Map.of("status", s.status.name()));
        return solutionDetail(s, s.currentVersionId == null ? null : sVersions.findById(s.currentVersionId).orElse(null));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.SubjectItem> publicSubjects(int page, int size) {
        return subjects.findByActiveTrueOrderByDisplayOrderAsc(pageable(page, size))
                .map(s -> new PublicCatalogDtos.SubjectItem(s.getId(), s.code, s.name, s.displayOrder));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.PaperItem> publicPapers(UUID subjectId, int page, int size) {
        if (!subjects.existsById(subjectId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found");
        return papers.findBySubjectIdOrderByDisplayOrderAsc(subjectId, pageable(page, size))
                .map(p -> new PublicCatalogDtos.PaperItem(
                        p.getId(), p.getSubject().getId(), p.getSubject().getCode(),
                        p.code, p.name, p.paperNumber, p.displayOrder));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.BookItem> publicBooks(UUID subjectId, int page, int size) {
        if (!subjects.existsById(subjectId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Subject not found");
        return books.findBySubjectIdOrderByTitleAsc(subjectId, pageable(page, size))
                .map(b -> new PublicCatalogDtos.BookItem(
                        b.getId(), b.getSubject().getId(), b.title, b.publisher, b.isbn));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.EditionItem> publicBookEditions(UUID bookId, int page, int size) {
        if (!books.existsById(bookId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found");
        return editions.findByBookIdOrderByPublicationYearDescEditionLabelAsc(bookId, pageable(page, size))
                .map(e -> new PublicCatalogDtos.EditionItem(
                        e.getId(), e.getBook().getId(), e.editionLabel, e.publicationYear));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.ChapterItem> publicChapters(UUID paperId, int page, int size) {
        if (!papers.existsById(paperId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found");
        return chapters.findByPaperIdOrderByDisplayOrderAsc(paperId, pageable(page, size))
                .map(c -> new PublicCatalogDtos.ChapterItem(
                        c.getId(), c.getPaper().getId(), c.getPaper().getCode(),
                        c.code, c.title, c.displayOrder));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.TopicItem> publicTopics(UUID chapterId, int page, int size) {
        if (!chapters.existsById(chapterId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        return topics.findByChapterIdOrderByDisplayOrderAsc(chapterId, pageable(page, size))
                .map(t -> new PublicCatalogDtos.TopicItem(
                        t.getId(), t.getChapter().getId(), t.code, t.title, t.displayOrder));
    }

    @Transactional(readOnly = true)
    public Page<PublicCatalogDtos.PublishedQuestionItem> publicQuestions(
            UUID chapterId, QuestionType type, Difficulty difficulty,
            java.math.BigDecimal minMarks, java.math.BigDecimal maxMarks, int page, int size) {
        if (!chapters.existsById(chapterId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter not found");
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").ascending());
        return questions.searchPublished(chapterId, ContentStatus.PUBLISHED, type, difficulty, minMarks, maxMarks, pageable)
                .map(q -> {
                    QuestionVersion pv = q.publishedVersionId == null
                            ? null : qVersions.findById(q.publishedVersionId).orElse(null);
                    return new PublicCatalogDtos.PublishedQuestionItem(
                            q.getId(), q.getChapter().getId(), q.getChapter().getCode(),
                            q.questionType, q.difficulty,
                            pv == null ? null : pv.marks,
                            q.publishedVersionId);
                });
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    }

    public Page<QuestionSummary> questions(UUID chapterId, boolean publishedOnly, QuestionType type, Difficulty difficulty, java.math.BigDecimal minMarks, java.math.BigDecimal maxMarks, int page, int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").ascending());
        return questions.search(chapterId, publishedOnly, ContentStatus.PUBLISHED, type, difficulty, minMarks, maxMarks, p)
                .map(q -> new QuestionSummary(q.getId(), q.getChapter().getId(), q.getChapter().getCode(), q.questionType, q.difficulty, q.status, q.currentVersionId));
    }

    @Transactional(readOnly = true)
    public QuestionDetail publicDetail(UUID id, String language) {
        Question q = or404(questions.findById(id), "Question");
        if (q.status != ContentStatus.PUBLISHED)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published question not found");
        QuestionVersion v = q.publishedVersionId == null ? null : qVersions.findById(q.publishedVersionId).orElse(null);
        String targetLang = lang(language);
//        if (v != null && !targetLang.equalsIgnoreCase(v.languageCode)) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
//                    "Published language version not found for question: " + q.getId() + ", expected: " + targetLang + ", found: " + v.languageCode);
//        }
        if (v != null && !targetLang.equalsIgnoreCase(v.getLanguageCode()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published language version not found");
        return detail(q, v);
    }

    private QuestionDetail detail(Question q, QuestionVersion v) {
        return new QuestionDetail(
                q.getId(), q.getChapter().getId(), q.getChapter().getCode(), null,
                q.questionType, q.difficulty, q.status,
                v == null ? null : versionDetail(v),
                q.currentVersionId == null ? null : solutions.findByQuestionId(q.getId()).map(s -> {
                    UUID versionId = q.status == ContentStatus.PUBLISHED && s.publishedVersionId != null
                            ? s.publishedVersionId : s.currentVersionId;
                    return solutionDetail(s, versionId == null ? null : sVersions.findById(versionId).orElse(null));
                }).orElse(null)
        );
    }

    private QuestionVersionDetail versionDetail(QuestionVersion v) {
        List<McqOptionDto> os = options.findAll().stream()
                .filter(x -> x.getQuestionVersion() != null && Objects.equals(x.getQuestionVersion().getId(), v.getId()))
                .sorted(Comparator.comparingInt(x -> x.displayOrder))
                .map(x -> new McqOptionDto(x.getId(), x.optionKey, x.optionText, x.correct, x.displayOrder))
                .toList();

        List<McqExplanationDto> es = explanations.findAll().stream()
                .filter(x -> x.getQuestionVersion() != null && Objects.equals(x.getQuestionVersion().getId(), v.getId()))
                .map(x -> new McqExplanationDto(x.getId(), x.languageCode, x.explanation))
                .toList();

        var media = questionMedia.findByQuestionVersionIdOrderByDisplayOrderAsc(v.getId()).stream()
                .map(a -> a.getMediaAsset() == null ? null : mediaAssets.findById(a.getMediaAsset().getId()).map(x -> new bd.hscsolution.media.MediaDtos.MediaAssetResponse(
                        x.getId(), a.attachmentRole, a.displayOrder, x.caption, x.altText,
                        x.getWidth() == null ? 0 : x.getWidth(), x.getHeight() == null ? 0 : x.getHeight(),
                        x.getByteSize(), x.getMimeType(), x.getChecksumSha256(),
                        x.getLifecycleState() != null ? x.getLifecycleState().name() : "PENDING",
                        ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/media/").path(x.getId().toString()).toUriString(),
                        mediaAssets.questionUsage(x.getId()) + mediaAssets.stepUsage(x.getId()))).orElse(null))
                .filter(Objects::nonNull).toList();

        return new QuestionVersionDetail(v.getId(), v.versionNo, v.languageCode, v.stem, v.marks, v.lockVersion, os, es, media);
    }


    private SolutionDetail solutionDetail(Solution s, SolutionVersion v) {
        return new SolutionDetail(
                s.getId(),
                s.status,
                s.currentVersionId,
                v == null ? null : solutionVersionDetail(v)
        );
    }

    private SolutionVersionDetail solutionVersionDetail(SolutionVersion v) {
        List<SolutionStepDto> ss = steps.findAll().stream()
                .filter(x -> x.getSolutionVersion() != null && Objects.equals(x.getSolutionVersion().getId(), v.getId()))
                .sorted(Comparator.comparingInt(x -> x.stepNo))
                .map(x -> {
                    var media = stepMedia.findBySolutionStepIdOrderByDisplayOrderAsc(x.getId()).stream()
                            .map(a -> a.getMediaAsset() == null ? null : mediaAssets.findById(a.getMediaAsset().getId())
                                    .map(m -> new bd.hscsolution.media.MediaDtos.MediaAssetResponse(
                                            m.getId(),
                                            a.attachmentRole,
                                            a.displayOrder,
                                            m.caption,
                                            m.altText,
                                            m.getWidth() == null ? 0 : m.getWidth(),
                                            m.getHeight() == null ? 0 : m.getHeight(),
                                            m.getByteSize(),
                                            m.getMimeType(),
                                            m.getChecksumSha256(),
                                            m.lifecycleState != null ? m.lifecycleState.name() : null, // <-- এখানে সমাধান করা হয়েছে
                                            "/api/v1/media/" + m.getId(),
                                            mediaAssets.questionUsage(m.getId()) + mediaAssets.stepUsage(m.getId())))
                                    .orElse(null))
                            .filter(Objects::nonNull)
                            .toList();
                    return new SolutionStepDto(x.getId(), x.stepNo, x.title, x.body, media);
                })
                .toList();
        return new SolutionVersionDetail(v.getId(), v.versionNo, v.languageCode, v.summary, finalAnswer(v.metadata), v.lockVersion, ss);
    }


    private String finalAnswer(String metadata) {
        if (metadata == null || metadata.isBlank()) return null;
        try {
            var node = objectMapper.readTree(metadata);
            var answer = node.get("finalAnswer");
            return answer == null || answer.isNull() ? null : answer.asText();
        } catch (Exception ignored) {
            return null;
        }
    }


    private void transition(ContentStatus from, ContentStatus to) {
        boolean ok = switch (from) {
            case DRAFT -> to == ContentStatus.SUBMITTED || to == ContentStatus.ARCHIVED;
            case SUBMITTED -> to == ContentStatus.CHANGES_REQUESTED || to == ContentStatus.APPROVED;
            case CHANGES_REQUESTED -> to == ContentStatus.SUBMITTED || to == ContentStatus.ARCHIVED;
            case APPROVED -> to == ContentStatus.PUBLISHED || to == ContentStatus.CHANGES_REQUESTED;
            case PUBLISHED -> to == ContentStatus.ARCHIVED;
            case ARCHIVED -> false;
        };
        if (!ok)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid workflow transition: " + from + " -> " + to);
    }

    private String norm(String s) {
        if (s == null || s.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code/name is required");
        return s.trim().toUpperCase(Locale.ROOT);
    }

    private String lang(String s) {
        return "en".equalsIgnoreCase(s) ? "en" : "bn";
    }

    private PaperResponse to(Paper p) {
        return new PaperResponse(p.getId(), p.getSubject().getId(), p.getSubject().getCode(), p.code, p.name, p.paperNumber, p.displayOrder);
    }
}