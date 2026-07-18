export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type QuestionType = 'MCQ' | 'MRQ' | 'MATCHING';
export type QuestionSource = 'PMA' | 'MANUAL' | 'CSV' | 'JSON';
export type ExplanationStatus = 'NOT_PROVIDED' | 'IMPORTED' | 'MANUAL';
export type FinalExplanationSource = 'NONE' | 'PMA' | 'AI' | 'MERGED' | 'MANUAL';
export type ExplanationReviewStatus = 'PENDING' | 'REVIEWED';
export type Taxonomy = 'PMBOK8_DOMAIN' | 'PMA_HANDOUT_TOPIC';

export interface QuestionOption {
  id?: number;
  key: string;
  text: string;
  correct?: boolean;
  displayOrder?: number;
}

export interface MatchingPair {
  id?: number;
  left: string;
  right: string;
  displayOrder?: number;
}

export interface QuestionCategory {
  code: string;
  name: string;
  taxonomy: Taxonomy;
}

export interface CategorySummary {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  taxonomy: Taxonomy;
  displayOrder: number;
  questionCount: number;
}

export interface Question {
  id?: number;
  externalId?: string | null;
  examName?: string | null;
  questionType: QuestionType;
  questionText: string;
  imageUrl?: string | null;
  options: QuestionOption[];
  correctAnswers: string[];
  matchingPairs: MatchingPair[];
  pmaExplanation?: string | null;
  aiExplanation?: string | null;
  finalExplanation?: string | null;
  finalExplanationSource?: FinalExplanationSource;
  explanationReviewStatus?: ExplanationReviewStatus;
  explanationReviewNotes?: string | null;
  explanationReviewedAt?: string | null;
  explanationStatus?: ExplanationStatus;
  explanationType?: string | null;
  explanationPromptVersion?: number | null;
  numberOfAnswers?: number | null;
  source?: QuestionSource;
  difficulty?: Difficulty | null;
  reference?: string | null;
  tags?: string | null;
  categories?: QuestionCategory[];
  categoryCodes?: string[];
  createdAt?: string;
  updatedAt?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface ImportResult {
  totalRows: number;
  importedRows: number;
  updatedRows: number;
  skippedRows: number;
  errors: string[];
}
