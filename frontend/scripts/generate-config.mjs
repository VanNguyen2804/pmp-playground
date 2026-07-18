import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const host = process.env.API_BASE_HOST?.trim();
const explicitUrl = process.env.API_BASE_URL?.trim();
const apiBaseUrl = explicitUrl || (host ? `https://${host}/api` : 'http://localhost:8080/api');
const outputPath = resolve('public/app-config.json');

await mkdir(resolve('public'), { recursive: true });
await writeFile(outputPath, `${JSON.stringify({ apiBaseUrl }, null, 2)}\n`, 'utf8');
console.log(`Generated ${outputPath} with API base URL: ${apiBaseUrl}`);
