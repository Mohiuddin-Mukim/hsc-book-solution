import { BrowserRouter, Routes, Route } from "react-router-dom";
import LoginPage from "./pages/LoginPage";
import ForbiddenPage from "./pages/ForbiddenPage";
import ProtectedRoute from "./auth/ProtectedRoute";
import PermissionRoute from "./auth/PermissionRoute";
import AdminLayout from "./components/AdminLayout";
import DashboardPage from "./pages/DashboardPage";
import ContentLibraryPage from "./pages/ContentLibraryPage";
import QuestionEditorPage from "./pages/QuestionEditorPage";
import ReviewQueuePage from "./pages/ReviewQueuePage";
import MediaLibraryPage from "./pages/MediaLibraryPage";
import GenericAdminPage from "./pages/GenericAdminPage";
import CatalogPage from "./pages/CatalogPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/forbidden" element={<ForbiddenPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AdminLayout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route element={<PermissionRoute permission="CONTENT_READ" />}>
              <Route path="/content" element={<ContentLibraryPage />} />
            </Route>
            <Route element={<PermissionRoute permission="CONTENT_READ" />}>
              <Route path="/catalog" element={<CatalogPage />} />
            </Route>
            <Route element={<PermissionRoute permission="CONTENT_CREATE" />}>
              <Route path="/questions/new" element={<QuestionEditorPage />} />
            </Route>
            <Route element={<PermissionRoute permission="CONTENT_READ" />}>
              <Route path="/questions/:id" element={<QuestionEditorPage />} />
            </Route>
            <Route element={<PermissionRoute permission="MEDIA_MANAGE" />}>
              <Route path="/media" element={<MediaLibraryPage />} />
            </Route>
            <Route element={<PermissionRoute permission="CONTENT_REVIEW" />}>
              <Route path="/review" element={<ReviewQueuePage />} />
            </Route>
            <Route element={<PermissionRoute permission="CONTENT_CREATE" />}>
              <Route
                path="/imports"
                element={
                  <GenericAdminPage
                    title="Imports"
                    eyebrow="INGESTION"
                    description="Bulk JSON, CSV and media-pack import will plug into this workspace."
                    items={[
                      "Import jobs",
                      "Validation errors",
                      "Preview & stage",
                    ]}
                  />
                }
              />
            </Route>
            <Route element={<PermissionRoute permission="USER_READ" />}>
              <Route
                path="/users"
                element={
                  <GenericAdminPage
                    title="Users & Devices"
                    eyebrow="ACCESS"
                    description="Manage editorial accounts, roles and device support workflows."
                    items={["Users", "Roles", "Devices", "Audit events"]}
                  />
                }
              />
            </Route>
            <Route
              element={<PermissionRoute permission="ENTITLEMENT_ADJUST" />}
            >
              <Route
                path="/payments"
                element={
                  <GenericAdminPage
                    title="Payments"
                    eyebrow="COMMERCE"
                    description="Plans, orders, subscriptions and reconciliation."
                    items={[
                      "Plans",
                      "Orders",
                      "Subscriptions",
                      "Reconciliation",
                    ]}
                  />
                }
              />
            </Route>
            <Route element={<PermissionRoute permission="ANALYTICS_READ" />}>
              <Route
                path="/analytics"
                element={
                  <GenericAdminPage
                    title="Analytics"
                    eyebrow="INSIGHTS"
                    description="Editorial and product reporting surface."
                    items={["Content throughput", "Review SLA", "Usage"]}
                  />
                }
              />
            </Route>
            <Route element={<PermissionRoute permission="ROLE_MANAGE" />}>
              <Route
                path="/settings"
                element={
                  <GenericAdminPage
                    title="Settings"
                    eyebrow="SYSTEM"
                    description="Role-aware configuration and operational controls."
                    items={["Permissions", "Feature flags", "System health"]}
                  />
                }
              />
            </Route>
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
