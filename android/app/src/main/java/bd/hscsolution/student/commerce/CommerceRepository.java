package bd.hscsolution.student.commerce;

import bd.hscsolution.student.network.ApiClient;
import bd.hscsolution.student.network.ApiErrorMapper;
import retrofit2.Response;

import java.util.*;

public final class CommerceRepository {
    private final CommerceApi api;
    public CommerceRepository(ApiClient c) { api = c.commerceApi(); }

    private static <T> T body(Response<T> r) throws Exception {
        if (!r.isSuccessful() || r.body() == null) throw ApiErrorMapper.from(r);
        return r.body();
    }

    public List<CommerceModels.StudentPlan> studentPlans() throws Exception {
        return body(api.studentPlans().execute());
    }

    public List<CommerceModels.Plan> plans() throws Exception {
        return body(api.plans().execute());
    }

    public CommerceModels.Checkout checkout(String planId, String key) throws Exception {
        return body(api.checkout(Map.of("planId", planId, "idempotencyKey", key)).execute());
    }

    public CommerceModels.ManualPortalCreate manualPaymentPortal(String planId) throws Exception {
        return body(api.manualPaymentPortal(Map.of("planId", planId)).execute());
    }

    public List<CommerceModels.Order> orders() throws Exception {
        return body(api.orders().execute());
    }

    public List<CommerceModels.Order> transactions() throws Exception {
        return body(api.transactions().execute());
    }

    public CommerceModels.PaymentStatus status(String id) throws Exception {
        return body(api.status(id).execute());
    }

    public List<CommerceModels.Notification> unreadNotifications() throws Exception {
        return body(api.unreadNotifications().execute());
    }

    public void markNotificationRead(String id) throws Exception {
        body(api.markNotificationRead(id).execute());
    }
}
