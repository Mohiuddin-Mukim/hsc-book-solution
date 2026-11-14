import {useState, type ChangeEvent} from "react";
import {mediaApi} from "../api/media";
import type {MediaAssetResponse} from "../api/mediaContracts";

type Props={
  targetType:"questionVersion"|"solutionStep";
  targetId:string;
  existing?:MediaAssetResponse[];
  onChanged?:(items:MediaAssetResponse[])=>void;
};

export default function MediaUploader({targetType,targetId,existing=[],onChanged}:Props){
  const [items,setItems]=useState(existing);
  const [progress,setProgress]=useState(0);
  const [busy,setBusy]=useState(false);
  const [error,setError]=useState("");

  async function upload(file:File){
    setError("");setBusy(true);setProgress(0);
    try{
      const asset=await mediaApi.upload(file,undefined,file.name,setProgress);
      const attached=targetType==="questionVersion"
        ? await mediaApi.attachQuestion(targetId,{assetId:asset.id,role:"ILLUSTRATION",displayOrder:items.length})
        : await mediaApi.attachStep(targetId,{assetId:asset.id,role:"ILLUSTRATION",displayOrder:items.length});
      const next=[...items,attached];setItems(next);onChanged?.(next);
    }catch(e){setError(e instanceof Error?e.message:"Upload failed");}
    finally{setBusy(false);}
  }

  async function move(index:number,delta:number){
    const next=[...items];const to=index+delta;if(to<0||to>=next.length)return;
    [next[index],next[to]]=[next[to],next[index]];
    const payload=next.map((x,i)=>({assetId:x.id,displayOrder:i}));
    if(targetType==="questionVersion")await mediaApi.reorderQuestion(targetId,payload);
    else await mediaApi.reorderStep(targetId,payload);
    setItems(next);onChanged?.(next);
  }

  async function remove(assetId:string){
    if(targetType==="questionVersion")await mediaApi.detachQuestion(targetId,assetId);
    else await mediaApi.detachStep(targetId,assetId);
    const next=items.filter(x=>x.id!==assetId).map((x,i)=>({...x,order:i}));
    setItems(next);onChanged?.(next);
  }

  return <section className="media-uploader">
    <h3>Figures & graphs</h3>
    <input type="file" accept="image/jpeg,image/png,image/webp" disabled={busy}
      onChange={(e:ChangeEvent<HTMLInputElement>)=>{const f=e.target.files?.[0];if(f)void upload(f);e.currentTarget.value="";}}/>
    {busy&&<div>Uploading… {progress}%</div>}
    {error&&<div role="alert" className="error">{error}</div>}
    <div className="media-grid">
      {items.map((x,i)=><article key={x.id} className="media-card">
        <img src={x.deliveryUrl} alt={x.altText??x.caption??"Figure preview"} />
        <div>{x.role} · {x.width}×{x.height}</div>
        <div className="row">
          <button type="button" onClick={()=>void move(i,-1)} disabled={i===0}>↑</button>
          <button type="button" onClick={()=>void move(i,1)} disabled={i===items.length-1}>↓</button>
          <button type="button" onClick={()=>void remove(x.id)}>Remove</button>
        </div>
      </article>)}
    </div>
  </section>;
}
