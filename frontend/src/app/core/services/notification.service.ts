import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription, forkJoin, interval, of } from 'rxjs';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';
import { API_BASE_URL } from '../constants/api.config';

export interface NotificationItem {
  id: string;
  message: string;
  timestamp: string;
  machineLabel: string;
  read: boolean;
  severity?: string;
  category?: string;
}

const READ_STORAGE_KEY = 'machcare-notifications-read';

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private notificationsSubject = new BehaviorSubject<NotificationItem[]>([]);
  notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  unreadCount$ = this.unreadCountSubject.asObservable();

  private pollSub: Subscription | null = null;

  constructor(private http: HttpClient) {
    this.startPolling();
  }

  getNotifications(): Observable<NotificationItem[]> {
    return this.fetchNotifications();
  }

  markAsRead(id: string): void {
    const readIds = this.getReadIds();
    if (!readIds.has(id)) {
      readIds.add(id);
      this.persistReadIds(readIds);
    }
    this.applyReadState();
  }

  markAllRead(): void {
    const readIds = this.getReadIds();
    this.notificationsSubject.value.forEach((item) => readIds.add(item.id));
    this.persistReadIds(readIds);
    this.applyReadState();
  }

  private startPolling(): void {
    if (this.pollSub) {
      return;
    }

    this.pollSub = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => this.fetchNotifications()),
      )
      .subscribe();
  }

  private fetchNotifications(): Observable<NotificationItem[]> {
    const roleId = Number(localStorage.getItem('roleId'));
    const source$: Observable<any> = roleId === 1
      ? forkJoin({
          faults: this.http.get<any>(`${API_BASE_URL}/faultlogs`).pipe(catchError(() => of([]))),
          machines: this.http.get<any>(`${API_BASE_URL}/admin/machines`).pipe(catchError(() => of([]))),
          alerts: this.http.get<any>(`${API_BASE_URL}/engineer/alerts`).pipe(catchError(() => of([]))),
        })
      : roleId === 2
        ? forkJoin({
            pending: this.http.get<any>(`${API_BASE_URL}/engineer/faults/pending`).pipe(catchError(() => of([]))),
            alerts: this.http.get<any>(`${API_BASE_URL}/engineer/alerts`).pipe(catchError(() => of([]))),
            analyses: this.http.get<any>(`${API_BASE_URL}/fault-analysis`).pipe(catchError(() => of([]))),
          })
        : forkJoin({
            faults: this.http.get<any>(`${API_BASE_URL}/faultlogs`).pipe(catchError(() => of([]))),
            machines: this.http.get<any>(`${API_BASE_URL}/machines`).pipe(catchError(() => of([]))),
          });

    return source$.pipe(
      map<any, NotificationItem[]>((response: any) => this.buildRoleNotifications(response, roleId)),
      map<NotificationItem[], NotificationItem[]>((items: NotificationItem[]) => {
        this.notificationsSubject.next(items);
        this.unreadCountSubject.next(items.filter((item: NotificationItem) => !item.read).length);
        return items;
      }),
      catchError(() => {
        const currentItems = this.notificationsSubject.value;
        this.unreadCountSubject.next(
          currentItems.filter((item) => !item.read).length,
        );
        return of(currentItems);
      }),
    );
  }

  private buildRoleNotifications(response: any, roleId: number): NotificationItem[] {
    if (roleId === 1) {
      return [
        ...this.buildFaultNotifications(this.unwrap(response.faults), 'admin-fault'),
        ...this.buildMachineNotifications(this.unwrap(response.machines)),
        ...this.buildAlertNotifications(this.unwrap(response.alerts)),
      ].slice(0, 12);
    }

    if (roleId === 2) {
      return [
        ...this.buildPendingAnalysisNotifications(this.unwrap(response.pending)),
        ...this.buildAlertNotifications(this.unwrap(response.alerts)),
        ...this.buildAnalysisNotifications(this.unwrap(response.analyses)),
      ].slice(0, 12);
    }

    return [
      ...this.buildFaultNotifications(this.unwrap(response.faults), 'operator-fault'),
      ...this.buildMachineStatusNotifications(this.unwrap(response.machines)),
    ].slice(0, 12);
  }

  private buildFaultNotifications(faults: any[], idPrefix = 'fault'): NotificationItem[] {
    const readIds = this.getReadIds();
    const sorted = [...faults].sort((a, b) => {
      const dateA = this.parseFaultTimestamp(a);
      const dateB = this.parseFaultTimestamp(b);
      return dateB.getTime() - dateA.getTime();
    });

    return sorted.slice(0, 10).map((fault, index) => {
      const id = `${idPrefix}-${fault.faultId || fault.machineId || index}`;
      const machineLabel = fault.machineName || fault.machineId || 'Machine';
      const timestamp = this.parseFaultTimestamp(fault).toISOString();
      const severity = fault.severity || 'Info';
      const message = this.getRoleAwareMessage(machineLabel, severity, fault);

      return {
        id,
        message,
        timestamp,
        machineLabel,
        severity,
        category: this.getNotificationCategory(severity),
        read: readIds.has(id),
      };
    });
  }

  private buildMachineNotifications(machines: any[]): NotificationItem[] {
    const readIds = this.getReadIds();
    return machines.slice(0, 4).map((machine, index) => ({
      id: `machine-${machine.machineId || index}`,
      message: `Machine ${machine.machineName || machine.machineId} registered with ${machine.productionCriticality || 'Medium'} criticality`,
      timestamp: new Date().toISOString(),
      machineLabel: machine.machineName || machine.machineId || 'Machine',
      category: 'Machine event',
      read: readIds.has(`machine-${machine.machineId || index}`),
    }));
  }

  private buildMachineStatusNotifications(machines: any[]): NotificationItem[] {
    const readIds = this.getReadIds();
    return machines.slice(0, 4).map((machine, index) => ({
      id: `operator-machine-${machine.machineId || index}`,
      message: `${machine.machineName || machine.machineId} status is ${machine.status || 'available'}`,
      timestamp: new Date().toISOString(),
      machineLabel: machine.machineName || machine.machineId || 'Machine',
      category: 'Machine status',
      read: readIds.has(`operator-machine-${machine.machineId || index}`),
    }));
  }

  private buildPendingAnalysisNotifications(faults: any[]): NotificationItem[] {
    const readIds = this.getReadIds();
    return faults.slice(0, 8).map((fault, index) => ({
      id: `pending-${fault.faultId || index}`,
      message: `${fault.priorityLevel || 'P3'} analysis pending for ${fault.machineId || 'machine'}`,
      timestamp: this.parseFaultTimestamp(fault).toISOString(),
      machineLabel: fault.machineId || 'Machine',
      severity: fault.severity,
      category: 'Pending analysis',
      read: readIds.has(`pending-${fault.faultId || index}`),
    }));
  }

  private buildAnalysisNotifications(analyses: any[]): NotificationItem[] {
    const readIds = this.getReadIds();
    return analyses.slice(0, 4).map((analysis, index) => ({
      id: `analysis-${analysis.analysisId || index}`,
      message: `Analysis completed for fault ${analysis.faultId}`,
      timestamp: new Date().toISOString(),
      machineLabel: analysis.faultId || 'Fault',
      category: 'Analysis completion',
      read: readIds.has(`analysis-${analysis.analysisId || index}`),
    }));
  }

  private buildAlertNotifications(alerts: any[]): NotificationItem[] {
    const readIds = this.getReadIds();
    return alerts.slice(0, 6).map((alert, index) => ({
      id: `alert-${alert.alertId || alert.linkedAnalysisId || index}`,
      message: `${alert.alertPriority || 'Alert'} generated for ${alert.machineId || 'machine'}`,
      timestamp: new Date().toISOString(),
      machineLabel: alert.machineId || 'Machine',
      severity: alert.alertPriority?.includes('Critical') ? 'Critical' : 'High',
      category: 'Critical alert',
      read: readIds.has(`alert-${alert.alertId || alert.linkedAnalysisId || index}`),
    }));
  }

  private unwrap(response: any): any[] {
    const data = response?.data || response || [];
    return Array.isArray(data) ? data : [];
  }

  private getRoleAwareMessage(
    machineLabel: string,
    severity: string,
    fault: any,
  ): string {
    const roleId = Number(localStorage.getItem('roleId'));
    const normalizedSeverity = (severity || '').toLowerCase();
    const faultType = fault.faultType || fault.description || 'fault';

    if (normalizedSeverity === 'critical') {
      return `Critical machine alert on ${machineLabel}`;
    }

    if (roleId === 1) {
      return `${severity} fault logged on ${machineLabel}`;
    }

    if (roleId === 2) {
      return `Maintenance review needed for ${machineLabel}`;
    }

    if (roleId === 3) {
      return `Latest ${faultType} update for ${machineLabel}`;
    }

    return `New fault reported on ${machineLabel}`;
  }

  private getNotificationCategory(severity: string): string {
    switch ((severity || '').toLowerCase()) {
      case 'critical':
        return 'Critical alert';
      case 'high':
        return 'High priority';
      case 'medium':
        return 'Maintenance notice';
      default:
        return 'System update';
    }
  }

  private parseFaultTimestamp(fault: any): Date {
    if (fault?.faultDate && fault?.faultTime) {
      return new Date(`${fault.faultDate}T${fault.faultTime}`);
    }
    if (fault?.faultDate) {
      return new Date(fault.faultDate);
    }
    return new Date();
  }

  private getReadIds(): Set<string> {
    const stored = localStorage.getItem(READ_STORAGE_KEY);
    if (!stored) {
      return new Set();
    }
    try {
      const parsed = JSON.parse(stored) as string[];
      return new Set(parsed || []);
    } catch {
      return new Set();
    }
  }

  private persistReadIds(readIds: Set<string>): void {
    localStorage.setItem(READ_STORAGE_KEY, JSON.stringify(Array.from(readIds)));
  }

  private applyReadState(): void {
    const readIds = this.getReadIds();
    const updated = this.notificationsSubject.value.map((item) => ({
      ...item,
      read: readIds.has(item.id),
    }));
    this.notificationsSubject.next(updated);
    this.unreadCountSubject.next(updated.filter((item) => !item.read).length);
  }
}

