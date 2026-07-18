import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CorrectOption, Difficulty, Question } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-question-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './question-form.html',
  styleUrl: './question-form.css'
})
export class QuestionForm implements OnInit {
  id?: number;
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
      questionText: ['', Validators.required],
      optionA: ['', Validators.required],
      optionB: ['', Validators.required],
      optionC: ['', Validators.required],
      optionD: ['', Validators.required],
      correctOption: fb.nonNullable.control<CorrectOption>('A', Validators.required),
      explanation: [''],
      category: [''],
      difficulty: fb.nonNullable.control<Difficulty>('MEDIUM'),
      source: [''],
      reference: [''],
      tags: ['']
    });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (Number.isFinite(id) && id > 0) {
      this.id = id;
      this.service.get(id).subscribe({
        next: q => this.form.patchValue({
          questionText: q.questionText,
          optionA: q.optionA,
          optionB: q.optionB,
          optionC: q.optionC,
          optionD: q.optionD,
          correctOption: q.correctOption,
          explanation: q.explanation ?? '',
          category: q.category ?? '',
          difficulty: q.difficulty ?? 'MEDIUM',
          source: q.source ?? '',
          reference: q.reference ?? '',
          tags: q.tags ?? ''
        }),
        error: err => this.error = err?.error?.message ?? 'Không thể tải câu hỏi.'
      });
    }
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.error = '';
    const request = this.form.getRawValue() as Question;
    const operation = this.id ? this.service.update(this.id, request) : this.service.create(request);
    operation.subscribe({
      next: () => this.router.navigateByUrl('/questions'),
      error: err => {
        this.error = err?.error?.message ?? 'Không thể lưu câu hỏi.';
        this.saving = false;
      }
    });
  }
}
