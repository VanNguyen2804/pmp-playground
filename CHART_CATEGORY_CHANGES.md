# Chart Category Changes

## Category

- Code: `TOPIC_CHART`
- Name: `Chart`
- Taxonomy: `PMP_TOPIC`
- Display order: `5` so it appears before the general topics and is shown as the primary chip for curated visual questions.

## Existing question aggregation

`ChartQuestionSeeder` scans imported questions at startup. Questions mentioning chart/diagram keywords are assigned to `TOPIC_CHART`. When an existing chart question has no image, a matching local SVG illustration is assigned.

## Curated visual bank

The seeder idempotently creates 20 questions (`CHART-GUIDE-001` through `CHART-GUIDE-020`) covering:

- Pareto, histogram, control chart, scatter diagram, fishbone
- Gantt, milestone, network diagram, S-curve
- Burndown, burnup, cumulative flow, velocity, Kanban
- Tornado, probability-impact matrix, decision tree
- Power-interest grid, RACI, flowchart

Every question includes four options, one correct answer, a reviewed explanation, a local SVG illustration, and one or more related PMP topic categories.

## Frontend

No new page is required. The existing `/exam` category dropdown loads categories from the backend. Selecting `Chart` automatically calls the backend and shows all imported plus curated chart questions.
