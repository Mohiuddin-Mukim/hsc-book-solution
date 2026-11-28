import { useEffect, useState } from "react";
import {
  manualPaymentChannels,
  updateManualPaymentChannel,
} from "../api/commerce";

type Channel = {
  id: string;
  method: string;
  accountNumber: string;
  paymentType: string;
  instruction: string;
  active: boolean;
  sortOrder: number;
};

export default function ManualPaymentSettingsPage() {
  const [rows, setRows] = useState<Channel[]>([]);
  const [msg, setMsg] = useState("");
  const [saving, setSaving] = useState("");

  const load = () => {
    manualPaymentChannels()
      .then(setRows)
      .catch((e: any) => setMsg(e?.message || "Failed to load channels"));
  };

  useEffect(() => {
    load();
  }, []);

  const update = (id: string, key: string, val: any) =>
    setRows((r) => r.map((x) => (x.id === id ? { ...x, [key]: val } : x)));

  async function save(c: Channel) {
    setSaving(c.id);
    setMsg("");
    try {
      await updateManualPaymentChannel(c.id, c);
      setMsg(`${c.method} settings saved.`);
    } catch (e: any) {
      setMsg(e?.message || "Save failed");
    } finally {
      setSaving("");
    }
  }

  return (
    <div className="mps">
      <style>{styles}</style>
      <div className="mps-head">
        <div>
          <span>MANUAL PAYMENT</span>
          <h1>MFS payment settings</h1>
          <p>
            Configure the production bKash, Nagad and Rocket receiving accounts
            shown on the payment portal.
          </p>
        </div>
      </div>
      {msg && <div className="mps-msg">{msg}</div>}
      {rows.map((c) => (
        <section className="mps-card" key={c.id}>
          <div className="mps-title">
            <h2>{c.method}</h2>
            <label>
              <input
                type="checkbox"
                checked={c.active}
                onChange={(e) => update(c.id, "active", e.target.checked)}
              />{" "}
              Show on portal
            </label>
          </div>
          <div className="grid">
            <label>
              Account number
              <input
                value={c.accountNumber}
                onChange={(e) =>
                  update(
                    c.id,
                    "accountNumber",
                    e.target.value.replace(/\D/g, "").slice(0, 32),
                  )
                }
              />
            </label>
            <label>
              Payment type
              <select
                value={c.paymentType}
                onChange={(e) => update(c.id, "paymentType", e.target.value)}
              >
                <option>Send Money</option>
                <option>Payment</option>
              </select>
            </label>
          </div>
          <label>
            Instruction
            <textarea
              value={c.instruction}
              onChange={(e) => update(c.id, "instruction", e.target.value)}
            />
          </label>
          <button disabled={saving === c.id} onClick={() => save(c)}>
            {saving === c.id ? "Saving…" : "Save settings"}
          </button>
        </section>
      ))}
    </div>
  );
}

const styles = `.mps{max-width:900px;margin:auto;padding:28px 24px}.mps-head span{font-size:11px;font-weight:800;letter-spacing:.1em;color:#0f8f87}.mps h1{margin:6px 0}.mps-head p{color:#64748b}.mps-card{background:#fff;border:1px solid #e2e8f0;border-radius:15px;padding:20px;margin:14px 0}.mps-title{display:flex;justify-content:space-between;align-items:center}.mps-title h2{margin:0}.mps-title label{font-size:12px}.grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}label{display:block;font-size:12px;font-weight:700;margin:15px 0}input,select,textarea{display:block;width:100%;margin-top:6px;padding:10px;border:1px solid #d8e0ea;border-radius:8px;font:inherit}textarea{min-height:90px}.mps-card button{background:#173f8a;color:#fff;border:0;border-radius:9px;padding:10px 14px;font-weight:700}.mps-msg{padding:11px;background:#ecfdf5;border:1px solid #b7ead9;border-radius:9px;color:#05624f;margin:12px 0}@media(max-width:650px){.grid{grid-template-columns:1fr}}`;