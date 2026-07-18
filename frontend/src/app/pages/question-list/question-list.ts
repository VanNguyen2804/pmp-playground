import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, take } from 'rxjs';
import {
  CategorySummary,
  Difficulty,
  ExplanationReviewStatus,
  Question,
  QuestionType,
  Taxonomy
} from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-question-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './question-list.html',
  styleUrl: './question-list.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionList implements OnInit, OnDestroy {
  readonly questions = signal<Question[]>([]);
  readonly categories = signal<CategorySummary[]>([]);

  readonly search = signal('');
  readonly categoryCode = signal('');
  readonly taxonomy = signal<Taxonomy | ''>('');
  readonly difficulty = signal<Difficulty | ''>('');
  readonly questionType = signal<QuestionType | ''>('');
  readonly reviewStatus = signal<ExplanationReviewStatus | ''>('');

  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly error = signal('');

  private loadSubscription?: Subscription;
  private categoriesSubscription?: Subscription;
  private requestVersion = 0;

  constructor(
    private readonly service: QuestionService,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.categoryCode.set(this.route.snapshot.queryParamMap.get('categoryCode') ?? '');
    this.loadCategories();
    this.load();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.categoriesSubscription?.unsubscribe();
  }

  load(page = 0): void {
    this.loadSubscription?.unsubscribe();
    const version = ++this.requestVersion;

    this.loading.set(true);
    this.error.set('');

    this.loadSubscription = this.service.list({
      search: this.search(),
      categoryCode: this.categoryCode(),
      taxonomy: this.taxonomy(),
      difficulty: this.difficulty(),
      questionType: this.questionType(),
      reviewStatus: this.reviewStatus(),
      page,
      size: 20
    }).pipe(take(1)).subscribe({
      next: result => {
        if (version !== this.requestVersion) return;

        // Update the result and loading state in the same response callback.
        // Signals immediately schedule Angular rendering in zoneless mode.
        this.questions.set(result.content ?? []);
        this.page.set(result.number ?? page);
        this.totalPages.set(result.totalPages ?? 0);
        this.totalElements.set(result.totalElements ?? 0);
        this.loading.set(false);
      },
      error: () => {
        if (version !== this.requestVersion) return;
        this.error.set('Không tải được danh sách câu hỏi. Xem modal lỗi hoặc nhấn “Tải lại dữ liệu” để thử lại.');
        this.loading.set(false);
      }
    });
  }

  refresh(): void {
    this.load(this.page());
    this.loadCategories();
  }

  clearFilters(): void {
    this.search.set('');
    this.categoryCode.set('');
    this.taxonomy.set('');
    this.difficulty.set('');
    this.questionType.set('');
    this.reviewStatus.set('');
    this.load(0);
  }

  remove(question: Question): void {
    if (!question.id || !confirm('Xóa câu hỏi này?')) return;

    this.service.delete(question.id).pipe(take(1)).subscribe({
      next: () => this.load(this.page()),
      error: () => this.error.set('Không thể xóa câu hỏi. Vui lòng thử lại.')
    });
  }

  trackQuestion(_: number, question: Question): number | string {
    return question.id ?? question.externalId ?? question.questionText;
  }

  trackCategory(_: number, category: CategorySummary): number | string {
    return category.id ?? category.code;
  }

  private loadCategories(): void {
    this.categoriesSubscription?.unsubscribe();
    this.categoriesSubscription = this.service.categories().pipe(take(1)).subscribe({
      next: categories => this.categories.set(categories ?? []),
      error: () => this.categories.set([])
    });
  }
}
