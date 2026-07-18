import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Difficulty, Question } from '../../models/question';
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
  search = '';
  category = '';
  difficulty: Difficulty | '' = '';
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error = '';

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.service.list({
      search: this.search,
      category: this.category,
      difficulty: this.difficulty,
      page,
      size: 20
    }).subscribe({
      next: result => {
        this.questions = result.content;
        this.page = result.number;
        this.totalPages = result.totalPages;
        this.totalElements = result.totalElements;
        this.loading = false;
      },
      error: err => {
        this.error = err?.error?.message ?? 'Không thể tải danh sách câu hỏi.';
        this.loading = false;
      }
    });
  }

  clearFilters(): void {
    this.search = '';
    this.category = '';
    this.difficulty = '';
    this.load(0);
  }

  remove(question: Question): void {
    if (!question.id || !confirm('Xóa câu hỏi này?')) return;
    this.service.delete(question.id).subscribe({
      next: () => this.load(this.page),
      error: err => this.error = err?.error?.message ?? 'Không thể xóa câu hỏi.'
    });
  }
}
