import { ApplicationConfig } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import {
  APP_RUNTIME_CONFIG,
  AppRuntimeConfig,
  DEFAULT_APP_RUNTIME_CONFIG
} from './app/app-config';

function normalizeApiBaseUrl(value: string): string {
  const trimmed = value.trim().replace(/\/+$/, '');
  if (!trimmed) return DEFAULT_APP_RUNTIME_CONFIG.apiBaseUrl;
  return trimmed.endsWith('/api') ? trimmed : `${trimmed}/api`;
}

async function loadRuntimeConfig(): Promise<AppRuntimeConfig> {
  try {
    const response = await fetch('/app-config.json', { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const config = (await response.json()) as Partial<AppRuntimeConfig>;
    const apiBaseUrl = normalizeApiBaseUrl(config.apiBaseUrl ?? '');

    if (
      window.location.hostname !== 'localhost' &&
      window.location.hostname !== '127.0.0.1' &&
      apiBaseUrl.includes('localhost')
    ) {
      throw new Error(
        'Production frontend was built with a localhost API URL. Configure API_BASE_URL and redeploy.'
      );
    }

    return { apiBaseUrl };
  } catch (error) {
    console.error('Cannot load a valid app-config.json.', error);
    throw error;
  }
}

loadRuntimeConfig()
  .then((runtimeConfig) => {
    console.info(`PMP API base URL: ${runtimeConfig.apiBaseUrl}`);
    const runtimeAppConfig: ApplicationConfig = {
      providers: [
        ...(appConfig.providers ?? []),
        { provide: APP_RUNTIME_CONFIG, useValue: runtimeConfig }
      ]
    };
    return bootstrapApplication(App, runtimeAppConfig);
  })
  .catch((error) => {
    const message = document.createElement('pre');
    message.style.whiteSpace = 'pre-wrap';
    message.style.padding = '24px';
    message.style.fontFamily = 'system-ui, sans-serif';
    message.textContent =
      'Không thể kết nối cấu hình backend.\n\n' +
      'Trên Render frontend, hãy đặt API_BASE_URL thành URL public của backend, ví dụ:\n' +
      'https://pmp-playground-api.onrender.com\n\n' +
      `Chi tiết: ${error instanceof Error ? error.message : String(error)}`;
    document.body.replaceChildren(message);
  });
