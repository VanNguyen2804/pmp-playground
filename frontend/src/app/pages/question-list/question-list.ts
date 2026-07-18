import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize, Subscription } from 'rxjs';
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
  styleUrl: './question-list.css'
})
export class QuestionList implements OnInit, OnDestroy {
  questions: Question[] = [];
  categories: CategorySummary[] = [];
  search = '';
  categoryCode = '';
  taxonomy: Taxonomy | '' = '';
  difficulty: Difficulty | '' = '';
  questionType: QuestionType | '' = '';
  reviewStatus: ExplanationReviewStatus | '' = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error = '';

  private loadSubscription?: Subscription;
  private categoriesSubscription?: Subscription;

  constructor(private readonly service: QuestionService, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.categoryCode = this.route.snapshot.queryParamMap.get('categoryCode') ?? '';
    this.loadCategories();
    this.load();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.categoriesSubscription?.unsubscribe();
  }

  load(page = 0): void {
    // Cancel a hanging/older request before starting a new refresh.
    this.loadSubscription?.unsubscribe();
    this.loading = true;
    this.error = '';

    this.loadSubscription = this.service.list({
      search: this.search,
      categoryCode: this.categoryCode,
      taxonomy: this.taxonomy,
      difficulty: this.difficulty,
      questionType: this.questionType,
      reviewStatus: this.reviewStatus,
      page,
      size: 20
    }).pipe(
      finalize(() => this.loading = false)
    ).subscribe({
      next: result => {
        this.questions = result.content;
        this.page = result.number;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
      },
      error: () => {
        this.error = 'Không tải được danh sách câu hỏi. Xem modal lỗi hoặc nhấn “Tải lại dữ liệu” để thử lại.';
      }
    });
  }

  refresh(): void {
    this.load(this.page);
    this.loadCategories();
  }

  clearFilters(): void {
    this.search = '';
    this.categoryCode = '';
    this.taxonomy = '';
    this.difficulty = '';
    this.questionType = '';
    this.reviewStatus = '';
    this.load(0);
  }

  remove(question: Question): void {
    if (!question.id || !confirm('Xóa câu hỏi này?')) return;
    this.service.delete(question.id).subscribe({
      next: () => this.load(this.page),
      error: () => this.error = 'Không thể xóa câu hỏi. Vui lòng thử lại.'
    });
  }

  private loadCategories(): void {
    this.categoriesSubscription?.unsubscribe();
    this.categoriesSubscription = this.service.categories().subscribe({
      next: categories => this.categories = categories,
      error: () => {
        // The global interceptor already shows the full backend response.
        this.categories = [];
      }
    });
  }
}
