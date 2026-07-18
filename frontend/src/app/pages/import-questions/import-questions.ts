import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnDestroy, signal } from '@angular/core';
import { Subscription, take } from 'rxjs';
import { ImportResult } from '../../models/question';
import { QuestionService } from '../../services/question.service';

@Component({
  selector: 'app-import-questions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './import-questions.html',
  styleUrl: './import-questions.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportQuestions implements OnDestroy {
  readonly selected = signal<File | undefined>(undefined);
  readonly result = signal<ImportResult | undefined>(undefined);
  readonly uploading = signal(false);
  readonly error = signal('');

  private uploadSubscription?: Subscription;

  constructor(private readonly service: QuestionService) {}

  ngOnDestroy(): void {
    this.uploadSubscription?.unsubscribe();
  }

  choose(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selected.set(input.files?.[0]);
    this.result.set(undefined);
    this.error.set('');
  }

  upload(): void {
    const file = this.selected();
    if (!file) return;

    this.uploadSubscription?.unsubscribe();
    this.uploading.set(true);
    this.error.set('');

    this.uploadSubscription = this.service.importFile(file).pipe(take(1)).subscribe({
      next: result => {
        this.result.set(result);
        this.uploading.set(false);
      },
      error: () => {
        this.error.set('Không thể import file. Xem modal lỗi để biết chi tiết.');
        this.uploading.set(false);
      }
    });
  }
}
