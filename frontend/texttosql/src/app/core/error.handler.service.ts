// core/error-handler.service.ts

import { Injectable } from '@angular/core';

export interface BackendError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
}

export interface ParsedError {
  message: string;
  status: number;
  isNetworkError: boolean;
  isUnauthorized: boolean;
  isForbidden: boolean;
  isNotFound: boolean;
  isConflict: boolean;
  isGone: boolean;
  isServerError: boolean;
  isTooManyRequests: boolean;
}

@Injectable({ providedIn: 'root' })
export class ErrorHandlerService {

  parse(error: any): ParsedError {
    const status = error?.status ?? 0;
    const backendError = this.extractBackendError(error);

    return {
      message: backendError?.message ?? this.fallbackMessage(status),
      status,
      isNetworkError:      status === 0,
      isUnauthorized:      status === 401,
      isForbidden:         status === 403,
      isNotFound:          status === 404,
      isConflict:          status === 409,
      isGone:              status === 410,
      isTooManyRequests:   status === 429,
      isServerError:       status >= 500,
    };
  }

  /** Returns only the message string — shorthand for simple usage */
  message(error: any): string {
    return this.parse(error).message;
  }

  private extractBackendError(error: any): BackendError | null {

    if (error?.message && error?.timestamp) {
      return error as BackendError;
    }

    // Case 1: error.error parsed object
    if (error?.error && typeof error.error === 'object') {
      const msg = error.error.message ?? error.error.error;
      if (msg) return error.error as BackendError;
    }

    // Case 2: error.error JSON string
    if (typeof error?.error === 'string' && error.error.trim().startsWith('{')) {
      try {
        const parsed = JSON.parse(error.error);
        if (parsed?.message) return parsed as BackendError;
      } catch { /* fall through */ }
    }

    // Case 3: error.error plain string
    if (typeof error?.error === 'string' && error.error.trim()) {
      return { message: error.error, status: error?.status ?? 0, error: '', timestamp: '' };
    }

    // Case 4: JS Error
    if (error?.message && !error.message.startsWith('Http failure')) {
      return { message: error.message, status: error?.status ?? 0, error: '', timestamp: '' };
    }

    return null;
  }

  private fallbackMessage(status: number): string {
    switch (status) {
      case 0:   return 'Unable to connect to the server. Please check your connection.';
      case 400: return 'Invalid request. Please check your input.';
      case 401: return 'Authentication failed. Please log in again.';
      case 403: return 'You do not have permission to perform this action.';
      case 404: return 'The requested resource was not found.';
      case 409: return 'A conflict occurred. The resource may already exist.';
      case 410: return 'This link has expired.';
      case 422: return 'Validation failed. Please check your input.';
      case 429: return 'Too many requests. Please wait a moment and try again.';
      case 503: return 'Service is temporarily unavailable. Please try again later.';
      default:
        if (status >= 500) return 'An unexpected server error occurred. Please try again.';
        return 'An unexpected error occurred.';
    }
  }
}
