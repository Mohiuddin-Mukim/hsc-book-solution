import { rawJson } from "./http";

export type Plan = {
  id: string;
  code: string;
  name: string;
  planType: string;
  resourceType?: string;
  resourceId?: string;
  originalPriceMinor: number;
  salePriceMinor: number;
  discountPercent: number;
  currency: string;
  durationDays: number;
  description?: string;
  active: boolean;
  bookIds: string[];
};

export type ManualPaymentChannel = {
  id: string;
  method: string;
  accountNumber: string;
  paymentType: string;
  instruction: string;
  active: boolean;
  sortOrder: number;
};

export async function adminPlans() {
  return rawJson<Plan[]>("/admin/commerce/plans");
}

export async function createPlan(body: unknown) {
  return rawJson<Plan>("/admin/commerce/plans", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export async function updatePlan(id: string, body: unknown) {
  return rawJson<Plan>(`/admin/commerce/plans/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}

export async function setFreeChapter(bookId: string, chapterId: string) {
  return rawJson("/admin/commerce/free-chapters", {
    method: "POST",
    body: JSON.stringify({ bookId, chapterId }),
  });
}

export async function reconciliation() {
  return rawJson<Record<string, unknown>[]>("/admin/commerce/reconciliation");
}

export type ManualPayment = {
  id: string;
  referenceCode: string;
  userId: string;
  userEmail: string;
  userName: string;
  planName: string;
  expectedAmountMinor: number;
  currency: string;
  paymentMethod: string;
  senderMobile: string;
  transactionId: string;
  appliedAt: string;
  status: string;
  reviewedAt?: string;
  rejectionReason?: string;
  adminMessage?: string;
  expiresAt?: string;
};

export async function manualPayments(status = "PENDING", page = 0, size = 50) {
  return rawJson<ManualPayment[]>(
    `/admin/commerce/manual-payments?status=${encodeURIComponent(status)}&page=${page}&size=${size}`,
  );
}

export async function approveManualPayment(id: string, message?: string) {
  return rawJson<ManualPayment>(`/admin/commerce/manual-payments/${id}/approve`, {
    method: "POST",
    body: JSON.stringify({ message: message || "" }),
  });
}

export async function rejectManualPayment(id: string, message?: string) {
  return rawJson<ManualPayment>(`/admin/commerce/manual-payments/${id}/reject`, {
    method: "POST",
    body: JSON.stringify({ message: message || "" }),
  });
}

export async function manualPaymentChannels() {
  return rawJson<ManualPaymentChannel[]>("/admin/commerce/manual-payment-channels");
}

export async function updateManualPaymentChannel(id: string, body: unknown) {
  return rawJson<ManualPaymentChannel>(`/admin/commerce/manual-payment-channels/${id}`, {
    method: "PUT",
    body: JSON.stringify(body),
  });
}