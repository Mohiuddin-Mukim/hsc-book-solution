package bd.hscsolution.commerce;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@ConditionalOnProperty(name = "commerce.provider", havingValue = "sandbox", matchIfMissing = true)
@RequestMapping("/api/v1/commerce/sandbox")
public class SandboxCheckoutController {

    private final SandboxPaymentProvider sandboxProvider;

    public SandboxCheckoutController(SandboxPaymentProvider sandboxProvider) {
        this.sandboxProvider = sandboxProvider;
    }

    @GetMapping(value = "/checkout/{tx}", produces = MediaType.TEXT_HTML_VALUE)
    public String checkout(@PathVariable String tx) {
        var paymentOpt = sandboxProvider.findPayment(tx);
        String orderCode = paymentOpt.map(PaymentProvider.VerifiedPayment::orderCode).orElse("");
        long amountMinor = paymentOpt.map(PaymentProvider.VerifiedPayment::amountMinor).orElse(0L);
        String currency = paymentOpt.map(PaymentProvider.VerifiedPayment::currency).orElse("BDT");
        String formattedAmount = String.format("%.2f %s", amountMinor / 100.0, currency);

        String safeTx = escapeHtml(tx);
        String safeOrderCode = escapeHtml(orderCode);
        String safeAmount = escapeHtml(formattedAmount);

        return """
                <!DOCTYPE html>
                <html lang="bn">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>Sandbox Checkout</title>
                  <style>
                    * { box-sizing: border-box; font-family: system-ui, -apple-system, sans-serif; }
                    body {
                      margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
                      background: #f1f5f9; color: #0f172a; padding: 20px;
                    }
                    .card {
                      width: 100%%; max-width: 420px; background: #fff; border-radius: 16px;
                      padding: 24px; box-shadow: 0 10px 25px rgba(15, 23, 42, 0.08);
                    }
                    .badge {
                      display: inline-block; background: #dbeafe; color: #1d4ed8;
                      font-size: 12px; font-weight: 700; padding: 4px 10px; border-radius: 999px;
                    }
                    h2 { margin: 12px 0 6px; font-size: 20px; }
                    .sub { color: #64748b; font-size: 13px; margin-bottom: 18px; }
                    .meta {
                      background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px;
                      padding: 14px; margin-bottom: 20px; font-size: 14px;
                    }
                    .row { display: flex; justify-content: space-between; margin: 6px 0; gap: 10px; }
                    .label { color: #64748b; }
                    .val { font-weight: 600; word-break: break-all; text-align: right; }
                    button {
                      width: 100%%; border: 0; border-radius: 12px; padding: 14px;
                      font-size: 15px; font-weight: 700; cursor: pointer;
                    }
                    .btn-pay { background: #16a34a; color: #fff; }
                    .btn-pay:disabled { opacity: 0.6; cursor: not-allowed; }
                    .btn-app {
                      margin-top: 10px; background: #0f172a; color: #fff; display: none;
                    }
                    .status {
                      margin-top: 14px; font-size: 13px; line-height: 1.5; color: #334155;
                    }
                    .ok { color: #15803d; font-weight: 700; }
                    .err { color: #b91c1c; font-weight: 700; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <span class="badge">SANDBOX GATEWAY</span>
                    <h2>ডেমো পেমেন্ট কনফার্মেশন</h2>
                    <div class="sub">টেস্টের জন্য নিচের বাটনে চাপলে webhook কল হয়ে অর্ডার PAID হবে।</div>

                    <div class="meta">
                      <div class="row"><span class="label">Order Code</span><span class="val">%s</span></div>
                      <div class="row"><span class="label">Amount</span><span class="val">%s</span></div>
                      <div class="row"><span class="label">Transaction ID</span><span class="val">%s</span></div>
                    </div>

                    <button id="payBtn" class="btn-pay" onclick="simulateSuccess()">Simulate Payment Success</button>
                    <button id="appBtn" class="btn-app" onclick="returnToApp()">Return to Student App</button>
                    <div id="statusMsg" class="status"></div>
                  </div>

                  <script>
                    const txId = "%s";
                    const orderCode = "%s";
                    const amountMinor = %d;
                    const currency = "%s";
                    const appDeepLink = "hscsolution://payment-result?status=success&tx=" + encodeURIComponent(txId) + "&orderCode=" + encodeURIComponent(orderCode);

                    async function simulateSuccess() {
                      const btn = document.getElementById("payBtn");
                      const appBtn = document.getElementById("appBtn");
                      const msg = document.getElementById("statusMsg");
                      btn.disabled = true;
                      btn.textContent = "Processing Webhook...";

                      const body = {
                        provider: "SANDBOX",
                        eventId: "evt-" + Date.now(),
                        eventType: "PAYMENT_SUCCEEDED",
                        transactionId: txId,
                        orderCode: orderCode,
                        status: "PAID",
                        amountMinor: amountMinor,
                        currency: currency,
                        signature: "sandbox-simulated-signature",
                        payload: {}
                      };

                      try {
                        const res = await fetch("/api/v1/webhooks/sandbox", {
                          method: "POST",
                          headers: {
                            "Content-Type": "application/json",
                            "X-Webhook-Signature": "sandbox-simulated-signature",
                            "X-Raw-Body": JSON.stringify(body)
                          },
                          body: JSON.stringify(body)
                        });

                        if (!res.ok) {
                          const txt = await res.text();
                          throw new Error("HTTP " + res.status + ": " + txt);
                        }

                        msg.innerHTML = '<span class="ok">পেমেন্ট সফলভাবে PAID হয়েছে!</span><br/>এখন অ্যাপে ফিরে গিয়ে Orders / Plans রিফ্রেশ করুন।';
                        btn.textContent = "Paid ✓";
                        appBtn.style.display = "block";
                      } catch (e) {
                        btn.disabled = false;
                        btn.textContent = "Simulate Payment Success";
                        msg.innerHTML = '<span class="err">Webhook ব্যর্থ হয়েছে:</span> ' + e.message;
                      }
                    }

                    function returnToApp() {
                      window.location.href = appDeepLink;
                    }
                  </script>
                </body>
                </html>
                """.formatted(
                safeOrderCode, safeAmount, safeTx,
                safeTx, safeOrderCode, amountMinor, escapeHtml(currency)
        );
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}