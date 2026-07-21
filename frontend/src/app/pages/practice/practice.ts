import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, finalize, forkJoin, take } from 'rxjs';
import {
  AnswerAttemptResponse,
  AnswerHistorySummary,
  CategorySummary,
  PracticeDashboard,
  PracticeSessionReport,
  Question,
  StudyAnnotation,
  StudyHighlight,
  StudyHighlightColor,
  StudyHighlightTarget
} from '../../models/question';
import { QuestionService } from '../../services/question.service';

interface TextSegment {
  text: string;
  highlight?: StudyHighlight;
}

interface PendingTextSelection {
  target: StudyHighlightTarget;
  targetKey?: string | null;
  startOffset: number;
  endOffset: number;
  text: string;
}

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
    todayAccuracyPercentage: 0,
    todayCorrectAttempts: 0,
    todayTotalAttempts: 0,
    yesterdayAccuracyPercentage: 0,
    yesterdayCorrectAttempts: 0,
    yesterdayTotalAttempts: 0,
    dailyAccuracyDeltaPercentagePoints: 0,
    dailyPerformanceStatus: 'NO_DATA',
    averageDailyAccuracyPercentage: 0,
    activePerformanceDays: 0,
    dailyPerformanceHistory: [],
    answeredQuestions: 0,
    totalQuestions: 0,
    questionBankCoveragePercentage: 0,
    currentCorrectStreak: 0,
    dueToday: 0,
    timeZone: 'UTC'
  });
  readonly index = signal(0);
  readonly score = signal(0);
  readonly finished = signal(false);
  readonly categoryCode = signal('');
  readonly wrongOnly = signal(false);
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
  readonly sessionReport = signal<PracticeSessionReport | null>(null);
  readonly sessionReportLoading = signal(false);
  readonly sessionReportError = signal('');

  readonly annotations = signal<Record<number, StudyAnnotation>>({});
  readonly annotationLoading = signal(false);
  readonly annotationSavingIds = signal<Set<number>>(new Set());
  readonly annotationMessage = signal('');
  readonly pendingSelection = signal<PendingTextSelection | null>(null);
  readonly activeHighlightId = signal<string | null>(null);
  readonly noteOpen = signal(false);
  readonly noteDraft = signal('');

  readonly averagePerformanceValue = computed(() => {
    const value = this.dashboard();
    return value.activePerformanceDays === 0
      ? 'Chưa có dữ liệu'
      : `${value.averageDailyAccuracyPercentage.toFixed(1)}%`;
  });

  readonly averagePerformanceDetail = computed(() => {
    const days = this.dashboard().activePerformanceDays;
    if (days === 0) return 'Chưa có ngày nào được ghi nhận';
    return `Trung bình của ${days} ngày có làm bài`;
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

  readonly currentAnnotation = computed<StudyAnnotation>(() => {
    const questionId = this.current()?.id;
    if (!questionId) return this.emptyAnnotation(0);
    return this.annotations()[questionId] ?? this.emptyAnnotation(questionId);
  });

  readonly currentAnnotationSaving = computed(() => {
    const questionId = this.current()?.id;
    return !!questionId && this.annotationSavingIds().has(questionId);
  });

  readonly hasNote = computed(() => !!this.currentAnnotation().note?.trim());
  readonly questionSegments = computed(() => {
    const question = this.current();
    return question ? this.buildSegments('QUESTION', null, question.questionText) : [];
  });
  readonly optionSegmentsByKey = computed<Record<string, TextSegment[]>>(() => {
    const question = this.current();
    if (!question) return {};
    return Object.fromEntries(question.options.map(option => [
      option.key,
      this.buildSegments('OPTION', option.key, option.text)
    ]));
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

  readonly sessionCategoryBars = computed(() => {
    const rows = this.sessionReport()?.categoryResults
      ?.filter(item => item.incorrectAnswers > 0)
      .slice(0, 10) ?? [];
    const maximum = Math.max(0, ...rows.map(item => item.incorrectAnswers));
    return rows.map(item => ({
      ...item,
      widthPercentage: maximum === 0 ? 0 : Math.max(6, item.incorrectAnswers * 100 / maximum)
    }));
  });

  private loadSubscription?: Subscription;
  private answerSubscription?: Subscription;
  private historySubscription?: Subscription;
  private annotationLoadSubscription?: Subscription;
  private sessionReportSubscription?: Subscription;
  private readonly annotationSaveSubscriptions = new Map<number, Subscription>();
  private readonly annotationSaveTimers = new Map<number, ReturnType<typeof setTimeout>>();
  private readonly annotationRevisions = new Map<number, number>();
  private readonly annotationDirtyQuestions = new Set<number>();
  private requestVersion = 0;
  private readonly userTimeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    forkJoin({
      categories: this.service.categories().pipe(take(1)),
      dashboard: this.service.practiceDashboard(this.userTimeZone).pipe(take(1))
    }).subscribe({
      next: result => {
        this.categories.set(result.categories ?? []);
        this.dashboard.set(result.dashboard);
      },
      error: () => undefined
    });
    this.start();
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.answerSubscription?.unsubscribe();
    this.historySubscription?.unsubscribe();
    this.annotationLoadSubscription?.unsubscribe();
    this.sessionReportSubscription?.unsubscribe();
    this.annotationSaveSubscriptions.forEach(subscription => subscription.unsubscribe());
    this.annotationSaveTimers.forEach(timer => clearTimeout(timer));
  }

  onWrongOnlyChange(value: boolean): void {
    this.wrongOnly.set(value);
    this.start();
  }

  onMinIncorrectChange(value: string | number): void {
    this.minIncorrect.set(Number(value));
    this.start();
  }

  onCategoryChange(value: string): void {
    this.categoryCode.set(value ?? '');
    this.start();
  }

  onShuffleChange(value: boolean): void {
    this.shuffle.set(value);
    this.start();
  }

  start(): void {
    this.loadSubscription?.unsubscribe();
    this.answerSubscription?.unsubscribe();
    const version = ++this.requestVersion;
    this.loading.set(true);
    this.error.set('');
    this.finished.set(false);
    this.sessionReport.set(null);
    this.sessionReportError.set('');
    this.sessionReportLoading.set(false);
    this.sessionReportSubscription?.unsubscribe();
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

    this.loadSubscription = this.service.priorityPracticeQuestions(20, this.categoryCode()).pipe(take(1)).subscribe({
      next: questions => this.acceptQuestions(version, questions ?? [], questions?.length ?? 0),
      error: () => this.failLoad(version)
    });
  }

  toggle(key: string): void {
    if (window.getSelection()?.toString().trim()) return;
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
      this.finishSession();
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

  optionSegments(key: string): TextSegment[] {
    return this.optionSegmentsByKey()[key] ?? [];
  }

  captureSelection(target: StudyHighlightTarget, targetKey: string | null, event: MouseEvent): void {
    const root = event.currentTarget as HTMLElement | null;
    const selection = window.getSelection();
    if (!root || !selection || selection.rangeCount === 0 || selection.isCollapsed) return;
    const range = selection.getRangeAt(0);
    if (!root.contains(range.commonAncestorContainer)) return;

    const startOffset = this.offsetWithin(root, range.startContainer, range.startOffset);
    const endOffset = this.offsetWithin(root, range.endContainer, range.endOffset);
    const sourceText = this.sourceText(target, targetKey);
    if (startOffset < 0 || endOffset <= startOffset || endOffset > sourceText.length) return;
    const selectedText = sourceText.substring(startOffset, endOffset);
    if (!selectedText.trim()) return;

    this.pendingSelection.set({ target, targetKey, startOffset, endOffset, text: selectedText });
    this.activeHighlightId.set(null);
    this.annotationMessage.set(`Đã chọn: “${selectedText.trim().slice(0, 80)}${selectedText.trim().length > 80 ? '…' : ''}”`);
  }

  activateHighlight(highlight: StudyHighlight, event: MouseEvent): void {
    if (window.getSelection()?.toString().trim()) return;
    event.preventDefault();
    event.stopPropagation();
    window.getSelection()?.removeAllRanges();
    this.pendingSelection.set(null);
    this.activeHighlightId.set(highlight.id);
    this.annotationMessage.set('Đã chọn highlight. Chọn màu khác để đổi màu hoặc bấm Xóa.');
  }

  applyHighlightColor(color: StudyHighlightColor): void {
    const questionId = this.current()?.id;
    if (!questionId) return;
    const current = this.currentAnnotation();
    const activeId = this.activeHighlightId();
    let highlights: StudyHighlight[];

    if (activeId) {
      highlights = current.highlights.map(item => item.id === activeId ? { ...item, color } : item);
    } else {
      const selection = this.pendingSelection();
      if (!selection) {
        this.annotationMessage.set('Hãy bôi đen một keyword trong câu hỏi hoặc đáp án trước.');
        return;
      }
      const created: StudyHighlight = {
        id: this.createHighlightId(),
        target: selection.target,
        targetKey: selection.targetKey ?? null,
        startOffset: selection.startOffset,
        endOffset: selection.endOffset,
        text: selection.text,
        color
      };
      highlights = current.highlights
        .filter(item => !this.overlaps(item, created))
        .concat(created);
    }

    this.updateAnnotation(questionId, { ...current, highlights });
    this.clearTextSelection();
    this.annotationMessage.set('Đã cập nhật highlight. Hệ thống đang tự lưu.');
  }

  removeActiveHighlight(): void {
    const questionId = this.current()?.id;
    const activeId = this.activeHighlightId();
    if (!questionId || !activeId) return;
    const current = this.currentAnnotation();
    this.updateAnnotation(questionId, {
      ...current,
      highlights: current.highlights.filter(item => item.id !== activeId)
    });
    this.clearTextSelection();
    this.annotationMessage.set('Đã xóa highlight. Hệ thống đang tự lưu.');
  }

  clearTextSelection(): void {
    window.getSelection()?.removeAllRanges();
    this.pendingSelection.set(null);
    this.activeHighlightId.set(null);
  }

  openNotes(): void {
    this.noteDraft.set(this.currentAnnotation().note ?? '');
    this.noteOpen.set(true);
  }

  closeNotes(): void { this.noteOpen.set(false); }

  saveNote(): void {
    const questionId = this.current()?.id;
    if (!questionId) return;
    const current = this.currentAnnotation();
    this.updateAnnotation(questionId, { ...current, note: this.noteDraft().trim() || null }, 0);
    this.noteOpen.set(false);
    this.annotationMessage.set('Đã cập nhật ghi chú. Hệ thống đang tự lưu.');
  }

  deleteNote(): void {
    this.noteDraft.set('');
    this.saveNote();
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

  reloadSessionReport(): void {
    this.loadSessionReport();
  }

  suggestionPriorityLabel(priority: string): string {
    switch (priority) {
      case 'HIGH': return 'Ưu tiên cao';
      case 'MEDIUM': return 'Cần củng cố';
      default: return 'Ôn lại';
    }
  }

  trackSessionCategory(_: number, item: { categoryCode: string }): string {
    return item.categoryCode;
  }

  trackSuggestion(_: number, item: { categoryCode: string }): string {
    return item.categoryCode;
  }

  shouldShowQuestionImage(question: Question): boolean {
    const imageUrl = question.imageUrl?.trim();
    if (!imageUrl) return false;
    if (!imageUrl.startsWith('/chart-guides/')) return true;
    return question.categories?.some(category => category.code === 'TOPIC_CHART')
      || question.categoryCodes?.includes('TOPIC_CHART')
      || false;
  }

  trackCategory(_: number, category: CategorySummary): number | string { return category.id ?? category.code; }
  trackSegment(index: number, segment: TextSegment): string { return segment.highlight?.id ?? `plain-${index}-${segment.text.length}`; }

  private finishSession(): void {
    this.finished.set(true);
    this.scrollToSessionResults();
    this.loadSessionReport();
  }

  private loadSessionReport(): void {
    const questionIds = this.questions()
      .map(question => question.id)
      .filter((id): id is number => !!id);
    if (!questionIds.length) {
      this.sessionReportError.set('Không có dữ liệu câu hỏi để tạo báo cáo.');
      return;
    }

    this.sessionReportSubscription?.unsubscribe();
    this.sessionReportLoading.set(true);
    this.sessionReportError.set('');
    this.sessionReportSubscription = this.service
      .practiceSessionReport(this.sessionId(), questionIds)
      .pipe(take(1))
      .subscribe({
        next: report => {
          this.sessionReport.set(report);
          this.sessionReportLoading.set(false);
          this.scrollToSessionResults();
        },
        error: () => {
          this.sessionReportError.set('Không tải được biểu đồ và gợi ý ôn tập. Hãy thử lại.');
          this.sessionReportLoading.set(false);
        }
      });
  }

  private scrollToSessionResults(): void {
    setTimeout(() => {
      document.querySelector('.session-results')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 0);
  }

  private acceptQuestions(version: number, questions: Question[], total: number): void {
    if (version !== this.requestVersion) return;
    this.totalReviewQuestions.set(total);
    this.questions.set(questions);
    this.index.set(0);
    this.score.set(0);
    this.prepare();
    this.loading.set(false);
    this.loadStudyAnnotations(questions);
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
    this.annotationMessage.set('');
    this.noteOpen.set(false);
    this.clearTextSelection();
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

  private loadStudyAnnotations(questions: Question[]): void {
    const ids = questions.map(question => question.id).filter((id): id is number => !!id);
    if (!ids.length) return;
    this.annotationLoadSubscription?.unsubscribe();
    this.annotationLoading.set(true);
    this.annotationLoadSubscription = this.service.studyAnnotations(ids).pipe(take(1)).subscribe({
      next: annotations => {
        this.annotations.update(current => ({
          ...current,
          ...Object.fromEntries(annotations.map(annotation => [annotation.questionId, annotation]))
        }));
        this.annotationLoading.set(false);
      },
      error: () => {
        this.annotationLoading.set(false);
        this.annotationMessage.set('Không tải được highlight và ghi chú đã lưu.');
      }
    });
  }

  private updateAnnotation(questionId: number, annotation: StudyAnnotation, delay = 350): void {
    this.annotations.update(current => ({ ...current, [questionId]: annotation }));
    this.annotationRevisions.set(questionId, (this.annotationRevisions.get(questionId) ?? 0) + 1);
    this.queueAnnotationSave(questionId, delay);
  }

  private queueAnnotationSave(questionId: number, delay: number): void {
    const existing = this.annotationSaveTimers.get(questionId);
    if (existing) clearTimeout(existing);
    const timer = setTimeout(() => {
      this.annotationSaveTimers.delete(questionId);
      this.persistAnnotation(questionId);
    }, delay);
    this.annotationSaveTimers.set(questionId, timer);
  }

  private persistAnnotation(questionId: number): void {
    if (this.annotationSavingIds().has(questionId)) {
      this.annotationDirtyQuestions.add(questionId);
      return;
    }
    const annotation = this.annotations()[questionId];
    if (!annotation) return;
    const snapshotRevision = this.annotationRevisions.get(questionId) ?? 0;
    this.setAnnotationSaving(questionId, true);

    const subscription = this.service.saveStudyAnnotation(questionId, {
      note: annotation.note ?? null,
      highlights: annotation.highlights
    }).pipe(
      take(1),
      finalize(() => {
        this.setAnnotationSaving(questionId, false);
        this.annotationSaveSubscriptions.delete(questionId);
        const latestRevision = this.annotationRevisions.get(questionId) ?? 0;
        if (this.annotationDirtyQuestions.delete(questionId) || latestRevision !== snapshotRevision) {
          this.queueAnnotationSave(questionId, 0);
        }
      })
    ).subscribe({
      next: saved => {
        const latestRevision = this.annotationRevisions.get(questionId) ?? 0;
        this.annotations.update(current => {
          const latest = current[questionId] ?? saved;
          return {
            ...current,
            [questionId]: latestRevision === snapshotRevision
              ? saved
              : { ...latest, version: saved.version, updatedAt: saved.updatedAt }
          };
        });
        if (this.current()?.id === questionId && latestRevision === snapshotRevision) {
          this.annotationMessage.set('Highlight và ghi chú đã được lưu.');
        }
      },
      error: () => {
        if (this.current()?.id === questionId) {
          this.annotationMessage.set('Không lưu được highlight/ghi chú. Dữ liệu vẫn được giữ trên màn hình để thử lại.');
        }
      }
    });
    this.annotationSaveSubscriptions.set(questionId, subscription);
  }

  private setAnnotationSaving(questionId: number, saving: boolean): void {
    this.annotationSavingIds.update(current => {
      const next = new Set(current);
      if (saving) next.add(questionId); else next.delete(questionId);
      return next;
    });
  }

  private buildSegments(target: StudyHighlightTarget, targetKey: string | null, text: string): TextSegment[] {
    const highlights = this.currentAnnotation().highlights
      .filter(item => item.target === target && (item.targetKey ?? null) === targetKey)
      .filter(item => item.startOffset >= 0 && item.endOffset <= text.length && item.endOffset > item.startOffset)
      .sort((a, b) => a.startOffset - b.startOffset);
    if (!highlights.length) return [{ text }];

    const segments: TextSegment[] = [];
    let cursor = 0;
    for (const highlight of highlights) {
      if (highlight.startOffset < cursor) continue;
      if (highlight.startOffset > cursor) segments.push({ text: text.slice(cursor, highlight.startOffset) });
      segments.push({ text: text.slice(highlight.startOffset, highlight.endOffset), highlight });
      cursor = highlight.endOffset;
    }
    if (cursor < text.length) segments.push({ text: text.slice(cursor) });
    return segments;
  }

  private sourceText(target: StudyHighlightTarget, targetKey: string | null): string {
    const question = this.current();
    if (!question) return '';
    if (target === 'QUESTION') return question.questionText;
    return question.options.find(option => option.key === targetKey)?.text ?? '';
  }

  private offsetWithin(root: HTMLElement, node: Node, offset: number): number {
    try {
      const range = document.createRange();
      range.selectNodeContents(root);
      range.setEnd(node, offset);
      return range.toString().length;
    } catch {
      return -1;
    }
  }

  private overlaps(left: StudyHighlight, right: StudyHighlight): boolean {
    return left.target === right.target
      && (left.targetKey ?? null) === (right.targetKey ?? null)
      && left.startOffset < right.endOffset
      && right.startOffset < left.endOffset;
  }

  private emptyAnnotation(questionId: number): StudyAnnotation {
    return { questionId, note: null, highlights: [], version: 0, updatedAt: null };
  }

  private refreshDashboard(): void {
    this.service.practiceDashboard(this.userTimeZone).pipe(take(1)).subscribe({ next: value => this.dashboard.set(value) });
  }

  private selectedAnswerKeys(): string[] {
    return Object.keys(this.selected()).filter(key => this.selected()[key]);
  }

  private createHighlightId(): string {
    return typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `highlight-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }

  private createSessionId(): string {
    return typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : `practice-${Date.now()}-${Math.random().toString(36).slice(2)}`;
  }
}
