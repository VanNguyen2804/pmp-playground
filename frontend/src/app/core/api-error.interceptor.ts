import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, mergeMap, Observable, of, throwError } from 'rxjs';
import { ApiErrorResponse } from '../models/question';
import { ErrorModalService } from './error-modal.service';

export const apiErrorInterceptor: HttpInterceptorFn = (request, next) => {
  const modal = inject(ErrorModalService);

  return next(request).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        modal.show({
          title: 'Lỗi ứng dụng',
          message: error instanceof Error ? error.message : 'Đã xảy ra lỗi ngoài dự kiến.',
          code: 'CLIENT_ERROR'
        });
        return throwError(() => error);
      }

      return readApiError(error.error).pipe(
        mergeMap(body => {
          modal.show({
            title: titleFor(error.status),
            message: body?.message || fallbackMessage(error),
            code: body?.code || (error.status === 0 ? 'BACKEND_UNREACHABLE' : 'HTTP_ERROR'),
            status: error.status || undefined,
            path: body?.path || request.url,
            traceId: body?.traceId || error.headers.get('X-Trace-Id') || undefined,
            fieldErrors: body?.fieldErrors,
            details: body?.details
          });

          // Rethrow so each page can still reset loading/saving/uploading state.
          return throwError(() => error);
        })
      );
    })
  );
};

function readApiError(value: unknown): Observable<ApiErrorResponse | undefined> {
  if (isApiError(value)) return of(value);

  if (typeof value === 'string') {
    return of(parseApiError(value));
  }

  if (value instanceof Blob) {
    return from(value.text()).pipe(mergeMap(text => of(parseApiError(text))));
  }

  return of(undefined);
}

function parseApiError(value: string): ApiErrorResponse | undefined {
  const trimmed = value.trim();
  if (!trimmed) return undefined;

  try {
    const parsed: unknown = JSON.parse(trimmed);
    return isApiError(parsed) ? parsed : undefined;
  } catch {
    return undefined;
  }
}

function isApiError(value: unknown): value is ApiErrorResponse {
  return !!value && typeof value === 'object' && 'message' in value && 'code' in value;
}

function titleFor(status: number): string {
  if (status === 0) return 'Không kết nối được backend';
  if (status === 400 || status === 422) return 'Dữ liệu không hợp lệ';
  if (status === 404) return 'Không tìm thấy dữ liệu';
  if (status === 409) return 'Xung đột dữ liệu';
  if (status === 413) return 'File quá lớn';
  if (status >= 500) return 'Backend gặp sự cố';
  return 'Request thất bại';
}

function fallbackMessage(error: HttpErrorResponse): string {
  if (error.status === 0) {
    return 'Frontend không gọi được backend. Hãy kiểm tra API_BASE_URL, trạng thái backend và cấu hình CORS.';
  }
  if (typeof error.error === 'string' && error.error.trim()) return error.error;
  return error.message || 'Request không thể hoàn tất.';
}
