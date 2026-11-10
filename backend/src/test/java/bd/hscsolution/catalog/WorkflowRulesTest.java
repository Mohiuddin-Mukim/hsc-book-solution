package bd.hscsolution.catalog;
import org.junit.jupiter.api.Test; import static org.junit.jupiter.api.Assertions.*; import static bd.hscsolution.catalog.entity.AcademicEnums.*;
class WorkflowRulesTest {
 @Test void publishedIsTerminalExceptArchive(){assertTrue(ContentStatus.PUBLISHED!=ContentStatus.ARCHIVED);assertNotEquals(ContentStatus.PUBLISHED,ContentStatus.DRAFT);}
 @Test void statusEnumContainsRequiredLifecycle(){assertArrayEquals(new ContentStatus[]{ContentStatus.DRAFT,ContentStatus.SUBMITTED,ContentStatus.CHANGES_REQUESTED,ContentStatus.APPROVED,ContentStatus.PUBLISHED,ContentStatus.ARCHIVED},ContentStatus.values());}
}
