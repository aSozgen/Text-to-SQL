import { HttpErrorResponse } from '@angular/common/http';

export function extractErrorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    if (body && typeof body === 'object') {
      if (body.message) {
        return body.error ? `${body.error}: ${body.message}` : body.message;
      }
    }
    return `Error (${err.status}): ${err.statusText}`;
  }

  if (err && typeof err === 'object') {
    const e = err as Record<string, any>;
    if (e['error']?.['message']) {
      return e['error']['message'];
    }
  }

  if (typeof err === 'string') return err;
  return 'An unexpected error occurred.';
}
