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
  error = '';

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.loading = true;
    this.service.categories().subscribe({
      next: categories => { this.categories = categories; this.loading = false; },
      error: err => { this.error = err?.error?.message ?? 'Không thể tải categories.'; this.loading = false; }
    });
  }

  get pmbokCategories(): CategorySummary[] { return this.categories.filter(c => c.taxonomy === 'PMBOK8_DOMAIN'); }
  get pmaCategories(): CategorySummary[] { return this.categories.filter(c => c.taxonomy === 'PMA_HANDOUT_TOPIC'); }
}
