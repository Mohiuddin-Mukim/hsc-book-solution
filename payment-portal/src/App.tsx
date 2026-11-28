import { useCallback, useEffect, useMemo, useState } from "react";

type Channel = {
  id: string;
  method: string;
  accountNumber: string;
  paymentType: string;
  instruction: string;
  active: boolean;
  sortOrder: number;
};
type Portal = {
  referenceCode: string | null;
  status: "NEW" | "PENDING" | "APPROVED" | "REJECTED";
  planName: string;
  expectedAmountMinor: number;
  currency: string;
  durationDays: number;
  accessStatus: string;
  expiresAt: string | null;
  senderMobile: string | null;
  transactionId: string | null;
  appliedAt: string | null;
  reviewedAt: string | null;
  rejectionReason: string | null;
  adminMessage: string | null;
  supportWhatsappUrl: string | null;
  channels: Channel[];
};

const API = (
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080/api/v1"
).replace(/\/$/, "");
const token = new URLSearchParams(window.location.search).get("token");

async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const r = await fetch(`${API}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...(init?.headers || {}) },
  });
  const body = await r.json().catch(() => ({}));
  if (!r.ok) throw new Error(body.message || body.error || "Request failed");
  return body as T;
}

const money = (minor: number, currency: string) =>
  `${currency === "BDT" ? "৳" : currency + " "}${(minor / 100).toFixed(2)}`;

function App() {
  const [data, setData] = useState<Portal | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [method, setMethod] = useState("");
  const [sender, setSender] = useState("");
  const [trx, setTrx] = useState("");
  const [saving, setSaving] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const load = useCallback(async () => {
    if (!token) {
      setError(
        "This payment link is incomplete. Please open Buy / Unlock again from the app.",
      );
      setLoading(false);
      return;
    }
    try {
      setData(
        await api<Portal>(`/payment-portal/${encodeURIComponent(token)}`),
      );
      setError("");
    } catch (e) {
      setError(
        e instanceof Error ? e.message : "Unable to load payment details.",
      );
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    if (!data || data.status !== "PENDING") return;
    const id = window.setInterval(load, 10000);
    return () => window.clearInterval(id);
  }, [data, load]);

  const channels = useMemo(() => data?.channels ?? [], [data]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!token) return;
    setSaving(true);
    setError("");
    try {
      const result = await api<Portal>(
        `/payment-portal/${encodeURIComponent(token)}/submit`,
        {
          method: "POST",
          body: JSON.stringify({
            paymentMethod: method,
            senderMobile: sender.replace(/\s+/g, ""),
            transactionId: trx.trim().toUpperCase().replace(/\s+/g, ""),
          }),
        },
      );
      setData(result);
      setSubmitted(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Could not submit payment.");
    } finally {
      setSaving(false);
    }
  }

  if (loading)
    return (
      <Shell>
        <div className="state">
          <div className="spinner" />
          Loading secure payment details…
        </div>
      </Shell>
    );
  if (error && !data)
    return (
      <Shell>
        <div className="state error">
          <h2>Payment portal unavailable</h2>
          <p>{error}</p>
          <button onClick={load}>Try again</button>
        </div>
      </Shell>
    );

  if (!data) return null;
  if (data.status === "APPROVED" || data.accessStatus === "ACTIVE") {
    return (
      <Shell>
        <StatusCard
          title="Access Active"
          text="Your payment has been verified and the package is now unlocked."
          badge="APPROVED"
          data={data}
        >
          <a className="button" href="hscsolution://payment-result">
            Return to app
          </a>
        </StatusCard>
      </Shell>
    );
  }
  if (data.status === "PENDING") {
    return (
      <Shell>
        <StatusCard
          title="Payment under review"
          text="Confirmation usually takes 1–2 hours. You can leave this page open; the status refreshes automatically."
          badge="PENDING"
          data={data}
        >
          {data.supportWhatsappUrl && (
            <a
              className="secondary"
              href={data.supportWhatsappUrl}
              target="_blank"
              rel="noreferrer"
            >
              Contact Support / WhatsApp
            </a>
          )}
        </StatusCard>
      </Shell>
    );
  }

  return (
    <Shell>
      <header className="hero">
        <span className="eyebrow">HSC ACADEMIC SOLUTION</span>
        <h1>Unlock your package</h1>
        <p>
          Complete the local MFS payment and submit the TrxID for manual
          verification.
        </p>
      </header>

      {error && <div className="alert">{error}</div>}
      {submitted && (
        <div className="success">
          Submitted successfully. Reference: <b>{data.referenceCode}</b>
        </div>
      )}

      <section className="card summary">
        <div>
          <span>Package</span>
          <strong>{data.planName}</strong>
        </div>
        <div>
          <span>Amount</span>
          <strong>{money(data.expectedAmountMinor, data.currency)}</strong>
        </div>
        <div>
          <span>Validity</span>
          <strong>{data.durationDays} days</strong>
        </div>
      </section>

      <section className="card">
        <h2>1. Make payment</h2>
        <div className="channels">
          {channels.map((c) => (
            <ChannelCard key={c.id} c={c} />
          ))}
        </div>
        {channels.length === 0 && (
          <div className="warning">
            Payment instructions are not configured yet. Please contact support.
          </div>
        )}
      </section>

      {data.status === "REJECTED" && (
        <div className="rejected">
          <strong>Previous request was rejected.</strong>
          <p>
            {data.rejectionReason ||
              data.adminMessage ||
              "Please review your payment details and submit again."}
          </p>
        </div>
      )}

      <section className="card">
        <h2>2. Submit payment details</h2>
        <form onSubmit={submit}>
          <label>
            Payment method
            <select
              value={method}
              onChange={(e) => setMethod(e.target.value)}
              required
            >
              <option value="">Select method</option>
              {channels.map((c) => (
                <option key={c.id} value={c.method}>
                  {c.method}
                </option>
              ))}
            </select>
          </label>
          <label>
            Sender mobile number
            <input
              inputMode="numeric"
              maxLength={11}
              placeholder="01XXXXXXXXX"
              value={sender}
              onChange={(e) =>
                setSender(e.target.value.replace(/\D/g, "").slice(0, 11))
              }
              required
            />
            <small>
              Use the mobile number from which you sent the payment.
            </small>
          </label>
          <label>
            Transaction ID (TrxID)
            <input
              placeholder="e.g. 8A7B6C5D"
              value={trx}
              onChange={(e) => setTrx(e.target.value.toUpperCase())}
              required
            />
          </label>
          <button disabled={saving || channels.length === 0}>
            {saving ? "Submitting…" : "Submit for verification"}
          </button>
        </form>
      </section>

      <footer>
        Manual verification normally takes 1–2 hours. Keep your TrxID for
        reference.
      </footer>
    </Shell>
  );
}

function ChannelCard({ c }: { c: Channel }) {
  const copy = async () => {
    await navigator.clipboard?.writeText(c.accountNumber);
  };
  return (
    <div className="channel">
      <div>
        <b>{c.method}</b>
        <span>{c.paymentType}</span>
      </div>
      <div className="number">
        {c.accountNumber}
        <button type="button" onClick={copy}>
          Copy
        </button>
      </div>
      <p>{c.instruction}</p>
    </div>
  );
}

function StatusCard({
  title,
  text,
  badge,
  data,
  children,
}: {
  title: string;
  text: string;
  badge: string;
  data: Portal;
  children: React.ReactNode;
}) {
  return (
    <Shell>
      <div className="status-card">
        <div className={`status-badge ${badge.toLowerCase()}`}>{badge}</div>
        <h1>{title}</h1>
        <p>{text}</p>
        {data.referenceCode && (
          <div className="details">
            <div>
              <span>Reference ID</span>
              <b>{data.referenceCode}</b>
            </div>
            {data.transactionId && (
              <div>
                <span>TrxID</span>
                <b>{data.transactionId}</b>
              </div>
            )}
            {data.expiresAt && (
              <div>
                <span>Valid until</span>
                <b>{new Date(data.expiresAt).toLocaleString()}</b>
              </div>
            )}
          </div>
        )}
        <div className="actions">{children}</div>
      </div>
    </Shell>
  );
}
function Shell({ children }: { children: React.ReactNode }) {
  return (
    <main className="page">
      <div className="wrap">{children}</div>
    </main>
  );
}
export default App;
