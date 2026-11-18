
export type ImportMode = "CREATE_ONLY" | "UPDATE_AS_NEW_VERSION" | "SKIP_DUPLICATES";
export interface ImportJob {
  id: string; sourceType: string; fileName?: string; status: string; mode: ImportMode;
  dryRun: boolean; totalRows: number; successRows: number; errorRows: number;
  createdAt: string; updatedAt: string; committedAt?: string | null;
}
export interface ImportError {
  fieldName?: string; errorCode: string; severity: "ERROR" | "WARNING"; message: string;
}
export interface ImportRow {
  id: string; rowNo: number; externalId?: string; status: string;
  questionId?: string; payload: unknown; errors: ImportError[];
}
export interface ImportPreview { job: ImportJob; rows: ImportRow[]; warningCount: number; }
export interface CommitResult {
  job: ImportJob; createdCount: number; versionCreatedCount: number; skippedCount: number; failedCount: number;
}
