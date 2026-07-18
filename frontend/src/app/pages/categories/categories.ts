import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
import { CategorySummary } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './categories.html',
  styleUrl: './categories.css'
})
export class Categories implements OnInit, OnDestroy {
  categories: CategorySummary[] = [];
  loading = false;
  reclassifying = false;
  success = '';
  error = '';

  private loadSubscription?: Subscription;

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
  }

  reclassify(): void {
    this.reclassifying = true;
    this.success = '';
    this.error = '';
    this.service.reclassifyAll().pipe(
      finalize(() => this.reclassifying = false)
    ).subscribe({
      next: result => {
        this.success = `Đã phân loại lại ${result.reclassifiedQuestions}/${result.totalQuestions} câu hỏi.`;
        this.load();
      },
      error: () => this.error = 'Không thể phân loại lại câu hỏi. Vui lòng thử lại.'
    });
  }

  load(): void {
    this.loadSubscription?.unsubscribe();
    this.loading = true;
    this.error = '';
    this.loadSubscription = this.service.categories('PMP_TOPIC').pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: categories => this.categories = categories,
      error: () => this.error = 'Không tải được categories. Nhấn “Tải lại” để thử lại.'
    });
  }
}
