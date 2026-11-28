package bd.hscsolution.commerce;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

public final class CommerceDtos {
    private CommerceDtos() {}

    public record PlanRequest(@NotBlank @Size(max = 60) String code, @NotBlank @Size(max = 160) String name,
                              @NotBlank String planType, String resourceType, UUID resourceId,
                              @NotNull @PositiveOrZero Long originalPriceMinor,
                              @NotNull @PositiveOrZero Long salePriceMinor, @NotNull @Positive Integer durationDays,
                              String currency, String description, List<UUID> bookIds, Boolean active) {}

    public record PlanResponse(UUID id, String code, String name, String planType, String resourceType, UUID resourceId,
                               long originalPriceMinor, long salePriceMinor, BigDecimal discountPercent,
                               String currency, int durationDays, String description, boolean active,
                               List<UUID> bookIds) {}

    public record StudentPlanResponse(UUID id, String code, String name, String planType, String resourceType, UUID resourceId,
                                      long originalPriceMinor, long salePriceMinor, BigDecimal discountPercent,
                                      String currency, int durationDays, String description, boolean active,
                                      List<UUID> bookIds, String accessStatus, String expiresAt) {}

    public record CheckoutRequest(@NotNull UUID planId, @NotBlank @Size(max = 160) String idempotencyKey) {}

    public record CheckoutResponse(UUID orderId, String orderCode, long amountMinor, String currency, String status,
                                   String provider, String checkoutUrl, String providerTransactionId,
                                   boolean alreadyPurchased) {}

    public record PaymentStatusResponse(UUID orderId, String orderCode, String status, long amountMinor,
                                        String currency, String provider, String providerTransactionId,
                                        String entitlementStatus) {}

    public record OrderResponse(UUID id, String orderCode, String status, long amountMinor, String currency,
                                String planName, String createdAt, String provider, String paymentMethod,
                                String senderMobile, String transactionId, String paidAt, String entitlementStatus,
                                String expiresAt, String source, String referenceCode) {}

    public record TransactionResponse(UUID id, String referenceCode, String planName, long amountMinor, String currency,
                                      String status, String paymentMethod, String senderMobile, String transactionId,
                                      String appliedAt, String reviewedAt, String rejectionReason, String adminMessage,
                                      String entitlementStatus, String expiresAt, String source) {}

    public record WebhookRequest(String provider, String eventId, String eventType, String transactionId,
                                 String orderCode, String status, Long amountMinor, String currency, String signature,
                                 Map<String, Object> payload) {}

    public record ReconciliationItemResponse(UUID id, String provider, String providerTransactionId, UUID orderId,
                                             Long expectedAmountMinor, Long actualAmountMinor, String expectedStatus,
                                             String actualStatus, String mismatchType, boolean resolved,
                                             String createdAt) {}

    public record FreeChapterRequest(@NotNull UUID bookId, @NotNull UUID chapterId) {}
    public record FreeChapterResponse(UUID id, UUID bookId, UUID chapterId) {}

    public record ManualPortalCreateRequest(@NotNull UUID planId) {}
    public record ManualPortalCreateResponse(String portalUrl, String expiresAt) {}

    public record ManualPaymentChannelResponse(UUID id, String method, String accountNumber, String paymentType,
                                               String instruction, boolean active, int sortOrder) {}
    public record ManualPaymentChannelRequest(@NotBlank String method, @NotBlank @Size(max=32) String accountNumber,
                                              @NotBlank String paymentType, String instruction, boolean active,
                                              @NotNull Integer sortOrder) {}

    public record ManualPortalResponse(String referenceCode, String status, String planName, long expectedAmountMinor,
                                       String currency, int durationDays, String accessStatus, String expiresAt,
                                       String senderMobile, String transactionId, String appliedAt, String reviewedAt,
                                       String rejectionReason, String adminMessage, String supportWhatsappUrl,
                                       List<ManualPaymentChannelResponse> channels) {}

    public record ManualPaymentSubmitRequest(@NotBlank String paymentMethod,
                                             @NotBlank @Pattern(regexp="^01[3-9][0-9]{8}$", message="Sender mobile must be a valid 11-digit Bangladesh number") String senderMobile,
                                             @NotBlank @Size(max=64) String transactionId) {}

    public record ManualPaymentAdminResponse(UUID id, String referenceCode, UUID userId, String userEmail,
                                             String userName, String planName, long expectedAmountMinor, String currency,
                                             String paymentMethod, String senderMobile, String transactionId,
                                             String appliedAt, String status, String reviewedAt, String rejectionReason,
                                             String adminMessage, String expiresAt) {}

    public record ManualPaymentReviewRequest(@Size(max=500) String message) {}

    public record NotificationResponse(UUID id, String title, String message, String type, String entityType,
                                       UUID entityId, String createdAt) {}
}
