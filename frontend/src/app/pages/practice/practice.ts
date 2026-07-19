import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, forkJoin, take } from 'rxjs';
import {
  AnswerAttemptResponse,
  AnswerHistorySummary,
  CategorySummary,
  PageResponse,
  PracticeAnalytics,
  PracticeDashboard,
  Question
} from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-practice',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './practice.html',
  styleUrl: './practice.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Practice implements OnInit, OnDestroy {
  readonly questions = signal<Question[]>([]);
  readonly categories = signal<CategorySummary[]>([]);
  readonly dashboard = signal<PracticeDashboard>({
    wrongQuestions: 0,
    weeklyAccuracyPercentage: 0,
    weeklyCorrectAttempts: 0,
    weeklyTotalAttempts: 0,
    currentCorrectStreak: 0,
    dueToday: 0
  });
  readonly index = signal(0);
  readonly score = signal(0);
  readonly finished = signal(false);
  readonly categoryCode = signal('');
  readonly wrongOnly = signal(true);
  readonly minIncorrect = signal(1);
  readonly shuffle = signal(true);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly revealed = signal(false);
  readonly selected = signal<Record<string, boolean>>({});
  readonly matching = signal<Record<string, string>>({});
  readonly matchingChoices = signal<string[]>([]);
  readonly submittingAnswer = signal(false);
  readonly answerError = signal('');
  readonly attempt = signal<AnswerAttemptResponse | null>(null);
  readonly history = signal<AnswerAttemptResponse[]>([]);
  readonly historySummary = signal<AnswerHistorySummary | null>(null);
  readonly historyLoading = signal(false);
  readonly totalReviewQuestions = signal(0);
  readonly sessionId = signal(this.createSessionId());
  readonly editingExplanation = signal(false);
  readonly savingExplanation = signal(false);
  readonly explanationDraft = signal('');
  readonly explanationNotes = signal('');
  readonly saveMessage = signal('');
  readonly analytics = signal<PracticeAnalytics | null>(null);
  readonly analyticsDays = signal(14);
  readonly analyticsLoading = signal(false);
  readonly analyticsError = signal('');

  readonly categoryMistakeBars = computed(() => {
    const rows = this.analytics()?.categoryStats?.slice(0, 10) ?? [];
    const maxIncorrect = Math.max(0, ...rows.map(row => row.incorrectAttempts));
    return rows.map(row => ({
      ...row,
      widthPercentage: maxIncorrect === 0 ? 0 : Math.max(4, row.incorrectAttempts * 100 / maxIncorrect)
    }));
  });

  readonly progressDots = computed(() => {
    const rows = this.analytics()?.dailyTrend ?? [];
    const left = 48;
    const right = 708;
    const top = 20;
    const bottom = 190;
    return rows.map((row, index) => ({
      ...row,
      x: rows.length <= 1 ? left : left + index * (right - left) / (rows.length - 1),
      y: bottom - row.accuracyPercentage * (bottom - top) / 100,
      visible: row.totalAttempts > 0
    }));
  });

  readonly progressPolyline = computed(() => this.progressDots()
    .filter(point => point.visible)
    .map(point => `${point.x},${point.y}`)
    .join(' '));

  readonly progressAxisLabels = computed(() => {
    const rows = this.analytics()?.dailyTrend ?? [];
    if (!rows.length) return [];
    const indexes = [...new Set([0, Math.floor((rows.length - 1) / 2), rows.length - 1])];
    const left = 48;
    const right = 708;
    return indexes.map(index => ({
      x: rows.length <= 1 ? left : left + index * (right - left) / (rows.length - 1),
      label: this.shortDate(rows[index].date)
    }));
  });

  readonly progressMessage = computed(() => {
    const summary = this.analytics()?.summary;
    if (!summary) return '';
    switch (summary.progressStatus) {
      case 'IMPROVING': return `Bạn đang tiến bộ +${summary.improvementPercentagePoints.toFixed(1)} điểm %.`;
      case 'DECLINING': return `Độ chính xác giảm ${Math.abs(summary.improvementPercentagePoints).toFixed(1)} điểm %.`;
      case 'STABLE': return `Kết quả ổn định (${summary.improvementPercentagePoints >= 0 ? '+' : ''}${summary.improvementPercentagePoints.toFixed(1)} điểm %).`;
      case 'NEW_BASELINE': return 'Đây là kỳ dữ liệu đầu tiên để làm mốc so sánh.';
      default: return 'Chưa đủ dữ liệu để đánh giá tiến bộ.';
    }
  });

  readonly current = computed(() => this.questions()[this.index()]);
  readonly currentExplanation = computed(() => {
    const question = this.current();
    if (!question) return '';
    return question.finalExplanation?.trim()
      || question.aiExplanation?.trim()
      || question.pmaExplanation?.trim()
      || '';
  });

  readonly pmaExamName = computed(() => {
    const name = this.current()?.examName?.trim();
    if (!name) return 'Không xác định';
    return name.replace(/\s*-\s*\d+\s*$/, '').trim() || name;
  });

  readonly pmaQuestionNumber = computed(() => {
    const name = this.current()?.examName?.trim() ?? '';
    const match = name.match(/(?:-|#|question\s*)\s*(\d+)\s*$/i);
    if (match) return String(Number(match[1]));
    return 'Không xác định';
  });

  private loadSubscription?: Subscription;
  private answerSubscription?: Subscription;
  private historySubscription?: Subscription;
  private analyticsSubscription?: Subscription;
  private requestVersion = 0;

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    forkJoin({
      categories: this.service.categories().pipe(take(1)),
      dashboard: this.service.practiceDashboard().pipe(take(1))
    }).subscribe({
      next: result => {
        this.categories.set(result.categories ?? []);
        this.dashboard.set(result.dashboard);
      },
      error: () => undefined
    });
    this.loadAnalytics();
    this.start();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.answerSubscription?.unsubscribe();
    this.historySubscription?.unsubscribe();
    this.analyticsSubscription?.unsubscribe();
  }

  loadAnalytics(): void {
    this.analyticsSubscription?.unsubscribe();
    this.analyticsLoading.set(true);
    this.analyticsError.set('');
    this.analyticsSubscription = this.service.practiceAnalytics(this.analyticsDays(), 10)
      .pipe(take(1))
      .subscribe({
        next: report => {
          this.analytics.set(report);
          this.analyticsLoading.set(false);
        },
        error: () => {
          this.analyticsError.set('Không tải được thống kê. Hãy bấm tải lại.');
          this.analyticsLoading.set(false);
        }
      });
  }

  start(): void {
    this.loadSubscription?.unsubscribe();
    this.answerSubscription?.unsubscribe();
    const version = ++this.requestVersion;
    this.loading.set(true);
    this.error.set('');
    this.finished.set(false);
    this.sessionId.set(this.createSessionId());

    if (this.wrongOnly()) {
      this.loadSubscription = this.service.wrongQuestions({
        categoryCode: this.categoryCode(),
        minIncorrect: this.minIncorrect(),
        count: 50,
        shuffle: this.shuffle()
      }).pipe(take(1)).subscribe({
        next: response => this.acceptQuestions(version, response.questions ?? [], response.totalQuestions),
        error: () => this.failLoad(version)
      });
      return;
    }

    this.loadSubscription = this.service.random(20, this.categoryCode()).pipe(take(1)).subscribe({
      next: questions => this.acceptQuestions(version, questions ?? [], questions?.length ?? 0),
      error: () => this.failLoad(version)
    });
  }

  toggle(key: string): void {
    if (this.revealed() || this.submittingAnswer()) return;
    if (this.current()?.questionType === 'MCQ') {
      this.selected.set({ [key]: true });
      return;
    }
    this.selected.update(value => ({ ...value, [key]: !value[key] }));
  }

  setMatching(left: string, right: string): void {
    if (this.revealed() || this.submittingAnswer()) return;
    this.matching.update(value => ({ ...value, [left]: right }));
  }

  check(): void {
    const question = this.current();
    if (!question?.id || this.revealed() || this.submittingAnswer() || !this.canCheck()) return;
    this.submittingAnswer.set(true);
    this.answerError.set('');
    this.answerSubscription = this.service.submitAttempt(question.id, {
      selectedAnswers: this.selectedAnswerKeys(),
      matchingAnswers: { ...this.matching() },
      sessionId: this.sessionId()
    }).pipe(take(1)).subscribe({
      next: response => {
        this.attempt.set(response);
        this.historySummary.set(response.summary);
        this.revealed.set(true);
        if (response.correct) this.score.update(value => value + 1);
        this.submittingAnswer.set(false);
        this.loadHistory(question.id!);
        this.refreshDashboard();
      },
      error: () => {
        this.answerError.set('Không lưu được lần trả lời. Hãy thử lại.');
        this.submittingAnswer.set(false);
      }
    });
  }

  next(): void {
    if (this.index() + 1 >= this.questions().length) {
      this.finished.set(true);
      return;
    }
    this.index.update(value => value + 1);
    this.prepare();
  }

  skip(): void { this.next(); }

  canCheck(): boolean {
    const current = this.current();
    if (!current || this.submittingAnswer()) return false;
    if (current.questionType === 'MATCHING') {
      return current.matchingPairs.every(pair => !!this.matching()[pair.left]);
    }
    return Object.values(this.selected()).some(Boolean);
  }

  beginEdit(): void {
    const question = this.current();
    if (!question) return;
    this.explanationDraft.set(this.currentExplanation());
    this.explanationNotes.set(question.explanationReviewNotes ?? '');
    this.saveMessage.set('');
    this.editingExplanation.set(true);
  }

  cancelEdit(): void { this.editingExplanation.set(false); }

  saveExplanation(setAsFinal = false): void {
    const question = this.current();
    if (!question?.id || !this.explanationDraft().trim()) return;
    this.savingExplanation.set(true);
    this.saveMessage.set('');
    const finalExplanation = this.explanationDraft().trim();

    this.service.updateExplanations(question.id, {
      pmaExplanation: question.pmaExplanation ?? null,
      aiExplanation: question.aiExplanation ?? null,
      finalExplanation,
      finalExplanationSource: finalExplanation ? 'MANUAL' : (question.finalExplanationSource ?? 'NONE'),
      explanationReviewStatus: finalExplanation ? 'REVIEWED' : (question.explanationReviewStatus ?? 'PENDING'),
      explanationReviewNotes: this.explanationNotes()
    }).pipe(take(1)).subscribe({
      next: updated => {
        this.questions.update(items => items.map(item => item.id === updated.id ? updated : item));
        this.savingExplanation.set(false);
        this.editingExplanation.set(false);
        this.saveMessage.set('Đã lưu giải thích.');
      },
      error: () => {
        this.savingExplanation.set(false);
        this.saveMessage.set('Không lưu được giải thích.');
      }
    });
  }

  trackCategory(_: number, category: CategorySummary): number | string { return category.id ?? category.code; }

  private acceptQuestions(version: number, questions: Question[], total: number): void {
    if (version !== this.requestVersion) return;
    this.totalReviewQuestions.set(total);
    this.questions.set(questions);
    this.index.set(0);
    this.score.set(0);
    this.prepare();
    this.loading.set(false);
  }

  private failLoad(version: number): void {
    if (version !== this.requestVersion) return;
    this.error.set('Không tải được bộ câu hỏi. Hãy thử lại.');
    this.loading.set(false);
  }

  private prepare(): void {
    this.selected.set({});
    this.matching.set({});
    this.revealed.set(false);
    this.attempt.set(null);
    this.answerError.set('');
    this.editingExplanation.set(false);
    this.saveMessage.set('');
    const question = this.current();
    this.matchingChoices.set(question?.questionType === 'MATCHING'
      ? [...question.matchingPairs.map(pair => pair.right)].sort(() => Math.random() - 0.5)
      : []);
    if (question?.id) this.loadHistory(question.id);
  }

  private loadHistory(questionId: number): void {
    this.historySubscription?.unsubscribe();
    this.historyLoading.set(true);
    this.historySubscription = forkJoin({
      history: this.service.answerHistory(questionId, 0, 10).pipe(take(1)),
      summary: this.service.answerHistorySummary(questionId).pipe(take(1))
    }).subscribe({
      next: result => {
        this.history.set(result.history.content ?? []);
        this.historySummary.set(result.summary);
        this.historyLoading.set(false);
      },
      error: () => this.historyLoading.set(false)
    });
  }

  private refreshDashboard(): void {
    this.service.practiceDashboard().pipe(take(1)).subscribe({ next: value => this.dashboard.set(value) });
  }

  private shortDate(value: string): string {
    const date = new Date(`${value}T00:00:00`);
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(date);
  }

  private selectedAnswerKeys(): string[] {
    return Object.keys(this.selected()).filter(key => this.selected()[key]);
  }

  private createSessionId(): string {
    return typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `practice-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
