import { useEffect, useState } from "react";
import { catalogApi } from "../api/catalog";
import { Link } from "react-router-dom";
export default function ContentLibraryPage() {
  const [subjects, setSubjects] = useState<any[]>([]);
  const [papers, setPapers] = useState<any[]>([]);
  const [chapters, setChapters] = useState<any[]>([]);
  const [questions, setQuestions] = useState<any[]>([]);
  const [sid, setSid] = useState("");
  const [pid, setPid] = useState("");
  const [cid, setCid] = useState("");
  const [error, setError] = useState("");
    useEffect(() => {
    catalogApi
      .subjects()
      .then((x: any) => setSubjects(x.content ?? x.items ?? x ?? []))
      .catch((e) => setError(e.message));
  }, []);
  
  async function pickSubject(v: string) {
    setSid(v);
    setPid("");
    setCid("");
    setQuestions([]);
    if (v) {
      const x: any = await catalogApi.papers(v);
      setPapers(x.content ?? x.items ?? []);
    }
  }
    async function pickPaper(v: string) {
    setPid(v);
    setCid("");
    setQuestions([]);
    if (v) {
      const x: any = await catalogApi.chapters(v);
      setChapters(x.content ?? x.items ?? x ?? []);
    }
  }

  async function pickChapter(v: string) {
    setCid(v);
    if (v) {
      const x: any = await catalogApi.questions(v);
      setQuestions(x.content ?? x.items ?? x ?? []);
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <span className="eyebrow">CONTENT</span>
          <h1>Content Library</h1>
          <p>
            Navigate the academic tree and open a draft in the authoring
            workflow.
          </p>
        </div>
        <Link className="primary" to="/questions/new">
          + New question
        </Link>
      </div>
      {error && <div className="error-banner">{error}</div>}
      <div className="panel">
        <div className="filters">
          <label>
            Subject
            <select
              value={sid}
              onChange={(e) => void pickSubject(e.target.value)}
            >
              <option value="">Select subject</option>
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.code} — {s.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Paper
            <select
              value={pid}
              onChange={(e) => void pickPaper(e.target.value)}
              disabled={!sid}
            >
              <option value="">Select paper</option>
              {papers.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.code} — {p.name}
                </option>
              ))}
            </select>
          </label>
          <label>
            Chapter
            <select
              value={cid}
              onChange={(e) => void pickChapter(e.target.value)}
              disabled={!pid}
            >
              <option value="">Select chapter</option>
              {chapters.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.code} — {c.title}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>
      <div className="panel">
        <div className="panel-title">
          <div>
            <h2>Questions</h2>
            <span>{questions.length} loaded</span>
          </div>
        </div>
        {questions.length === 0 ? (
          <div className="empty">Select a chapter to see questions.</div>
        ) : (
          <div className="table">
            {questions.map((q) => (
              <Link className="table-row" key={q.id} to={`/questions/${q.id}`}>
                <span className="mono">{q.id.slice(0, 8)}…</span>
                <span>{q.questionType}</span>
                <span>{q.difficulty ?? "—"}</span>
                <span className={`badge ${String(q.status).toLowerCase()}`}>
                  {q.status}
                </span>
              </Link>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
