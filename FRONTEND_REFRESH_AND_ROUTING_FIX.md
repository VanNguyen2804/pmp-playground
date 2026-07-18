# Frontend refresh and loading recovery

## Changes

- Angular now uses hash routing (`/#/questions`) so refreshing a nested route never asks the static host for `/questions`.
- Render's SPA rewrite remains enabled as a second layer of protection.
- The production build creates `dist/frontend/browser/404.html` from `index.html` for static hosts that use a 404 fallback file.
- API requests time out after 120 seconds and show a modal instead of leaving the page on `Đang tải...` indefinitely.
- Question list, categories, and practice pages can cancel an old request and retry without reloading the whole browser tab.
- A global `Làm mới` button is available in the top navigation.

## Expected URLs after deployment

- `https://<frontend>.onrender.com/#/questions`
- `https://<frontend>.onrender.com/#/categories`
- `https://<frontend>.onrender.com/#/practice`

The part after `#` is handled by Angular and is not sent to Render, so F5 always requests `/`.
