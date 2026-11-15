import { Link } from "react-router-dom";
export default function DashboardPage() {
  return (
    <div>
      <div className="page-head">
        <div>
          <span className="eyebrow">OVERVIEW</span>
          <h1>Editorial Dashboard</h1>
          <p>One workspace for content creation, review and publication.</p>
        </div>
        <Link className="primary" to="/questions/new">
          + Create question
        </Link>
      </div>
      <div className="cards">
        <div className="stat-card">
          <span className="icon">Q</span>
          <strong>Question authoring</strong>
          <p>Create structured questions, formulas and solution steps.</p>
          <Link to="/content">Open library →</Link>
        </div>
        <div className="stat-card">
          <span className="icon">R</span>
          <strong>Review queue</strong>
          <p>Approve, request changes and publish with audit history.</p>
          <Link to="/review">Open review →</Link>
        </div>
        <div className="stat-card">
          <span className="icon">M</span>
          <strong>Media</strong>
          <p>Attach multiple figures to questions and exact solution steps.</p>
          <Link to="/media">Manage media →</Link>
        </div>
        <div className="stat-card">
          <span className="icon">A</span>
          <strong>Audit-ready</strong>
          <p>
            Workflow transitions and editorial comments are recorded
            server-side.
          </p>
        </div>
      </div>
    </div>
  );
}
