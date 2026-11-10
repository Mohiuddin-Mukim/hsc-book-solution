package bd.hscsolution.catalog.service;

import bd.hscsolution.catalog.dto.AcademicDtos.*;
import bd.hscsolution.catalog.entity.*;
import bd.hscsolution.catalog.repository.*;
import bd.hscsolution.security.AuditService;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    public AcademicCatalogService(SubjectRepository subjects, PaperRepository papers, BookRepository books, BookEditionRepository editions, ChapterRepository chapters, TopicRepository topics, QuestionRepository questions, QuestionVersionRepository qVersions, SolutionRepository solutions, SolutionVersionRepository sVersions, McqOptionRepository options, McqExplanationRepository explanations, SolutionStepRepository steps, AuditService audit, bd.hscsolution.catalog.repository.TagRepository tags, bd.hscsolution.catalog.repository.QuestionTagRepository questionTags) {
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
        if (!v.question.id.equals(qid))
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
        return papers.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder").ascending())).map(p -> new PaperResponse(p.id, p.subject.id, p.subject.code, p.code, p.name, p.paperNumber, p.displayOrder));
    }

    @Transactional
    public PaperResponse createPaper(PaperRequest r, UUID actor) {
        Paper p = new Paper();
        p.subject = or404(subjects.findById(r.subjectId()), "Subject");
        p.code = norm(r.code());
        p.name = r.name().trim();
        p.paperNumber = r.paperNumber();
        p.displayOrder = r.displayOrder();
        papers.save(p);
        audit.record(actor, "CONTENT_CREATE", "PAPER", p.id, null, Map.of("subjectId", p.subject.id, "code", p.code));
        return to(p);
    }

    public Page<BookResponse> books(UUID subjectId, int page, int size) {
        Page<Book> p = subjectId == null ? books.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("title"))) : books.findAll(PageRequest.of(page, Math.min(size, 100)));
        return p.map(b -> new BookResponse(b.id, b.subject.id, b.title, b.publisher, b.isbn));
    }

    @Transactional
    public BookResponse createBook(BookRequest r, UUID actor) {
        Book b = new Book();
        b.subject = or404(subjects.findById(r.subjectId()), "Subject");
        b.title = r.title().trim();
        b.publisher = r.publisher();
        b.isbn = r.isbn();
        books.save(b);
        audit.record(actor, "CONTENT_CREATE", "BOOK", b.id, null, Map.of("title", b.title));
        return new BookResponse(b.id, b.subject.id, b.title, b.publisher, b.isbn);
    }

    @Transactional
    public EditionResponse createEdition(UUID bookId, EditionRequest r, UUID actor) {
        BookEdition e = new BookEdition();
        e.book = or404(books.findById(bookId), "Book");
        e.editionLabel = r.editionLabel().trim();
        e.publicationYear = r.publicationYear();
        editions.save(e);
        return new EditionResponse(e.id, e.book.id, e.editionLabel, e.publicationYear);
    }

    public Page<ChapterResponse> chapters(UUID paperId, int page, int size) {
        Page<Chapter> p = chapters.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder")));
        return p.map(c -> new ChapterResponse(c.id, c.paper.id, c.paper.code, c.code, c.title, c.displayOrder));
    }

    @Transactional
    public ChapterResponse createChapter(ChapterRequest r, UUID actor) {
        Chapter c = new Chapter();
        c.paper = or404(papers.findById(r.paperId()), "Paper");
        c.code = norm(r.code());
        c.title = r.title().trim();
        c.displayOrder = r.displayOrder();
        chapters.save(c);
        audit.record(actor, "CONTENT_CREATE", "CHAPTER", c.id, null, Map.of("paperId", c.paper.id, "code", c.code));
        return new ChapterResponse(c.id, c.paper.id, c.paper.code, c.code, c.title, c.displayOrder);
    }

    public Page<TopicResponse> topics(UUID chapterId, int page, int size) {
        Page<Topic> p = topics.findAll(PageRequest.of(page, Math.min(size, 100), Sort.by("displayOrder")));
        return p.map(t -> new TopicResponse(t.id, t.chapter.id, t.code, t.title, t.displayOrder));
    }

    @Transactional
    public TopicResponse createTopic(TopicRequest r, UUID actor) {
        Topic t = new Topic();
        t.chapter = or404(chapters.findById(r.chapterId()), "Chapter");
        t.code = norm(r.code());
        t.title = r.title().trim();
        t.displayOrder = r.displayOrder();
        topics.save(t);
        return new TopicResponse(t.id, t.chapter.id, t.code, t.title, t.displayOrder);
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
        questions.save(q);
        QuestionVersion v = new QuestionVersion();
        v.question = q;
        v.versionNo = 1;
        v.languageCode = lang(r.languageCode());
        v.stem = r.stem().trim();
        v.marks = r.marks();
        v.createdBy = actor;
        qVersions.save(v);
        q.currentVersionId = v.id;
        questions.save(q);
        audit.record(actor, "CONTENT_CREATE", "QUESTION", q.id, null, Map.of("versionId", v.id));
        return detail(q, v);
    }

    @Transactional
    public QuestionVersionDetail addQuestionVersion(UUID qid, QuestionVersionRequest r, UUID actor) {
        Question q = or404(questions.findById(qid), "Question");
        if (q.status == ContentStatus.PUBLISHED) {
        }
        int next = qVersions.countByQuestionIdAndLanguageCode(qid, lang(r.languageCode())) + 1;
        QuestionVersion v = new QuestionVersion();
        v.question = q;
        v.versionNo = next;
        v.languageCode = lang(r.languageCode());
        v.stem = r.stem().trim();
        v.marks = r.marks();
        v.createdBy = actor;
        qVersions.save(v);
        q.currentVersionId = v.id;
        if (q.status == ContentStatus.PUBLISHED) {
            q.status = ContentStatus.DRAFT;
        }
        questions.save(q);
        audit.record(actor, "CONTENT_EDIT", "QUESTION_VERSION", v.id, null, Map.of("questionId", qid, "versionNo", next, "language", v.languageCode));
        return versionDetail(v);
    }

    @Transactional
    public SolutionVersionDetail addSolutionVersion(UUID qid, SolutionVersionRequest r, UUID actor) {
        Question q = or404(questions.findById(qid), "Question");
        Solution s = solutions.findByQuestionId(qid).orElseGet(() -> {
            Solution x = new Solution();
            x.question = q;
            x.createdBy = actor;
            return solutions.save(x);
        });
        if (s.status == ContentStatus.PUBLISHED) {
        }
        int next = sVersions.countBySolutionIdAndLanguageCode(s.id, lang(r.languageCode())) + 1;
        SolutionVersion v = new SolutionVersion();
        v.solution = s;
        v.versionNo = next;
        v.languageCode = lang(r.languageCode());
        v.summary = r.summary();
        v.createdBy = actor;
        sVersions.save(v);
        for (SolutionStepRequest sr : r.steps() == null ? List.<SolutionStepRequest>of() : r.steps()) {
            SolutionStep st = new SolutionStep();
            st.solutionVersion = v;
            st.stepNo = sr.stepNo();
            st.title = sr.title();
            st.body = sr.body().trim();
            steps.save(st);
        }
        if (s.status != ContentStatus.PUBLISHED) {
            s.currentVersionId = v.id;
            solutions.save(s);
        }
        audit.record(actor, "CONTENT_EDIT", "SOLUTION_VERSION", v.id, null, Map.of("questionId", qid, "versionNo", next, "language", v.languageCode));
        return solutionVersionDetail(v);
    }

    @Transactional
    public QuestionVersionDetail addMcqOptions(UUID qid, UUID versionId, List<McqOptionRequest> rs, UUID actor) {
        QuestionVersion v = or404(qVersions.findById(versionId), "Question version");
        if (!v.question.id.equals(qid))
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
        if (!v.question.id.equals(qid))
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

    public Page<QuestionSummary> questions(UUID chapterId, boolean publishedOnly, QuestionType type, Difficulty difficulty, java.math.BigDecimal minMarks, java.math.BigDecimal maxMarks, int page, int size) {
        Pageable p = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("id").ascending());
        return questions.search(chapterId, publishedOnly, ContentStatus.PUBLISHED, type, difficulty, minMarks, maxMarks, p).map(q -> new QuestionSummary(q.id, q.chapter.id, q.chapter.code, q.questionType, q.difficulty, q.status, q.currentVersionId));
    }

    @Transactional(readOnly = true)
    public QuestionDetail publicDetail(UUID id, String language) {
        Question q = or404(questions.findById(id), "Question");
        if (q.status != ContentStatus.PUBLISHED)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Published question not found");
        QuestionVersion v = q.publishedVersionId == null ? null : qVersions.findById(q.publishedVersionId).orElse(null);
        if (v != null && !v.languageCode.equals(lang(language)))
            v = qVersions.findByQuestionIdAndLanguageCodeOrderByVersionNoDesc(q.id, lang(language)).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Published language version not found"));
        return detail(q, v);
    }

    private QuestionDetail detail(Question q, QuestionVersion v) {
        return new QuestionDetail(q.id, q.chapter.id, q.chapter.code, null, q.questionType, q.difficulty, q.status, v == null ? null : versionDetail(v), q.currentVersionId == null ? null : solutions.findByQuestionId(q.id).map(s -> solutionDetail(s, s.publishedVersionId == null ? null : sVersions.findById(s.publishedVersionId).orElse(null))).orElse(null));
    }

    private QuestionVersionDetail versionDetail(QuestionVersion v) {
        List<McqOptionDto> os = options.findAll().stream().filter(x -> x.questionVersion.id.equals(v.id)).sorted(Comparator.comparingInt(x -> x.displayOrder)).map(x -> new McqOptionDto(x.id, x.optionKey, x.optionText, x.correct, x.displayOrder)).toList();
        List<McqExplanationDto> es = explanations.findAll().stream().filter(x -> x.questionVersion.id.equals(v.id)).map(x -> new McqExplanationDto(x.id, x.languageCode, x.explanation)).toList();
        return new QuestionVersionDetail(v.id, v.versionNo, v.languageCode, v.stem, v.marks, os, es);
    }

    private SolutionDetail solutionDetail(Solution s, SolutionVersion v) {
        return new SolutionDetail(s.id, s.status, s.currentVersionId, v == null ? null : solutionVersionDetail(v));
    }

    private SolutionVersionDetail solutionVersionDetail(SolutionVersion v) {
        List<SolutionStepDto> ss = steps.findAll().stream().filter(x -> x.solutionVersion.id.equals(v.id)).sorted(Comparator.comparingInt(x -> x.stepNo)).map(x -> new SolutionStepDto(x.id, x.stepNo, x.title, x.body)).toList();
        return new SolutionVersionDetail(v.id, v.versionNo, v.languageCode, v.summary, ss);
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
        return new PaperResponse(p.id, p.subject.id, p.subject.code, p.code, p.name, p.paperNumber, p.displayOrder);
    }
}
