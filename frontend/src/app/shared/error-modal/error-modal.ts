import { CommonModule } from '@angular/common';
import { Component, HostListener } from '@angular/core';
import { ErrorModalService } from '../../core/error-modal.service';

@Component({
  selector: 'app-error-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './error-modal.html',
  styleUrl: './error-modal.css'
})
export class ErrorModal {
  readonly state;

  constructor(private readonly modal: ErrorModalService) {
    this.state = modal.state;
  }

  close(): void {
    this.modal.close();
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.state()) this.close();
  }

  fieldEntries(errors?: Record<string, string>): Array<[string, string]> {
    return Object.entries(errors ?? {});
  }
}
