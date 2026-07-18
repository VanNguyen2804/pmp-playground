import { Injectable, signal } from '@angular/core';

export interface ErrorModalState {
  title: string;
  message: string;
  code?: string;
  status?: number;
  path?: string;
  traceId?: string;
  fieldErrors?: Record<string, string>;
  details?: string[];
}

@Injectable({ providedIn: 'root' })
export class ErrorModalService {
  private readonly modalState = signal<ErrorModalState | null>(null);
  readonly state = this.modalState.asReadonly();

  show(error: ErrorModalState): void {
    this.modalState.set(error);
  }

  close(): void {
    this.modalState.set(null);
  }
}
