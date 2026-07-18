import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CorrectOption, Question } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-practice',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './practice.html',
  styleUrl: './practice.css'
})
export class Practice implements OnInit {
  questions: Question[] = [];
  index = 0;
  selected?: CorrectOption;
  revealed = false;
  score = 0;
  finished = false;
  category = '';
  loading = false;
  error = '';

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.start();
  }

  get current(): Question | undefined { return this.questions[this.index]; }

  start(): void {
    this.loading = true;
    this.error = '';
    this.service.random(10, this.category).subscribe({
      next: questions => {
        this.questions = questions;
        this.index = 0;
        this.score = 0;
        this.finished = false;
        this.selected = undefined;
        this.revealed = false;
        this.loading = false;
      },
      error: err => {
        this.error = err?.error?.message ?? 'Không thể tạo bài luyện tập.';
        this.loading = false;
      }
    });
  }

  choose(option: CorrectOption): void {
    if (this.revealed) return;
    this.selected = option;
  }

  check(): void {
    if (!this.selected || !this.current || this.revealed) return;
    this.revealed = true;
    if (this.selected === this.current.correctOption) this.score++;
  }

  next(): void {
    if (this.index + 1 >= this.questions.length) {
      this.finished = true;
      return;
    }
    this.index++;
    this.selected = undefined;
    this.revealed = false;
  }

  optionText(q: Question, option: CorrectOption): string {
    return q[`option${option}` as keyof Question] as string;
  }
}
