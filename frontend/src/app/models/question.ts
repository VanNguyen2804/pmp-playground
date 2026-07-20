export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type QuestionType = 'MCQ' | 'MRQ' | 'MATCHING';
export type QuestionSource = 'PMA' | 'MANUAL' | 'CSV' | 'JSON';
export type ExplanationStatus = 'NOT_PROVIDED' | 'IMPORTED' | 'MANUAL';
export type FinalExplanationSource = 'NONE' | 'PMA' | 'AI' | 'MERGED' | 'MANUAL';
export type ExplanationReviewStatus = 'PENDING' | 'REVIEWED';
export type Taxonomy = 'PMP_TOPIC' | 'PMBOK8_DOMAIN' | 'PMA_HANDOUT_TOPIC';

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

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  code: string;
  message: string;
  path?: string;
  fieldErrors?: Record<string, string>;
  details?: string[];
  traceId?: string;
}

export interface ReclassificationResult {
  totalQuestions: number;
  reclassifiedQuestions: number;
}

export interface AnswerAttemptRequest {
  selectedAnswers: string[];
  matchingAnswers: Record<string, string>;
  sessionId?: string;
}

export interface AnswerHistorySummary {
  questionId: number;
  totalAttempts: number;
  correctAttempts: number;
  incorrectAttempts: number;
  accuracyPercentage: number;
  lastAnsweredAt?: string | null;
  lastAnswerCorrect?: boolean | null;
}

export interface AnswerAttemptResponse {
  id: number;
  questionId: number;
  questionType: QuestionType;
  selectedAnswers: string[];
  matchingAnswers: Record<string, string>;
  correct: boolean;
  correctAnswers: string[];
  correctMatchingAnswers: Record<string, string>;
  sessionId?: string | null;
  answeredAt: string;
  summary: AnswerHistorySummary;
}

export type DailyPerformanceStatus = 'NO_DATA' | 'NEW_BASELINE' | 'IMPROVING' | 'STABLE' | 'DECLINING';

export interface PracticeDashboard {
  wrongQuestions: number;
  todayAccuracyPercentage: number;
  todayCorrectAttempts: number;
  todayTotalAttempts: number;
  yesterdayAccuracyPercentage: number;
  yesterdayCorrectAttempts: number;
  yesterdayTotalAttempts: number;
  dailyAccuracyDeltaPercentagePoints: number;
  dailyPerformanceStatus: DailyPerformanceStatus;
  answeredQuestions: number;
  totalQuestions: number;
  questionBankCoveragePercentage: number;
  currentCorrectStreak: number;
  dueToday: number;
  timeZone: string;
}

export interface WrongQuestionReviewResponse {
  totalQuestions: number;
  questions: Question[];
}


export type PracticeProgressStatus = 'NO_DATA' | 'NEW_BASELINE' | 'IMPROVING' | 'STABLE' | 'DECLINING';

export interface PracticeAnalyticsSummary {
  totalAttempts: number;
  correctAttempts: number;
  incorrectAttempts: number;
  accuracyPercentage: number;
  previousTotalAttempts: number;
  previousAccuracyPercentage: number;
  improvementPercentagePoints: number;
  progressStatus: PracticeProgressStatus;
  mostWrongCategoryCode?: string | null;
  mostWrongCategoryName?: string | null;
  mostWrongCategoryIncorrectAttempts: number;
}

export interface CategoryMistakeStatistic {
  categoryCode: string;
  categoryName: string;
  totalAttempts: number;
  correctAttempts: number;
  incorrectAttempts: number;
  uniqueWrongQuestions: number;
  accuracyPercentage: number;
  incorrectPercentage: number;
}

export interface DailyAccuracyStatistic {
  date: string;
  totalAttempts: number;
  correctAttempts: number;
  incorrectAttempts: number;
  accuracyPercentage: number;
}

export interface TopWrongQuestionStatistic {
  questionId: number;
  examName?: string | null;
  questionNumber: string;
  questionText: string;
  categoryNames: string[];
  totalAttempts: number;
  correctAttempts: number;
  incorrectAttempts: number;
  lastAnswerCorrect?: boolean | null;
  lastAnsweredAt?: string | null;
}

export interface PracticeAnalytics {
  generatedAt: string;
  periodDays: number;
  timeZone: string;
  summary: PracticeAnalyticsSummary;
  categoryStats: CategoryMistakeStatistic[];
  dailyTrend: DailyAccuracyStatistic[];
  topWrongQuestions: TopWrongQuestionStatistic[];
}


export interface PracticeSessionSummary {
  totalQuestions: number;
  answeredQuestions: number;
  skippedQuestions: number;
  correctAnswers: number;
  incorrectAnswers: number;
  accuracyPercentage: number;
}

export interface PracticeSessionCategoryResult {
  categoryCode: string;
  categoryName: string;
  answeredQuestions: number;
  correctAnswers: number;
  incorrectAnswers: number;
  accuracyPercentage: number;
  wrongQuestionIds: number[];
}

export type StudySuggestionPriority = 'HIGH' | 'MEDIUM' | 'REVIEW';

export interface CategoryStudySuggestion {
  categoryCode: string;
  categoryName: string;
  priority: StudySuggestionPriority;
  pmbokReference: string;
  focusAreas: string[];
  decisionRule: string;
  recommendedPractice: string;
}

export interface PracticeSessionReport {
  generatedAt: string;
  sessionId: string;
  summary: PracticeSessionSummary;
  categoryResults: PracticeSessionCategoryResult[];
  suggestions: CategoryStudySuggestion[];
}

export type StudyHighlightTarget = 'QUESTION' | 'OPTION';
export type StudyHighlightColor = 'YELLOW' | 'GREEN' | 'BLUE' | 'PINK';

export interface StudyHighlight {
  id: string;
  target: StudyHighlightTarget;
  targetKey?: string | null;
  startOffset: number;
  endOffset: number;
  text: string;
  color: StudyHighlightColor;
}

export interface StudyAnnotation {
  questionId: number;
  note?: string | null;
  highlights: StudyHighlight[];
  version: number;
  updatedAt?: string | null;
}

export interface StudyAnnotationRequest {
  note?: string | null;
  highlights: StudyHighlight[];
}
