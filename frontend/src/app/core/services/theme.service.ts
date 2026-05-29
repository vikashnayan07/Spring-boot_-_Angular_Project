import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  private isDarkSubject = new BehaviorSubject<boolean>(true);
  isDark$ = this.isDarkSubject.asObservable();

  get isDark(): boolean {
    return this.isDarkSubject.value;
  }

  constructor() {
    const saved = localStorage.getItem('machcare-theme');
    if (saved) {
      this.setTheme(saved === 'dark');
    } else {
      const prefersDark =
        window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? true;
      this.setTheme(prefersDark);
    }
  }

  toggle(): void {
    this.setTheme(!this.isDark);
  }

  setTheme(dark: boolean): void {
    this.isDarkSubject.next(dark);
    document.documentElement.classList.toggle('dark', dark);
    document.documentElement.setAttribute(
      'data-theme',
      dark ? 'dark' : 'light'
    );
    localStorage.setItem('machcare-theme', dark ? 'dark' : 'light');
    localStorage.setItem('theme', dark ? 'dark' : 'light');
  }
}
