import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { authStore } from "../auth/authStore";
import { logout } from "../api/http";

const items = [
  { to: "/", label: "Dashboard" },
  { to: "/content", label: "Content Library", perm: "CONTENT_READ" },
  { to: "/questions/new", label: "Question Editor", perm: "CONTENT_CREATE" },
  { to: "/media", label: "Media Library", perm: "MEDIA_MANAGE" },
  { to: "/review", label: "Review Queue", perm: "CONTENT_REVIEW" },
  { to: "/imports", label: "Imports", perm: "CONTENT_CREATE" },
  { to: "/users", label: "Users & Devices", perm: "USER_READ" },
  { to: "/payments", label: "Plans & Payments", perm: "ENTITLEMENT_ADJUST" },
  { to: "/payments/manual", label: "Manual Payment Queue", perm: "ENTITLEMENT_ADJUST" },
  { to: "/payments/manual-settings", label: "MFS Payment Settings", perm: "ENTITLEMENT_ADJUST" },
  { to: "/analytics", label: "Analytics", perm: "ANALYTICS_READ" },
  { to: "/settings", label: "Settings", perm: "ROLE_MANAGE" },
];
export default function AdminLayout() {
  const u = authStore.getUser();
  const nav = useNavigate();
  return (
    <div className="admin-shell">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">H</span>
          <div>
            <strong>HSC Academic</strong>
            <small>Editorial Workspace</small>
          </div>
        </div>
        <nav>
          {items
            .filter((x) => !x.perm || authStore.has(x.perm))
            .map((x) => (
              <NavLink key={x.to} to={x.to} end={x.to === "/"}>
                {x.label}
              </NavLink>
            ))}
        </nav>
        <div className="sidebar-bottom">
          <span>{u?.roles.join(", ")}</span>
          <button
            onClick={async () => {
              await logout();
              nav("/login");
            }}
          >
            Sign out
          </button>
        </div>
      </aside>
      <section className="workspace">
        <header className="topbar">
          <div>
            <span className="eyebrow">ADMIN</span>
            <strong>{u?.displayName ?? u?.email}</strong>
          </div>
          <div className="status-dot">● Online</div>
        </header>
        <main className="content">
          <Outlet />
        </main>
      </section>
    </div>
  );
}
