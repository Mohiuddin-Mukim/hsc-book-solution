import {Navigate,Outlet} from 'react-router-dom'; import {authStore} from './authStore';
export default function ProtectedRoute(){return authStore.getToken()&&!authStore.isExpired()?<Outlet/>:<Navigate to="/login" replace/>;}
