package bd.hscsolution.commerce;

import java.util.Map;

public interface PaymentProvider {
    String name();

    PaymentProviderResult initiate(CommerceOrderContext order, String idempotencyKey);

    VerifiedPayment verify(String transactionId, String orderCode);

    boolean verifyWebhookSignature(String rawPayload, String signature);

    record PaymentProviderResult(String provider, String checkoutUrl, String providerTransactionId, String status) {
    }

    record VerifiedPayment(String provider, String providerTransactionId, String orderCode, long amountMinor,
                           String currency, String status, Map<String, Object> raw) {
    }

    record CommerceOrderContext(String orderCode, long amountMinor, String currency, String customerEmail,
                                String customerName) {
    }
}