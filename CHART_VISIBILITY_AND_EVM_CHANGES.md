# Practice defaults, chart visibility, Critical Path and EVM questions

## Frontend

- `wrongOnly` now defaults to `false`. The user enables **Chỉ câu sai** only when they want the wrong-question review mode.
- Removed the **Hôm nay cần ôn** dashboard card. The dashboard now uses three columns.
- Chart-guide images are displayed only when the question belongs to `TOPIC_CHART`.
- Regular non-chart question images remain supported.

## Backend

- Tightened automatic chart classification. The old broad substring rule matched `chart` inside words such as `charter`.
- On startup, legacy questions that received `/chart-guides/master-reference.svg` through that false match have the generated image and Chart category removed.
- Existing questions are still classified when they mention a concrete chart or diagram such as Pareto, histogram, control chart, network diagram, burndown, tornado, RACI, and so on.

## New visual questions

Six idempotent questions were added to `ChartQuestionSeeder`:

- Critical path identification
- Total float calculation
- Critical-path crashing decision
- CPI/SPI status interpretation
- CPI/SPI calculation from EV, PV, and AC
- Correct next action for CPI 0.90 and SPI 1.20

Each question has a dedicated SVG under `frontend/public/chart-guides/` and belongs to `TOPIC_CHART` plus the relevant Schedule and/or Cost category.
