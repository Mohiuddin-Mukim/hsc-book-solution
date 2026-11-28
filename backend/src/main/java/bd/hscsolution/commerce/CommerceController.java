package bd.hscsolution.commerce;

import bd.hscsolution.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class CommerceController {
    private final CommerceService commerce;
    private final ManualPaymentService manual;
    public CommerceController(CommerceService commerce, ManualPaymentService manual){this.commerce=commerce;this.manual=manual;}
    private static UUID uid(AuthPrincipal p){return UUID.fromString(p.userId());}

    @GetMapping("/plans") public List<CommerceDtos.PlanResponse> plans(){return commerce.plans(true);}
    @GetMapping("/plans/{id}") public CommerceDtos.PlanResponse plan(@PathVariable UUID id){return commerce.getPlan(id);}

    @GetMapping("/student/plans") @PreAuthorize("hasRole('STUDENT')")
    public List<CommerceDtos.StudentPlanResponse> studentPlans(@AuthenticationPrincipal AuthPrincipal p){return commerce.studentPlans(uid(p));}

    @PostMapping("/student/checkout") @PreAuthorize("hasRole('STUDENT')")
    public CommerceDtos.CheckoutResponse checkout(@Valid @RequestBody CommerceDtos.CheckoutRequest r,@AuthenticationPrincipal AuthPrincipal p){return commerce.checkout(uid(p),r);}

    @PostMapping("/student/manual-payment-portal") @PreAuthorize("hasRole('STUDENT')")
    public CommerceDtos.ManualPortalCreateResponse manualPortal(@Valid @RequestBody CommerceDtos.ManualPortalCreateRequest r,@AuthenticationPrincipal AuthPrincipal p){return manual.createPortal(uid(p),r.planId());}

    @GetMapping("/student/orders") @PreAuthorize("hasRole('STUDENT')")
    public List<CommerceDtos.OrderResponse> orders(@AuthenticationPrincipal AuthPrincipal p){return commerce.orders(uid(p));}

    @GetMapping("/student/transactions") @PreAuthorize("hasRole('STUDENT')")
    public List<CommerceDtos.TransactionResponse> transactions(@AuthenticationPrincipal AuthPrincipal p){return commerce.transactions(uid(p));}

    @GetMapping("/student/orders/{id}") @PreAuthorize("hasRole('STUDENT')")
    public CommerceDtos.PaymentStatusResponse status(@PathVariable UUID id,@AuthenticationPrincipal AuthPrincipal p){return commerce.status(uid(p),id);}

    @GetMapping("/student/notifications/unread") @PreAuthorize("hasRole('STUDENT')")
    public List<CommerceDtos.NotificationResponse> unread(@AuthenticationPrincipal AuthPrincipal p){return manual.unread(uid(p));}

    @PostMapping("/student/notifications/{id}/read") @PreAuthorize("hasRole('STUDENT')")
    public void read(@PathVariable UUID id,@AuthenticationPrincipal AuthPrincipal p){manual.markRead(uid(p),id);}

    @PostMapping("/webhooks/{provider}")
    public void webhook(@PathVariable String provider,@RequestBody CommerceDtos.WebhookRequest r,@RequestHeader(value="X-Webhook-Signature",required=false) String sig,@RequestHeader(value="X-Raw-Body",required=false) String raw){
        commerce.webhook(new CommerceDtos.WebhookRequest(provider,r.eventId(),r.eventType(),r.transactionId(),r.orderCode(),r.status(),r.amountMinor(),r.currency(),sig,r.payload()),raw==null?"":raw);
    }

    @GetMapping("/payment-portal/{token}")
    public CommerceDtos.ManualPortalResponse portal(@PathVariable String token){return manual.portal(token);}

    @PostMapping("/payment-portal/{token}/submit")
    public CommerceDtos.ManualPortalResponse submit(@PathVariable String token,
                                                    @Valid @RequestBody CommerceDtos.ManualPaymentSubmitRequest r){
        return manual.submit(token, r);
    }

    @GetMapping("/admin/commerce/plans") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public List<CommerceDtos.PlanResponse> adminPlans(){return commerce.plans(false);}
    @PostMapping("/admin/commerce/plans") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public CommerceDtos.PlanResponse create(@Valid @RequestBody CommerceDtos.PlanRequest r,@AuthenticationPrincipal AuthPrincipal p){return commerce.createPlan(r,uid(p));}
    @PutMapping("/admin/commerce/plans/{id}") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public CommerceDtos.PlanResponse update(@PathVariable UUID id,@Valid @RequestBody CommerceDtos.PlanRequest r,@AuthenticationPrincipal AuthPrincipal p){return commerce.updatePlan(id,r,uid(p));}
    @PostMapping("/admin/commerce/free-chapters") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public CommerceDtos.FreeChapterResponse free(@Valid @RequestBody CommerceDtos.FreeChapterRequest r,@AuthenticationPrincipal AuthPrincipal p){return commerce.free(r,uid(p));}
    @DeleteMapping("/admin/commerce/free-chapters") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public void unfree(@RequestParam UUID bookId,@RequestParam UUID chapterId,@AuthenticationPrincipal AuthPrincipal p){commerce.unfree(bookId,chapterId,uid(p));}
    @GetMapping("/admin/commerce/reconciliation") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public List<CommerceDtos.ReconciliationItemResponse> reconciliation(){return commerce.reconciliation();}

    @GetMapping("/admin/commerce/manual-payments") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public List<CommerceDtos.ManualPaymentAdminResponse> manualQueue(@RequestParam(defaultValue="PENDING") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return manual.queue(status,page,size);}
    @PostMapping("/admin/commerce/manual-payments/{id}/approve") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public CommerceDtos.ManualPaymentAdminResponse approve(@PathVariable UUID id,@Valid @RequestBody CommerceDtos.ManualPaymentReviewRequest r,@AuthenticationPrincipal AuthPrincipal p){return manual.approve(id,uid(p),r.message());}
    @PostMapping("/admin/commerce/manual-payments/{id}/reject") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public CommerceDtos.ManualPaymentAdminResponse reject(@PathVariable UUID id,@Valid @RequestBody CommerceDtos.ManualPaymentReviewRequest r,@AuthenticationPrincipal AuthPrincipal p){return manual.reject(id,uid(p),r.message());}
    @GetMapping("/admin/commerce/manual-payment-channels") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public List<CommerceDtos.ManualPaymentChannelResponse> channels(){return manual.allChannels();}
    @PutMapping("/admin/commerce/manual-payment-channels/{id}") @PreAuthorize("hasAuthority('PERM_ENTITLEMENT_ADJUST')")
    public CommerceDtos.ManualPaymentChannelResponse updateChannel(@PathVariable UUID id,@Valid @RequestBody CommerceDtos.ManualPaymentChannelRequest r,@AuthenticationPrincipal AuthPrincipal p){return manual.updateChannel(id,r,uid(p));}
}
