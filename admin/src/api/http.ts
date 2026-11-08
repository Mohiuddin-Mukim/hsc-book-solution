import { authStore } from "../auth/authStore";
import type { LoginResponse, AuthUser } from "../auth/types";
const API = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
async function raw(path: string, init: RequestInit = {}, auth = true) {
  const h = new Headers(init.headers);
  h.set("Content-Type", "application/json");
  if (auth && authStore.getToken())
    h.set("Authorization", `Bearer ${authStore.getToken()}`);
  h.set("X-Client-Type", "ADMIN_WEB");
  const r = await fetch(`${API}${path}`, {
    ...init,
    headers: h,
    credentials: "include",
  });
  if (r.status === 401 && auth) {
    const rr = await fetch(`${API}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-Client-Type": "ADMIN_WEB",
      },
      credentials: "include",
    });
    if (rr.ok) {
      const x: LoginResponse = await rr.json();
      authStore.setLogin(x);
      return raw(path, init, false);
    }
    authStore.clear();
    location.href = "/login";
  }
  if (!r.ok) {
    const e = await r.json().catch(() => ({ message: "Request failed" }));
    throw new Error(e.message ?? "Request failed");
  }
  return r;
}
export async function login(email: string, password: string) {
  const r = await raw(
    "/auth/login",
    { method: "POST", body: JSON.stringify({ email, password }) },
    false,
  );
  const x: LoginResponse = await r.json();
  authStore.setLogin(x);
  return x;
}
export async function logout() {
  await raw("/auth/logout", { method: "POST", body: "{}" }, true).catch(
    () => {},
  );
  authStore.clear();
}
export async function me() {
  const r = await raw("/auth/me");
  return r.json() as Promise<AuthUser>;
}
