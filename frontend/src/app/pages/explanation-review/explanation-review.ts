import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, take } from 'rxjs';
import { ExplanationReviewStatus, FinalExplanationSource, Question } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-explanation-review',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './explanation-review.html',
  styleUrl: './explanation-review.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExplanationReview implements OnInit, OnDestroy {
  readonly question = signal<Question | undefined>(undefined);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form;

  private loadSubscription?: Subscription;
  private saveSubscription?: Subscription;

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
      this.error.set('Question ID không hợp lệ.');
      return;
    }

    this.loading.set(true);
    this.loadSubscription = this.service.get(id).pipe(take(1)).subscribe({
      next: question => {
        this.question.set(question);
        this.form.patchValue({
          pmaExplanation: question.pmaExplanation ?? '',
          aiExplanation: question.aiExplanation ?? '',
          finalExplanation: question.finalExplanation ?? '',
          finalExplanationSource: question.finalExplanationSource ?? 'NONE',
          explanationReviewStatus: question.explanationReviewStatus ?? 'PENDING',
          explanationReviewNotes: question.explanationReviewNotes ?? ''
        });
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Không tải được câu hỏi. Xem modal lỗi để biết chi tiết.');
        this.loading.set(false);
      }
    });
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.saveSubscription?.unsubscribe();
  }

  usePma(): void {
    const text = this.form.controls.pmaExplanation.value.trim();
    if (text) {
      this.form.patchValue({
        finalExplanation: text,
        finalExplanationSource: 'PMA',
        explanationReviewStatus: 'PENDING'
      });
    }
  }

  useAi(): void {
    const text = this.form.controls.aiExplanation.value.trim();
    if (text) {
      this.form.patchValue({
        finalExplanation: text,
        finalExplanationSource: 'AI',
        explanationReviewStatus: 'PENDING'
      });
    }
  }

  merge(): void {
    const pma = this.form.controls.pmaExplanation.value.trim();
    const ai = this.form.controls.aiExplanation.value.trim();
    if (!pma && !ai) return;

    const merged = [
      pma ? `PMA:\n${pma}` : '',
      ai ? `ChatGPT bổ sung:\n${ai}` : ''
    ].filter(Boolean).join('\n\n');

    this.form.patchValue({
      finalExplanation: merged,
      finalExplanationSource: 'MERGED',
      explanationReviewStatus: 'PENDING'
    });
  }

  clearFinal(): void {
    this.form.patchValue({
      finalExplanation: '',
      finalExplanationSource: 'NONE',
      explanationReviewStatus: 'PENDING'
    });
  }

  save(): void {
    const question = this.question();
    if (!question?.id) return;

    this.saveSubscription?.unsubscribe();
    this.saving.set(true);
    this.error.set('');

    this.saveSubscription = this.service.updateExplanations(question.id, this.form.getRawValue()).pipe(take(1)).subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigateByUrl('/questions');
      },
      error: () => {
        this.error.set('Không thể lưu lời giải. Xem modal lỗi để biết chi tiết.');
        this.saving.set(false);
      }
    });
  }
}
