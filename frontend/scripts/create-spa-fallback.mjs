import { copyFile, access } from 'node:fs/promises';
import { constants } from 'node:fs';
import { resolve } from 'node:path';

const outputDir = resolve(process.cwd(), 'dist/frontend/browser');
const indexFile = resolve(outputDir, 'index.html');
const fallbackFile = resolve(outputDir, '404.html');

await access(indexFile, constants.R_OK);
await copyFile(indexFile, fallbackFile);
console.log(`SPA fallback generated: ${fallbackFile}`);
