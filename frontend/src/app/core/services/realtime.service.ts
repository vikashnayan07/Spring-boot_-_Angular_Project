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
  private readonly eventSubject = new Subject<RealtimeMessage>();
  readonly events$ = this.eventSubject.asObservable();

  constructor(private zone: NgZone) {}

  connect(): void {
    const token = localStorage.getItem('token');
    if (!token || this.source) {
      return;
    }

    const streamUrl = `${API_BASE_URL}/realtime/stream?token=${encodeURIComponent(token)}`;
    this.source = new EventSource(streamUrl);

    [
      'connected',
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
      setTimeout(() => this.connect(), 5000);
    };
  }

  disconnect(): void {
    this.source?.close();
    this.source = null;
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
