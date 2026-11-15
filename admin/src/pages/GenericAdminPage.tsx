export default function GenericAdminPage({
  title,
  eyebrow,
  description,
  items,
}: {
  title: string;
  eyebrow: string;
  description: string;
  items: string[];
}) {
  return (
    <div>
      <div className="page-head">
        <div>
          <span className="eyebrow">{eyebrow}</span>
          <h1>{title}</h1>
          <p>{description}</p>
        </div>
      </div>
      <div className="cards">
        {items.map((x) => (
          <div className="stat-card" key={x}>
            <span className="icon">•</span>
            <strong>{x}</strong>
            <p>Ready for the next workflow phase.</p>
          </div>
        ))}
      </div>
    </div>
  );
}
