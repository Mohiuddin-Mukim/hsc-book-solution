import {authStore} from "../auth/authStore";
import type {MediaAssetResponse, ReorderItem} from "./mediaContracts";

const API = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

async function req(path:string, init:RequestInit={}) {
  const h = new Headers(init.headers);
  h.set("X-Client-Type","ADMIN_WEB");
  const t=authStore.getToken(); if(t) h.set("Authorization",`Bearer ${t}`);
  const r=await fetch(`${API}${path}`,{...init,headers:h,credentials:"include"});
  if(!r.ok) throw new Error((await r.json().catch(()=>({message:"Request failed"}))).message ?? "Request failed");
  return r;
}

export const mediaApi={
  upload:(file:File,caption?:string,altText?:string,onProgress?:(percent:number)=>void)=>new Promise<MediaAssetResponse>((resolve,reject)=>{
    const xhr=new XMLHttpRequest(); xhr.open("POST",`${API}/admin/media/upload`);
    xhr.withCredentials=true; xhr.setRequestHeader("X-Client-Type","ADMIN_WEB");
    const token=authStore.getToken(); if(token)xhr.setRequestHeader("Authorization",`Bearer ${token}`);
    xhr.upload.onprogress=e=>{if(e.lengthComputable)onProgress?.(Math.round(e.loaded/e.total*100));};
    xhr.onerror=()=>reject(new Error("Upload failed"));
    xhr.onload=()=>{try{const data=JSON.parse(xhr.responseText);if(xhr.status>=200&&xhr.status<300)resolve(data);else reject(new Error(data.message??"Upload failed"));}catch{reject(new Error("Invalid upload response"));}};
    const fd=new FormData();fd.append("file",file);if(caption)fd.append("caption",caption);if(altText)fd.append("altText",altText);xhr.send(fd);
  }),
  attachQuestion:(versionId:string, body:unknown)=>req(`/admin/question-versions/${versionId}/media`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)}).then(r=>r.json() as Promise<MediaAssetResponse>),
  attachStep:(stepId:string, body:unknown)=>req(`/admin/solution-steps/${stepId}/media`,{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body)}).then(r=>r.json() as Promise<MediaAssetResponse>),
  reorderQuestion:(versionId:string,items:ReorderItem[])=>req(`/admin/question-versions/${versionId}/media/reorder`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(items)}),
  reorderStep:(stepId:string,items:ReorderItem[])=>req(`/admin/solution-steps/${stepId}/media/reorder`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(items)}),
  detachQuestion:(versionId:string,assetId:string)=>req(`/admin/question-versions/${versionId}/media/${assetId}`,{method:"DELETE"}),
  detachStep:(stepId:string,assetId:string)=>req(`/admin/solution-steps/${stepId}/media/${assetId}`,{method:"DELETE"})
};
