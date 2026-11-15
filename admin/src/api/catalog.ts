import { authStore } from "../auth/authStore";
import type { PageResponse, SubjectSummary } from "./contracts";
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
export const catalogApi = {
  subjects: (page = 0, size = 20) =>
    req(`/admin/catalog/subjects?page=${page}&size=${size}`).then(
      (r) => r.json() as Promise<PageResponse<SubjectSummary>>,
    ),
  createSubject: (x: {
    code: string;
    name: string;
    displayOrder: number;
    active: boolean;
  }) =>
    req("/admin/catalog/subjects", {
      method: "POST",
      body: JSON.stringify(x),
    }).then((r) => r.json() as Promise<SubjectSummary>),
  papers: (subjectId: string) =>
    req(`/admin/catalog/papers?subjectId=${subjectId}&page=0&size=100`).then(
      (r) => r.json(),
    ),
  chapters: (paperId: string) =>
    req(`/admin/catalog/chapters?paperId=${paperId}&page=0&size=100`).then(
      (r) => r.json(),
    ),
  questions: (chapterId: string) =>
    req(
      `/admin/catalog/questions?chapterId=${chapterId}&publishedOnly=false&page=0&size=100`,
    ).then((r) => r.json()),
  tags: (page = 0, size = 100) =>
    req(`/admin/catalog/tags?page=${page}&size=${size}`).then((r) => r.json()),
  createTag: (x: { code: string; label: string }) =>
    req("/admin/catalog/tags", {
      method: "POST",
      body: JSON.stringify(x),
    }).then((r) => r.json()),
  attachTag: (tagId: string, qid: string, vid: string) =>
    req(`/admin/catalog/tags/${tagId}/questions/${qid}/versions/${vid}`, {
      method: "POST",
    }),
};
