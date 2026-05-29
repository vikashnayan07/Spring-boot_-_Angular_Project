import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { HistoryService } from '../../../core/services/history-service';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

@Component({
  selector: 'app-maintenance-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './maintenance-history.component.html',
  styleUrls: ['./maintenance-history.component.css'],
})
export class MaintenanceHistoryComponent implements OnInit {
  history: any[] = [];
  loading = true;
  searchTerm = '';
  statusFilter = 'All';
  page = 1;
  rowsPerPage = 10;
  sortKey:
    | 'historyId'
    | 'scheduleId'
    | 'machineId'
    | 'empId'
    | 'status'
    | 'resolvedDate'
    | 'lastUpdated' = 'historyId';
  sortDirection: 'asc' | 'desc' = 'desc';
  backRoute = '/admin/maintenance';
  backLabel = 'Back to Maintenance';

  constructor(
    private historyService: HistoryService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    if (this.router.url.includes('/engineer')) {
      this.backRoute = '/engineer/tasks';
      this.backLabel = 'Back to Tasks';
    } else if (this.router.url.includes('/operator')) {
      this.backRoute = '/operator/dashboard';
      this.backLabel = 'Back to Dashboard';
    } else {
      this.backRoute = '/admin/maintenance';
      this.backLabel = 'Back to Maintenance';
    }
    this.loadHistory();
  }

  goBack(): void {
    this.router.navigateByUrl(this.backRoute);
  }

  loadHistory(): void {
    this.loading = true;
    this.historyService
      .getAllHistory()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response: any) => {
          this.history = response.data || response || [];
        },
        error: () => {
          this.history = [];
        },
      });
  }

  get filteredHistory(): any[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.history.filter((item) => {
      const matchesSearch =
        !term ||
        `${item.historyId || ''}`.includes(term) ||
        `${item.scheduleId || ''}`.includes(term) ||
        `${item.machineId || ''}`.toLowerCase().includes(term) ||
        `${item.empId || ''}`.includes(term) ||
        `${item.remarks || ''}`.toLowerCase().includes(term);

      const matchesStatus =
        this.statusFilter === 'All' || item.status === this.statusFilter;
      return matchesSearch && matchesStatus;
    });
    return this.sortHistory(filtered);
  }

  get paginatedHistory(): any[] {
    const start = (this.page - 1) * this.rowsPerPage;
    return this.filteredHistory.slice(start, start + this.rowsPerPage);
  }

  get totalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredHistory.length / this.rowsPerPage),
    );
  }

  changePage(page: number): void {
    this.page = Math.min(Math.max(page, 1), this.totalPages);
  }

  sortBy(
    key:
      | 'historyId'
      | 'scheduleId'
      | 'machineId'
      | 'empId'
      | 'status'
      | 'resolvedDate'
      | 'lastUpdated',
  ): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
  }

  pages(): PaginationItem[] {
    return buildPagination(this.page, this.totalPages);
  }

  changePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.changePage(item);
  }

  statusClass(status: string): string {
    if (status === 'Blocked')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (status === 'Completed')
      return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
    if (status === 'In_progress')
      return 'bg-cyan-400/15 text-cyan-300 border-cyan-400/20';
    return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
  }

  resolvedTimestamp(item: any): string {
    if (!item?.resolvedDate || !item?.resolvedTime) return '—';
    return `${item.resolvedDate} ${item.resolvedTime}`;
  }

  private sortHistory(list: any[]): any[] {
    const direction = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.sortValue(a);
      const right = this.sortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private sortValue(item: any): string | number {
    switch (this.sortKey) {
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
}
