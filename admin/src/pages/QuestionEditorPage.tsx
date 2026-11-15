import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { catalogApi } from "../api/catalog";
import { authoringApi } from "../api/authoring";
import MediaUploader from "../components/MediaUploader";
import MathText from "../components/MathText";

function latexWarning(s: string) {
  const dollars = (s.match(/\$/g) || []).length;
  const opens = (s.match(/\\\(/g) || []).length,
    closes = (s.match(/\\\)/g) || []).length;
  return dollars % 2 !== 0 || opens !== closes
    ? "Possible LaTeX delimiter mismatch — preview carefully."
    : "";
}

export default function QuestionEditorPage() {
  const { id } = useParams();
  const editing = Boolean(id);
  const nav = useNavigate();
  const [subjects, setSubjects] = useState<any[]>([]),
    [papers, setPapers] = useState<any[]>([]),
    [chapters, setChapters] = useState<any[]>([]);
  const [chapterId, setChapterId] = useState(""),
    [type, setType] = useState("CQ"),
    [difficulty, setDifficulty] = useState("MEDIUM"),
    [lang, setLang] = useState("bn"),
    [stem, setStem] = useState(""),
    [marks, setMarks] = useState("2"),
    [finalAnswer, setFinalAnswer] = useState("");
  const [question, setQuestion] = useState<any>(null),
    [solution, setSolution] = useState<any>(null),
    [steps, setSteps] = useState<any[]>([]),
    [saving, setSaving] = useState(false),
    [error, setError] = useState(""),
    [message, setMessage] = useState("");
  const [mcqOptions, setMcqOptions] = useState<any[]>([]),
    [mcqExplanation, setMcqExplanation] = useState(""),
    [tagCode, setTagCode] = useState(""),
    [tagLabel, setTagLabel] = useState("");
  const [subjectId, setSubjectId] = useState(""),
    [paperId, setPaperId] = useState("");
  const draftKey = editing
    ? `hsc-draft-question-${id}`
    : "hsc-new-question-draft";

    useEffect(() => {
    catalogApi
      .subjects()
      .then((x: any) => setSubjects(x.content ?? x.items ?? x ?? []))
      .catch((e) => setError(e.message));
    if (id) localStorage.removeItem(draftKey);
  }, [id]);

  useEffect(() => {
    if (subjectId)
      catalogApi
        .papers(subjectId)
        .then((x: any) => setPapers(x.content ?? x.items ?? x ?? []));
  }, [subjectId]);

  useEffect(() => {
    if (paperId)
      catalogApi
        .chapters(paperId)
        .then((x: any) => setChapters(x.content ?? x.items ?? x ?? []));
  }, [paperId]);


  useEffect(() => {
    const raw = localStorage.getItem(draftKey);
    if (raw && !editing) {
      try {
        const d = JSON.parse(raw);
        setChapterId(d.chapterId ?? "");
        setType(d.type ?? "CQ");
        setDifficulty(d.difficulty ?? "MEDIUM");
        setLang(d.lang ?? "bn");
        setStem(d.stem ?? "");
        setMarks(d.marks ?? "2");
        setFinalAnswer(d.finalAnswer ?? "");
        setMessage("Recovered an unsaved local draft.");
      } catch {}
    }
  }, []);
  useEffect(() => {
    const t = setTimeout(
      () =>
        localStorage.setItem(
          draftKey,
          JSON.stringify({
            chapterId,
            type,
            difficulty,
            lang,
            stem,
            marks,
            finalAnswer,
            savedAt: new Date().toISOString(),
          }),
        ),
      700,
    );
    return () => clearTimeout(t);
  }, [chapterId, type, difficulty, lang, stem, marks, finalAnswer, draftKey]);
  useEffect(() => {
    if (!id) return;
    authoringApi
      .detail(id)
      .then((q: any) => {
        setQuestion(q);
        setStem(q.version?.stem ?? "");
        setMarks(String(q.version?.marks ?? ""));
        setLang(q.version?.languageCode ?? "bn");
        setType(q.questionType ?? "CQ");
        setDifficulty(q.difficulty ?? "MEDIUM");
        setSolution(q.solution ?? null);
        setSteps(Array.isArray(q.solution?.version?.steps) ? q.solution.version.steps : []);
        setMcqOptions(Array.isArray(q.version?.options) ? q.version.options : []);
        setMcqExplanation(q.version?.explanations?.[0]?.explanation ?? "");
        const raw = localStorage.getItem(draftKey);
        if (raw) {
          try {
            const d = JSON.parse(raw);
            setStem(d.stem ?? q.version?.stem ?? "");
            setMarks(d.marks ?? String(q.version?.marks ?? ""));
            setLang(d.lang ?? q.version?.languageCode ?? "bn");
            setFinalAnswer(d.finalAnswer ?? "");
            setMessage("Recovered a local draft. Save draft when ready.");
          } catch {}
        }
      })
      .catch((e) => setError(e.message));
  }, [id, draftKey]);

  async function create() {
    if (!chapterId || !stem.trim()) {
      setError("Chapter and question statement are required.");
      return;
    }
    setSaving(true);
    setError("");
    try {
      const q = await authoringApi.createQuestion({
        chapterId,
        questionType: type,
        difficulty,
        languageCode: lang,
        stem,
        marks: Number(marks),
      });
      setQuestion(q);
      localStorage.removeItem(draftKey);
      setMessage("Question draft created.");
      nav(`/questions/${q.id}`, { replace: true });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }
  async function saveVersion() {
    if (!question?.version) return;
    setSaving(true);
    setError("");
    try {
      const lock = question.version.lockVersion ?? 0;
      const v = await authoringApi.updateVersion(
        question.id,
        question.version.id,
        {
          languageCode: lang,
          stem,
          marks: Number(marks),
          finalAnswer,
          expectedLockVersion: lock,
        },
      );
      setQuestion({ ...question, version: { ...v } });
      localStorage.removeItem(draftKey);
      setMessage("Draft saved.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Save failed");
    } finally {
      setSaving(false);
    }
  }
  async function createOrUpdateSolution() {
    setSaving(true);
    setError("");
    try {
      if (solution?.version) {
        const v = await authoringApi.updateSolution(
          question.id,
          solution.version.id,
          {
            languageCode: lang,
            summary: solution.version.summary ?? "",
            expectedLockVersion: solution.version.lockVersion ?? 0,
            steps: steps.map((s, i) => ({
              stepNo: i + 1,
              title: s.title,
              body: s.body,
            })),
          },
        );
        setSolution({ ...solution, version: v });
      } else {
        const v = await authoringApi.createSolution(question.id, {
          languageCode: lang,
          summary: "",
          steps: steps.map((s, i) => ({
            stepNo: i + 1,
            title: s.title,
            body: s.body,
          })),
        });
        setSolution({ version: v });
      }
      setMessage("Solution saved.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Solution save failed");
    } finally {
      setSaving(false);
    }
  }
  const warning = useMemo(() => latexWarning(stem), [stem]);
  return (
    <div>
      <div className="page-head">
        <div>
          <span className="eyebrow">AUTHORING</span>
          <h1>{editing ? "Edit question" : "Create question"}</h1>
          <p>
            Draft safely, preview formulas, attach figures, then submit for
            review.
          </p>
        </div>
        <button className="ghost" onClick={() => nav("/content")}>
          Back to library
        </button>
      </div>
      {error && <div className="error-banner">{error}</div>}
      {message && <div className="success-banner">{message}</div>}
      <div className="editor-grid">
        <section className="panel">
          <div className="panel-title">
            <div>
              <h2>Question</h2>
              <span>Structured academic metadata</span>
            </div>
            <span
              className={`badge ${question?.status?.toLowerCase() ?? "draft"}`}
            >
              {question?.status ?? "DRAFT"}
            </span>
          </div>
                    {!editing && (
            <div className="filters three">
              <label>
                Subject
                <select
                  value={subjectId}
                  onChange={(e) => {
                    setSubjectId(e.target.value);
                    setPaperId("");
                    setChapterId("");
                    setPapers([]);
                    setChapters([]);
                  }}
                >
                  <option value="">Select</option>
                  {subjects?.map((s) => (
                    <option key={s.id} value={s.id}>
                      {s.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Paper
                <select
                  value={paperId}
                  onChange={(e) => {
                    setPaperId(e.target.value);
                    setChapterId("");
                    setChapters([]);
                  }}
                  disabled={!subjectId}
                >
                  <option value="">Select</option>
                  {papers?.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Chapter
                <select
                  value={chapterId}
                  onChange={(e) => setChapterId(e.target.value)}
                  disabled={!paperId}
                >
                  <option value="">Select</option>
                  {chapters?.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.title}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          )}
          <div className="filters three">
            <label>
              Language
              <select value={lang} onChange={(e) => setLang(e.target.value)}>
                <option value="bn">Bangla</option>
                <option value="en">English</option>
              </select>
            </label>
            <label>
              Type
              <select value={type} onChange={(e) => setType(e.target.value)}>
                <option>MCQ</option>
                <option>CQ</option>
                <option>NUMERICAL</option>
                <option>SHORT</option>
                <option>OTHER</option>
              </select>
            </label>
            <label>
              Difficulty
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value)}
              >
                <option>EASY</option>
                <option>MEDIUM</option>
                <option>HARD</option>
              </select>
            </label>
          </div>
          <div className="filters two">
            <label>
              Marks
              <input
                type="number"
                min="0"
                value={marks}
                onChange={(e) => setMarks(e.target.value)}
              />
            </label>
            <label>
              Final answer
              <input
                value={finalAnswer}
                onChange={(e) => setFinalAnswer(e.target.value)}
                placeholder="Optional short final answer"
              />
            </label>
          </div>
          <label>
            Question statement
            <textarea
              className="large"
              value={stem}
              onChange={(e) => setStem(e.target.value)}
              placeholder="Use LaTeX such as $v=u+at$ or \(x^2\)."
            />
          </label>
          {warning && <div className="warning">{warning}</div>}
          <div className="preview">
            <strong>Student preview</strong>
            <div className="preview-box">
              {stem ? <MathText text={stem} /> : "Your question will appear here."}
            </div>
          </div>
          <div className="actions">
            {editing ? (
              <button
                className="primary"
                disabled={saving}
                onClick={() => void saveVersion()}
              >
                {saving ? "Saving…" : "Save draft"}
              </button>
            ) : (
              <button
                className="primary"
                disabled={saving}
                onClick={() => void create()}
              >
                {saving ? "Creating…" : "Create draft"}
              </button>
            )}
          </div>
          {question?.version && type === "MCQ" && (
            <div className="mcq-box">
              <div className="panel-title">
                <div>
                  <h3>MCQ options & explanation</h3>
                  <span>Keep option order stable.</span>
                </div>
              </div>
              {mcqOptions.map((o, i) => (
                <div className="filters three" key={i}>
                  <input
                    placeholder="A"
                    value={o.optionKey ?? ""}
                    onChange={(e) =>
                      setMcqOptions(
                        mcqOptions.map((x, j) =>
                          j === i ? { ...x, optionKey: e.target.value } : x,
                        ),
                      )
                    }
                  />
                  <input
                    placeholder="Option text"
                    value={o.optionText ?? ""}
                    onChange={(e) =>
                      setMcqOptions(
                        mcqOptions.map((x, j) =>
                          j === i ? { ...x, optionText: e.target.value } : x,
                        ),
                      )
                    }
                  />
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={Boolean(o.correct)}
                      onChange={(e) =>
                        setMcqOptions(
                          mcqOptions.map((x, j) =>
                            j === i ? { ...x, correct: e.target.checked } : x,
                          ),
                        )
                      }
                    />{" "}
                    Correct
                  </label>
                </div>
              ))}
              <button
                className="ghost"
                onClick={() =>
                  setMcqOptions([
                    ...mcqOptions,
                    {
                      optionKey: String.fromCharCode(65 + mcqOptions.length),
                      optionText: "",
                      correct: false,
                      displayOrder: mcqOptions.length,
                    },
                  ])
                }
              >
                + Add option
              </button>
              <label>
                Explanation
                <textarea
                  value={mcqExplanation}
                  onChange={(e) => setMcqExplanation(e.target.value)}
                  placeholder="Explain why the correct answer is correct."
                />
              </label>
              <div className="actions">
                <button
                  className="ghost"
                  onClick={async () => {
                    try {
                      await authoringApi.mcqOptions(
                        question.id,
                        question.version.id,
                        mcqOptions.map((x, i) => ({
                          optionKey: x.optionKey,
                          optionText: x.optionText,
                          correct: Boolean(x.correct),
                          displayOrder: i,
                        })),
                      );
                      await authoringApi.mcqExplanation(
                        question.id,
                        question.version.id,
                        { languageCode: lang, explanation: mcqExplanation },
                      );
                      setMessage("MCQ saved.");
                    } catch (e) {
                      setError(e instanceof Error ? e.message : String(e));
                    }
                  }}
                >
                  Save MCQ
                </button>
              </div>
            </div>
          )}
        </section>
        {question?.version && (
          <section className="panel">
            <div className="panel-title">
              <div>
                <h2>Figures & graphs</h2>
                <span>Attach to this exact question version</span>
              </div>
            </div>
            <MediaUploader
              targetType="questionVersion"
              targetId={question.version.id}
              existing={question.version.media ?? []}
            />
          </section>
        )}
      </div>
      {question?.id && (
        <section className="panel solution-panel">
          <div className="panel-title">
            <div>
              <h2>Solution</h2>
              <span>Ordered steps with exact-step media</span>
            </div>
          </div>
          {steps.map((s, i) => (
            <div className="step-card" key={s.id ?? i}>
              <div className="step-head">
                <strong>Step {i + 1}</strong>
                <button
                  className="ghost"
                  onClick={() => setSteps(steps.filter((_, j) => j !== i))}
                >
                  Remove
                </button>
              </div>
              <input
                placeholder="Step title"
                value={s.title ?? ""}
                onChange={(e) =>
                  setSteps(
                    steps.map((x, j) =>
                      j === i ? { ...x, title: e.target.value } : x,
                    ),
                  )
                }
              />
              <textarea
                value={s.body ?? ""}
                onChange={(e) =>
                  setSteps(
                    steps.map((x, j) =>
                      j === i ? { ...x, body: e.target.value } : x,
                    ),
                  )
                }
                placeholder="Explain the calculation or reasoning. LaTeX is supported."
              />
              {s.id && (
                <MediaUploader
                  targetType="solutionStep"
                  targetId={s.id}
                  existing={s.media ?? []}
                />
              )}
            </div>
          ))}
          <div className="actions">
            <button
              className="ghost"
              onClick={() =>
                setSteps([
                  ...steps,
                  { stepNo: steps.length + 1, title: "", body: "" },
                ])
              }
            >
              + Add solution step
            </button>
            <button
              className="primary"
              disabled={saving}
              onClick={() => void createOrUpdateSolution()}
            >
              Save solution
            </button>
          </div>
        </section>
      )}
      {question?.version && (
        <section className="panel">
          <div className="panel-title">
            <div>
              <h2>Tags</h2>
              <span>Create and attach a reusable tag to this version.</span>
            </div>
          </div>
          <div className="filters two">
            <label>
              Code
              <input
                value={tagCode}
                onChange={(e) => setTagCode(e.target.value)}
                placeholder="mechanics"
              />
            </label>
            <label>
              Label
              <input
                value={tagLabel}
                onChange={(e) => setTagLabel(e.target.value)}
                placeholder="Mechanics"
              />
            </label>
          </div>
          <div className="actions">
            <button
              className="ghost"
              disabled={!tagCode || !tagLabel}
              onClick={async () => {
                try {
                  const t = await catalogApi.createTag({
                    code: tagCode,
                    label: tagLabel,
                  });
                  await catalogApi.attachTag(
                    t.id,
                    question.id,
                    question.version.id,
                  );
                  setTagCode("");
                  setTagLabel("");
                  setMessage("Tag attached.");
                } catch (e) {
                  setError(e instanceof Error ? e.message : String(e));
                }
              }}
            >
              Create & attach tag
            </button>
          </div>
        </section>
      )}
      {question?.id && (
        <section className="panel">
          <div className="panel-title">
            <div>
              <h2>Workflow</h2>
              <span>Role-aware editorial transitions</span>
            </div>
          </div>
          <div className="actions">
            {question.status === "DRAFT" ||
            question.status === "CHANGES_REQUESTED" ? (
              <button
                className="primary"
                onClick={async () => {
                  try {
                    const q = await authoringApi.submit(question.id);
                    setQuestion(q);
                    setMessage("Submitted for review.");
                  } catch (e) {
                    setError(String(e));
                  }
                }}
              >
                Submit for review
              </button>
            ) : null}
            {question.status === "SUBMITTED" && (
              <span className="muted">Waiting for reviewer action.</span>
            )}
          </div>
        </section>
      )}
    </div>
  );
}
