# Angular Signals response rendering

The frontend uses Angular 21 without Zone.js. Asynchronous mutations of ordinary class fields may not schedule a render in zoneless mode. This can leave the UI showing `Đang tải...` even after `HttpClient` has received the response.

The asynchronous UI state of all pages now uses Angular signals:

- `questions`, `categories`, `question`, and import result data;
- `loading`, `saving`, `uploading`, and error state;
- pagination and filters;
- practice state, selected answers, and matching answers.

Each HTTP `next` callback updates the data signal and sets the loading signal to `false` immediately. The UI therefore renders when the backend response arrives rather than relying on Zone.js or waiting for another browser event.

Components also use `ChangeDetectionStrategy.OnPush`, request cancellation, `take(1)`, and stale-response guards for list requests.
