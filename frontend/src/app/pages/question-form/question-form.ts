import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import {
  CategorySummary,
  Difficulty,
  ExplanationReviewStatus,
  FinalExplanationSource,
  Question,
  QuestionType
} from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({ selector: 'app-question-form', standalone: true, imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './question-form.html', styleUrl: './question-form.css' })
export class QuestionForm implements OnInit {
  id?: number;
  originalQuestion?: Question;
  categories: CategorySummary[] = [];
  saving = false;
  error = '';
  readonly form;

  constructor(fb: FormBuilder, private readonly route: ActivatedRoute, private readonly router: Router,
              private readonly service: QuestionService) {
    this.form = fb.nonNullable.group({
      questionType: fb.nonNullable.control<QuestionType>('MCQ'),
      questionText: ['', Validators.required],
      optionA: ['', Validators.required], optionB: ['', Validators.required],
      optionC: ['', Validators.required], optionD: ['', Validators.required],
      correctMcq: ['A'], correctA: [true], correctB: [false], correctC: [false], correctD: [false],
      pmaExplanation: [''], aiExplanation: [''], finalExplanation: [''],
      finalExplanationSource: fb.nonNullable.control<FinalExplanationSource>('NONE'),
      explanationReviewStatus: fb.nonNullable.control<ExplanationReviewStatus>('PENDING'),
      explanationReviewNotes: [''],
      difficulty: fb.nonNullable.control<Difficulty>('MEDIUM'),
      reference: [''], tags: [''], categoryCodes: fb.nonNullable.control<string[]>([])
    });
  }

  ngOnInit(): void {
    this.service.categories().subscribe({ next: categories => this.categories = categories });
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isFinite(id) && id > 0) {
      this.id = id;
      this.service.get(id).subscribe({
        next: q => {
          this.originalQuestion = q;
          const byKey = (key: string) => q.options.find(o => o.key === key)?.text ?? '';
          this.form.patchValue({ questionType: q.questionType, questionText: q.questionText,
            optionA: byKey('A'), optionB: byKey('B'), optionC: byKey('C'), optionD: byKey('D'),
            correctMcq: q.correctAnswers[0] ?? 'A', correctA: q.correctAnswers.includes('A'),
            correctB: q.correctAnswers.includes('B'), correctC: q.correctAnswers.includes('C'), correctD: q.correctAnswers.includes('D'),
            pmaExplanation: q.pmaExplanation ?? '', aiExplanation: q.aiExplanation ?? '',
            finalExplanation: q.finalExplanation ?? '', finalExplanationSource: q.finalExplanationSource ?? 'NONE',
            explanationReviewStatus: q.explanationReviewStatus ?? 'PENDING',
            explanationReviewNotes: q.explanationReviewNotes ?? '', difficulty: q.difficulty ?? 'MEDIUM',
            reference: q.reference ?? '', tags: q.tags ?? '', categoryCodes: q.categories?.map(c => c.code) ?? [] });
        }, error: err => this.error = err?.error?.message ?? 'Không thể tải câu hỏi.'
      });
    }
  }

  usePmaExplanation(): void {
    const value = this.form.controls.pmaExplanation.value.trim();
    if (!value) return;
    this.form.patchValue({ finalExplanation: value, finalExplanationSource: 'PMA', explanationReviewStatus: 'PENDING' });
  }

  useAiExplanation(): void {
    const value = this.form.controls.aiExplanation.value.trim();
    if (!value) return;
    this.form.patchValue({ finalExplanation: value, finalExplanationSource: 'AI', explanationReviewStatus: 'PENDING' });
  }

  mergeExplanations(): void {
    const pma = this.form.controls.pmaExplanation.value.trim();
    const ai = this.form.controls.aiExplanation.value.trim();
    if (!pma && !ai) return;
    const merged = [pma ? `PMA:\n${pma}` : '', ai ? `AI bổ sung:\n${ai}` : ''].filter(Boolean).join('\n\n');
    this.form.patchValue({ finalExplanation: merged, finalExplanationSource: 'MERGED', explanationReviewStatus: 'PENDING' });
  }

  clearFinalExplanation(): void {
    this.form.patchValue({ finalExplanation: '', finalExplanationSource: 'NONE', explanationReviewStatus: 'PENDING' });
  }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const v = this.form.getRawValue();
    const flags: Record<string, boolean> = { A: v.correctA, B: v.correctB, C: v.correctC, D: v.correctD };
    const correctAnswers = v.questionType === 'MCQ' ? [v.correctMcq] :
      ['A','B','C','D'].filter(k => flags[k]);
    const request: Question = {
      externalId: this.originalQuestion?.externalId,
      examName: this.originalQuestion?.examName,
      imageUrl: this.originalQuestion?.imageUrl,
      questionType: v.questionType, questionText: v.questionText,
      options: [{key:'A',text:v.optionA},{key:'B',text:v.optionB},{key:'C',text:v.optionC},{key:'D',text:v.optionD}],
      correctAnswers, matchingPairs: [], pmaExplanation: v.pmaExplanation,
      aiExplanation: v.aiExplanation, finalExplanation: v.finalExplanation,
      finalExplanationSource: v.finalExplanationSource, explanationReviewStatus: v.explanationReviewStatus,
      explanationReviewNotes: v.explanationReviewNotes,
      difficulty: v.difficulty, source: this.originalQuestion?.source ?? 'MANUAL', reference: v.reference, tags: v.tags, categoryCodes: v.categoryCodes
    };
    this.saving = true; this.error = '';
    const operation = this.id ? this.service.update(this.id, request) : this.service.create(request);
    operation.subscribe({ next: () => this.router.navigateByUrl('/questions'),
      error: err => { this.error = err?.error?.message ?? 'Không thể lưu câu hỏi.'; this.saving = false; } });
  }
}
