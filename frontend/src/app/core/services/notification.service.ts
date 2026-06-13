import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, interval, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { API_BASE_URL } from '../constants/api.config';
import { RealtimeService } from './realtime.service';
import { ToastService } from '../../shared/components/toast/toast.service';

export interface NotificationItem {
  id: string;
  message: string;
  timestamp: string;
  machineLabel: string;
  read: boolean;
  severity?: string;
  category?: string;
  title?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<NotificationItem[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  unreadCount$ = this.unreadCountSubject.asObservable();

  private channel?: BroadcastChannel;
  private refreshSub?: Subscription;

  constructor(
    private http: HttpClient,
    private realtime: RealtimeService,
    private toastService: ToastService,
  ) {
    this.setupCrossTabSync();
    this.realtime.connect();
    this.refresh().subscribe();
    this.refreshSub = interval(30000).subscribe(() => this.refresh().subscribe());
    this.realtime.events$.subscribe((event) => {
      if (event.type === 'notification') {
        this.addRealtimeNotification(event.payload);
      }
      if (event.type === 'notifications_updated') {
        this.fetchNotifications().subscribe();
      }
      if (event.type === 'suspension_updated' || event.type === 'session_status_updated') {
        this.fetchNotifications().subscribe();
      }
    });
  }

  getNotifications(): Observable<NotificationItem[]> {
    this.realtime.connect();
    return this.refresh();
  }

  refresh(): Observable<NotificationItem[]> {
    if (!localStorage.getItem('token')) {
      this.notificationsSubject.next([]);
      this.unreadCountSubject.next(0);
      return of([]);
    }
    return this.fetchNotifications();
  }

  markAsRead(id: string): void {
    this.http.put(`${API_BASE_URL}/notifications/${id}/read`, {}).subscribe({
      next: () => {
        this.applyReadLocally(id);
        this.broadcast('notification-read', id);
      },
      error: () => this.applyReadLocally(id),
    });
  }

  markAllRead(): void {
    this.http.put(`${API_BASE_URL}/notifications/read-all`, {}).subscribe({
      next: () => {
        const updated = this.notificationsSubject.value.map((item) => ({ ...item, read: true }));
        this.notificationsSubject.next(updated);
        this.unreadCountSubject.next(0);
        this.broadcast('notifications-read-all', '');
      },
    });
  }

  private fetchNotifications(): Observable<NotificationItem[]> {
    return this.http.get<any>(`${API_BASE_URL}/notifications`).pipe(
      map((response) => {
        const items = this.unwrap(response).map((item: any) => this.toNotificationItem(item));
        this.notificationsSubject.next(items);
        this.unreadCountSubject.next(Number(response?.unreadCount ?? items.filter((item) => !item.read).length));
        return items;
      }),
      catchError(() => of(this.notificationsSubject.value)),
    );
  }

  private addRealtimeNotification(payload: any): void {
    const item = this.toNotificationItem(payload);
    const current = this.notificationsSubject.value.filter((existing) => existing.id !== item.id);
    const next = [item, ...current].slice(0, 50);
    this.notificationsSubject.next(next);
    this.unreadCountSubject.next(next.filter((entry) => !entry.read).length);
    this.toastService.info(item.title || item.category || 'Notification', item.message);
    this.broadcast('notification-new', item);
  }

  private toNotificationItem(item: any): NotificationItem {
    return {
      id: `${item.notificationId ?? item.id}`,
      title: item.title || item.category || 'Notification',
      message: item.message || 'System update',
      timestamp: item.createdAt || item.timestamp || new Date().toISOString(),
      machineLabel: item.referenceId || item.referenceType || item.category || 'MachCare',
      read: item.read === true,
      severity: item.severity,
      category: item.category,
    };
  }

  private unwrap(response: any): any[] {
    const data = response?.data || response || [];
    return Array.isArray(data) ? data : [];
  }

  private applyReadLocally(id: string): void {
    const updated = this.notificationsSubject.value.map((item) =>
      item.id === id ? { ...item, read: true } : item,
    );
    this.notificationsSubject.next(updated);
    this.unreadCountSubject.next(updated.filter((item) => !item.read).length);
  }

  private setupCrossTabSync(): void {
    if (typeof BroadcastChannel === 'undefined') {
      return;
    }
    this.channel = new BroadcastChannel('machcare-notifications');
    this.channel.onmessage = (event) => {
      const { type, payload } = event.data || {};
      if (type === 'notification-read') {
        this.applyReadLocally(payload);
      }
      if (type === 'notifications-read-all') {
        const updated = this.notificationsSubject.value.map((item) => ({ ...item, read: true }));
        this.notificationsSubject.next(updated);
        this.unreadCountSubject.next(0);
      }
      if (type === 'notification-new') {
        const item = payload as NotificationItem;
        const current = this.notificationsSubject.value.filter((existing) => existing.id !== item.id);
        const next = [item, ...current].slice(0, 50);
        this.notificationsSubject.next(next);
        this.unreadCountSubject.next(next.filter((entry) => !entry.read).length);
      }
    };
  }

  private broadcast(type: string, payload: any): void {
    this.channel?.postMessage({ type, payload });
  }
}
