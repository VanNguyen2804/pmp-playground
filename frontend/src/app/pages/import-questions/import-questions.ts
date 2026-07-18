import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ImportResult } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-import-questions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './import-questions.html',
  styleUrl: './import-questions.css'
})
export class ImportQuestions {
  selected?: File;
  result?: ImportResult;
  uploading = false;
  error = '';

  constructor(private readonly service: QuestionService) {}

  choose(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selected = input.files?.[0];
    this.result = undefined;
    this.error = '';
  }

  upload(): void {
    if (!this.selected) return;
    this.uploading = true;
    this.error = '';
    this.service.importFile(this.selected).subscribe({
      next: result => {
        this.result = result;
        this.uploading = false;
      },
      error: err => {
        this.error = err?.error?.message ?? 'Upload thất bại.';
        this.uploading = false;
      }
    });
  }
}
