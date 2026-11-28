package bd.hscsolution.student.commerce;

import java.util.*;

public final class CommerceModels {
    private CommerceModels() {}

    public record Plan(String id, String code, String name, String planType, String resourceType,
                       String resourceId, long originalPriceMinor, long salePriceMinor,
                       double discountPercent, String currency, int durationDays,
                       String description, boolean active, List<String> bookIds) {}

    public record StudentPlan(String id, String code, String name, String planType, String resourceType,
                              String resourceId, long originalPriceMinor, long salePriceMinor,
                              double discountPercent, String currency, int durationDays,
                              String description, boolean active, List<String> bookIds,
                              String accessStatus, String expiresAt) {}

    public record Checkout(String orderId, String orderCode, long amountMinor, String currency,
                           String status, String provider, String checkoutUrl,
                           String providerTransactionId, boolean alreadyPurchased) {}

    public record ManualPortalCreate(String portalUrl, String expiresAt) {}
    public record Notification(String id, String title, String message, String type,
                               String entityType, String entityId, String createdAt) {}

    public record Order(String id, String orderCode, String status, long amountMinor,
                        String currency, String planName, String createdAt, String provider,
                        String paymentMethod, String senderMobile, String transactionId,
                        String paidAt, String entitlementStatus, String expiresAt,
                        String source, String referenceCode) {}

    public record PaymentStatus(String orderId, String orderCode, String status, long amountMinor,
                                String currency, String provider, String providerTransactionId,
                                String entitlementStatus, String paidAt, String expiresAt) {}
}
