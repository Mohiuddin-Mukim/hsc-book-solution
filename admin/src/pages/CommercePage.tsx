import { useEffect, useRef, useState } from "react";
import { adminPlans, createPlan, updatePlan, type Plan } from "../api/commerce";

const money = (n: number) => `৳${((Number(n) || 0) / 100).toFixed(2)}`;

const INITIAL_FORM = {
  code: "",
  name: "",
  planType: "CHAPTER",
  resourceType: "CHAPTER",
  resourceId: "",
  originalPriceMinor: 5000,
  salePriceMinor: 2500,
  durationDays: 30,
  currency: "BDT",
  description: "",
  active: true,
  bookIds: [] as string[],
  freeBookId: "",
  freeChapterId: "",
};

export default function CommercePage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  const [editing, setEditing] = useState<Plan | null>(null);
  const [msg, setMsg] = useState("");
  const [msgType, setMsgType] = useState<"success" | "error">("success");
  const [saving, setSaving] = useState(false);
  const [savingFree, setSavingFree] = useState(false);
  const [f, setF] = useState<any>(INITIAL_FORM);

  const formRef = useRef<HTMLDivElement | null>(null);

  const notify = (text: string, type: "success" | "error" = "success") => {
    setMsg(text);
    setMsgType(type);
  };

  const load = () =>
    adminPlans()
      .then(setPlans)
      .catch((e) => notify(e.message, "error"));

  useEffect(() => {
    load();
  }, []);

  const discount =
    f.originalPriceMinor > 0
      ? Math.round((1 - f.salePriceMinor / f.originalPriceMinor) * 10000) / 100
      : 0;

  const resetOfferForm = () => {
    setEditing(null);
    setF((prev: any) => ({
      ...INITIAL_FORM,
      freeBookId: prev.freeBookId || "",
      freeChapterId: prev.freeChapterId || "",
    }));
  };

  const save = async () => {
    setSaving(true);
    try {
      const body = {
        ...f,
        resourceId: f.planType === "PACKAGE" ? null : f.resourceId || null,
        bookIds: f.planType === "PACKAGE" ? f.bookIds : [],
      };
      if (editing) await updatePlan(editing.id, body);
      else await createPlan(body);
      notify(editing ? "Offer updated successfully" : "Saved successfully", "success");
      resetOfferForm();
      load();
    } catch (e: any) {
      notify(e.message || "Failed to save offer", "error");
    } finally {
      setSaving(false);
    }
  };

  const handleSetFreeChapter = async () => {
    setSavingFree(true);
    try {
      await (await import("../api/commerce")).setFreeChapter(
        f.freeBookId,
        f.freeChapterId
      );
      notify("Free chapter saved", "success");
    } catch (e: any) {
      notify(e.message || "Failed to set free chapter", "error");
    } finally {
      setSavingFree(false);
    }
  };

  const startEdit = (p: Plan) => {
    setEditing(p);
    setF((prev: any) => ({
      ...prev,
      ...p,
      bookIds: p.bookIds || [],
    }));
    formRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
  };

  return (
    <div className="cp-root">
      <style>{styles}</style>

      {/* Page Header */}
      <header className="cp-header">
        <div>
          <span className="cp-eyebrow">COMMERCE</span>
          <h1 className="cp-title">Plans &amp; pricing</h1>
          <p className="cp-subtitle">
            Set chapter/book prices, package offers, and automatically calculated discounts.
          </p>
        </div>
        <div className="cp-header-meta">
          <div className="cp-stat-pill">
            <span className="cp-stat-dot" />
            <span>
              <strong>{plans.length}</strong> {plans.length === 1 ? "Plan" : "Plans"} configured
            </span>
          </div>
          {editing && (
            <button type="button" className="cp-btn cp-btn-ghost" onClick={resetOfferForm}>
              + New offer
            </button>
          )}
        </div>
      </header>

      {/* Toast / Feedback Banner */}
      {msg && (
        <div className={`cp-alert ${msgType === "error" ? "cp-alert-error" : "cp-alert-success"}`}>
          <div className="cp-alert-body">
            <span className="cp-alert-icon">{msgType === "error" ? "!" : "✓"}</span>
            <span>{msg}</span>
          </div>
          <button
            type="button"
            className="cp-alert-close"
            onClick={() => setMsg("")}
            aria-label="Dismiss message"
          >
            ×
          </button>
        </div>
      )}

      {/* Active Plans Grid */}
      <section className="cp-section">
        <div className="cp-section-head">
          <h2 className="cp-section-title">Existing plans</h2>
          <span className="cp-muted">{plans.length} total</span>
        </div>

        {plans.length === 0 ? (
          <div className="cp-empty">
            <div className="cp-empty-icon">৳</div>
            <div>
              <strong>No pricing plans yet</strong>
              <p>Create your first chapter, book, or package offer below.</p>
            </div>
          </div>
        ) : (
          <div className="cp-plan-grid">
            {plans.map((p) => {
              const isSelected = editing?.id === p.id;
              return (
                <article
                  className={`cp-plan-card ${isSelected ? "cp-plan-card-active" : ""}`}
                  key={p.id}
                >
                  <div className="cp-plan-top">
                    <span className={`cp-badge cp-badge-${p.planType.toLowerCase()}`}>
                      {p.planType}
                    </span>
                    {p.discountPercent > 0 && (
                      <span className="cp-discount-pill">{p.discountPercent}% OFF</span>
                    )}
                  </div>

                  <h3 className="cp-plan-name">{p.name}</h3>
                  {(p as any).code && <div className="cp-plan-code">{(p as any).code}</div>}

                  <div className="cp-plan-price">
                    <span className="cp-price-sale">{money(p.salePriceMinor)}</span>
                    {p.originalPriceMinor > p.salePriceMinor && (
                      <s className="cp-price-orig">{money(p.originalPriceMinor)}</s>
                    )}
                  </div>

                  <div className="cp-plan-footer">
                    <span className="cp-muted">
                      {(p as any).durationDays ? `${(p as any).durationDays} days` : "Standard"}
                    </span>
                    <button
                      type="button"
                      className="cp-btn cp-btn-secondary cp-btn-sm"
                      onClick={() => startEdit(p)}
                    >
                      {isSelected ? "Editing…" : "Edit"}
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      {/* Main 2-Column Workspace */}
      <div className="cp-workspace" ref={formRef}>
        {/* Left: Create / Edit Offer Form */}
        <section className="cp-card">
          <div className="cp-card-head">
            <div>
              <h2 className="cp-card-title">{editing ? "Edit offer" : "Create offer"}</h2>
              <p className="cp-card-desc">
                {editing
                  ? `Updating "${editing.name}". Changes take effect immediately upon saving.`
                  : "Configure pricing, validity duration, and target resources."}
              </p>
            </div>
            {editing && <span className="cp-editing-tag">Editing Mode</span>}
          </div>

          <div className="cp-form-body">
            {/* Row 1: Identity */}
            <div className="cp-grid-2">
              <label className="cp-field">
                <span className="cp-label">Plan code</span>
                <input
                  className="cp-input"
                  placeholder="e.g. PHY1-CH01"
                  value={f.code}
                  onChange={(e) => setF({ ...f, code: e.target.value })}
                />
              </label>

              <label className="cp-field">
                <span className="cp-label">Display name</span>
                <input
                  className="cp-input"
                  placeholder="e.g. Physics 1st Paper — Vector"
                  value={f.name}
                  onChange={(e) => setF({ ...f, name: e.target.value })}
                />
              </label>
            </div>

            {/* Row 2: Plan Type & Target Resource */}
            <div className="cp-grid-2">
              <label className="cp-field">
                <span className="cp-label">Plan type</span>
                <select
                  className="cp-input cp-select"
                  value={f.planType}
                  onChange={(e) =>
                    setF({
                      ...f,
                      planType: e.target.value,
                      resourceType: e.target.value === "PACKAGE" ? null : e.target.value,
                    })
                  }
                >
                  <option value="CHAPTER">CHAPTER</option>
                  <option value="BOOK">BOOK</option>
                  <option value="PACKAGE">PACKAGE</option>
                </select>
              </label>

              <label className="cp-field">
                <span className="cp-label">Resource UUID (chapter / book)</span>
                <input
                  className="cp-input"
                  placeholder={
                    f.planType === "PACKAGE"
                      ? "Not required for packages"
                      : "Paste chapter or book UUID"
                  }
                  disabled={f.planType === "PACKAGE"}
                  value={f.resourceId || ""}
                  onChange={(e) => setF({ ...f, resourceId: e.target.value })}
                />
              </label>
            </div>

            {/* Conditional Package Book UUIDs */}
            {f.planType === "PACKAGE" && (
              <label className="cp-field">
                <span className="cp-label">Included Book UUIDs</span>
                <input
                  className="cp-input"
                  placeholder="uuid-1, uuid-2, uuid-3 (comma separated)"
                  value={(f.bookIds || []).join(",")}
                  onChange={(e) =>
                    setF({
                      ...f,
                      bookIds: e.target.value
                        .split(",")
                        .map((x: string) => x.trim())
                        .filter(Boolean),
                    })
                  }
                />
                <span className="cp-hint">
                  {(f.bookIds || []).length} book{(f.bookIds || []).length === 1 ? "" : "s"} linked
                </span>
              </label>
            )}

            <div className="cp-divider" />

            {/* Row 3: Pricing & Duration */}
            <div className="cp-grid-3">
              <label className="cp-field">
                <span className="cp-label">Original price (paisa)</span>
                <input
                  type="number"
                  min={0}
                  className="cp-input"
                  placeholder="5000"
                  value={f.originalPriceMinor}
                  onChange={(e) =>
                    setF({ ...f, originalPriceMinor: Number(e.target.value) })
                  }
                />
                <span className="cp-hint">Displays as {money(f.originalPriceMinor)}</span>
              </label>

              <label className="cp-field">
                <span className="cp-label">Sale price (paisa)</span>
                <input
                  type="number"
                  min={0}
                  className="cp-input"
                  placeholder="2500"
                  value={f.salePriceMinor}
                  onChange={(e) =>
                    setF({ ...f, salePriceMinor: Number(e.target.value) })
                  }
                />
                <span className="cp-hint">Displays as {money(f.salePriceMinor)}</span>
              </label>

              <label className="cp-field">
                <span className="cp-label">Duration (days)</span>
                <input
                  type="number"
                  min={1}
                  className="cp-input"
                  placeholder="30"
                  value={f.durationDays}
                  onChange={(e) =>
                    setF({ ...f, durationDays: Number(e.target.value) })
                  }
                />
                <span className="cp-hint">Access validity</span>
              </label>
            </div>

            {/* Row 4: Description */}
            <label className="cp-field">
              <span className="cp-label">Description</span>
              <input
                className="cp-input"
                placeholder="Short summary shown to students at checkout"
                value={f.description || ""}
                onChange={(e) => setF({ ...f, description: e.target.value })}
              />
            </label>

            {/* Active Status Toggle */}
            <label className="cp-toggle-row">
              <input
                type="checkbox"
                checked={Boolean(f.active)}
                onChange={(e) => setF({ ...f, active: e.target.checked })}
              />
              <span>
                <strong>Active offer</strong>
                <small>Make this plan available for purchase immediately</small>
              </span>
            </label>
          </div>

          {/* Form Actions */}
          <div className="cp-card-footer">
            <button
              type="button"
              className="cp-btn cp-btn-primary"
              onClick={save}
              disabled={saving}
            >
              {saving ? "Saving…" : editing ? "Update offer" : "Save offer"}
            </button>
            {editing && (
              <button
                type="button"
                className="cp-btn cp-btn-ghost"
                onClick={resetOfferForm}
              >
                Cancel
              </button>
            )}
          </div>
        </section>

        {/* Right: Live Preview + Free Chapter Tool */}
        <aside className="cp-sidebar">
          {/* Live Offer Preview */}
          <div className="cp-preview-card">
            <div className="cp-preview-top">
              <span className="cp-preview-label">LIVE OFFER PREVIEW</span>
              <span className={`cp-badge cp-badge-${(f.planType || "chapter").toLowerCase()}`}>
                {f.planType}
              </span>
            </div>

            <div className="cp-preview-name">
              {f.name || "Untitled Offer"}
            </div>
            {f.code && <div className="cp-preview-code">{f.code}</div>}

            <div className="cp-preview-price-row">
              <span className="cp-preview-sale">{money(f.salePriceMinor)}</span>
              {f.originalPriceMinor > f.salePriceMinor && (
                <s className="cp-preview-orig">{money(f.originalPriceMinor)}</s>
              )}
              {discount > 0 && (
                <span className="cp-discount-pill">{discount}% discount</span>
              )}
            </div>

            <p className="cp-preview-desc">
              {f.description || "Offer description will appear here."}
            </p>

            <div className="cp-preview-meta">
              <span>Validity: <strong>{f.durationDays || 0} days</strong></span>
              <span>Currency: <strong>{f.currency || "BDT"}</strong></span>
            </div>
          </div>

          {/* Free Chapter Utility */}
          <section className="cp-card">
            <div className="cp-card-head">
              <div>
                <h3 className="cp-card-title">Free chapter</h3>
                <p className="cp-card-desc">
                  Mark an individual chapter as free for a book.
                </p>
              </div>
            </div>

            <div className="cp-form-body">
              <label className="cp-field">
                <span className="cp-label">Book UUID</span>
                <input
                  className="cp-input"
                  placeholder="Enter book UUID"
                  value={f.freeBookId || ""}
                  onChange={(e) => setF({ ...f, freeBookId: e.target.value })}
                />
              </label>

              <label className="cp-field">
                <span className="cp-label">Chapter UUID</span>
                <input
                  className="cp-input"
                  placeholder="Enter chapter UUID"
                  value={f.freeChapterId || ""}
                  onChange={(e) => setF({ ...f, freeChapterId: e.target.value })}
                />
              </label>
            </div>

            <div className="cp-card-footer">
              <button
                type="button"
                className="cp-btn cp-btn-secondary cp-btn-full"
                onClick={handleSetFreeChapter}
                disabled={savingFree}
              >
                {savingFree ? "Updating…" : "Set free"}
              </button>
            </div>
          </section>
        </aside>
      </div>
    </div>
  );
}

const styles = `
  .cp-root {
    max-width: 1180px;
    margin: 0 auto;
    padding: 28px 24px 56px;
    color: #0f172a;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Inter, sans-serif;
  }

  /* Header */
  .cp-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    flex-wrap: wrap;
    margin-bottom: 24px;
  }
  .cp-eyebrow {
    display: inline-block;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    color: #059669;
    background: #ecfdf5;
    border: 1px solid #a7f3d0;
    padding: 3px 9px;
    border-radius: 999px;
    margin-bottom: 8px;
  }
  .cp-title {
    margin: 0;
    font-size: 28px;
    font-weight: 700;
    letter-spacing: -0.02em;
    color: #0f172a;
    line-height: 1.2;
  }
  .cp-subtitle {
    margin: 6px 0 0;
    font-size: 14px;
    color: #64748b;
  }
  .cp-header-meta {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .cp-stat-pill {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 999px;
    padding: 7px 14px;
    font-size: 13px;
    color: #334155;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }
  .cp-stat-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #10b981;
  }

  /* Alert */
  .cp-alert {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    margin-bottom: 20px;
    border: 1px solid transparent;
  }
  .cp-alert-success {
    background: #ecfdf5;
    border-color: #a7f3d0;
    color: #065f46;
  }
  .cp-alert-error {
    background: #fef2f2;
    border-color: #fecaca;
    color: #991b1b;
  }
  .cp-alert-body {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .cp-alert-icon {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    font-weight: 700;
    background: rgba(0, 0, 0, 0.08);
  }
  .cp-alert-close {
    background: none;
    border: none;
    font-size: 20px;
    line-height: 1;
    cursor: pointer;
    color: inherit;
    opacity: 0.7;
    padding: 0 4px;
  }
  .cp-alert-close:hover { opacity: 1; }

  /* Plans Section */
  .cp-section {
    margin-bottom: 28px;
  }
  .cp-section-head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 12px;
  }
  .cp-section-title {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: #334155;
  }
  .cp-muted {
    font-size: 12.5px;
    color: #64748b;
  }
  .cp-empty {
    display: flex;
    align-items: center;
    gap: 14px;
    background: #f8fafc;
    border: 1px dashed #cbd5e1;
    border-radius: 12px;
    padding: 20px 22px;
  }
  .cp-empty-icon {
    width: 40px;
    height: 40px;
    border-radius: 10px;
    background: #ffffff;
    border: 1px solid #e2e8f0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 700;
    color: #059669;
    flex-shrink: 0;
  }
  .cp-empty p {
    margin: 2px 0 0;
    font-size: 13px;
    color: #64748b;
  }

  .cp-plan-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
    gap: 16px;
  }
  .cp-plan-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    padding: 18px;
    display: flex;
    flex-direction: column;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  }
  .cp-plan-card:hover {
    border-color: #cbd5e1;
    box-shadow: 0 4px 12px rgba(15, 23, 42, 0.06);
  }
  .cp-plan-card-active {
    border-color: #0f172a;
    box-shadow: 0 0 0 2px rgba(15, 23, 42, 0.1);
  }
  .cp-plan-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 12px;
  }
  .cp-plan-name {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #0f172a;
  }
  .cp-plan-code {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 11.5px;
    color: #64748b;
    margin-top: 3px;
  }
  .cp-plan-price {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin: 14px 0 16px;
  }
  .cp-price-sale {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
    letter-spacing: -0.02em;
  }
  .cp-price-orig {
    font-size: 13.5px;
    color: #94a3b8;
  }
  .cp-plan-footer {
    margin-top: auto;
    padding-top: 12px;
    border-top: 1px solid #f1f5f9;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  /* Badges */
  .cp-badge {
    display: inline-flex;
    align-items: center;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.04em;
    padding: 3px 8px;
    border-radius: 6px;
  }
  .cp-badge-chapter { background: #eff6ff; color: #1d4ed8; }
  .cp-badge-book { background: #f5f3ff; color: #6d28d9; }
  .cp-badge-package { background: #fef3c7; color: #b45309; }

  .cp-discount-pill {
    font-size: 11px;
    font-weight: 700;
    color: #047857;
    background: #d1fae5;
    padding: 3px 8px;
    border-radius: 999px;
  }

  /* Workspace Grid */
  .cp-workspace {
    display: grid;
    grid-template-columns: minmax(0, 1.55fr) minmax(300px, 1fr);
    gap: 24px;
    align-items: start;
  }
  @media (max-width: 900px) {
    .cp-workspace {
      grid-template-columns: 1fr;
    }
  }

  /* Cards */
  .cp-card {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
    overflow: hidden;
  }
  .cp-card-head {
    padding: 20px 24px 16px;
    border-bottom: 1px solid #f1f5f9;
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
  }
  .cp-card-title {
    margin: 0;
    font-size: 17px;
    font-weight: 600;
    color: #0f172a;
  }
  .cp-card-desc {
    margin: 4px 0 0;
    font-size: 13px;
    color: #64748b;
  }
  .cp-editing-tag {
    font-size: 11px;
    font-weight: 600;
    color: #b45309;
    background: #fef3c7;
    padding: 4px 9px;
    border-radius: 6px;
    white-space: nowrap;
  }
  .cp-form-body {
    padding: 20px 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .cp-card-footer {
    padding: 16px 24px;
    background: #f8fafc;
    border-top: 1px solid #f1f5f9;
    display: flex;
    align-items: center;
    gap: 10px;
  }

  /* Form Controls */
  .cp-grid-2 {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }
  .cp-grid-3 {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14px;
  }
  @media (max-width: 640px) {
    .cp-grid-2, .cp-grid-3 {
      grid-template-columns: 1fr;
    }
  }
  .cp-field {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }
  .cp-label {
    font-size: 12.5px;
    font-weight: 600;
    color: #334155;
  }
  .cp-input {
    width: 100%;
    box-sizing: border-box;
    padding: 9px 12px;
    font-size: 14px;
    color: #0f172a;
    background: #ffffff;
    border: 1px solid #cbd5e1;
    border-radius: 8px;
    outline: none;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
  }
  .cp-input::placeholder {
    color: #94a3b8;
  }
  .cp-input:focus {
    border-color: #0f172a;
    box-shadow: 0 0 0 3px rgba(15, 23, 42, 0.08);
  }
  .cp-input:disabled {
    background: #f1f5f9;
    color: #94a3b8;
    cursor: not-allowed;
  }
  .cp-select {
    cursor: pointer;
  }
  .cp-hint {
    font-size: 11.5px;
    color: #64748b;
  }
  .cp-divider {
    height: 1px;
    background: #f1f5f9;
    margin: 4px 0;
  }
  .cp-toggle-row {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 12px 14px;
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    cursor: pointer;
  }
  .cp-toggle-row input {
    margin-top: 3px;
    width: 16px;
    height: 16px;
    accent-color: #0f172a;
    cursor: pointer;
  }
  .cp-toggle-row strong {
    display: block;
    font-size: 13.5px;
    color: #0f172a;
  }
  .cp-toggle-row small {
    font-size: 12px;
    color: #64748b;
  }

  /* Sidebar & Live Preview */
  .cp-sidebar {
    display: flex;
    flex-direction: column;
    gap: 20px;
    position: sticky;
    top: 20px;
  }
  .cp-preview-card {
    background: linear-gradient(145deg, #0f172a 0%, #1e293b 100%);
    color: #f8fafc;
    border-radius: 14px;
    padding: 22px;
    box-shadow: 0 10px 25px rgba(15, 23, 42, 0.14);
  }
  .cp-preview-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;
  }
  .cp-preview-label {
    font-size: 10.5px;
    font-weight: 700;
    letter-spacing: 0.08em;
    color: #94a3b8;
  }
  .cp-preview-name {
    font-size: 19px;
    font-weight: 700;
    color: #ffffff;
  }
  .cp-preview-code {
    font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
    font-size: 12px;
    color: #94a3b8;
    margin-top: 2px;
  }
  .cp-preview-price-row {
    display: flex;
    align-items: baseline;
    flex-wrap: wrap;
    gap: 10px;
    margin: 16px 0 12px;
  }
  .cp-preview-sale {
    font-size: 28px;
    font-weight: 800;
    letter-spacing: -0.02em;
    color: #ffffff;
  }
  .cp-preview-orig {
    font-size: 15px;
    color: #94a3b8;
  }
  .cp-preview-desc {
    margin: 0 0 16px;
    font-size: 13px;
    line-height: 1.5;
    color: #cbd5e1;
  }
  .cp-preview-meta {
    display: flex;
    justify-content: space-between;
    padding-top: 14px;
    border-top: 1px solid rgba(255, 255, 255, 0.12);
    font-size: 12.5px;
    color: #94a3b8;
  }
  .cp-preview-meta strong {
    color: #f8fafc;
  }

  /* Buttons */
  .cp-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 13.5px;
    font-weight: 600;
    padding: 9px 16px;
    border-radius: 8px;
    border: 1px solid transparent;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .cp-btn:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  .cp-btn-primary {
    background: #0f172a;
    color: #ffffff;
  }
  .cp-btn-primary:hover:not(:disabled) {
    background: #1e293b;
  }
  .cp-btn-secondary {
    background: #ffffff;
    color: #0f172a;
    border-color: #cbd5e1;
  }
  .cp-btn-secondary:hover:not(:disabled) {
    background: #f8fafc;
    border-color: #94a3b8;
  }
  .cp-btn-ghost {
    background: transparent;
    color: #475569;
  }
  .cp-btn-ghost:hover {
    background: #f1f5f9;
    color: #0f172a;
  }
  .cp-btn-sm {
    padding: 5px 12px;
    font-size: 12.5px;
  }
  .cp-btn-full {
    width: 100%;
  }
`;