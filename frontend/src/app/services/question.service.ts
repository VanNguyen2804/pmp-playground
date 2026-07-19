import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { APP_RUNTIME_CONFIG, AppRuntimeConfig } from '../app-config';
import { AnswerAttemptRequest, AnswerAttemptResponse, AnswerHistorySummary, CategorySummary, Difficulty, ExplanationReviewStatus, ImportResult, PageResponse, Question, QuestionType, ReclassificationResult, Taxonomy, PracticeAnalytics, PracticeDashboard, PracticeSessionReport, WrongQuestionReviewResponse, StudyAnnotation, StudyAnnotationRequest } from '../models/question';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly apiBaseUrl: string;
  private readonly questionsUrl: string;

  constructor(
    private readonly http: HttpClient,
    @Inject(APP_RUNTIME_CONFIG) config: AppRuntimeConfig
  ) {
    this.apiBaseUrl = config.apiBaseUrl.replace(/\/$/, '');
    this.questionsUrl = `${this.apiBaseUrl}/questions`;
  }

  list(filters: {
    search?: string;
    categoryCode?: string;
    taxonomy?: Taxonomy | '';
    difficulty?: Difficulty | '';
    questionType?: QuestionType | '';
    reviewStatus?: ExplanationReviewStatus | '';
    page?: number;
    size?: number;
  }): Observable<PageResponse<Question>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));
    if (filters.search?.trim()) params = params.set('search', filters.search.trim());
    if (filters.categoryCode?.trim()) params = params.set('categoryCode', filters.categoryCode.trim());
    if (filters.taxonomy) params = params.set('taxonomy', filters.taxonomy);
    if (filters.difficulty) params = params.set('difficulty', filters.difficulty);
    if (filters.questionType) params = params.set('questionType', filters.questionType);
    if (filters.reviewStatus) params = params.set('reviewStatus', filters.reviewStatus);
    return this.http.get<PageResponse<Question>>(this.questionsUrl, { params });
  }

  exportCsv(): Observable<HttpResponse<Blob>> {
    return this.http.get(`${this.questionsUrl}/export/csv`, {
      observe: 'response',
      responseType: 'blob'
    });
  }

  categories(taxonomy?: Taxonomy): Observable<CategorySummary[]> {
    let params = new HttpParams();
    if (taxonomy) params = params.set('taxonomy', taxonomy);
    return this.http.get<CategorySummary[]>(`${this.apiBaseUrl}/categories`, { params });
  }

  get(id: number): Observable<Question> { return this.http.get<Question>(`${this.questionsUrl}/${id}`); }
  create(question: Question): Observable<Question> { return this.http.post<Question>(this.questionsUrl, question); }
  update(id: number, question: Question): Observable<Question> { return this.http.put<Question>(`${this.questionsUrl}/${id}`, question); }
  updateExplanations(id: number, question: Pick<Question, 'pmaExplanation' | 'aiExplanation' | 'finalExplanation' | 'finalExplanationSource' | 'explanationReviewStatus' | 'explanationReviewNotes'>): Observable<Question> {
    return this.http.put<Question>(`${this.questionsUrl}/${id}/explanations`, question);
  }
  delete(id: number): Observable<void> { return this.http.delete<void>(`${this.questionsUrl}/${id}`); }

  importFile(file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    const type = file.name.toLowerCase().endsWith('.json') ? 'json' : 'csv';
    return this.http.post<ImportResult>(`${this.questionsUrl}/import/${type}`, form);
  }

  reclassifyAll(): Observable<ReclassificationResult> {
    return this.http.post<ReclassificationResult>(`${this.questionsUrl}/reclassify`, {});
  }

  priorityPracticeQuestions(count = 20, categoryCode?: string, questionType?: QuestionType): Observable<Question[]> {
    let params = new HttpParams().set('count', String(count));
    if (categoryCode?.trim()) params = params.set('categoryCode', categoryCode.trim());
    if (questionType) params = params.set('questionType', questionType);
    return this.http.get<Question[]>(`${this.questionsUrl}/practice/priority`, { params });
  }

  random(count = 10, categoryCode?: string, questionType?: QuestionType): Observable<Question[]> {
    let params = new HttpParams().set('count', String(count));
    if (categoryCode?.trim()) params = params.set('categoryCode', categoryCode.trim());
    if (questionType) params = params.set('questionType', questionType);
    return this.http.get<Question[]>(`${this.questionsUrl}/random`, { params });
  }


  practiceDashboard(): Observable<PracticeDashboard> {
    return this.http.get<PracticeDashboard>(`${this.questionsUrl}/practice/dashboard`);
  }

  practiceSessionReport(sessionId: string, questionIds: number[]): Observable<PracticeSessionReport> {
    return this.http.post<PracticeSessionReport>(`${this.questionsUrl}/practice/session-report`, {
      sessionId,
      questionIds
    });
  }

  practiceAnalytics(days = 14, top = 10): Observable<PracticeAnalytics> {
    const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    const params = new HttpParams()
      .set('days', String(days))
      .set('top', String(top))
      .set('timeZone', timeZone)
      .set('_refresh', String(Date.now()));
    return this.http.get<PracticeAnalytics>(`${this.questionsUrl}/practice/analytics`, { params });
  }

  wrongQuestions(filters: { categoryCode?: string; minIncorrect?: number; count?: number; shuffle?: boolean }): Observable<WrongQuestionReviewResponse> {
    let params = new HttpParams()
      .set('minIncorrect', String(filters.minIncorrect ?? 1))
      .set('count', String(filters.count ?? 50))
      .set('shuffle', String(filters.shuffle ?? true));
    if (filters.categoryCode?.trim()) params = params.set('categoryCode', filters.categoryCode.trim());
    return this.http.get<WrongQuestionReviewResponse>(`${this.questionsUrl}/review/wrong`, { params });
  }


  studyAnnotations(questionIds: number[]): Observable<StudyAnnotation[]> {
    return this.http.post<StudyAnnotation[]>(`${this.questionsUrl}/study-annotations/batch`, { questionIds });
  }

  studyAnnotation(questionId: number): Observable<StudyAnnotation> {
    return this.http.get<StudyAnnotation>(`${this.questionsUrl}/${questionId}/study-annotation`);
  }

  saveStudyAnnotation(questionId: number, request: StudyAnnotationRequest): Observable<StudyAnnotation> {
    return this.http.put<StudyAnnotation>(`${this.questionsUrl}/${questionId}/study-annotation`, request);
  }

  submitAttempt(questionId: number, request: AnswerAttemptRequest): Observable<AnswerAttemptResponse> {
    return this.http.post<AnswerAttemptResponse>(`${this.questionsUrl}/${questionId}/attempts`, request);
  }

  answerHistory(questionId: number, page = 0, size = 20): Observable<PageResponse<AnswerAttemptResponse>> {
    const params = new HttpParams().set('page', String(page)).set('size', String(size));
    return this.http.get<PageResponse<AnswerAttemptResponse>>(
      `${this.questionsUrl}/${questionId}/attempts`,
      { params }
    );
  }

  answerHistorySummary(questionId: number): Observable<AnswerHistorySummary> {
    return this.http.get<AnswerHistorySummary>(`${this.questionsUrl}/${questionId}/attempts/summary`);
  }
}
