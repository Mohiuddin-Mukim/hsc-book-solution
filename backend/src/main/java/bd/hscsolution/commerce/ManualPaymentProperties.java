package bd.hscsolution.commerce;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "commerce.manual")
public record ManualPaymentProperties(
    String portalBaseUrl,
    String supportWhatsappUrl,
    long portalTtlMinutes
) {}
