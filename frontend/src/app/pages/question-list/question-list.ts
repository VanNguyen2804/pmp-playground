import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
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
export class QuestionList implements OnInit {
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

  constructor(private readonly service: QuestionService, private readonly route: ActivatedRoute) {}

  ngOnInit(): void {
    this.categoryCode = this.route.snapshot.queryParamMap.get('categoryCode') ?? '';
    this.service.categories().subscribe({ next: c => this.categories = c });
    this.load();
  }

  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.service.list({ search: this.search, categoryCode: this.categoryCode, taxonomy: this.taxonomy,
      difficulty: this.difficulty, questionType: this.questionType, reviewStatus: this.reviewStatus,
      page, size: 20 }).subscribe({
      next: result => {
        this.questions = result.content;
        this.page = result.number;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: () => { this.error = ''; this.loading = false; }
    });
  }

  clearFilters(): void {
    this.search = ''; this.categoryCode = ''; this.taxonomy = ''; this.difficulty = '';
    this.questionType = ''; this.reviewStatus = ''; this.load(0);
  }

  remove(question: Question): void {
    if (!question.id || !confirm('Xóa câu hỏi này?')) return;
    this.service.delete(question.id).subscribe({ next: () => this.load(this.page),
      error: () => this.error = '' });
  }
}
