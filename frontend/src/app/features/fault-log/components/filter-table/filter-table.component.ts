import { Component, OnChanges, SimpleChanges, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FaultService } from '../../../../core/services/fault.services';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { buildPagination, PaginationItem } from '../../../../shared/utils/pagination';

@Component({
  selector: 'app-filter-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './filter-table.component.html',
  styleUrls: ['./filter-table.component.css'],
})
export class FilterTableComponent implements OnChanges {
  @Input() data: any[] = [];
  @Input() searchQuery = '';
  @Input() isLoading = false;

  filteredData: any[] = [];
  paginatedData: any[] = [];
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 0;
  pageSizeOptions = [10, 20, 50];
  sortKey: 'faultId' | 'machineId' | 'severity' | 'faultType' | 'faultDate' = 'faultId';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private faultService: FaultService,
    private toastService: ToastService,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] || changes['searchQuery']) {
      this.applySearchAndPaginate();
    }
  }

  updatePaginatedData(): void {
    this.totalPages = Math.ceil(this.filteredData.length / this.itemsPerPage);
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    this.paginatedData = this.filteredData.slice(start, end);
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePaginatedData();
  }

  onPageSizeChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.updatePaginatedData();
  }

  onPageSizeSelect(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const value = Number(target.value);
    if (!Number.isNaN(value)) {
      this.onPageSizeChange(value);
    }
  }

  onPageClick(page: PaginationItem): void {
    if (page === '...') return;
    this.goToPage(page);
  }

  exportCSV(): void {
    if (this.filteredData.length === 0) {
      this.toastService.warning('No Data', 'There are no rows to export.');
      return;
    }

    const filename = `machcare-faults-${new Date().toISOString().split('T')[0]}.csv`;
    this.faultService.exportCSV(this.filteredData, filename);
    this.toastService.success('Export Complete', 'CSV exported successfully.');
  }

  get pageInfo(): { start: number; end: number; total: number } {
    const total = this.filteredData.length;
    if (!total) {
      return { start: 0, end: 0, total: 0 };
    }
    const start = (this.currentPage - 1) * this.itemsPerPage + 1;
    const end = Math.min(start + this.itemsPerPage - 1, total);
    return { start, end, total };
  }

  get visiblePages(): PaginationItem[] {
    return buildPagination(this.currentPage, this.totalPages);
  }

  trackPage(_: number, value: PaginationItem): PaginationItem {
    return value;
  }

  sortBy(
    key: 'faultId' | 'machineId' | 'severity' | 'faultType' | 'faultDate',
  ): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.updatePaginatedData();
  }

  private applySearchAndPaginate(): void {
    const query = this.searchQuery.toLowerCase().trim();
    const source = Array.isArray(this.data) ? this.data : [];

    if (!query) {
      this.filteredData = [...source];
    } else {
      this.filteredData = source.filter((fault) => {
        const faultId = (fault.faultId || '').toLowerCase();
        const machineId = (fault.machineId || '').toLowerCase();
        const description = (fault.description || '').toLowerCase();
        const severity = (fault.severity || '').toLowerCase();
        const faultType = (fault.faultType || '').toLowerCase();
        return (
          faultId.includes(query) ||
          machineId.includes(query) ||
          description.includes(query) ||
          severity.includes(query) ||
          faultType.includes(query)
        );
      });
    }

    this.filteredData = this.sortData(this.filteredData);

    this.currentPage = 1;
    this.updatePaginatedData();
  }

  getSeverityClass(severity: string): string {
    switch ((severity || '').toLowerCase()) {
      case 'low':
        return 'severity-low';
      case 'medium':
        return 'severity-medium';
      case 'high':
        return 'severity-high';
      case 'critical':
        return 'severity-critical';
      default:
        return '';
    }
  }

  isEngineerRaised(fault: any): boolean {
    return (
      fault?.engineerRaised === true ||
      fault?.requestSource === 'ENGINEER_RAISED' ||
      Number(fault?.reporterRoleId || fault?.reportedByRoleId) === 2 ||
      `${fault?.reportedByRole || fault?.roleName || ''}`.toLowerCase().includes('engineer')
    );
  }

  private sortData(list: any[]): any[] {
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
      case 'machineId':
        return `${item?.machineId || ''}`.toLowerCase();
      case 'severity':
        return `${item?.severity || ''}`.toLowerCase();
      case 'faultType':
        return `${item?.faultType || ''}`.toLowerCase();
      case 'faultDate':
        return `${item?.faultDate || ''}`.toLowerCase();
      default:
        return `${item?.faultId || ''}`.toLowerCase();
    }
  }
}
