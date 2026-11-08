export type AuthUser={userId:string;email:string;displayName:string;roles:string[];permissions:string[]};
export type LoginResponse={accessToken:string;refreshToken?:string|null;expiresAt:string;userId:string;email:string;roles:string[];permissions:string[]};
