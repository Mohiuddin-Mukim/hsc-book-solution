export type UUID = string;
export interface PageResponse<T>{items:T[];page:number;size:number;totalItems:number;totalPages:number}
export interface ApiError{code:string;message:string;correlationId:string;timestamp:string;details?:Record<string,unknown>}
export interface SubjectSummary{id:UUID;code:string;name:string;displayOrder:number;isActive:boolean}
