# Analytics menu separation

## Frontend

- Added a dedicated `/analytics` route and `Biểu đồ` sidebar menu item.
- Moved all mistake-category, progress-trend, summary, and top-wrong-question charts out of the practice page.
- The `/practice` page now contains only practice dashboard metrics, filters, question answering, answer history, and explanation editing.
- Removed the `Làm mới` sidebar action and the root component `refresh()` method.
- Analytics load automatically when the analytics page opens or the browser is refreshed with F5.
- Changing the analytics period (7/14/30/60 days) reloads analytics automatically.
- The analytics request continues to include a timestamp query parameter to avoid cached responses.

## Backend

No backend change was required. The existing endpoint is reused:

`GET /api/questions/practice/analytics`
