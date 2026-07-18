# Angular clean URL routing on Render

The frontend uses Angular History API routing, so routes stay clean:

- `/questions`
- `/categories`
- `/practice`

A direct visit or browser refresh on one of these routes must return `index.html` so Angular can resolve the route in the browser.

The root `render.yaml` already contains the required Static Site rewrite:

```yaml
routes:
  - type: rewrite
    source: /*
    destination: /index.html
```

For a Render Static Site created manually, add the same rule in:

`Redirects/Rewrites -> Add Rule`

- Source: `/*`
- Destination: `/index.html`
- Action: `Rewrite`

After changing the rule, use **Clear build cache & deploy**.
