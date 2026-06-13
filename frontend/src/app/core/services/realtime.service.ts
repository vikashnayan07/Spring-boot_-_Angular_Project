import { Injectable, NgZone } from '@angular/core';
import { Subject } from 'rxjs';
import { API_BASE_URL } from '../constants/api.config';

export interface RealtimeMessage {
  type: string;
  payload: any;
  timestamp?: string;
}

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private source: EventSource | null = null;
  private activeToken: string | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly eventSubject = new Subject<RealtimeMessage>();
  readonly events$ = this.eventSubject.asObservable();

  constructor(private zone: NgZone) {}

  connect(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      this.disconnect();
      return;
    }

    if (this.source && this.activeToken === token) {
      return;
    }

    this.disconnect();
    this.activeToken = token;
    const streamUrl = `${API_BASE_URL}/realtime/stream?token=${encodeURIComponent(token)}`;
    this.source = new EventSource(streamUrl);

    [
      'connected',
      'heartbeat',
      'notification',
      'notifications_updated',
      'maintenance_updated',
      'alerts_updated',
      'faults_updated',
      'tasks_updated',
      'employees_updated',
      'suspension_updated',
      'session_status_updated',
    ].forEach((eventName) => {
      this.source?.addEventListener(eventName, (event: MessageEvent) => {
        this.zone.run(() => this.eventSubject.next(this.parseEvent(eventName, event)));
      });
    });

    this.source.onerror = () => {
      this.disconnect();
      this.reconnectTimer = setTimeout(() => this.connect(), 3000);
    };
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.source?.close();
    this.source = null;
    this.activeToken = null;
  }

  private parseEvent(type: string, event: MessageEvent): RealtimeMessage {
    try {
      const parsed = JSON.parse(event.data);
      return {
        type: parsed.type || type,
        payload: parsed.payload ?? parsed,
        timestamp: parsed.timestamp,
      };
    } catch {
      return { type, payload: event.data };
    }
  }
}
