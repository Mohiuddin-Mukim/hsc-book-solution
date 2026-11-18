package bd.hscsolution.imports;

import bd.hscsolution.catalog.entity.*;
import bd.hscsolution.catalog.repository.*;
import bd.hscsolution.media.MediaDtos;
import bd.hscsolution.media.MediaService;
import bd.hscsolution.security.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static bd.hscsolution.catalog.entity.AcademicEnums.*;

@Service
public class BulkImportService {
    private final JdbcTemplate db;
    private final ObjectMapper mapper;
    private final ImportProperties props;
    private final ChapterRepository chapters;
    private final QuestionRepository questions;
    private final QuestionVersionRepository qVersions;
    private final SolutionRepository solutions;
    private final SolutionVersionRepository sVersions;
    private final SolutionStepRepository steps;
    private final TagRepository tags;
    private final QuestionTagRepository questionTags;
    private final McqOptionRepository options;
    private final McqExplanationRepository explanations;
    private final MediaService media;
    private final AuditService audit;
    private final TransactionTemplate tx;

    public BulkImportService(JdbcTemplate db, ObjectMapper mapper, ImportProperties props, ChapterRepository chapters,
                             QuestionRepository questions, QuestionVersionRepository qVersions, SolutionRepository solutions,
                             SolutionVersionRepository sVersions, SolutionStepRepository steps, TagRepository tags,
                             QuestionTagRepository questionTags, McqOptionRepository options, McqExplanationRepository explanations,
                             MediaService media, AuditService audit, org.springframework.transaction.PlatformTransactionManager txManager) {
        this.db = db;
        this.mapper = mapper;
        this.props = props;
        this.chapters = chapters;
        this.questions = questions;
        this.qVersions = qVersions;
        this.solutions = solutions;
        this.sVersions = sVersions;
        this.steps = steps;
        this.tags = tags;
        this.questionTags = questionTags;
        this.options = options;
        this.explanations = explanations;
        this.media = media;
        this.audit = audit;
        this.tx = new TransactionTemplate(txManager);
    }

    @Transactional
    public ImportDtos.ImportJobResponse stageBytes(byte[] bytes, String sourceType, String fileName, ImportDtos.ImportMode mode,
                                                   boolean dryRun, String idempotencyKey, UUID actor) {
        String hash = sha256(bytes);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ImportJobOld old = findJob(idempotencyKey);
            if (old != null) {
                if (!Objects.equals(hash, old.sourceHash))
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key was already used with different content");
                return job(old.id);
            }
        }
        if (bytes.length > props.maxFileBytes())
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Import file exceeds configured limit");
        UUID id = UUID.randomUUID();
        Path dir = Path.of(props.storageRoot()).toAbsolutePath().normalize().resolve(id.toString());
        try {
            Files.createDirectories(dir);
            Path source = dir.resolve("source.bin");
            Files.write(source, bytes, StandardOpenOption.CREATE_NEW);
            db.update("insert into import_job(id,source_type,file_name,status,requested_by,total_rows,success_rows,error_rows,idempotency_key,mode,dry_run,source_sha256,staging_path) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, sourceType, safeFileName(fileName), "STAGED", actor, 0, 0, 0, blankToNull(idempotencyKey), mode.name(), dryRun, hash, dir.toString());
            if ("ZIP".equals(sourceType)) extractZip(source, dir.resolve("pack"));
            List<Map<String, Object>> rows = parse(sourceType, source, dir.resolve("pack"));
            if (rows.size() > props.maxRows())
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Import row limit exceeded");
            stageRows(id, rows);
            db.update("update import_job set status='READY',total_rows=(select count(*) from import_row where import_job_id=?),updated_at=now() where id=?", id, id);
            audit.record(actor, "IMPORT_STAGED", "IMPORT_JOB", id, null, Map.of("sourceType", sourceType, "rows", rows.size(), "dryRun", dryRun));
            return job(id);
        } catch (IOException e) {
            db.update("update import_job set status='FAILED',updated_at=now() where id=?", id);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to stage import: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            db.update("update import_job set status='FAILED',updated_at=now() where id=?", id);
            throw e;
        }
    }

    @Transactional
    public ImportDtos.ImportJobResponse stageJsonPaste(ImportDtos.JsonPasteRequest req, UUID actor) {
        validateMode(req.mode());
        return stageBytes(req.content().getBytes(StandardCharsets.UTF_8), "JSON",
                req.fileName() == null ? "pasted-content.json" : req.fileName(), ImportDtos.ImportMode.valueOf(req.mode()),
                req.dryRun(), req.idempotencyKey(), actor);
    }

    public ImportDtos.ImportPreviewResponse preview(UUID jobId) {
        ImportDtos.ImportJobResponse j = job(jobId);
        List<ImportDtos.ImportRowResponse> rows = db.query("select id,row_no,external_id,status,question_id,payload from import_row where import_job_id=? order by row_no",
                (rs, n) -> {
                    UUID rid = rs.getObject("id", UUID.class);
                    List<ImportDtos.ImportErrorResponse> es = db.query("select field_name,error_code,severity,message from import_error where import_row_id=? order by id",
                            (er, en) -> new ImportDtos.ImportErrorResponse(er.getString(1), er.getString(2), er.getString(3), er.getString(4)), rid);
                    Object payload;
                    try {
                        payload = mapper.readTree(rs.getString("payload"));
                    } catch (Exception e) {
                        payload = rs.getString("payload");
                    }
                    return new ImportDtos.ImportRowResponse(rid, rs.getInt("row_no"), rs.getString("external_id"), rs.getString("status"),
                            rs.getObject("question_id", UUID.class), payload, es);
                }, jobId);
        int warnings = (int) rows.stream().flatMap(r -> r.errors().stream()).filter(e -> "WARNING".equals(e.severity())).count();
        return new ImportDtos.ImportPreviewResponse(j, rows, warnings);
    }

    @Transactional
    public ImportDtos.CommitResponse commit(UUID jobId, UUID actor, boolean submitForReview) {
        ImportDtos.ImportJobResponse j = job(jobId);
        if (j.dryRun()) return new ImportDtos.CommitResponse(j, 0, 0, 0, 0);
        if ("COMMITTED".equals(j.status()) && j.errorRows() == 0)
            return new ImportDtos.CommitResponse(j, j.successRows(), 0, 0, 0);
        db.update("update import_job set status='VALIDATING',updated_at=now() where id=?", jobId);
        int created = 0, versions = 0, skipped = 0, failed = 0;
        List<UUID> rows = db.query("select id from import_row where import_job_id=? and status in ('PENDING','ERROR') order by row_no",
                (rs, n) -> rs.getObject(1, UUID.class), jobId);
        for (UUID rowId : rows) {
            try {
                RowCommitResult r = tx.execute(status -> commitRow(rowId, jobId, actor, submitForReview));
                created += r.created ? 1 : 0;
                versions += r.versionCreated ? 1 : 0;
                skipped += r.skipped ? 1 : 0;
            } catch (Exception e) {
                failed++;
                recordRowFailure(rowId, e);
            }
        }
        String status = failed == 0 ? "COMMITTED" : (created + versions + skipped > 0 ? "PARTIAL" : "FAILED");
        db.update("update import_job set status=?,success_rows=?,error_rows=?,committed_at=case when ? in ('COMMITTED','PARTIAL') then now() else committed_at end,updated_at=now() where id=?",
                status, created + versions + skipped, failed, status, jobId);
        audit.record(actor, "IMPORT_COMMIT", "IMPORT_JOB", jobId, null, Map.of("created", created, "versions", versions, "skipped", skipped, "failed", failed));
        return new ImportDtos.CommitResponse(job(jobId), created, versions, skipped, failed);
    }

    public ImportDtos.CommitResponse retry(UUID jobId, UUID actor, boolean submitForReview) {
        return commit(jobId, actor, submitForReview);
    }

    public List<ImportDtos.ErrorReportRow> errors(UUID jobId) {
        return db.query("select r.row_no,r.external_id,e.field_name,e.error_code,e.severity,e.message from import_error e join import_row r on r.id=e.import_row_id where r.import_job_id=? order by r.row_no,e.id",
                (rs, n) -> new ImportDtos.ErrorReportRow(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)), jobId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RowCommitResult commitRow(UUID rowId, UUID jobId, UUID actor, boolean submitForReview) {
        Map<String, Object> row = db.queryForObject("select payload,external_id from import_row where id=?",
                (rs, n) -> {
                    try {
                        Map<String, Object> m = mapper.convertValue(mapper.readTree(rs.getString("payload")), new TypeReference<>() {
                        });
                        m.putIfAbsent("externalId", rs.getString("external_id"));
                        return m;
                    } catch (Exception e) {
                        throw new IllegalStateException("Stored payload is invalid JSON");
                    }
                }, rowId);
        String ext = required(row, "externalId");
        Optional<Question> existing = questions.findByExternalId(ext);
        ImportDtos.ImportMode mode = jobMode(jobId);
        if (existing.isPresent()) {
            if (mode == ImportDtos.ImportMode.CREATE_ONLY)
                throw rowError("externalId", "DUPLICATE_EXTERNAL_ID", "Question already exists for externalId " + ext);
            if (mode == ImportDtos.ImportMode.SKIP_DUPLICATES) {
                markRow(rowId, "SKIPPED", existing.get().getId());
                return new RowCommitResult(false, false, true);
            }
        }
        Question q = existing.orElseGet(Question::new);
        boolean created = existing.isEmpty(), versionCreated = !created;
        if (created) {
            q.externalId = ext;
            q.chapter = resolveChapter(row);
            q.questionType = enumValue(QuestionType.class, required(row, "questionType"));
            q.difficulty = optionalEnum(Difficulty.class, str(row.get("difficulty")));
            q.status = ContentStatus.DRAFT;
            q.createdBy = actor;
            q = questions.saveAndFlush(q);
        }
        QuestionVersion v = new QuestionVersion();
        v.question = q;
        v.versionNo = qVersions.countByQuestionIdAndLanguageCode(q.getId(), required(row, "languageCode")) + 1;
        v.languageCode = required(row, "languageCode");
        v.stem = required(row, "stem");
        v.marks = decimal(row.get("marks"));
        v.metadata = mapper.createObjectNode().put("finalAnswer", String.valueOf(row.getOrDefault("finalAnswer", ""))).toString();
        v.createdBy = actor;
        v = qVersions.saveAndFlush(v);
        q.currentVersionId = v.getId();
        q.status = submitForReview ? ContentStatus.SUBMITTED : ContentStatus.DRAFT;
        questions.saveAndFlush(q);
        addTags(v.getId(), row);
        addMcq(v.getId(), row);
        addSolution(q, row, jobId, actor, submitForReview);
        attachMedia(row, v.getId(), jobId, actor);
        if (submitForReview)
            db.update("insert into review_task(entity_type,entity_id,status,assigned_to,reviewer_note) values('QUESTION',?,'OPEN',null,null)", q.getId());
        markRow(rowId, "COMMITTED", q.getId());
        return new RowCommitResult(created, versionCreated, false);
    }

    private void addSolution(Question q, Map<String, Object> row, UUID jobId, UUID actor, boolean submitForReview) {
        Object so = row.get("solution");
        if (!(so instanceof Map<?, ?> sm)) return;

        Solution s = solutions.findByQuestionId(q.getId()).orElseGet(() -> {
            Solution x = new Solution();
            x.question = q;
            x.status = ContentStatus.DRAFT;
            x.createdBy = actor;
            return solutions.saveAndFlush(x);
        });

        String lang = String.valueOf(sm.get("languageCode") == null ? row.get("languageCode") : sm.get("languageCode"));
        SolutionVersion sv = new SolutionVersion();
        sv.solution = s;
        sv.versionNo = sVersions.countBySolutionIdAndLanguageCode(s.getId(), lang) + 1;
        sv.languageCode = lang;
        sv.summary = sm.get("summary") == null ? null : String.valueOf(sm.get("summary"));
        sv.createdBy = actor;

        // এটিকে একটি নতুন লোকাল ভেরিয়েবলে রাখতে পারেন অথবা সরাসরি ব্যবহার করতে পারেন,
        // তবে এর মান আর রি-অ্যাসাইন না করাই ভালো।
        SolutionVersion savedSv = sVersions.saveAndFlush(sv);

        s.currentVersionId = savedSv.getId();
        s.status = submitForReview ? ContentStatus.SUBMITTED : ContentStatus.DRAFT;
        solutions.saveAndFlush(s);

        Object stepsObj = sm.get("steps");
        if (stepsObj instanceof List<?> list) {
            // স্টেপ সেভ করার সাথে সাথে ম্যাপে সেভ করে রাখব, যাতে পরে আর stream() দিয়ে খুঁজতে না হয়
            Map<SolutionStep, Map<?, ?>> savedSteps = new LinkedHashMap<>();

            for (Object o : list) {
                if (o instanceof Map<?, ?> m) {
                    SolutionStep st = new SolutionStep();
                    st.solutionVersion = savedSv; // এখানে savedSv ব্যবহার করা হয়েছে
                    st.stepNo = Integer.parseInt(String.valueOf(m.get("stepNo")));
                    st.title = m.get("title") == null ? null : String.valueOf(m.get("title"));
                    st.body = required(m, "body");

                    SolutionStep savedStep = steps.save(st);
                    savedSteps.put(savedStep, m);
                }
            }
            steps.flush();

            // খুব সহজেই মিডিয়া এটাচ করে ফেলুন কোনো stream বা ল্যাম্বডা ঝামেলা ছাড়া!
            for (var entry : savedSteps.entrySet()) {
                SolutionStep step = entry.getKey();
                Map<?, ?> stepMap = entry.getValue();
                attachStepMedia(stepMap, step.getId(), jobId, actor);
            }
        }
    }

    private void attachMedia(Map<String, Object> row, UUID versionId, UUID jobId, UUID actor) {
        Object mo = row.get("media");
        if (!(mo instanceof List<?> list)) return;
        for (Object o : list) if (o instanceof Map<?, ?> m) attachOneMedia(m, versionId, null, jobId, actor);
    }

    private void attachStepMedia(Map<?, ?> step, UUID stepId, UUID jobId, UUID actor) {
        Object mo = step.get("media");
        if (!(mo instanceof List<?> list)) return;
        for (Object o : list) if (o instanceof Map<?, ?> m) attachOneMedia(m, null, stepId, jobId, actor);
    }

    private void attachOneMedia(Map<?, ?> m, UUID versionId, UUID stepId, UUID jobId, UUID actor) {
        String rel = required(m, "path");
        Path root = Path.of(jobDir(jobId)).toAbsolutePath().normalize();
        Path pack = root.resolve("pack").normalize();
        Path base = Files.isDirectory(pack) ? pack : root;
        Path p = base.resolve(rel).normalize();
        if (!p.startsWith(base) || !Files.isRegularFile(p))
            throw rowError("media.path", "MISSING_MEDIA", "Media file not found or unsafe: " + rel);
        try {
            byte[] bytes = Files.readAllBytes(p);
            MediaDtos.MediaAssetResponse a = media.upload(new BytesMultipartFile(p.getFileName().toString(), bytes), str(m.get("caption")), str(m.get("altText")), actor);
            MediaDtos.AttachRequest ar = new MediaDtos.AttachRequest(a.id(), str(m.get("role")), intOrNull(m.get("order")), str(m.get("caption")), str(m.get("altText")));
            if (versionId != null) media.attachQuestion(versionId, ar, actor);
            else media.attachStep(stepId, ar, actor);
        } catch (IOException e) {
            throw rowError("media.path", "MEDIA_READ_FAILED", e.getMessage());
        }
    }

    private void addMcq(UUID versionId, Map<String, Object> row) {
        if (!"MCQ".equalsIgnoreCase(String.valueOf(row.getOrDefault("questionType", "")))) return;
        Object oo = row.get("mcqOptions");
        if (!(oo instanceof List<?> list)) throw rowError("mcqOptions", "MCQ_OPTIONS_REQUIRED", "MCQ requires options");
        int correct = 0, order = 0;
        for (Object o : list)
            if (o instanceof Map<?, ?> m) {
                McqOption x = new McqOption();
                x.questionVersion = qVersions.getReferenceById(versionId);
                x.optionKey = required(m, "optionKey");
                x.optionText = required(m, "optionText");
                x.correct = Boolean.parseBoolean(String.valueOf(m.get("correct") == null ? false : m.get("correct")));
                x.displayOrder = intOrDefault(m.get("displayOrder"), ++order);
                if (x.correct) correct++;
                options.save(x);
            }
        if (correct != 1)
            throw rowError("mcqOptions", "MCQ_CORRECT_COUNT", "MCQ must contain exactly one correct option");
        if (row.get("mcqExplanation") != null) {
            McqExplanation e = new McqExplanation();
            e.questionVersion = qVersions.getReferenceById(versionId);
            e.languageCode = String.valueOf(row.getOrDefault("languageCode", "bn"));
            e.explanation = String.valueOf(row.get("mcqExplanation"));
            explanations.save(e);
        }
    }

    private void addTags(UUID versionId, Map<String, Object> row) {
        Object t = row.get("tags");
        if (!(t instanceof List<?> list)) return;
        for (Object o : list) {
            String code = String.valueOf(o);
            Tag tag = tags.findByCode(code).orElseGet(() -> {
                Tag x = new Tag();
                x.code = code;
                x.label = code;
                return tags.saveAndFlush(x);
            });
            questionTags.save(new QuestionTag(new QuestionTag.Key(versionId, tag.id)));
        }
    }

    private Chapter resolveChapter(Map<String, Object> row) {
        String id = str(row.get("chapterId"));
        if (id != null && !id.isBlank()) try {
            return chapters.findById(UUID.fromString(id)).orElseThrow(() -> rowError("chapterId", "MISSING_CHAPTER", "Chapter not found"));
        } catch (IllegalArgumentException e) {
            throw rowError("chapterId", "INVALID_UUID", "chapterId is not a UUID");
        }
        throw rowError("chapterId", "MISSING_CHAPTER", "A valid chapterId is required");
    }

    private void stageRows(UUID jobId, List<Map<String, Object>> rows) {
        int n = 0;
        for (Map<String, Object> row : rows) {
            n++;
            UUID id = UUID.randomUUID();
            String ext = str(row.get("externalId"));
            db.update("insert into import_row(id,import_job_id,row_no,external_id,status,payload) values(?,?,?,?,?,?::jsonb)", id, jobId, n, ext, "PENDING", writeJson(row));
            validateRow(id, row);
        }
        refreshCounts(jobId);
    }

    private void validateRow(UUID rowId, Map<String, Object> row) {
        try {
            required(row, "externalId");
            required(row, "chapterId");
            required(row, "languageCode");
            required(row, "questionType");
            required(row, "stem");
            enumValue(QuestionType.class, required(row, "questionType"));
            optionalEnum(Difficulty.class, str(row.get("difficulty")));
            resolveChapter(row);
            if (unbalancedLatex(String.valueOf(row.get("stem"))))
                warning(rowId, "stem", "LATEX_WARNING", "Possible unbalanced LaTeX delimiters; import is still allowed");
            if ("MCQ".equalsIgnoreCase(String.valueOf(row.get("questionType")))) validateMcq(rowId, row);
            validateMediaPaths(rowId, row);
            markRow(rowId, "VALID", null);
        } catch (RowImportException e) {
            error(rowId, e.field, e.code, e.getMessage());
            markRow(rowId, "ERROR", null);
        } catch (Exception e) {
            error(rowId, "", "VALIDATION_ERROR", e.getMessage() == null ? "Invalid row" : e.getMessage());
            markRow(rowId, "ERROR", null);
        }
    }

    private void validateMcq(UUID rowId, Map<String, Object> row) {
        Object oo = row.get("mcqOptions");
        if (!(oo instanceof List<?> list) || list.size() < 2)
            throw rowError("mcqOptions", "MCQ_OPTIONS_REQUIRED", "MCQ requires at least two options");
        int correct = 0;
        Set<String> keys = new HashSet<>();
        for (Object o : list)
            if (o instanceof Map<?, ?> m) {
                String k = required(m, "optionKey");
                required(m, "optionText");
                if (!keys.add(k)) throw rowError("mcqOptions", "DUPLICATE_OPTION_KEY", "Duplicate MCQ optionKey: " + k);
                if (Boolean.parseBoolean(String.valueOf(m.get("correct") == null ? false : m.get("correct"))))
                    correct++;
            }
        if (correct != 1)
            throw rowError("mcqOptions", "MCQ_CORRECT_COUNT", "MCQ must contain exactly one correct option");
    }

    private void validateMediaPaths(UUID rowId, Map<String, Object> row) {
        validateMediaList(rowId, row.get("media"), "media");
        Object so = row.get("solution");
        if (so instanceof Map<?, ?> m && m.get("steps") instanceof List<?> list) for (Object o : list)
            if (o instanceof Map<?, ?> step) validateMediaList(rowId, step.get("media"), "solution.steps.media");
    }

    private void validateMediaList(UUID rowId, Object x, String field) {
        if (!(x instanceof List<?> list)) return;
        for (Object o : list)
            if (o instanceof Map<?, ?> m) {
                String p = str(m.get("path"));
                if (p == null) throw rowError(field + ".path", "MISSING_MEDIA_PATH", "Media path is required");
                if (p.startsWith("/") || p.contains("\\") || p.contains(".."))
                    throw rowError(field + ".path", "ZIP_PATH_TRAVERSAL", "Unsafe media path: " + p);
            }
    }

    private List<Map<String, Object>> parse(String type, Path source, Path pack) throws IOException {
        return switch (type) {
            case "JSON" -> parseJson(Files.readString(source));
            case "CSV" -> parseCsv(Files.readString(source));
            case "XLSX" -> parseXlsx(source);
            case "ZIP" -> {
                Path content = pack.resolve("content.json");
                if (!Files.exists(content)) throw new IOException("ZIP must contain content.json");
                yield parseJson(Files.readString(content));
            }
            default -> throw new IOException("Unsupported source type");
        };
    }

    private List<Map<String, Object>> parseJson(String text) throws IOException {
        JsonNode n = mapper.readTree(text);
        JsonNode items = n.isArray() ? n : n.get("items");
        if (items == null || !items.isArray()) throw new IOException("Canonical JSON must contain an items array");
        return mapper.convertValue(items, new TypeReference<List<Map<String, Object>>>() {
        });
    }

    private List<Map<String, Object>> parseCsv(String text) throws IOException {
        List<List<String>> rows = csv(text);
        if (rows.isEmpty()) return List.of();
        List<String> h = rows.get(0);
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            for (int c = 0; c < h.size(); c++) {
                String v = c < r.size() ? r.get(c) : "";
                if (Set.of("mcqOptions", "tags", "solution", "media").contains(h.get(c)) && !v.isBlank()) try {
                    m.put(h.get(c), mapper.readValue(v, Object.class));
                } catch (Exception e) {
                    throw new IOException("Invalid JSON in CSV column " + h.get(c));
                }
                else m.put(h.get(c), v);
            }
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> parseXlsx(Path p) throws IOException {
        try (InputStream in = Files.newInputStream(p); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sh = wb.getSheetAt(0);
            if (sh.getLastRowNum() < 1) return List.of();
            Row hr = sh.getRow(0);
            List<String> h = new ArrayList<>();
            for (Cell c : hr) h.add(c.getStringCellValue().trim());
            DataFormatter f = new DataFormatter();
            List<Map<String, Object>> out = new ArrayList<>();
            for (int i = 1; i <= sh.getLastRowNum(); i++) {
                Row r = sh.getRow(i);
                if (r == null) continue;
                Map<String, Object> m = new LinkedHashMap<>();
                for (int c = 0; c < h.size(); c++) {
                    String v = f.formatCellValue(r.getCell(c));
                    if (Set.of("mcqOptions", "tags", "solution", "media").contains(h.get(c)) && !v.isBlank()) try {
                        m.put(h.get(c), mapper.readValue(v, Object.class));
                    } catch (Exception e) {
                        throw new IOException("Invalid JSON in XLSX column " + h.get(c));
                    }
                    else m.put(h.get(c), v);
                }
                out.add(m);
            }
            return out;
        } catch (Exception e) {
            if (e instanceof IOException io) throw io;
            throw new IOException("Invalid XLSX file", e);
        }
    }

    private void extractZip(Path zip, Path target) throws IOException {
        Files.createDirectories(target);
        long total = 0;
        int count = 0;
        try (ZipFile z = new ZipFile(zip.toFile())) {
            var en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                count++;
                if (count > props.maxZipEntries()) throw new IOException("ZIP entry count limit exceeded");
                if (e.isDirectory()) continue;
                String name = e.getName().replace('\\', '/');
                if (name.startsWith("/") || name.contains("../") || name.contains("..\\") || name.matches("^[A-Za-z]:.*"))
                    throw new IOException("ZIP path traversal rejected: " + name);
                if (e.getSize() > props.maxZipEntryBytes())
                    throw new IOException("ZIP entry exceeds per-file limit: " + name);
                Path out = target.resolve(name).normalize();
                if (!out.startsWith(target)) throw new IOException("ZIP path traversal rejected");
                Files.createDirectories(out.getParent());
                long copied = 0;
                try (InputStream in = z.getInputStream(e); OutputStream os = Files.newOutputStream(out, StandardOpenOption.CREATE_NEW)) {
                    byte[] b = new byte[8192];
                    int n;
                    while ((n = in.read(b)) > 0) {
                        copied += n;
                        total += n;
                        if (copied > props.maxZipEntryBytes() || total > props.maxZipUncompressedBytes())
                            throw new IOException("ZIP uncompressed size limit exceeded");
                        os.write(b, 0, n);
                    }
                }
            }
        }
    }

    private void recordRowFailure(UUID rowId, Exception e) {
        db.update("delete from import_error where import_row_id=?", rowId);
        error(rowId, "", "COMMIT_FAILED", rootMessage(e));
        markRow(rowId, "ERROR", null);
    }

    private void refreshCounts(UUID jobId) {
        db.update("update import_job set total_rows=(select count(*) from import_row where import_job_id=?),success_rows=(select count(*) from import_row where import_job_id=? and status in ('VALID','COMMITTED','SKIPPED')),error_rows=(select count(*) from import_row r where r.import_job_id=? and exists(select 1 from import_error e where e.import_row_id=r.id and e.severity='ERROR')),updated_at=now() where id=?", jobId, jobId, jobId, jobId);
    }

    private void markRow(UUID id, String status, UUID qid) {
        db.update("update import_row set status=?,question_id=? where id=?", status, qid, id);
    }

    private void error(UUID rowId, String field, String code, String msg) {
        db.update("insert into import_error(import_row_id,field_name,error_code,severity,message) values(?,?,?,?,?)", rowId, field, code, "ERROR", msg);
    }

    private void warning(UUID rowId, String field, String code, String msg) {
        db.update("insert into import_error(import_row_id,field_name,error_code,severity,message) values(?,?,?,?,?)", rowId, field, code, "WARNING", msg);
    }

    private ImportDtos.ImportMode jobMode(UUID id) {
        return ImportDtos.ImportMode.valueOf(db.queryForObject("select mode from import_job where id=?", (rs, n) -> rs.getString(1), id));
    }

    private ImportJobOld findJob(String key) {
        return db.query("select id,source_sha256 from import_job where idempotency_key=?", (rs, n) -> new ImportJobOld(rs.getObject(1, UUID.class), rs.getString(2)), key).stream().findFirst().orElse(null);
    }

    private ImportDtos.ImportJobResponse job(UUID id) {
        return db.queryForObject("select id,source_type,file_name,status,mode,dry_run,total_rows,success_rows,error_rows,created_at,updated_at,committed_at from import_job where id=?",
                (rs, n) -> new ImportDtos.ImportJobResponse(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getBoolean(6), rs.getInt(7), rs.getInt(8), rs.getInt(9), rs.getObject(10, OffsetDateTime.class), rs.getObject(11, OffsetDateTime.class), rs.getObject(12, OffsetDateTime.class)), id);
    }

    private String jobDir(UUID id) {
        return db.queryForObject("select staging_path from import_job where id=?", (rs, n) -> rs.getString(1), id);
    }

    private static String required(Map<?, ?> m, String k) {
        String v = str(m.get(k));
        if (v == null || v.isBlank()) throw rowError(k, "REQUIRED", "Field is required");
        return v.trim();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static BigDecimal decimal(Object o) {
        if (o == null || String.valueOf(o).isBlank()) return null;
        return new BigDecimal(String.valueOf(o));
    }

    private static int intOrDefault(Object o, int d) {
        try {
            return o == null ? d : Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return d;
        }
    }

    private static Integer intOrNull(Object o) {
        try {
            return o == null ? null : Integer.valueOf(String.valueOf(o));
        } catch (Exception e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> c, String s) {
        try {
            return Enum.valueOf(c, s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw rowError("questionType", "INVALID_ENUM", "Invalid value: " + s);
        }
    }

    private static <E extends Enum<E>> E optionalEnum(Class<E> c, String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Enum.valueOf(c, s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw rowError("difficulty", "INVALID_ENUM", "Invalid value: " + s);
        }
    }

    private static boolean unbalancedLatex(String s) {
        return count(s, "$") % 2 != 0 || count(s, "\\begin{") != count(s, "\\end{");
    }

    private static int count(String s, String x) {
        int n = 0, i = 0;
        while ((i = s.indexOf(x, i)) >= 0) {
            n++;
            i += x.length();
        }
        return n;
    }

    private static String writeJson(Map<String, Object> m) {
        try {
            return new ObjectMapper().writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String safeFileName(String s) {
        if (s == null || s.isBlank()) return "import";
        String x = s.replace('\\', '/');
        return x.substring(x.lastIndexOf('/') + 1);
    }

    private static String sha256(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getMessage() == null ? t.getClass().getSimpleName() : t.getMessage();
    }

    private static RowImportException rowError(String field, String code, String msg) {
        return new RowImportException(field, code, msg);
    }

    private static void validateMode(String mode) {
        try {
            ImportDtos.ImportMode.valueOf(mode);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid import mode");
        }
    }

    private record ImportJobOld(UUID id, String sourceHash) {
    }

    private record RowCommitResult(boolean created, boolean versionCreated, boolean skipped) {
    }

    private static class RowImportException extends RuntimeException {
        final String field, code;

        RowImportException(String f, String c, String m) {
            super(m);
            field = f;
            code = c;
        }
    }

    private static List<List<String>> csv(String text) {
        List<List<String>> out = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                if (q && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else q = !q;
            } else if (c == ',' && !q) {
                row.add(cell.toString());
                cell.setLength(0);
            } else if ((c == '\n' || c == '\r') && !q) {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString());
                cell.setLength(0);
                if (!row.isEmpty()) out.add(row);
                row = new ArrayList<>();
            } else cell.append(c);
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            out.add(row);
        }
        return out;
    }

    private static final class BytesMultipartFile implements MultipartFile {
        private final String name;
        private final byte[] bytes;

        BytesMultipartFile(String n, byte[] b) {
            name = n;
            bytes = b;
        }

        public String getName() {
            return name;
        }

        public String getOriginalFilename() {
            return name;
        }

        public String getContentType() {
            return null;
        }

        public boolean isEmpty() {
            return bytes.length == 0;
        }

        public long getSize() {
            return bytes.length;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(bytes);
        }

        public void transferTo(File d) throws IOException {
            Files.write(d.toPath(), bytes);
        }

        public void transferTo(Path d) throws IOException {
            Files.write(d, bytes);
        }
    }
}
