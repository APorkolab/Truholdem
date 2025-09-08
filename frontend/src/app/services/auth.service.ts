import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject, Observable, throwError, timer } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  roles: string[];
  totalGamesPlayed: number;
  totalWinnings: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  username: string;
  email: string;
  roles: string[];
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = 'http://localhost:8080/api/auth';
  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'user';

  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    this.initializeAuthState();
    this.startTokenRefreshTimer();
  }

  private initializeAuthState(): void {
    const token = this.getToken();
    const user = this.getStoredUser();

    if (token && user) {
      this.currentUserSubject.next(user);
      this.isAuthenticatedSubject.next(true);
    }
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => {
          this.handleAuthResponse(response);
        }),
        catchError(this.handleError)
      );
  }

  register(userData: RegisterRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/register`, userData)
      .pipe(
        catchError(this.handleError)
      );
  }

  logout(): Observable<any> {
    return this.http.post(`${this.API_URL}/logout`, {})
      .pipe(
        tap(() => {
          this.handleLogout();
        }),
        catchError(error => {
          // Even if the API call fails, clear local state
          this.handleLogout();
          return throwError(() => error);
        })
      );
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.getRefreshToken();
    
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    return this.http.post<AuthResponse>(`${this.API_URL}/refresh`, { refreshToken })
      .pipe(
        tap(response => {
          this.handleAuthResponse(response);
        }),
        catchError(error => {
          this.handleLogout();
          return throwError(() => error);
        })
      );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<any> {
    const body = { currentPassword, newPassword };
    return this.http.post(`${this.API_URL}/change-password`, body)
      .pipe(
        catchError(this.handleError)
      );
  }

  getCurrentUser(): Observable<User> {
    return this.http.get<User>('/api/users/profile')
      .pipe(
        tap(user => {
          this.currentUserSubject.next(user);
          this.storeUser(user);
        }),
        catchError(this.handleError)
      );
  }

  private handleAuthResponse(response: AuthResponse): void {
    this.storeToken(response.accessToken);
    this.storeRefreshToken(response.refreshToken);
    
    const user: User = {
      id: '', // Will be updated when we fetch full profile
      username: response.username,
      email: response.email,
      firstName: '',
      lastName: '',
      roles: response.roles,
      totalGamesPlayed: 0,
      totalWinnings: 0
    };

    this.storeUser(user);
    this.currentUserSubject.next(user);
    this.isAuthenticatedSubject.next(true);

    // Fetch complete user profile
    this.getCurrentUser().subscribe();
  }

  private handleLogout(): void {
    this.clearStorage();
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.router.navigate(['/login']);
  }

  private startTokenRefreshTimer(): void {
    // Refresh token every 50 minutes (assuming 60-minute expiry)
    timer(0, 50 * 60 * 1000).subscribe(() => {
      if (this.isAuthenticated()) {
        this.refreshToken().subscribe({
          error: () => {
            console.log('Token refresh failed, logging out');
            this.handleLogout();
          }
        });
      }
    });
  }

  private handleError = (error: any) => {
    console.error('Auth service error:', error);
    
    if (error.status === 401) {
      this.handleLogout();
    }
    
    return throwError(() => error);
  };

  // Token management
  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  private storeToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
  }

  private getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  private storeRefreshToken(token: string): void {
    localStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  private getStoredUser(): User | null {
    const userStr = localStorage.getItem(this.USER_KEY);
    return userStr ? JSON.parse(userStr) : null;
  }

  private storeUser(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }

  private clearStorage(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
  }

  // Public getters
  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  getCurrentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  hasRole(role: string): boolean {
    const user = this.getCurrentUserValue();
    return user ? user.roles.includes(role) : false;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }
}
