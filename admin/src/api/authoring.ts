import { authStore } from "../auth/authStore";
import type {
  QuestionDetail,
  QuestionVersionDetail,
  SolutionVersionDetail,
} from "./authoringContracts";
const API = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

async function req(path: string, init: RequestInit = {}) {
  const h = new Headers(init.headers);
  h.set("Content-Type", "application/json");
  h.set("X-Client-Type", "ADMIN_WEB");
  const t = authStore.getToken();
  if (t) h.set("Authorization", `Bearer ${t}`);
  const r = await fetch(`${API}${path}`, {
    ...init,
    headers: h,
    credentials: "include",
  });
  if (!r.ok)
    throw new Error(
      (await r.json().catch(() => ({ message: "Request failed" }))).message ??
        "Request failed",
    );
  return r;
}
export const authoringApi = {
  detail: (qid: string) =>
    req(`/admin/authoring/questions/${qid}`).then(
      (r) => r.json() as Promise<QuestionDetail>,
    ),
  createQuestion: (body: unknown) =>
    req("/admin/catalog/questions", {
      method: "POST",
      body: JSON.stringify(body),
    }).then((r) => r.json() as Promise<QuestionDetail>),
  mcqOptions: (qid: string, vid: string, body: unknown) =>
    req(`/admin/catalog/questions/${qid}/versions/${vid}/mcq-options`, {
      method: "POST",
      body: JSON.stringify(body),
    }).then((r) => r.json()),
  mcqExplanation: (qid: string, vid: string, body: unknown) =>
    req(`/admin/catalog/questions/${qid}/versions/${vid}/mcq-explanation`, {
      method: "POST",
      body: JSON.stringify(body),
    }).then((r) => r.json()),
  updateVersion: (qid: string, vid: string, body: unknown) =>
    req(`/admin/authoring/questions/${qid}/versions/${vid}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }).then((r) => r.json() as Promise<QuestionVersionDetail>),
  createSolution: (qid: string, body: unknown) =>
    req(`/admin/catalog/questions/${qid}/solution/versions`, {
      method: "POST",
      body: JSON.stringify(body),
    }).then((r) => r.json() as Promise<SolutionVersionDetail>),
  updateSolution: (qid: string, vid: string, body: unknown) =>
    req(`/admin/authoring/questions/${qid}/solution/versions/${vid}`, {
      method: "PUT",
      body: JSON.stringify(body),
    }).then((r) => r.json() as Promise<SolutionVersionDetail>),
  submit: (qid: string, note?: string) =>
    req(`/admin/authoring/questions/${qid}/submit`, {
      method: "POST",
      body: JSON.stringify({ note }),
    }).then((r) => r.json() as Promise<QuestionDetail>),
  review: (qid: string, status: string, note?: string) =>
    req(`/admin/authoring/questions/${qid}/review`, {
      method: "POST",
      body: JSON.stringify({ targetStatus: status, note }),
    }).then((r) => r.json() as Promise<QuestionDetail>),
  publish: (qid: string, note?: string) =>
    req(`/admin/authoring/questions/${qid}/publish`, {
      method: "POST",
      body: JSON.stringify({ note }),
    }).then((r) => r.json() as Promise<QuestionDetail>),
  unpublish: (qid: string, note?: string) =>
    req(`/admin/authoring/questions/${qid}/unpublish`, {
      method: "POST",
      body: JSON.stringify({ note }),
    }).then((r) => r.json() as Promise<QuestionDetail>),
  rollback: (qid: string, versionId: string) =>
    req(`/admin/authoring/questions/${qid}/rollback`, {
      method: "POST",
      body: JSON.stringify({ targetVersionId: versionId }),
    }).then((r) => r.json() as Promise<QuestionDetail>),
  history: (qid: string) =>
    req(`/admin/authoring/questions/${qid}/history`).then((r) => r.json()),
  diff: (qid: string, a: string, b: string) =>
    req(
      `/admin/authoring/questions/${qid}/diff?fromVersionId=${a}&toVersionId=${b}`,
    ).then((r) => r.json()),
  comment: (qid: string, body: string, versionId?: string) =>
    req(`/admin/authoring/questions/${qid}/comments`, {
      method: "POST",
      body: JSON.stringify({ body, versionId }),
    }).then((r) => r.json()),
  queue: (status = "SUBMITTED") =>
    req(`/admin/authoring/review-queue?status=${status}`).then((r) => r.json()),
};
