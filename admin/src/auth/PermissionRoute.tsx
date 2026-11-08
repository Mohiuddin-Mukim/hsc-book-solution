import {Navigate,Outlet} from 'react-router-dom'; import {authStore} from './authStore';
export default function PermissionRoute({permission}:{permission:string}){return authStore.has(permission)?<Outlet/>:<Navigate to="/forbidden" replace/>;}
