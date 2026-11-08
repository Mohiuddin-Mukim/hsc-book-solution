export type UUID = string;
export interface PageResponse<T>{items:T[];page:number;size:number;totalItems:number;totalPages:number}
export interface ApiError{code:string;message:string;correlationId:string;timestamp:string;details?:Record<string,unknown>}
export interface SubjectSummary{id:UUID;code:string;name:string;displayOrder:number;isActive:boolean}
export type AuthPermission='USER_READ'|'USER_MANAGE'|'ROLE_MANAGE'|'CONTENT_READ'|'CONTENT_CREATE'|'CONTENT_EDIT'|'CONTENT_REVIEW'|'CONTENT_PUBLISH'|'MEDIA_MANAGE'|'ENTITLEMENT_ADJUST'|'DEVICE_RESET'|'AUDIT_READ'|'ANALYTICS_READ'|'SUPPORT_MANAGE';
