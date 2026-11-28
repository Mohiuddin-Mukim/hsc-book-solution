import { useEffect, useState } from "react";
import {
  approveManualPayment, manualPayments, rejectManualPayment,
  type ManualPayment
} from "../api/commerce";

const money=(n:number,c:string)=>`${c==="BDT"?"৳":c+" "}${(Number(n)/100).toFixed(2)}`;

function getErrorMessage(err: unknown, fallback: string): string {
  if (err instanceof Error) return err.message;
  if (typeof err === "object" && err !== null && "message" in err) {
    return String((err as { message: unknown }).message);
  }
  return fallback;
}

export default function ManualPaymentQueuePage(){
  const [items,setItems]=useState<ManualPayment[]>([]);
  const [status,setStatus]=useState("PENDING");
  const [loading,setLoading]=useState(true);
  const [error,setError]=useState("");
  const [selected,setSelected]=useState<ManualPayment|null>(null);
  const [action,setAction]=useState<"APPROVE"|"REJECT"|null>(null);
  const [message,setMessage]=useState("");

  const load=()=>{setLoading(true);manualPayments(status).then(setItems).catch((e:unknown)=>setError(getErrorMessage(e,"Failed to load queue"))).finally(()=>setLoading(false));};
  useEffect(load,[status]);

  async function submit(){
    if(!selected||!action)return;
    try{
      if(action==="APPROVE") await approveManualPayment(selected.id,message);
      else await rejectManualPayment(selected.id,message);
      setSelected(null);setAction(null);setMessage("");load();
    }catch(e:unknown){setError(getErrorMessage(e,"Action failed"));}
  }

  return <div className="mq">
    <style>{styles}</style>
    <div className="mq-head">
      <div><span className="mq-eyebrow">MANUAL PAYMENT</span><h1>Verification queue</h1>
        <p>Review local MFS payment requests before unlocking access.</p></div>
      <select value={status} onChange={e=>setStatus(e.target.value)}>
        <option value="PENDING">PENDING</option><option value="APPROVED">APPROVED</option><option value="REJECTED">REJECTED</option>
      </select>
    </div>
    {error&&<div className="mq-error">{error}</div>}
    <div className="mq-card">
      {loading?<p>Loading queue…</p>:items.length===0?<p className="empty">No {status.toLowerCase()} requests.</p>:
      <div className="table-wrap"><table><thead><tr>
        <th>User</th><th>Package</th><th>Amount</th><th>Method</th><th>Sender</th><th>TrxID</th><th>Applied</th><th>Status</th><th>Action</th>
      </tr></thead><tbody>{items.map(x=><tr key={x.id}>
        <td><b>{x.userName||"—"}</b><small>{x.userEmail}</small></td>
        <td>{x.planName}<small>{x.referenceCode}</small></td>
        <td>{money(x.expectedAmountMinor,x.currency)}</td><td>{x.paymentMethod}</td><td>{x.senderMobile}</td>
        <td><code>{x.transactionId}</code><button className="copy" onClick={()=>navigator.clipboard?.writeText(x.transactionId)}>Copy</button></td>
        <td>{new Date(x.appliedAt).toLocaleString()}</td>
        <td><span className={`badge ${x.status.toLowerCase()}`}>{x.status}</span></td>
        <td>{x.status==="PENDING"&&<div className="actions">
          <button className="approve" onClick={()=>{setSelected(x);setAction("APPROVE");setMessage("Payment verified, access unlocked!");}}>Approve</button>
          <button className="reject" onClick={()=>{setSelected(x);setAction("REJECT");setMessage("Invalid TrxID / Payment not received / Amount mismatch");}}>Reject</button>
        </div>}</td>
      </tr>)}</tbody></table></div>}
    </div>
    {selected&&action&&<div className="overlay"><div className="modal">
      <h2>{action==="APPROVE"?"Approve payment":"Reject payment"}</h2>
      <p><b>{selected.planName}</b> · {selected.referenceCode} · {money(selected.expectedAmountMinor,selected.currency)}</p>
      <label>Admin message<textarea value={message} maxLength={500} onChange={e=>setMessage(e.target.value)}/></label>
      <div className="modal-actions"><button className="cancel" onClick={()=>{setSelected(null);setAction(null)}}>Cancel</button>
        <button className={action==="APPROVE"?"approve":"reject"} onClick={submit}>{action==="APPROVE"?"Approve & Unlock":"Reject request"}</button></div>
    </div></div>}
  </div>
}
const styles=`
.mq{max-width:1450px;margin:auto;padding:28px 24px;color:#0f172a}.mq-head{display:flex;justify-content:space-between;gap:18px;align-items:flex-end;margin-bottom:20px}.mq-eyebrow{font-size:11px;font-weight:800;letter-spacing:.1em;color:#0f8f87}.mq h1{margin:6px 0;font-size:28px}.mq-head p{margin:0;color:#64748b}.mq-head select{padding:10px 12px;border:1px solid #d8e0ea;border-radius:9px}.mq-card{background:#fff;border:1px solid #e2e8f0;border-radius:15px;overflow:auto}.table-wrap{overflow:auto}table{border-collapse:collapse;width:100%;min-width:1120px}th,td{padding:13px 12px;border-bottom:1px solid #edf1f5;text-align:left;vertical-align:top;font-size:12px}th{font-size:11px;text-transform:uppercase;letter-spacing:.05em;color:#64748b;background:#f8fafc;white-space:nowrap}td small{display:block;color:#718096;margin-top:4px}.copy{margin-left:7px;padding:4px 7px;border:1px solid #d8e0ea;background:#fff;border-radius:6px;font-size:10px}.badge{padding:4px 8px;border-radius:999px;font-size:10px;font-weight:800}.badge.pending{background:#fff6dc;color:#8a5a00}.badge.approved{background:#e9fbf3;color:#08764e}.badge.rejected{background:#fff0f0;color:#a12626}.actions{display:flex;gap:6px}.approve,.reject,.cancel{border:0;border-radius:8px;padding:7px 10px;font-weight:700;cursor:pointer}.approve{background:#0f766e;color:#fff}.reject{background:#b42318;color:#fff}.cancel{background:#eef2f7;color:#334155}.mq-error{background:#fff0f0;border:1px solid #fecaca;color:#991b1b;padding:11px;border-radius:9px;margin-bottom:15px}.empty{padding:30px;color:#64748b}.overlay{position:fixed;inset:0;background:rgba(15,23,42,.42);display:grid;place-items:center;padding:20px}.modal{background:#fff;width:min(520px,100%);border-radius:16px;padding:22px;box-shadow:0 20px 70px rgba(0,0,0,.18)}.modal h2{margin:0 0 8px}.modal p{color:#64748b}.modal label{display:block;font-size:12px;font-weight:700}.modal textarea{display:block;width:100%;min-height:110px;margin-top:7px;border:1px solid #d8e0ea;border-radius:9px;padding:10px;font:inherit}.modal-actions{display:flex;justify-content:flex-end;gap:9px;margin-top:15px}
`