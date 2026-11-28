package bd.hscsolution.student.commerce;

import java.util.*;

import retrofit2.Call;
import retrofit2.http.*;

public interface CommerceApi {
    @GET("plans")
    Call<List<CommerceModels.Plan>> plans();

    @GET("student/plans")
    Call<List<CommerceModels.StudentPlan>> studentPlans();

    @POST("student/checkout")
    Call<CommerceModels.Checkout> checkout(@Body Map<String, Object> body);

    @POST("student/manual-payment-portal")
    Call<CommerceModels.ManualPortalCreate> manualPaymentPortal(@Body Map<String, Object> body);

    @GET("student/orders")
    Call<List<CommerceModels.Order>> orders();

    @GET("student/transactions")
    Call<List<CommerceModels.Order>> transactions();

    @GET("student/orders/{id}")
    Call<CommerceModels.PaymentStatus> status(@Path("id") String id);

    @GET("student/notifications/unread")
    Call<List<CommerceModels.Notification>> unreadNotifications();

    @POST("student/notifications/{id}/read")
    Call<Void> markNotificationRead(@Path("id") String id);
}
