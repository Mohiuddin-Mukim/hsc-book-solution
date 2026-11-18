
import { authStore } from "../auth/authStore";
import type { CommitResult, ImportMode, ImportPreview, ImportJob } from "./importContracts";

const API = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";
async function req(path: string, init: RequestInit = {}) {
  const h = new Headers(init.headers);
  if (!(init.body instanceof FormData)) h.set("Content-Type", "application/json");
  h.set("X-Client-Type", "ADMIN_WEB");
  const t = authStore.getToken(); if (t) h.set("Authorization", `Bearer ${t}`);
  const r = await fetch(`${API}${path}`, {...init, headers:h, credentials:"include"});
  if (!r.ok) throw new Error((await r.json().catch(()=>({message:"Request failed"}))).message ?? "Request failed");
  return r;
}
export const importApi = {
  paste: (content:string, mode:ImportMode, dryRun:boolean, idempotencyKey?:string) =>
    req("/admin/imports/json",{method:"POST",body:JSON.stringify({content,mode,dryRun,idempotencyKey,fileName:"pasted-content.json"})}).then(r=>r.json() as Promise<ImportJob>),
  upload: (file:File, mode:ImportMode, dryRun:boolean, idempotencyKey?:string) => {
    const f=new FormData(); f.append("file",file); f.append("mode",mode); f.append("dryRun",String(dryRun));
    if(idempotencyKey) f.append("idempotencyKey",idempotencyKey);
    return req("/admin/imports/upload",{method:"POST",body:f}).then(r=>r.json() as Promise<ImportJob>);
  },
  preview: (id:string)=>req(`/admin/imports/${id}`).then(r=>r.json() as Promise<ImportPreview>),
  commit: (id:string,submitForReview:boolean)=>req(`/admin/imports/${id}/commit?submitForReview=${submitForReview}`,{method:"POST",body:"{}"}).then(r=>r.json() as Promise<CommitResult>),
  retry: (id:string,submitForReview:boolean)=>req(`/admin/imports/${id}/retry?submitForReview=${submitForReview}`,{method:"POST",body:"{}"}).then(r=>r.json() as Promise<CommitResult>),
  errorCsvUrl: (id:string)=>`${API}/admin/imports/${id}/errors.csv`,
};
