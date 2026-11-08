import type {AuthUser,LoginResponse} from './types';
let accessToken:string|null=null; let user:AuthUser|null=null; let expiresAt=0;
export const authStore={
 getToken:()=>accessToken,
 getUser:()=>user,
 setLogin:(r:LoginResponse)=>{accessToken=r.accessToken;expiresAt=Date.parse(r.expiresAt);user={userId:r.userId,email:r.email,displayName:r.email,roles:r.roles,permissions:r.permissions};},
 clear:()=>{accessToken=null;user=null;expiresAt=0;},
 isExpired:()=>!accessToken||Date.now()>=expiresAt-30000,
 has:(permission:string)=>user?.permissions.includes(permission)||user?.roles.includes('SUPER_ADMIN')||false,
};
