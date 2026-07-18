import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription, take } from 'rxjs';
import { CategorySummary } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './categories.html',
  styleUrl: './categories.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Categories implements OnInit, OnDestroy {
  readonly categories = signal<CategorySummary[]>([]);
  readonly loading = signal(false);
  readonly reclassifying = signal(false);
  readonly success = signal('');
  readonly error = signal('');

  private loadSubscription?: Subscription;
  private reclassifySubscription?: Subscription;
  private requestVersion = 0;

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.reclassifySubscription?.unsubscribe();
  }

  reclassify(): void {
    this.reclassifySubscription?.unsubscribe();
    this.reclassifying.set(true);
    this.success.set('');
    this.error.set('');

    this.reclassifySubscription = this.service.reclassifyAll().pipe(take(1)).subscribe({
      next: result => {
        this.success.set(`Đã phân loại lại ${result.reclassifiedQuestions}/${result.totalQuestions} câu hỏi.`);
        this.reclassifying.set(false);
        this.load();
      },
      error: () => {
        this.error.set('Không thể phân loại lại câu hỏi. Vui lòng thử lại.');
        this.reclassifying.set(false);
      }
    });
  }

  load(): void {
    this.loadSubscription?.unsubscribe();
    const version = ++this.requestVersion;
    this.loading.set(true);
    this.error.set('');

    this.loadSubscription = this.service.categories('PMP_TOPIC').pipe(take(1)).subscribe({
      next: categories => {
        if (version !== this.requestVersion) return;
        this.categories.set(categories ?? []);
        this.loading.set(false);
      },
      error: () => {
        if (version !== this.requestVersion) return;
        this.error.set('Không tải được categories. Nhấn “Tải lại” để thử lại.');
        this.loading.set(false);
      }
    });
  }

  trackCategory(_: number, category: CategorySummary): number | string {
    return category.id ?? category.code;
  }
}
