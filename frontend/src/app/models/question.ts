export type CorrectOption = 'A' | 'B' | 'C' | 'D';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface Question {
  id?: number;
  questionText: string;
  optionA: string;
  optionB: string;
  optionC: string;
  optionD: string;
  correctOption: CorrectOption;
  explanation?: string | null;
  category?: string | null;
  difficulty?: Difficulty | null;
  source?: string | null;
  reference?: string | null;
  tags?: string | null;
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
  skippedRows: number;
  errors: string[];
}
