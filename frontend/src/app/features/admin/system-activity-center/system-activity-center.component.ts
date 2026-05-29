import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Subscription, catchError, forkJoin, of, finalize } from 'rxjs';
import {
  AuditLogEntry,
  AuditLogService,
} from '../../../core/services/audit-log.service';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

type AuditStatus = 'All' | 'SUCCESS' | 'FAILED';

@Component({
  selector: 'app-system-activity-center',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './system-activity-center.component.html',
  styleUrls: ['./system-activity-center.component.css'],
})
export class SystemActivityCenterComponent implements OnInit, OnDestroy {
  loading = true;
  summaryLoading = true;
  liveFeed: AuditLogEntry[] = [];
  logs: AuditLogEntry[] = [];
  summary = {
    actionsToday: 0,
    successRate: 0,
    failedActions: 0,
    activeUsers: 0,
  };

  filters = {
    date: '',
    role: 'All',
    status: 'All' as AuditStatus,
    action: 'All',
  };

  page = 1;
  rowsPerPage = 10;
  totalItems = 0;
  totalPages = 1;
  loadError = '';
  sortKey: 'logId' | 'timestamp' | 'roleId' | 'action' | 'status' | 'message' = 'logId';
  sortDirection: 'asc' | 'desc' = 'desc';

  readonly roleOptions = [
    { label: 'All Roles', value: 'All' },
    { label: 'Admin', value: 1 },
    { label: 'Engineer', value: 2 },
    { label: 'Operator', value: 3 },
  ];

  readonly statusOptions: AuditStatus[] = ['All', 'SUCCESS', 'FAILED'];

  readonly actionOptions = [
    'All',
    'Login',
    'Password Reset',
    'First-Time Password Setup',
    'Add Employee',
    'Register Machine',
    'Manual Fault Log',
    'CSV Upload',
    'Analyze Fault',
    'Auto Assign',
    'Restock Inventory',
    'Start Work',
    'Update Progress',
    'Request Part',
    'Raise Fault',
    'Request Support',
    'Complete Task',
  ];

  private refreshHandle: number | null = null;

  constructor(
    private auditLogService: AuditLogService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.loadCenter();
    this.refreshHandle = window.setInterval(() => this.loadCenter(true), 20000);
  }

  ngOnDestroy(): void {
    if (this.refreshHandle !== null) {
      window.clearInterval(this.refreshHandle);
    }
  }

  goBack(): void {
    this.router.navigateByUrl('/admin/dashboard');
  }

  loadCenter(silent = false): void {
    this.loading = !silent;
    this.summaryLoading = !silent;
    this.loadError = '';

    forkJoin({
      summary: this.auditLogService
        .getSummary()
        .pipe(catchError(() => of({ success: false, data: this.summary }))),
      logs: this.auditLogService
        .getLogs({
          ...this.filters,
          page: this.page,
          size: this.rowsPerPage,
        })
        .pipe(
          catchError(() =>
            of({
              success: false,
              data: [],
              totalItems: 0,
              totalPages: 1,
              page: this.page,
            }),
          ),
        ),
    })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.summaryLoading = false;
        }),
      )
      .subscribe({
        next: ({ summary, logs }: any) => {
          this.summary = summary?.data || summary || this.summary;
          const payload = logs?.data || logs || [];
          this.logs = Array.isArray(payload) ? payload : [];
          this.totalItems = logs?.totalItems ?? this.logs.length;
          this.totalPages =
            logs?.totalPages ??
            Math.max(1, Math.ceil(this.totalItems / this.rowsPerPage));
          this.liveFeed = this.logs.slice(0, 6);
          if (this.page > this.totalPages) {
            this.page = this.totalPages;
          }
        },
        error: () => {
          this.loadError = 'Unable to load activity intelligence.';
        },
      });
  }

  get sortedLogs(): AuditLogEntry[] {
    const direction = this.sortDirection === 'asc' ? 1 : -1;
    return [...this.logs].sort((a, b) => {
      const left = this.sortValue(a);
      const right = this.sortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  applyFilters(): void {
    this.page = 1;
    this.loadCenter();
  }

  clearFilters(): void {
    this.filters = {
      date: '',
      role: 'All',
      status: 'All',
      action: 'All',
    };
    this.page = 1;
    this.loadCenter();
  }

  changeRowsPerPage(value: string): void {
    this.rowsPerPage = Number(value);
    this.page = 1;
    this.loadCenter();
  }

  changePage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.page = page;
    this.loadCenter();
  }

  pages(): PaginationItem[] {
    return buildPagination(this.page, this.totalPages);
  }

  changePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.changePage(item);
  }

  sortBy(key: 'logId' | 'timestamp' | 'roleId' | 'action' | 'status' | 'message'): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
  }

  showingStart(total: number, page: number): number {
    return total === 0 ? 0 : (page - 1) * this.rowsPerPage + 1;
  }

  showingEnd(total: number, page: number): number {
    return Math.min(page * this.rowsPerPage, total);
  }

  roleLabel(roleId: number | null): string {
    if (roleId === 1) return 'Admin';
    if (roleId === 2) return 'Engineer';
    if (roleId === 3) return 'Operator';
    return 'Unknown';
  }

  statusClass(status: string | null): string {
    const normalized = `${status || ''}`.toUpperCase();
    if (normalized === 'SUCCESS') return 'success';
    if (normalized === 'FAILED') return 'failed';
    return 'neutral';
  }

  actionClass(action: string | null): string {
    const normalized = `${action || ''}`.toLowerCase();
    if (normalized.includes('login') || normalized.includes('password'))
      return 'identity';
    if (normalized.includes('fault') || normalized.includes('analysis'))
      return 'operations';
    if (normalized.includes('inventory') || normalized.includes('part'))
      return 'inventory';
    return 'system';
  }

  trackByLogId(_: number, item: AuditLogEntry): number {
    return item.logId;
  }

  private sortValue(entry: AuditLogEntry): string | number {
    switch (this.sortKey) {
      case 'timestamp':
        return `${(entry as any)?.createdAt || (entry as any)?.timestamp || ''}`.toLowerCase();
      case 'roleId':
        return Number((entry as any)?.roleId || 0);
      case 'action':
        return `${entry.action || ''}`.toLowerCase();
      case 'status':
        return `${entry.status || ''}`.toLowerCase();
      case 'message':
        return `${entry.message || ''}`.toLowerCase();
      default:
        return Number(entry.logId || 0);
    }
  }
}
