import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CategorySummary, Question } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({ selector: 'app-practice', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './practice.html', styleUrl: './practice.css' })
export class Practice implements OnInit {
  questions: Question[] = [];
  categories: CategorySummary[] = [];
  index = 0; score = 0; finished = false; categoryCode = ''; loading = false; error = ''; revealed = false;
  selected: Record<string, boolean> = {};
  matching: Record<string, string> = {};
  matchingChoices: string[] = [];

  constructor(private readonly service: QuestionService) {}
  ngOnInit(): void { this.service.categories().subscribe({next: c => this.categories = c}); this.start(); }
  get current(): Question | undefined { return this.questions[this.index]; }

  start(): void {
    this.loading = true; this.error = '';
    this.service.random(10, this.categoryCode).subscribe({ next: questions => { this.questions = questions; this.index = 0; this.score = 0; this.finished = false; this.loading = false; this.prepare(); },
      error: err => { this.error = err?.error?.message ?? 'Không thể tạo bài luyện tập.'; this.loading = false; } });
  }

  toggle(key: string): void {
    if (this.revealed) return;
    if (this.current?.questionType === 'MCQ') this.selected = {[key]: true};
    else this.selected[key] = !this.selected[key];
  }

  check(): void {
    if (!this.current || this.revealed || !this.canCheck()) return;
    this.revealed = true;
    if (this.isCorrect()) this.score++;
  }

  next(): void {
    if (this.index + 1 >= this.questions.length) { this.finished = true; return; }
    this.index++; this.prepare();
  }

  canCheck(): boolean {
    if (!this.current) return false;
    if (this.current.questionType === 'MATCHING') return this.current.matchingPairs.every(p => !!this.matching[p.left]);
    return Object.values(this.selected).some(Boolean);
  }

  isCorrect(): boolean {
    const q = this.current; if (!q) return false;
    if (q.questionType === 'MATCHING') return q.matchingPairs.every(p => this.matching[p.left] === p.right);
    const chosen = Object.keys(this.selected).filter(k => this.selected[k]).sort();
    return JSON.stringify(chosen) === JSON.stringify([...q.correctAnswers].sort());
  }


  explanationText(question: Question): string | null {
    return question.finalExplanation?.trim() || question.aiExplanation?.trim() || question.pmaExplanation?.trim() || null;
  }

  explanationLabel(question: Question): string {
    if (question.finalExplanation?.trim()) return `Lời giải cuối (${question.finalExplanationSource ?? 'MANUAL'})`;
    if (question.aiExplanation?.trim()) return 'Lời giải ChatGPT (chưa chốt)';
    if (question.pmaExplanation?.trim()) return 'Lời giải PMA (chưa chốt)';
    return 'Chưa có lời giải';
  }

  private prepare(): void {
    this.selected = {}; this.matching = {}; this.revealed = false;
    this.matchingChoices = this.current?.questionType === 'MATCHING'
      ? [...this.current.matchingPairs.map(p => p.right)].sort(() => Math.random() - .5) : [];
  }
}
