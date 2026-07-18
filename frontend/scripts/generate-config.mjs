import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

function normalizeApiBaseUrl(value) {
  const trimmed = value.trim().replace(/\/+$/, '');
  if (!trimmed) return '';
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
}

const configuredUrl = process.env.API_BASE_URL?.trim() || '';
const isRenderBuild = process.env.RENDER === 'true';

if (isRenderBuild && !configuredUrl) {
  throw new Error(
    'API_BASE_URL is required for the Render frontend build. ' +
    'Set it to the public backend URL, for example https://pmp-playground-api.onrender.com, ' +
    'or deploy with the included render.yaml Blueprint.'
  );
}

const apiBaseUrl = normalizeApiBaseUrl(
  configuredUrl || 'http://localhost:8080'
);
const outputPath = resolve('public/app-config.json');

await mkdir(resolve('public'), { recursive: true });
await writeFile(outputPath, `${JSON.stringify({ apiBaseUrl }, null, 2)}\n`, 'utf8');
console.log(`Generated ${outputPath} with API base URL: ${apiBaseUrl}`);
