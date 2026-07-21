# Dashboard average performance update

- Renamed **Hiệu suất so với hôm qua** to **Hiệu suất**.
- The card now shows the arithmetic mean of daily accuracy percentages across all days that contain at least one answer attempt.
- Added `averageDailyAccuracyPercentage`, `activePerformanceDays`, and `dailyPerformanceHistory` to the dashboard response.
- Hovering or keyboard-focusing the card opens a scrollable breakdown with each date, daily accuracy, and correct/total attempts.
- Days with no answer attempts are excluded from the average.
