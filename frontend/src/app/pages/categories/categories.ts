import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CategorySummary } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './categories.html',
  styleUrl: './categories.css'
})
export class Categories implements OnInit {
  categories: CategorySummary[] = [];
  loading = false;
  reclassifying = false;
  success = '';

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.load();
  }

  reclassify(): void {
    this.reclassifying = true;
    this.success = '';
    this.service.reclassifyAll().subscribe({
      next: result => {
        this.success = `Đã phân loại lại ${result.reclassifiedQuestions}/${result.totalQuestions} câu hỏi.`;
        this.reclassifying = false;
        this.load();
      },
      error: () => this.reclassifying = false
    });
  }

  private load(): void {
    this.loading = true;
    this.service.categories('PMP_TOPIC').subscribe({
      next: categories => {
        this.categories = categories;
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }
}
