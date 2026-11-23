package bd.hscsolution.student;

import bd.hscsolution.student.content.CatalogModels;
import bd.hscsolution.student.content.SearchState;
import org.junit.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.Assert.*;

public class SearchStateTest {
    @Test public void successCarriesStableQuestionIdAndVersion() {
        CatalogModels.SearchResult r = new CatalogModels.SearchResult(
                "q-1", "PHY-001", "ch-1", "C1", "Motion",
                "sub-1", "PHY", "Physics", "p-1", "P1", "Physics 1st Paper",
                "bn", "CQ", "MEDIUM", new BigDecimal("5.0"), "v-9", 1000d);
        CatalogModels.SearchPageResponse page =
                new CatalogModels.SearchPageResponse(List.of(r), 0, 20, 1, 1);
        SearchState state = new SearchState.Success(page);
        assertEquals("q-1", ((SearchState.Success) state).data().content().get(0).id());
        assertEquals("v-9", ((SearchState.Success) state).data().content().get(0).versionId());
    }
}
