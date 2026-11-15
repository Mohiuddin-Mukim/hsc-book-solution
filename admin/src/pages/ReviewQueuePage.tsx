import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { authoringApi } from "../api/authoring";
export default function ReviewQueuePage() {
  const [status, setStatus] = useState("SUBMITTED"),
    [items, setItems] = useState<any[]>([]),
    [selected, setSelected] = useState<any>(null),
    [history, setHistory] = useState<any>(null),
    [comment, setComment] = useState(""),
    [error, setError] = useState("");
  async function load() {
    try {
      setItems(await authoringApi.queue(status));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }
  useEffect(() => {
    void load();
  }, [status]);
  async function open(q: any) {
    setSelected(q);
    try {
      setHistory(await authoringApi.history(q.id));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }
  return (
    <div>
      <div className="page-head">
        <div>
          <span className="eyebrow">REVIEW</span>
          <h1>Review Queue</h1>
          <p>
            Compare versions, leave comments and move content through controlled
            states.
          </p>
        </div>
      </div>
      {error && <div className="error-banner">{error}</div>}
      <div className="review-grid">
        <section className="panel">
          <div className="filters">
            <label>
              Status
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
              >
                <option>SUBMITTED</option>
                <option>CHANGES_REQUESTED</option>
                <option>APPROVED</option>
                <option>PUBLISHED</option>
                <option>ARCHIVED</option>
              </select>
            </label>
          </div>
          <div className="table">
            {items.map((q) => (
              <button
                className={`table-row ${selected?.id === q.id ? "selected" : ""}`}
                key={q.id}
                onClick={() => void open(q)}
              >
                <span className="mono">{q.id.slice(0, 8)}…</span>
                <span>{q.questionType}</span>
                <span>{q.chapterCode}</span>
                <span className="badge submitted">{q.status}</span>
              </button>
            ))}
          </div>
          {!items.length && (
            <div className="empty">No questions in this queue.</div>
          )}
        </section>
        {selected && (
          <section className="panel">
            <div className="panel-title">
              <div>
                <h2>Review detail</h2>
                <span>{selected.id}</span>
              </div>
              <Link to={`/questions/${selected.id}`}>Open editor</Link>
            </div>
            <div className="review-copy">
              {history?.versions?.map((v: any, i: number) => (
                <article key={v.id} className="version-card">
                  <div>
                    <strong>
                      v{v.versionNo} · {v.languageCode}
                    </strong>
                    <span>lock {v.lockVersion}</span>
                  </div>
                  <p>{v.stem}</p>
                  <small>Marks: {v.marks}</small>
                  {i > 0 && (
                    <button
                      className="ghost"
                      onClick={async () => {
                        const d = await authoringApi.diff(
                          selected.id,
                          history.versions[i].id,
                          history.versions[i - 1].id,
                        );
                        alert(
                          `Stem changed: ${d.stemChanged}; marks changed: ${d.marksChanged}`,
                        );
                      }}
                    >
                      Compare with newer
                    </button>
                  )}
                </article>
              ))}
            </div>
            <div className="comments">
              <h3>Reviewer comments</h3>
              {history?.comments?.map((c: any) => (
                <div className="comment" key={c.id}>
                  <small>{new Date(c.createdAt).toLocaleString()}</small>
                  <p>{c.body}</p>
                </div>
              ))}
              <textarea
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder="Leave a review comment"
              />
              <button
                className="ghost"
                disabled={!comment.trim()}
                onClick={async () => {
                  await authoringApi.comment(
                    selected.id,
                    comment,
                    history?.currentVersionId,
                  );
                  setComment("");
                  await open(selected);
                }}
              >
                Add comment
              </button>
            </div>
            <div className="actions">
              <button
                className="ghost"
                onClick={async () => {
                  const q = await authoringApi.review(
                    selected.id,
                    "CHANGES_REQUESTED",
                  );
                  setSelected(q);
                  await load();
                  await open(q);
                }}
              >
                Request changes
              </button>
              <button
                className="ghost"
                onClick={async () => {
                  const q = await authoringApi.review(selected.id, "APPROVED");
                  setSelected(q);
                  await load();
                  await open(q);
                }}
              >
                Approve
              </button>
              <button
                className="primary"
                onClick={async () => {
                  const q = await authoringApi.publish(selected.id);
                  setSelected(q);
                  await load();
                  await open(q);
                }}
              >
                Publish
              </button>
            </div>
          </section>
        )}
      </div>
    </div>
  );
}
