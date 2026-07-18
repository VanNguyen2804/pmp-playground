import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'questions' },
  { path: 'questions', loadComponent: () => import('./pages/question-list/question-list').then(m => m.QuestionList) },
  { path: 'questions/new', loadComponent: () => import('./pages/question-form/question-form').then(m => m.QuestionForm) },
  { path: 'questions/:id/edit', loadComponent: () => import('./pages/question-form/question-form').then(m => m.QuestionForm) },
  { path: 'import', loadComponent: () => import('./pages/import-questions/import-questions').then(m => m.ImportQuestions) },
  { path: 'practice', loadComponent: () => import('./pages/practice/practice').then(m => m.Practice) },
  { path: '**', redirectTo: 'questions' }
];
