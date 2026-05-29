import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { AlertService } from '../../../core/services/alert-service';
import { ScheduleService } from '../../../core/services/schedule.service';
import { HistoryService } from '../../../core/services/history-service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

@Component({
  selector: 'app-maintenance-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './maintenance-landing.component.html',
  styleUrls: ['./maintenance-landing.component.css'],
})
export class MaintenanceLandingComponent implements OnInit {
  baseRoute = '/admin';
  role = '';
  backRoute = '/admin/dashboard';
  backLabel = 'Back to Dashboard';
  activeTab: 'alerts' | 'workOrders' | 'schedule' | 'history' = 'alerts';
  searchTerm = '';
  statusFilter = 'All';
  loading = true;
  loadError = '';
  assigningAlertId: number | null = null;
  rowsPerPage = 8;
  alertPage = 1;
  workOrderPage = 1;
  schedulePage = 1;
  historyPage = 1;
  alertSortKey:
    | 'alertId'
    | 'machineId'
    | 'severity'
    | 'alertPriority'
    | 'alertReason' = 'alertId';
  alertSortDirection: 'asc' | 'desc' = 'desc';
  workOrderSortKey:
    | 'scheduleId'
    | 'alertId'
    | 'machineId'
    | 'empId'
    | 'status'
    | 'scheduleDate' = 'scheduleId';
  workOrderSortDirection: 'asc' | 'desc' = 'desc';
  historySortKey:
    | 'historyId'
    | 'scheduleId'
    | 'machineId'
    | 'empId'
    | 'status'
    | 'resolvedDate'
    | 'lastUpdated' = 'historyId';
  historySortDirection: 'asc' | 'desc' = 'desc';
  selectedHistory: any | null = null;
  pendingAssignAlert: any | null = null;

  alerts: any[] = [];
  schedules: any[] = [];
  history: any[] = [];

  constructor(
    private router: Router,
    private alertService: AlertService,
    private scheduleService: ScheduleService,
    private historyService: HistoryService,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.role = localStorage.getItem('role') || '';

    if (this.router.url.includes('/engineer')) {
      this.baseRoute = '/engineer';
    } else if (this.router.url.includes('/operator')) {
      this.baseRoute = '/operator';
    } else {
      this.baseRoute = '/admin';
    }

    this.backRoute = `${this.baseRoute}/dashboard`;
    this.backLabel =
      this.baseRoute === '/engineer'
        ? 'Back to Engineer Dashboard'
        : this.baseRoute === '/operator'
          ? 'Back to Operator Dashboard'
          : 'Back to Admin Dashboard';

    this.loadCommandCenter();
  }

  goBack(): void {
    this.router.navigateByUrl(this.backRoute);
  }

  loadCommandCenter(): void {
    this.loading = true;
    this.loadError = '';

    forkJoin({
      alerts: this.isAdmin
        ? this.alertService
            .getAllAlerts()
            .pipe(catchError(() => of({ data: [] })))
        : of({ data: [] }),
      schedules: this.isAdmin
        ? this.scheduleService.getAllSchedules().pipe(catchError(() => of([])))
        : this.scheduleService.getMySchedules().pipe(catchError(() => of([]))),
      history: this.historyService
        .getAllHistory()
        .pipe(catchError(() => of({ data: [] }))),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ alerts, schedules, history }: any) => {
          this.alerts = this.unwrap(alerts).filter(
            (alert: any) =>
              alert.status !== 'Assigned' && alert.status !== 'Resolved',
          );
          this.schedules = this.unwrap(schedules);
          this.history = this.unwrap(history);
        },
        error: () => {
          this.loadError =
            'Maintenance workspace could not be loaded. Please retry.';
        },
      });
  }

  navigateTo(page: string): void {
    this.router.navigate([this.baseRoute, page]);
  }

  autoAssign(alert: any): void {
    if (!this.isAdmin || !alert?.alertId || this.assigningAlertId) return;
    this.pendingAssignAlert = alert;
  }

  confirmAutoAssign(): void {
    const alert = this.pendingAssignAlert;
    if (!this.isAdmin || !alert?.alertId || this.assigningAlertId) return;
    this.pendingAssignAlert = null;

    this.assigningAlertId = alert.alertId;
    this.alertService
      .autoAssign(alert.alertId)
      .pipe(finalize(() => (this.assigningAlertId = null)))
      .subscribe({
        next: (response: any) => {
          this.alerts = this.alerts.filter(
            (item) => item.alertId !== alert.alertId,
          );
          this.toastService.success(
            'Work order created',
            response?.message ||
              `Alert ${alert.alertId} was assigned to an engineer.`,
          );
          this.loadCommandCenter();
          this.activeTab = 'workOrders';
        },
        error: (error) => {
          this.toastService.error(
            'Auto assignment failed',
            error?.error?.message ||
              'Unable to assign this alert. Please retry.',
          );
        },
      });
  }

  cancelAutoAssign(): void {
    this.pendingAssignAlert = null;
  }

  get isAdmin(): boolean {
    return this.role === 'ADMIN' || this.baseRoute === '/admin';
  }

  get openAlerts(): number {
    return this.alerts.length;
  }

  get activeWorkOrders(): number {
    return this.schedules.filter((item) => item.status !== 'Completed').length;
  }

  get inProgressTasks(): number {
    return this.schedules.filter(
      (item) => this.normalizeStatus(item.status) === 'in_progress',
    ).length;
  }

  get completedRepairs(): number {
    const completedHistory = this.history.filter(
      (item) => item.status === 'Completed',
    ).length;
    return (
      completedHistory ||
      this.schedules.filter((item) => item.status === 'Completed').length
    );
  }

  get filteredAlerts(): any[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.alerts.filter((alert) =>
      this.matchesTerm(alert, term, [
        'alertId',
        'machineId',
        'severity',
        'alertPriority',
        'alertReason',
      ]),
    );
    return this.sortAlerts(filtered);
  }

  get paginatedAlerts(): any[] {
    return this.paginate(this.filteredAlerts, this.alertPage);
  }

  get filteredSchedules(): any[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.schedules.filter(
      (item) =>
        this.matchesTerm(item, term, [
          'scheduleId',
          'alertId',
          'machineId',
          'empId',
          'status',
          'scheduleDate',
        ]) && this.matchesStatus(item.status),
    );
    return this.sortSchedules(filtered);
  }

  get activeSchedules(): any[] {
    return this.filteredSchedules.filter((item) => item.status !== 'Completed');
  }

  get paginatedActiveSchedules(): any[] {
    return this.paginate(this.activeSchedules, this.schedulePage);
  }

  get filteredHistory(): any[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.history.filter(
      (item) =>
        this.matchesTerm(item, term, [
          'historyId',
          'scheduleId',
          'machineId',
          'empId',
          'status',
          'remarks',
          'resolvedDate',
        ]) && this.matchesStatus(item.status),
    );
    return this.sortHistory(filtered);
  }

  get paginatedWorkOrders(): any[] {
    return this.paginate(this.filteredSchedules, this.workOrderPage);
  }

  get paginatedHistory(): any[] {
    return this.paginate(this.filteredHistory, this.historyPage);
  }

  get workOrderTotalPages(): number {
    return this.totalPages(this.filteredSchedules.length);
  }

  get historyTotalPages(): number {
    return this.totalPages(this.filteredHistory.length);
  }

  get scheduleTotalPages(): number {
    return this.totalPages(this.activeSchedules.length);
  }

  get alertTotalPages(): number {
    return this.totalPages(this.filteredAlerts.length);
  }

  statusClass(status: string): string {
    const normalized = this.normalizeStatus(status);
    if (normalized === 'blocked')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (normalized === 'completed')
      return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
    if (normalized === 'in_progress')
      return 'bg-cyan-400/15 text-cyan-300 border-cyan-400/20';
    return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
  }

  priorityLabel(alert: any): string {
    return (
      alert.alertPriority || alert.priority || alert.severity || 'System Alert'
    );
  }

  isSupportEscalation(alert: any): boolean {
    return (
      `${alert?.issueName || ''}`.toLowerCase().includes('support escalation') ||
      `${alert?.alertPriority || ''}`.toLowerCase().includes('support_escalation') ||
      `${alert?.alertReason || ''}`.toLowerCase().includes('type: support_request')
    );
  }

  changePage(
    type: 'alerts' | 'workOrders' | 'history' | 'schedule',
    page: number,
  ): void {
    const max =
      type === 'alerts'
        ? this.alertTotalPages
        : type === 'workOrders'
        ? this.workOrderTotalPages
        : type === 'schedule'
          ? this.scheduleTotalPages
          : this.historyTotalPages;
    const nextPage = Math.min(Math.max(page, 1), max || 1);
    if (type === 'alerts') {
      this.alertPage = nextPage;
    } else if (type === 'workOrders') {
      this.workOrderPage = nextPage;
    } else if (type === 'schedule') {
      this.schedulePage = nextPage;
    } else {
      this.historyPage = nextPage;
    }
  }

  pages(total: number, currentPage: number): PaginationItem[] {
    return buildPagination(currentPage, total);
  }

  changePageFromItem(
    type: 'alerts' | 'workOrders' | 'history' | 'schedule',
    item: PaginationItem,
  ): void {
    if (item === '...') return;
    this.changePage(type, item);
  }

  sortAlertsBy(
    key: 'alertId' | 'machineId' | 'severity' | 'alertPriority' | 'alertReason',
  ): void {
    if (this.alertSortKey === key) {
      this.alertSortDirection = this.alertSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.alertSortKey = key;
      this.alertSortDirection = 'asc';
    }
  }

  toggleAlertSortDirection(): void {
    this.alertSortDirection = this.alertSortDirection === 'asc' ? 'desc' : 'asc';
  }

  sortWorkOrdersBy(
    key:
      | 'scheduleId'
      | 'alertId'
      | 'machineId'
      | 'empId'
      | 'status'
      | 'scheduleDate',
  ): void {
    if (this.workOrderSortKey === key) {
      this.workOrderSortDirection = this.workOrderSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.workOrderSortKey = key;
      this.workOrderSortDirection = 'asc';
    }
  }

  sortHistoryBy(
    key:
      | 'historyId'
      | 'scheduleId'
      | 'machineId'
      | 'empId'
      | 'status'
      | 'resolvedDate'
      | 'lastUpdated',
  ): void {
    if (this.historySortKey === key) {
      this.historySortDirection = this.historySortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.historySortKey = key;
      this.historySortDirection = 'asc';
    }
  }

  showingStart(total: number, page: number): number {
    if (total === 0) return 0;
    return (page - 1) * this.rowsPerPage + 1;
  }

  showingEnd(total: number, page: number): number {
    return Math.min(page * this.rowsPerPage, total);
  }

  openHistoryDetail(item: any): void {
    this.selectedHistory = item;
  }

  closeHistoryDetail(): void {
    this.selectedHistory = null;
  }

  private unwrap(response: any): any[] {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.data)) return response.data;
    if (Array.isArray(response?.tasks)) return response.tasks;
    return [];
  }

  private matchesTerm(item: any, term: string, fields: string[]): boolean {
    if (!term) return true;
    return fields.some((field) =>
      `${item?.[field] ?? ''}`.toLowerCase().includes(term),
    );
  }

  private matchesStatus(status: string): boolean {
    return this.statusFilter === 'All' || status === this.statusFilter;
  }

  private paginate(items: any[], page: number): any[] {
    const start = (page - 1) * this.rowsPerPage;
    return items.slice(start, start + this.rowsPerPage);
  }

  private totalPages(total: number): number {
    return Math.max(1, Math.ceil(total / this.rowsPerPage));
  }

  private normalizeStatus(status: string): string {
    return `${status || ''}`.toLowerCase();
  }

  private sortAlerts(list: any[]): any[] {
    const direction = this.alertSortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.alertSortValue(a);
      const right = this.alertSortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private alertSortValue(alert: any): string | number {
    switch (this.alertSortKey) {
      case 'machineId':
        return `${alert?.machineId || ''}`.toLowerCase();
      case 'severity':
        return `${alert?.severity || ''}`.toLowerCase();
      case 'alertPriority':
        return `${alert?.alertPriority || ''}`.toLowerCase();
      case 'alertReason':
        return `${alert?.alertReason || ''}`.toLowerCase();
      default:
        return Number(alert?.alertId || 0);
    }
  }

  private sortSchedules(list: any[]): any[] {
    const direction = this.workOrderSortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.scheduleSortValue(a);
      const right = this.scheduleSortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private scheduleSortValue(schedule: any): string | number {
    switch (this.workOrderSortKey) {
      case 'alertId':
        return Number(schedule?.alertId || 0);
      case 'machineId':
        return `${schedule?.machineId || ''}`.toLowerCase();
      case 'empId':
        return Number(schedule?.empId || 0);
      case 'status':
        return `${schedule?.status || ''}`.toLowerCase();
      case 'scheduleDate':
        return `${schedule?.scheduleDate || ''}`.toLowerCase();
      default:
        return Number(schedule?.scheduleId || 0);
    }
  }

  private sortHistory(list: any[]): any[] {
    const direction = this.historySortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.historySortValue(a);
      const right = this.historySortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private historySortValue(item: any): string | number {
    switch (this.historySortKey) {
      case 'scheduleId':
        return Number(item?.scheduleId || 0);
      case 'machineId':
        return `${item?.machineId || ''}`.toLowerCase();
      case 'empId':
        return Number(item?.empId || 0);
      case 'status':
        return `${item?.status || ''}`.toLowerCase();
      case 'resolvedDate':
        return `${item?.resolvedDate || ''}`.toLowerCase();
      case 'lastUpdated':
        return `${item?.lastUpdated || ''}`.toLowerCase();
      default:
        return Number(item?.historyId || 0);
    }
  }

  formatTimestamp(
    dateValue: string | null | undefined,
    timeValue?: string | null,
  ): string {
    if (!dateValue) return '—';
    return timeValue ? `${dateValue} ${timeValue}` : dateValue;
  }
}
