import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'exam' },
  { path: 'questions', loadComponent: () => import('./pages/question-list/question-list').then(m => m.QuestionList) },
  { path: 'questions/new', loadComponent: () => import('./pages/question-form/question-form').then(m => m.QuestionForm) },
  { path: 'questions/:id/edit', loadComponent: () => import('./pages/question-form/question-form').then(m => m.QuestionForm) },
  { path: 'questions/:id/explanations', loadComponent: () => import('./pages/explanation-review/explanation-review').then(m => m.ExplanationReview) },
  { path: 'categories', loadComponent: () => import('./pages/categories/categories').then(m => m.Categories) },
  { path: 'import', loadComponent: () => import('./pages/import-questions/import-questions').then(m => m.ImportQuestions) },
  { path: 'exam', loadComponent: () => import('./pages/practice/practice').then(m => m.Practice) },
  { path: 'practice', pathMatch: 'full', redirectTo: 'exam' },
  { path: 'analytics', loadComponent: () => import('./pages/analytics/analytics').then(m => m.Analytics) },
  { path: '**', redirectTo: 'exam' }
];
