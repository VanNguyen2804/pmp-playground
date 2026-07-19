# Practice analytics

## New endpoint

`GET /api/questions/practice/analytics?days=14&top=10&timeZone=Asia/Ho_Chi_Minh`

The endpoint returns:

- current-period and previous-period accuracy;
- improvement in percentage points;
- mistakes grouped by question category;
- daily accuracy trend;
- questions with the most incorrect attempts.

Responses use `Cache-Control: no-store` so the manual chart refresh always requests fresh data.

## Frontend behavior

The Practice page has a **Tải lại biểu đồ** button. The chart data is fetched on initial page load and then only refreshed when that button is pressed. Answer submission refreshes the small dashboard cards but does not silently replace the analytics charts.

The charts use Angular Signals and native HTML/SVG, so no chart dependency was added.
