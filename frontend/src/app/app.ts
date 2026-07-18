import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ErrorModal } from './shared/error-modal/error-modal';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ErrorModal],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
