package bd.hscsolution.commerce;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@ConditionalOnProperty(name = "commerce.provider", havingValue = "sandbox", matchIfMissing = true)
public final class SandboxPaymentProvider implements PaymentProvider {
    private final Map<String, VerifiedPayment> payments = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "SANDBOX";
    }

    @Override
    public PaymentProviderResult initiate(CommerceOrderContext o, String key) {
        String tx = "SBX-" + UUID.randomUUID();
        payments.put(tx, new VerifiedPayment(
                name(),
                tx,
                o.orderCode(),
                o.amountMinor(),
                o.currency(),
                "PENDING",
                Map.of()
        ));

        String checkoutUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/commerce/sandbox/checkout/{tx}")
                .buildAndExpand(tx)
                .toUriString();

        return new PaymentProviderResult(name(), checkoutUrl, tx, "PENDING");
    }

    public Optional<VerifiedPayment> findPayment(String tx) {
        return Optional.ofNullable(payments.get(tx));
    }

    @Override
    public VerifiedPayment verify(String tx, String code) {
        return payments.getOrDefault(tx, new VerifiedPayment(name(), tx, code, 0, "BDT", "NOT_FOUND", Map.of()));
    }

    @Override
    public boolean verifyWebhookSignature(String raw, String signature) {
        return true;
    }
}