
import { useEffect, useMemo, useState } from "react";
import { importApi } from "../api/imports";
import type { ImportMode, ImportPreview } from "../api/importContracts";
import { authStore } from "../auth/authStore";

const sample = JSON.stringify({
  schemaVersion:"1.0",
  items:[{
    externalId:"PHYS-CH01-001",
    chapterId:"REPLACE_WITH_CHAPTER_UUID",
    languageCode:"bn", questionType:"CQ", difficulty:"MEDIUM",
    stem:"নিউটনের দ্বিতীয় সূত্রটি ব্যাখ্যা করুন। $F=ma$",
    marks:5, finalAnswer:"F = ma",
    tags:["newton-laws"],
    solution:{languageCode:"bn",summary:"বল নির্ণয়।",steps:[
      {stepNo:1,title:"সূত্র",body:"$F=ma$"},
      {stepNo:2,title:"ব্যাখ্যা",body:"ভর ও ত্বরণ ব্যবহার করে বল নির্ণয় করা হয়।"}
    ]}
  }]
},null,2);

export default function ImportsPage(){
  const [mode,setMode]=useState<ImportMode>("CREATE_ONLY");
  const [text,setText]=useState(sample); const [file,setFile]=useState<File|null>(null);
  const [idempotencyKey,setIdempotencyKey]=useState(""); const [dryRun,setDryRun]=useState(false);
  const [preview,setPreview]=useState<ImportPreview|null>(null); const [error,setError]=useState(""); const [busy,setBusy]=useState(false);
  const [submitForReview,setSubmitForReview]=useState(false);
  const [tab,setTab]=useState<"paste"|"upload">("paste");

  async function stage(){
    setError("");setBusy(true);
    try{const job=tab==="paste"?await importApi.paste(text,mode,dryRun,idempotencyKey||undefined):await importApi.upload(file!,mode,dryRun,idempotencyKey||undefined);
      setPreview(await importApi.preview(job.id));
    }catch(e){setError(e instanceof Error?e.message:"Import failed");}finally{setBusy(false);}
  }
  async function refresh(){if(preview)setPreview(await importApi.preview(preview.job.id));}
  async function commit(){if(!preview)return;setError("");setBusy(true);try{await importApi.commit(preview.job.id,submitForReview);await refresh();}catch(e){setError(e instanceof Error?e.message:"Commit failed");}finally{setBusy(false);}}
  async function retry(){if(!preview)return;setBusy(true);try{await importApi.retry(preview.job.id,submitForReview);await refresh();}catch(e){setError(e instanceof Error?e.message:"Retry failed");}finally{setBusy(false);}}
  const canStage=tab==="paste"?text.trim().length>0:!!file;
  const rows=preview?.rows??[];
  return <div>
    <div className="page-head"><div><span className="eyebrow">INGESTION</span><h1>Bulk Import</h1><p>Stage AI-generated content safely before creating drafts or review tasks.</p></div></div>
    {error&&<div className="error-banner">{error}</div>}
    <div className="panel">
      <div className="actions">
        <button className={tab==="paste"?"primary":"ghost"} onClick={()=>setTab("paste")}>Paste JSON</button>
        <button className={tab==="upload"?"primary":"ghost"} onClick={()=>setTab("upload")}>Upload pack</button>
      </div>
      <div className="filters three">
        <label>Mode<select value={mode} onChange={e=>setMode(e.target.value as ImportMode)}>
          <option>CREATE_ONLY</option><option>UPDATE_AS_NEW_VERSION</option><option>SKIP_DUPLICATES</option>
        </select></label>
        <label>Idempotency key<input value={idempotencyKey} onChange={e=>setIdempotencyKey(e.target.value)} placeholder="same key = safe retry"/></label>
        <label><span>Dry run</span><input type="checkbox" checked={dryRun} onChange={e=>setDryRun(e.target.checked)}/></label>
      </div>
      {tab==="paste"?<textarea className="large" value={text} onChange={e=>setText(e.target.value)}/>:<input type="file" accept=".json,.csv,.xlsx,.zip" onChange={e=>setFile(e.target.files?.[0]??null)}/>}
      <div className="actions"><button className="primary" disabled={!canStage||busy} onClick={stage}>{busy?"Working…":"Validate & stage"}</button>
      <span className="muted">Schema: hsc-academic-import.schema.json</span></div>
    </div>

    {preview&&<div className="panel">
      <div className="panel-title"><div><h2>Job {preview.job.id.slice(0,8)}…</h2><span>{preview.job.status} · {preview.job.totalRows} rows · {preview.warningCount} warnings</span></div>
      <div className="actions"><button className="ghost" onClick={refresh}>Refresh</button><button className="ghost" onClick={()=>window.open(importApi.errorCsvUrl(preview.job.id),"_blank")}>Error CSV</button>
      {preview.job.errorRows>0&&<button className="ghost" onClick={retry}>Retry failed</button>}
      <label className="muted"><input type="checkbox" checked={submitForReview} onChange={e=>setSubmitForReview(e.target.checked)}/> Submit valid rows to review</label>
      <button className="primary" disabled={dryRun||busy||preview.job.status==="COMMITTED"} onClick={commit}>Commit</button></div></div>
      <div className="table">{rows.map(r=><div className="table-row" key={r.id} style={{gridTemplateColumns:"80px 1fr 140px 2fr"}}>
        <span>#{r.rowNo}</span><span className="mono">{r.externalId}</span><span><span className={`badge ${r.status.toLowerCase()}`}>{r.status}</span></span>
        <span>{r.errors.map((e,i)=><div key={i} className={e.severity==="WARNING"?"warning":"error-banner"}>{e.fieldName?`${e.fieldName}: `:""}{e.message}</div>)}</span>
      </div>)}</div>
    </div>}
    <div className="panel"><div className="panel-title"><div><h2>Canonical JSON</h2><span>Use the versioned schema shipped with this phase.</span></div></div>
      <pre className="preview-box">{sample}</pre>
    </div>
  </div>;
}
