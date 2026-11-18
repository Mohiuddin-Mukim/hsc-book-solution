
package bd.hscsolution.imports;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationProperties(prefix = "hsc.import")
@EnableConfigurationProperties(ImportProperties.class)
public record ImportProperties(
        String storageRoot,
        long maxFileBytes,
        int maxRows,
        int maxZipEntries,
        long maxZipUncompressedBytes,
        long maxZipEntryBytes) {}
