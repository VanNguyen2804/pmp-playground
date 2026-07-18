import { ApplicationConfig } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { App } from './app/app';
import { appConfig } from './app/app.config';
import {
  APP_RUNTIME_CONFIG,
  AppRuntimeConfig,
  DEFAULT_APP_RUNTIME_CONFIG
} from './app/app-config';

async function loadRuntimeConfig(): Promise<AppRuntimeConfig> {
  try {
    const response = await fetch('/app-config.json', { cache: 'no-store' });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const config = (await response.json()) as Partial<AppRuntimeConfig>;
    return {
      apiBaseUrl: config.apiBaseUrl?.trim() || DEFAULT_APP_RUNTIME_CONFIG.apiBaseUrl
    };
  } catch (error) {
    console.warn('Cannot load app-config.json; using the local API URL.', error);
    return DEFAULT_APP_RUNTIME_CONFIG;
  }
}

loadRuntimeConfig()
  .then((runtimeConfig) => {
    const runtimeAppConfig: ApplicationConfig = {
      providers: [
        ...(appConfig.providers ?? []),
        { provide: APP_RUNTIME_CONFIG, useValue: runtimeConfig }
      ]
    };
    return bootstrapApplication(App, runtimeAppConfig);
  })
  .catch((error) => console.error(error));
