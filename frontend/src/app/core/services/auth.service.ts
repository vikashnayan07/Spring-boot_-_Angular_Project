import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';

// Adjust these imports based on your actual file structure!
import { environment } from '../../../environments/environment';
import { ToastService } from '../../shared/components/toast/toast.service';

export interface UserProfile {
  email: string;
  roleId: number;
  empId: number;
  name: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  // 👉 Unifying the API URL (Update this if you prefer API_BASE_URL)
  private apiUrl = `${environment.apiUrl}/auth`;

  // State Management for the Profile
  private profileSubject = new BehaviorSubject<UserProfile | null>(null);
  profile$ = this.profileSubject.asObservable();
  private profileLoaded = false;

  constructor(
    private http: HttpClient,
    private router: Router,
    private toastService: ToastService,
  ) {}

  // ==========================================
  // 1. LOGIN & TOKEN MANAGEMENT
  // ==========================================

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((res: any) => {
        if (res.success && res.token) {
          // 1. Save credentials safely to localStorage
          localStorage.setItem('token', res.token);
          localStorage.setItem('roleId', res.roleId.toString());

          // 2. Reset profile state so it fetches fresh data on the next page load
          this.profileLoaded = false;
        }
      }),
    );
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getRoleId(): number {
    return Number(localStorage.getItem('roleId'));
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  // ==========================================
  // 2. PROFILE STATE MANAGEMENT
  // ==========================================

  getProfile(): Observable<UserProfile | null> {
    // If we already downloaded the profile, don't spam the backend! Return the cache.
    if (this.profileLoaded && this.profileSubject.value) {
      return of(this.profileSubject.value);
    }

    // Otherwise, fetch it from your Spring Boot /me endpoint
    return this.http.get<UserProfile>(`${this.apiUrl}/me`).pipe(
      tap((profile) => {
        this.profileLoaded = true;
        this.profileSubject.next(profile);
      }),
      map((profile) => profile ?? null),
      catchError((error) => {
        // Smart Error Handling: If the token expired (401), automatically log them out!
        if (error?.status === 401) {
          this.logout('Session expired', 'Please sign in again.');
        }
        return throwError(() => error);
      }),
    );
  }

  // ==========================================
  // 3. SECURE LOGOUT
  // ==========================================

  logout(
    title = 'Logged Out',
    message = 'You have been logged out successfully',
  ): void {
    // 1. Wipe the tokens
    localStorage.removeItem('token');
    localStorage.removeItem('roleId');

    // 2. Wipe the observable memory state
    this.profileLoaded = false;
    this.profileSubject.next(null);

    // 3. Show a nice UI notification
    if (this.toastService) {
      this.toastService.success(title, message);
    }

    // 4. Boot them back to the login page
    this.router.navigate(['/']);
  }
}
