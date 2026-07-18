import { InjectionToken } from '@angular/core';

export interface AppRuntimeConfig {
  apiBaseUrl: string;
}

export const DEFAULT_APP_RUNTIME_CONFIG: AppRuntimeConfig = {
  apiBaseUrl: 'http://localhost:8080/api'
};

export const APP_RUNTIME_CONFIG = new InjectionToken<AppRuntimeConfig>('APP_RUNTIME_CONFIG');
