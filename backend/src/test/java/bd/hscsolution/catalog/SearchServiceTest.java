package bd.hscsolution.catalog;

import bd.hscsolution.catalog.repository.QuestionSearchRepository;
import bd.hscsolution.catalog.service.SearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchServiceTest {
    @Test
    void blankQueryIsRejected() {
        SearchService service = new SearchService(Mockito.mock(QuestionSearchRepository.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.studentSearch("   ", null, null, null, null, null, null, 0, 20));
    }

    @Test
    void oversizedQueryIsRejected() {
        SearchService service = new SearchService(Mockito.mock(QuestionSearchRepository.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.studentSearch("a".repeat(161), null, null, null, null, null, null, 0, 20));
    }
}
