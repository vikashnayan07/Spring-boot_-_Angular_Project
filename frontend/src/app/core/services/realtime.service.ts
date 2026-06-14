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
  private socket: WebSocket | null = null;
  private activeToken: string | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private fallbackTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly eventSubject = new Subject<RealtimeMessage>();
  readonly events$ = this.eventSubject.asObservable();

  constructor(private zone: NgZone) {}

  connect(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      this.disconnect();
      return;
    }

    if (
      this.activeToken === token &&
      (this.source ||
        this.socket?.readyState === WebSocket.OPEN ||
        this.socket?.readyState === WebSocket.CONNECTING)
    ) {
      return;
    }

    this.disconnect();
    this.activeToken = token;
    this.connectWebSocket(token);
  }

  private connectWebSocket(token: string): void {
    const socketUrl = this.buildWebSocketUrl(token);
    this.socket = new WebSocket(socketUrl);

    this.fallbackTimer = setTimeout(() => {
      if (this.socket?.readyState !== WebSocket.OPEN) {
        const pendingSocket = this.socket;
        this.socket = null;
        if (pendingSocket) {
          pendingSocket.onclose = null;
          pendingSocket.onerror = null;
          pendingSocket.close();
        }
        this.connectEventSource(token);
      }
    }, 3000);

    this.socket.onopen = () => {
      if (this.fallbackTimer) {
        clearTimeout(this.fallbackTimer);
        this.fallbackTimer = null;
      }
    };

    this.socket.onmessage = (message) => {
      this.zone.run(() => this.eventSubject.next(this.parseRawMessage(message.data)));
    };

    this.socket.onerror = () => {
      this.socket?.close();
    };

    this.socket.onclose = () => {
      if (this.fallbackTimer) {
        clearTimeout(this.fallbackTimer);
        this.fallbackTimer = null;
      }
      if (this.activeToken === token) {
        this.reconnectTimer = setTimeout(() => this.connect(), 3000);
      }
    };
  }

  private connectEventSource(token: string): void {
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
    if (this.fallbackTimer) {
      clearTimeout(this.fallbackTimer);
      this.fallbackTimer = null;
    }
    this.source?.close();
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.close();
    }
    this.source = null;
    this.socket = null;
    this.activeToken = null;
  }

  private buildWebSocketUrl(token: string): string {
    const encodedToken = encodeURIComponent(token);
    if (API_BASE_URL.startsWith('http://') || API_BASE_URL.startsWith('https://')) {
      return `${API_BASE_URL.replace(/^http/, 'ws')}/realtime/ws?token=${encodedToken}`;
    }
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${protocol}//${window.location.host}${API_BASE_URL}/realtime/ws?token=${encodedToken}`;
  }

  private parseRawMessage(data: string): RealtimeMessage {
    try {
      const parsed = JSON.parse(data);
      return {
        type: parsed.type || 'message',
        payload: parsed.payload ?? parsed,
        timestamp: parsed.timestamp,
      };
    } catch {
      return { type: 'message', payload: data };
    }
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
