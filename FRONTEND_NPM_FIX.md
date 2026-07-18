# Frontend npm install fix

## Root cause

The previous `frontend/package-lock.json` contained `resolved` URLs pointing to a private build-environment npm registry. That registry is not accessible from a developer machine or Render, so `npm ci` could not download dependencies.

## Fixes applied

- Regenerated/normalized `package-lock.json` to use `https://registry.npmjs.org/`.
- Added `frontend/.npmrc` to use the public npm registry.
- Pinned Node.js to `22.16.0` with `.node-version` and `.nvmrc`.
- Added Node/npm engine constraints to `package.json`.
- Added `NODE_VERSION=22.16.0` to the Render static-site configuration.
- Verified `npm ci` installs 471 packages with zero vulnerabilities.
- Verified `npm run build` succeeds.

## Local verification

```bash
cd frontend
rm -rf node_modules dist
npm cache verify
npm ci
npm run build
```

Expected output directory:

```text
dist/frontend/browser
```
