
package bd.hscsolution.imports;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ImportSchemaTest {
    @Test
    void canonicalSchemaResourceIsPresent() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/import/hsc-academic-import.schema.json")) {
            assertNotNull(in);
            JsonNode schema = new ObjectMapper().readTree(in);
            assertEquals("hsc-academic-import-1.0", schema.get("$id").asText());
            assertEquals("1.0", schema.at("/properties/schemaVersion/const").asText());
            assertTrue(schema.at("/properties/items").has("items"));
        }
    }
}
