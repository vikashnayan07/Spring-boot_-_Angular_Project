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
  private socket: WebSocket | null = null;
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

    if (
      this.activeToken === token &&
      (this.socket?.readyState === WebSocket.OPEN ||
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

    this.socket.onopen = () => {
      this.reconnectTimer = null;
    };

    this.socket.onmessage = (message) => {
      this.zone.run(() => this.eventSubject.next(this.parseRawMessage(message.data)));
    };

    this.socket.onerror = () => {
      this.socket?.close();
    };

    this.socket.onclose = () => {
      if (this.activeToken === token) {
        if (this.reconnectTimer) {
          clearTimeout(this.reconnectTimer);
        }
        this.reconnectTimer = setTimeout(() => this.connect(), 3000);
      }
    };
  }

  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.close();
    }
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
}
