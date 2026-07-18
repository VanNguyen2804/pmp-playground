import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ExplanationReviewStatus, FinalExplanationSource, Question } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-explanation-review',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './explanation-review.html',
  styleUrl: './explanation-review.css'
})
export class ExplanationReview implements OnInit {
  question?: Question;
  saving = false;
  error = '';
  readonly form;

  constructor(
    fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly service: QuestionService
  ) {
    this.form = fb.nonNullable.group({
      pmaExplanation: [''],
      aiExplanation: [''],
      finalExplanation: [''],
      finalExplanationSource: fb.nonNullable.control<FinalExplanationSource>('NONE'),
      explanationReviewStatus: fb.nonNullable.control<ExplanationReviewStatus>('PENDING'),
      explanationReviewNotes: ['']
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) {
      this.error = 'Question ID không hợp lệ.';
      return;
    }
    this.service.get(id).subscribe({
      next: question => {
        this.question = question;
        this.form.patchValue({
          pmaExplanation: question.pmaExplanation ?? '',
          aiExplanation: question.aiExplanation ?? '',
          finalExplanation: question.finalExplanation ?? '',
          finalExplanationSource: question.finalExplanationSource ?? 'NONE',
          explanationReviewStatus: question.explanationReviewStatus ?? 'PENDING',
          explanationReviewNotes: question.explanationReviewNotes ?? ''
        });
      },
      error: err => this.error = err?.error?.message ?? 'Không thể tải câu hỏi.'
    });
  }

  usePma(): void {
    const text = this.form.controls.pmaExplanation.value.trim();
    if (text) this.form.patchValue({ finalExplanation: text, finalExplanationSource: 'PMA', explanationReviewStatus: 'PENDING' });
  }

  useAi(): void {
    const text = this.form.controls.aiExplanation.value.trim();
    if (text) this.form.patchValue({ finalExplanation: text, finalExplanationSource: 'AI', explanationReviewStatus: 'PENDING' });
  }

  merge(): void {
    const pma = this.form.controls.pmaExplanation.value.trim();
    const ai = this.form.controls.aiExplanation.value.trim();
    if (!pma && !ai) return;
    const merged = [pma ? `PMA:\n${pma}` : '', ai ? `ChatGPT bổ sung:\n${ai}` : ''].filter(Boolean).join('\n\n');
    this.form.patchValue({ finalExplanation: merged, finalExplanationSource: 'MERGED', explanationReviewStatus: 'PENDING' });
  }

  clearFinal(): void {
    this.form.patchValue({ finalExplanation: '', finalExplanationSource: 'NONE', explanationReviewStatus: 'PENDING' });
  }

  save(): void {
    if (!this.question?.id) return;
    this.saving = true;
    this.error = '';
    this.service.updateExplanations(this.question.id, this.form.getRawValue()).subscribe({
      next: () => this.router.navigateByUrl('/questions'),
      error: err => {
        this.error = err?.error?.message ?? 'Không thể lưu lời giải.';
        this.saving = false;
      }
    });
  }
}
