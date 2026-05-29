import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration: number;
}

@Injectable({ providedIn: 'root' })
export class ToastService {

  private toastsSubject = new BehaviorSubject<Toast[]>([]);
  toasts$ = this.toastsSubject.asObservable();

  private maxToasts = 4;

  success(title: string, message: string, duration = 4000): void {
    this.addToast('success', title, message, duration);
  }

  error(title: string, message: string, duration = 5000): void {
    this.addToast('error', title, message, duration);
  }

  warning(title: string, message: string, duration = 4500): void {
    this.addToast('warning', title, message, duration);
  }

  info(title: string, message: string, duration = 4000): void {
    this.addToast('info', title, message, duration);
  }

  dismiss(id: string): void {
    const current = this.toastsSubject.value;
    this.toastsSubject.next(current.filter(t => t.id !== id));
  }

  private addToast(
    type: Toast['type'],
    title: string,
    message: string,
    duration: number
  ): void {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`;
    const toast: Toast = { id, type, title, message, duration };

    let current = this.toastsSubject.value;
    if (current.length >= this.maxToasts) {
      current = current.slice(1);
    }

    this.toastsSubject.next([...current, toast]);

    setTimeout(() => this.dismiss(id), duration);
  }
}