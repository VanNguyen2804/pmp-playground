# Session report chart visibility fix

## Root cause

The session result template and API were present, but the CSS classes used by the category bar chart were missing. The bar track and fill elements contained no text, so without an explicit height/background they rendered as an invisible zero-height chart.

## Fix

- Added complete responsive styles for the session report, summary cards, category bars, and PMBOK 8 suggestions.
- Added explicit bar track height, fill height, background, rounded corners, and transition.
- Scrolls to the session result panel after the user selects **Xem kết quả** and again after the report finishes loading.
- Keeps the empty-success state when the completed session has no incorrect answers.

No API or database change is required for this fix.
