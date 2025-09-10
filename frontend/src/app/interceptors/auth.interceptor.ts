import { Injectable, inject } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { ErrorHandlerService } from '../services/error-handler.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  private authService = inject(AuthService);
  private errorHandler = inject(ErrorHandlerService);
  
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Add auth header if we have a token
    const authRequest = this.addAuthHeader(request);

    return next.handle(authRequest).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle 401 unauthorized errors
        if (error.status === 401 && !authRequest.url.includes('/auth/login')) {
          return this.handle401Error(authRequest, next);
        }

        // Handle other HTTP errors
        this.handleHttpError(error);
        return throwError(() => error);
      })
    );
  }

  private addAuthHeader(request: HttpRequest<unknown>): HttpRequest<unknown> {
    const token = this.authService.getToken();
    
    if (token && !request.headers.has('Authorization')) {
      return request.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
    }
    
    return request;
  }

  private handle401Error(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response: { accessToken: string }) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.accessToken);
          
          // Retry the failed request with new token
          const retryRequest = this.addAuthHeader(request);
          return next.handle(retryRequest);
        }),
        catchError((refreshError) => {
          this.isRefreshing = false;
          
          // If refresh fails, logout user
          console.error('Token refresh failed, logging out user');
          this.authService.logout().subscribe({
            error: () => {
              // Force logout even if API call fails
              this.authService['handleLogout']();
            }
          });
          
          return throwError(() => refreshError);
        })
      );
    }

    // If already refreshing, wait for the new token
    return this.refreshTokenSubject.pipe(
      filter(token => token !== null),
      take(1),
      switchMap(() => {
        const retryRequest = this.addAuthHeader(request);
        return next.handle(retryRequest);
      })
    );
  }

  private handleHttpError(error: HttpErrorResponse): void {
    // Don't show errors for certain endpoints to avoid spam
    const silentEndpoints = ['/auth/refresh', '/auth/validate'];
    const shouldShowError = !silentEndpoints.some(endpoint => error.url?.includes(endpoint));

    if (shouldShowError) {
      switch (error.status) {
        case 400:
          this.handleBadRequestError(error);
          break;
        case 401:
          this.errorHandler.handleAuthError('You are not authorized to perform this action');
          break;
        case 403:
          this.errorHandler.addError('Access Denied', 'You do not have permission to access this resource');
          break;
        case 404:
          this.errorHandler.addError('Not Found', 'The requested resource was not found');
          break;
        case 409:
          this.errorHandler.addError('Conflict', 'The request conflicts with current state of the resource');
          break;
        case 422:
          this.handleValidationError(error);
          break;
        case 500:
          this.errorHandler.addError('Server Error', 'An internal server error occurred. Please try again later.');
          break;
        case 503:
          this.errorHandler.addError('Service Unavailable', 'The service is temporarily unavailable. Please try again later.');
          break;
        default:
          if (error.status >= 400) {
            this.errorHandler.handleHttpError(error);
          }
          break;
      }
    }
  }

  private handleBadRequestError(error: HttpErrorResponse): void {
    if (error.error && typeof error.error === 'object') {
      // Handle validation errors from Spring Boot
      if (error.error.errors) {
        this.errorHandler.handleValidationErrors(error.error.errors);
      } else if (error.error.message) {
        this.errorHandler.addError('Bad Request', error.error.message);
      } else {
        this.errorHandler.addError('Bad Request', 'The request was invalid');
      }
    } else {
      this.errorHandler.addError('Bad Request', 'The request was invalid');
    }
  }

  private handleValidationError(error: HttpErrorResponse): void {
    if (error.error && error.error.validationErrors) {
      this.errorHandler.handleValidationErrors(error.error.validationErrors);
    } else {
      this.errorHandler.addError('Validation Error', 'Please check your input and try again');
    }
  }
}
