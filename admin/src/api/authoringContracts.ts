export type QuestionType = "MCQ" | "CQ" | "NUMERICAL" | "SHORT" | "OTHER";
export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type ContentStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "CHANGES_REQUESTED"
  | "APPROVED"
  | "PUBLISHED"
  | "ARCHIVED";

export interface MediaAssetResponse {
  id: string;
  role: string;
  order: number;
  caption?: string | null;
  altText?: string | null;
  width: number;
  height: number;
  byteSize: number;
  mimeType: string;
  checksumSha256: string;
  lifecycleState: string;
  deliveryUrl: string;
  usageCount: number;
}

export interface McqOption {
  id: string;
  optionKey: string;
  optionText: string;
  correct: boolean;
  displayOrder: number;
}

export interface McqExplanation {
  id: string;
  languageCode: string;
  explanation: string;
}

export interface QuestionVersionDetail {
  id: string;
  versionNo: number;
  languageCode: string;
  stem: string;
  marks: number;
  lockVersion: number;
  options: McqOption[];
  explanations: McqExplanation[];
  media: MediaAssetResponse[];
}

export interface SolutionStep {
  id: string;
  stepNo: number;
  title?: string;
  body: string;
  media: MediaAssetResponse[];
}

export interface SolutionVersionDetail {
  id: string;
  versionNo: number;
  languageCode: string;
  summary?: string;
  lockVersion: number;
  steps: SolutionStep[];
}

export interface QuestionSummary {
  id: string;
  chapterId: string;
  chapterCode: string;
  questionType: QuestionType;
  difficulty?: Difficulty;
  status: ContentStatus;
  currentVersionId?: string;
}

export interface QuestionDetail {
  id: string;
  chapterId: string;
  chapterCode: string;
  questionType: QuestionType;
  difficulty?: Difficulty;
  status: ContentStatus;
  currentVersionId?: string;
  version?: QuestionVersionDetail | null;
  solution?: {
    id: string;
    status: ContentStatus;
    currentVersionId?: string;
    version?: SolutionVersionDetail | null;
  } | null;
}