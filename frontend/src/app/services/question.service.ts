import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Difficulty, ImportResult, PageResponse, Question } from '../models/question';

@Injectable({ providedIn: 'root' })
export class QuestionService {
  private readonly apiUrl = 'http://localhost:8080/api/questions';

  constructor(private readonly http: HttpClient) {}

  list(filters: {
    search?: string;
    category?: string;
    difficulty?: Difficulty | '';
    page?: number;
    size?: number;
  }): Observable<PageResponse<Question>> {
    let params = new HttpParams()
      .set('page', String(filters.page ?? 0))
      .set('size', String(filters.size ?? 20));

    if (filters.search?.trim()) params = params.set('search', filters.search.trim());
    if (filters.category?.trim()) params = params.set('category', filters.category.trim());
    if (filters.difficulty) params = params.set('difficulty', filters.difficulty);

    return this.http.get<PageResponse<Question>>(this.apiUrl, { params });
  }

  get(id: number): Observable<Question> {
    return this.http.get<Question>(`${this.apiUrl}/${id}`);
  }

  create(question: Question): Observable<Question> {
    return this.http.post<Question>(this.apiUrl, question);
  }

  update(id: number, question: Question): Observable<Question> {
    return this.http.put<Question>(`${this.apiUrl}/${id}`, question);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  importFile(file: File): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file);
    const type = file.name.toLowerCase().endsWith('.json') ? 'json' : 'csv';
    return this.http.post<ImportResult>(`${this.apiUrl}/import/${type}`, form);
  }

  random(count = 10, category?: string): Observable<Question[]> {
    let params = new HttpParams().set('count', String(count));
    if (category?.trim()) params = params.set('category', category.trim());
    return this.http.get<Question[]>(`${this.apiUrl}/random`, { params });
  }
}
