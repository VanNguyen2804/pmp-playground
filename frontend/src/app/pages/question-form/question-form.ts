import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, take } from 'rxjs';
import {
  CategorySummary,
  Difficulty,
  ExplanationReviewStatus,
  FinalExplanationSource,
  Question,
  QuestionType
} from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-question-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './question-form.html',
  styleUrl: './question-form.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionForm implements OnInit, OnDestroy {
  readonly id = signal<number | undefined>(undefined);
  readonly originalQuestion = signal<Question | undefined>(undefined);
  readonly categories = signal<CategorySummary[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form;

  private categoriesSubscription?: Subscription;
  private questionSubscription?: Subscription;
  private saveSubscription?: Subscription;

  constructor(
    fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly service: QuestionService
  ) {
    this.form = fb.nonNullable.group({
      questionType: fb.nonNullable.control<QuestionType>('MCQ'),
      questionText: ['', Validators.required],
      optionA: ['', Validators.required],
      optionB: ['', Validators.required],
      optionC: ['', Validators.required],
      optionD: ['', Validators.required],
      correctMcq: ['A'],
      correctA: [true],
      correctB: [false],
      correctC: [false],
      correctD: [false],
      pmaExplanation: [''],
      aiExplanation: [''],
      finalExplanation: [''],
      finalExplanationSource: fb.nonNullable.control<FinalExplanationSource>('NONE'),
      explanationReviewStatus: fb.nonNullable.control<ExplanationReviewStatus>('PENDING'),
      explanationReviewNotes: [''],
      difficulty: fb.nonNullable.control<Difficulty>('MEDIUM'),
      reference: [''],
      tags: [''],
      categoryCodes: fb.nonNullable.control<string[]>([])
    });
  }

  ngOnInit(): void {
    this.categoriesSubscription = this.service.categories().pipe(take(1)).subscribe({
      next: categories => this.categories.set(categories ?? []),
      error: () => this.categories.set([])
    });

    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id) || id <= 0) return;

    this.id.set(id);
    this.loading.set(true);
    this.questionSubscription = this.service.get(id).pipe(take(1)).subscribe({
      next: question => {
        this.originalQuestion.set(question);
        const byKey = (key: string) => question.options.find(option => option.key === key)?.text ?? '';
        this.form.patchValue({
          questionType: question.questionType,
          questionText: question.questionText,
          optionA: byKey('A'),
          optionB: byKey('B'),
          optionC: byKey('C'),
          optionD: byKey('D'),
          correctMcq: question.correctAnswers[0] ?? 'A',
          correctA: question.correctAnswers.includes('A'),
          correctB: question.correctAnswers.includes('B'),
          correctC: question.correctAnswers.includes('C'),
          correctD: question.correctAnswers.includes('D'),
          pmaExplanation: question.pmaExplanation ?? '',
          aiExplanation: question.aiExplanation ?? '',
          finalExplanation: question.finalExplanation ?? '',
          finalExplanationSource: question.finalExplanationSource ?? 'NONE',
          explanationReviewStatus: question.explanationReviewStatus ?? 'PENDING',
          explanationReviewNotes: question.explanationReviewNotes ?? '',
          difficulty: question.difficulty ?? 'MEDIUM',
          reference: question.reference ?? '',
          tags: question.tags ?? '',
          categoryCodes: question.categories?.map(category => category.code) ?? []
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
    this.categoriesSubscription?.unsubscribe();
    this.questionSubscription?.unsubscribe();
    this.saveSubscription?.unsubscribe();
  }

  usePmaExplanation(): void {
    const value = this.form.controls.pmaExplanation.value.trim();
    if (!value) return;
    this.form.patchValue({
      finalExplanation: value,
      finalExplanationSource: 'PMA',
      explanationReviewStatus: 'PENDING'
    });
  }

  useAiExplanation(): void {
    const value = this.form.controls.aiExplanation.value.trim();
    if (!value) return;
    this.form.patchValue({
      finalExplanation: value,
      finalExplanationSource: 'AI',
      explanationReviewStatus: 'PENDING'
    });
  }

  mergeExplanations(): void {
    const pma = this.form.controls.pmaExplanation.value.trim();
    const ai = this.form.controls.aiExplanation.value.trim();
    if (!pma && !ai) return;

    const merged = [
      pma ? `PMA:\n${pma}` : '',
      ai ? `AI bổ sung:\n${ai}` : ''
    ].filter(Boolean).join('\n\n');

    this.form.patchValue({
      finalExplanation: merged,
      finalExplanationSource: 'MERGED',
      explanationReviewStatus: 'PENDING'
    });
  }

  clearFinalExplanation(): void {
    this.form.patchValue({
      finalExplanation: '',
      finalExplanationSource: 'NONE',
      explanationReviewStatus: 'PENDING'
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const flags: Record<string, boolean> = {
      A: value.correctA,
      B: value.correctB,
      C: value.correctC,
      D: value.correctD
    };
    const correctAnswers = value.questionType === 'MCQ'
      ? [value.correctMcq]
      : ['A', 'B', 'C', 'D'].filter(key => flags[key]);
    const original = this.originalQuestion();

    const request: Question = {
      externalId: original?.externalId,
      examName: original?.examName,
      imageUrl: original?.imageUrl,
      questionType: value.questionType,
      questionText: value.questionText,
      options: [
        { key: 'A', text: value.optionA },
        { key: 'B', text: value.optionB },
        { key: 'C', text: value.optionC },
        { key: 'D', text: value.optionD }
      ],
      correctAnswers,
      matchingPairs: [],
      pmaExplanation: value.pmaExplanation,
      aiExplanation: value.aiExplanation,
      finalExplanation: value.finalExplanation,
      finalExplanationSource: value.finalExplanationSource,
      explanationReviewStatus: value.explanationReviewStatus,
      explanationReviewNotes: value.explanationReviewNotes,
      difficulty: value.difficulty,
      source: original?.source ?? 'MANUAL',
      reference: value.reference,
      tags: value.tags,
      categoryCodes: value.categoryCodes
    };

    this.saveSubscription?.unsubscribe();
    this.saving.set(true);
    this.error.set('');
    const id = this.id();
    const operation = id ? this.service.update(id, request) : this.service.create(request);

    this.saveSubscription = operation.pipe(take(1)).subscribe({
      next: () => {
        this.saving.set(false);
        void this.router.navigateByUrl('/questions');
      },
      error: () => {
        this.error.set('Không thể lưu câu hỏi. Xem modal lỗi để biết chi tiết.');
        this.saving.set(false);
      }
    });
  }
}
