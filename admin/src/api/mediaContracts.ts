export interface MediaAssetResponse {
  id:string; role:string; order:number; caption?:string|null; altText?:string|null;
  width:number; height:number; byteSize:number; mimeType:string; checksumSha256:string;
  lifecycleState:string; deliveryUrl:string; usageCount:number;
}
export interface ReorderItem {assetId:string;displayOrder:number;}
