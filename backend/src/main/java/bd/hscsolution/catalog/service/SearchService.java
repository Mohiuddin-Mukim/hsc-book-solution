package bd.hscsolution.catalog.service;

import bd.hscsolution.catalog.dto.SearchDtos;
import bd.hscsolution.catalog.repository.QuestionSearchRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
public class SearchService {
    private final QuestionSearchRepository repository;

    public SearchService(QuestionSearchRepository repository) {
        this.repository = repository;
    }

    public Page<SearchDtos.Result> studentSearch(String query, UUID subjectId, UUID paperId, UUID chapterId,
                                                  String languageCode, String questionType, String difficulty,
                                                  int page, int size) {
        return search(query, true, subjectId, paperId, chapterId,
                normalizeLanguage(languageCode), normalizeEnum(questionType),
                normalizeEnum(difficulty), page, size);
    }

    public Page<SearchDtos.Result> adminSearch(String query, UUID subjectId, UUID paperId, UUID chapterId,
                                               String languageCode, String questionType, String difficulty,
                                               int page, int size) {
        return search(query, false, subjectId, paperId, chapterId,
                normalizeLanguage(languageCode), normalizeEnum(questionType),
                normalizeEnum(difficulty), page, size);
    }

    private Page<SearchDtos.Result> search(String query, boolean publishedOnly, UUID subjectId, UUID paperId,
                                           UUID chapterId, String languageCode, String questionType,
                                           String difficulty, int page, int size) {
        String normalized = normalizeQuery(query);
        if (normalized.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query is required");
        if (normalized.length() > 160)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search query is too long");

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return repository.search(normalized, publishedOnly, subjectId, paperId, chapterId,
                        languageCode, questionType, difficulty, pageable)
                .map(this::map);
    }

    private SearchDtos.Result map(QuestionSearchRepository.Row r) {
        return new SearchDtos.Result(
                r.getId(), r.getExternalId(), r.getChapterId(), r.getChapterCode(), r.getChapterTitle(),
                r.getSubjectId(), r.getSubjectCode(), r.getSubjectName(),
                r.getPaperId(), r.getPaperCode(), r.getPaperName(),
                r.getLanguageCode(),
                r.getQuestionType() == null ? null :
                        bd.hscsolution.catalog.entity.AcademicEnums.QuestionType.valueOf(r.getQuestionType()),
                r.getDifficulty() == null ? null :
                        bd.hscsolution.catalog.entity.AcademicEnums.Difficulty.valueOf(r.getDifficulty()),
                r.getMarks(), r.getVersionId(),
                r.getRelevanceScore() == null ? 0d : r.getRelevanceScore());
    }

    private String normalizeQuery(String query) {
        if (query == null) return "";
        return query.trim().replaceAll("\\s+", " ");
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) return null;
        String value = language.trim().toLowerCase(Locale.ROOT);
        return value.equals("bn") || value.equals("en") ? value : null;
    }

    private String normalizeEnum(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
