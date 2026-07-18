import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  OnInit,
  computed,
  signal
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, take } from 'rxjs';
import { CategorySummary, Question } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-practice',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './practice.html',
  styleUrl: './practice.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Practice implements OnInit, OnDestroy {
  readonly questions = signal<Question[]>([]);
  readonly categories = signal<CategorySummary[]>([]);
  readonly index = signal(0);
  readonly score = signal(0);
  readonly finished = signal(false);
  readonly categoryCode = signal('');
  readonly loading = signal(false);
  readonly error = signal('');
  readonly revealed = signal(false);
  readonly selected = signal<Record<string, boolean>>({});
  readonly matching = signal<Record<string, string>>({});
  readonly matchingChoices = signal<string[]>([]);

  readonly current = computed(() => this.questions()[this.index()]);

  private loadSubscription?: Subscription;
  private categoriesSubscription?: Subscription;
  private requestVersion = 0;

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.categoriesSubscription = this.service.categories().pipe(take(1)).subscribe({
      next: categories => this.categories.set(categories ?? []),
      error: () => this.categories.set([])
    });
    this.start();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.categoriesSubscription?.unsubscribe();
  }

  start(): void {
    this.loadSubscription?.unsubscribe();
    const version = ++this.requestVersion;
    this.loading.set(true);
    this.error.set('');

    this.loadSubscription = this.service.random(10, this.categoryCode()).pipe(take(1)).subscribe({
      next: questions => {
        if (version !== this.requestVersion) return;
        this.questions.set(questions ?? []);
        this.index.set(0);
        this.score.set(0);
        this.finished.set(false);
        this.prepare();
        this.loading.set(false);
      },
      error: () => {
        if (version !== this.requestVersion) return;
        this.error.set('Không tải được bộ câu hỏi. Nhấn “Tạo lại bộ câu hỏi” để thử lại.');
        this.loading.set(false);
      }
    });
  }

  toggle(key: string): void {
    if (this.revealed()) return;

    if (this.current()?.questionType === 'MCQ') {
      this.selected.set({ [key]: true });
      return;
    }

    this.selected.update(value => ({ ...value, [key]: !value[key] }));
  }

  setMatching(left: string, right: string): void {
    if (this.revealed()) return;
    this.matching.update(value => ({ ...value, [left]: right }));
  }

  check(): void {
    if (!this.current() || this.revealed() || !this.canCheck()) return;
    this.revealed.set(true);
    if (this.isCorrect()) this.score.update(value => value + 1);
  }

  next(): void {
    if (this.index() + 1 >= this.questions().length) {
      this.finished.set(true);
      return;
    }
    this.index.update(value => value + 1);
    this.prepare();
  }

  canCheck(): boolean {
    const current = this.current();
    if (!current) return false;
    if (current.questionType === 'MATCHING') {
      const matching = this.matching();
      return current.matchingPairs.every(pair => !!matching[pair.left]);
    }
    return Object.values(this.selected()).some(Boolean);
  }

  isCorrect(): boolean {
    const question = this.current();
    if (!question) return false;

    if (question.questionType === 'MATCHING') {
      const matching = this.matching();
      return question.matchingPairs.every(pair => matching[pair.left] === pair.right);
    }

    const selected = this.selected();
    const chosen = Object.keys(selected).filter(key => selected[key]).sort();
    return JSON.stringify(chosen) === JSON.stringify([...question.correctAnswers].sort());
  }

  explanationText(question: Question): string | null {
    return question.finalExplanation?.trim()
      || question.aiExplanation?.trim()
      || question.pmaExplanation?.trim()
      || null;
  }

  explanationLabel(question: Question): string {
    if (question.finalExplanation?.trim()) return `Lời giải cuối (${question.finalExplanationSource ?? 'MANUAL'})`;
    if (question.aiExplanation?.trim()) return 'Lời giải ChatGPT (chưa chốt)';
    if (question.pmaExplanation?.trim()) return 'Lời giải PMA (chưa chốt)';
    return 'Chưa có lời giải';
  }

  trackQuestion(_: number, question: Question): number | string {
    return question.id ?? question.externalId ?? question.questionText;
  }

  trackCategory(_: number, category: CategorySummary): number | string {
    return category.id ?? category.code;
  }

  private prepare(): void {
    this.selected.set({});
    this.matching.set({});
    this.revealed.set(false);
    const question = this.current();
    this.matchingChoices.set(
      question?.questionType === 'MATCHING'
        ? [...question.matchingPairs.map(pair => pair.right)].sort(() => Math.random() - 0.5)
        : []
    );
  }
}
