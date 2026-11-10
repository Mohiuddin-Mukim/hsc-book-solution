import { useEffect, useState } from "react";
import { catalogApi } from "../api/catalog";
import type { SubjectSummary } from "../api/contracts";
export default function CatalogPage() {
  const [items, setItems] = useState<SubjectSummary[]>([]);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    code: "",
    name: "",
    displayOrder: 0,
    active: true,
  });
  const load = () =>
    catalogApi
      .subjects()
      .then((x) => setItems(x.items))
      .catch((e) => setError(e.message));
  useEffect(() => {
    load();
  }, []);
  return (
    <main>
      <h1>Academic Catalog</h1>
      <p>Phase 4: Subjects</p>
      {error && (
        <div role="alert" className="error">
          {error}
        </div>
      )}
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          await catalogApi.createSubject(form);
          setForm({ code: "", name: "", displayOrder: 0, active: true });
          load();
        }}
      >
        <div className="row">
          <input
            aria-label="Subject code"
            placeholder="PHYSICS"
            value={form.code}
            onChange={(e) => setForm({ ...form, code: e.target.value })}
          />
          <input
            aria-label="Subject name"
            placeholder="Physics"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
          />
          <input
            aria-label="Display order"
            type="number"
            value={form.displayOrder}
            onChange={(e) =>
              setForm({ ...form, displayOrder: Number(e.target.value) })
            }
          />
          <button>Add subject</button>
        </div>
      </form>
      <ul>
        {items.map((x) => (
          <li key={x.id}>
            {x.code} — {x.name}
          </li>
        ))}
      </ul>
    </main>
  );
}
