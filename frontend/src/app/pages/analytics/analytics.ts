import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription, take } from 'rxjs';
import { PracticeAnalytics } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './analytics.html',
  styleUrl: './analytics.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Analytics implements OnInit, OnDestroy {
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

  private analyticsSubscription?: Subscription;

  constructor(private readonly service: QuestionService) {}

  ngOnInit(): void {
    this.loadAnalytics();
  }

  ngOnDestroy(): void {
    this.analyticsSubscription?.unsubscribe();
  }

  changePeriod(days: number): void {
    this.analyticsDays.set(days);
    this.loadAnalytics();
  }

  private loadAnalytics(): void {
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
          this.analyticsError.set('Không tải được thống kê. Hãy thử nhấn F5 để tải lại trang.');
          this.analyticsLoading.set(false);
        }
      });
  }

  private shortDate(value: string): string {
    const date = new Date(`${value}T00:00:00`);
    return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(date);
  }
}
