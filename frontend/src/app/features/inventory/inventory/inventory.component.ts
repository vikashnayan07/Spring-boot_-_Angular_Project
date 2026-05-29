import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import {
  InventoryService,
  ReorderRecommendation,
  UsageHistory,
} from '../../../core/services/inventory.service';
import { ScheduleService } from '../../../core/services/schedule.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { InventoryItem } from '../inventory.model';
import { ApiService } from '../../../core/services/api.service';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

type InventoryModal = 'part' | 'restock' | 'usage' | null;

@Component({
  selector: 'app-inventory',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './inventory.component.html',
  styleUrls: ['./inventory.component.css'],
})
export class InventoryComponent implements OnInit {
  loading = true;
  submitting = false;
  activeModal: InventoryModal = null;
  searchTerm = '';
  stockFilter = 'All';
  lifecycleFilter = 'All';
  conditionFilter = 'All';
  sortKey: keyof InventoryItem = 'partId';
  sortDirection: 'asc' | 'desc' = 'asc';
  page = 1;
  usagePage = 1;
  rowsPerPage = 10;
  usageRowsPerPage = 8;
  usageMonth = '';
  usagePartId = 'All';
  appliedUsageMonth = '';
  appliedUsagePartId = 'All';
  usageSortKey: 'partId' | 'qtyAssigned' | 'scheduleId' | 'empId' | 'lastUpdated' = 'lastUpdated';
  usageSortDirection: 'asc' | 'desc' = 'desc';
  backRoute = '/admin/maintenance';
  backLabel = 'Back to Maintenance';

  inventory: InventoryItem[] = [];
  usageHistory: UsageHistory[] = [];
  schedules: any[] = [];
  machines: any[] = [];
  employees: any[] = [];
  reorderRecommendations: ReorderRecommendation[] = [];
  selectedPart: InventoryItem | null = null;
  newStock = 0;

  partForm = {
    partName: '',
    categoryId: 1,
    machineId: '',
    currentStock: 0,
    minStock: 0,
    manufactureDate: '',
    expiryDate: '',
    warrantyExpiryDate: '',
    shelfLifeDays: 0,
  };

  lifecycleOptions = [
    'All',
    'ACTIVE',
    'EXPIRING_SOON',
    'END_OF_LIFE',
    'OUT_OF_WARRANTY',
    'EXPIRED',
  ];
  conditionOptions = [
    'All',
    'AVAILABLE',
    'REORDER',
    'OUT_OF_STOCK',
    'DISCARDED',
  ];

  constructor(
    private inventoryService: InventoryService,
    private scheduleService: ScheduleService,
    private toastService: ToastService,
    private apiService: ApiService,
    private router: Router,
    private route: ActivatedRoute,
  ) {}

  ngOnInit(): void {
    this.backRoute = '/admin/maintenance';
    this.route.queryParamMap.subscribe((params) => {
      this.applyDrilldownParams(params.get('stock'), params.get('lifecycle'));
    });
    this.loadInventoryCenter();
  }

  goBack(): void {
    this.router.navigateByUrl(this.backRoute);
  }

  loadInventoryCenter(): void {
    this.loading = true;

    forkJoin({
      parts: this.inventoryService.getAllParts().pipe(catchError(() => of([]))),
      usage: this.inventoryService
        .getUsageHistory()
        .pipe(catchError(() => of([]))),
      reorderRecommendations: this.inventoryService
        .getReorderRecommendations()
        .pipe(catchError(() => of([]))),
      schedules: this.scheduleService
        .getAllSchedules()
        .pipe(catchError(() => of([]))),
      machines: this.inventoryService
        .getAllMachines()
        .pipe(catchError(() => of([]))),
      employees: this.apiService
        .getAllEmployees()
        .pipe(catchError(() => of([]))),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe(
        ({
          parts,
          usage,
          reorderRecommendations,
          schedules,
          machines,
          employees,
        }: any) => {
          this.inventory = Array.isArray(parts) ? parts : [];
          this.usageHistory = Array.isArray(usage) ? usage : [];
          this.reorderRecommendations = Array.isArray(reorderRecommendations)
            ? reorderRecommendations
            : [];
          this.schedules = this.unwrap(schedules);
          this.machines = this.unwrap(machines);
          this.employees = this.unwrap(employees);
          this.scrollToTableIfDrilldown();
        },
      );
  }

  get totalParts(): number {
    return this.inventory.length;
  }

  get lowStockParts(): number {
    return this.inventory.filter((part) => this.isLowStock(part)).length;
  }

  get reservedParts(): number {
    const activeScheduleIds = new Set(
      this.schedules
        .filter((schedule) => schedule.status !== 'Completed')
        .map((schedule) => Number(schedule.scheduleId)),
    );

    return this.usageHistory
      .filter(
        (usage) =>
          usage.scheduleId !== null &&
          activeScheduleIds.has(Number(usage.scheduleId)),
      )
      .reduce((total, usage) => total + Number(usage.qtyAssigned || 0), 0);
  }

  get partsUsedToday(): number {
    const today = new Date().toDateString();
    return this.usageHistory
      .filter(
        (usage) =>
          usage.lastUpdated &&
          new Date(usage.lastUpdated).toDateString() === today,
      )
      .reduce((total, usage) => total + Number(usage.qtyAssigned || 0), 0);
  }

  get filteredInventory(): InventoryItem[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.inventory.filter((part) => {
      const matchesSearch =
        !term ||
        `${part.partId}`.includes(term) ||
        `${part.partName || ''}`.toLowerCase().includes(term) ||
        `${part.machineId || ''}`.toLowerCase().includes(term) ||
        `${part.categoryId || ''}`.includes(term);

      const matchesStock =
        this.stockFilter === 'All' ||
        (this.stockFilter === 'Low' && this.isLowStock(part)) ||
        (this.stockFilter === 'Out' && Number(part.currentStock || 0) === 0) ||
        (this.stockFilter === 'Reorder' && this.isReorderRecommended(part));

      const matchesLifecycle =
        this.lifecycleFilter === 'All' ||
        `${part.lifecycleStatus || ''}`.toUpperCase() ===
          this.lifecycleFilter.toUpperCase();

      const matchesCondition =
        this.conditionFilter === 'All' ||
        `${part.conditionStatus || ''}`.toUpperCase() ===
          this.conditionFilter.toUpperCase();

      return (
        matchesSearch && matchesStock && matchesLifecycle && matchesCondition
      );
    });

    return [...filtered].sort((a: any, b: any) => {
      const left = a[this.sortKey] ?? '';
      const right = b[this.sortKey] ?? '';
      const result = left > right ? 1 : left < right ? -1 : 0;
      return this.sortDirection === 'asc' ? result : -result;
    });
  }

  get paginatedInventory(): InventoryItem[] {
    const start = (this.page - 1) * this.rowsPerPage;
    return this.filteredInventory.slice(start, start + this.rowsPerPage);
  }

  get totalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredInventory.length / this.rowsPerPage),
    );
  }

  get lowStockWarnings(): InventoryItem[] {
    return this.inventory
      .filter((part) => this.isReorderRecommended(part))
      .slice(0, 4);
  }

  get reservationRows(): UsageHistory[] {
    const activeScheduleIds = new Set(
      this.schedules
        .filter((schedule) => schedule.status !== 'Completed')
        .map((schedule) => Number(schedule.scheduleId)),
    );

    return this.usageHistory.filter(
      (usage) =>
        usage.scheduleId !== null &&
        activeScheduleIds.has(Number(usage.scheduleId)),
    );
  }

  get latestReorderAlerts(): ReorderRecommendation[] {
    return this.reorderRecommendations.slice(0, 3);
  }

  get filteredUsageHistory(): UsageHistory[] {
    const filtered = this.usageHistory.filter((usage) => {
      const matchesPart =
        this.appliedUsagePartId === 'All' ||
        Number(usage.partId) === Number(this.appliedUsagePartId);
      const matchesMonth =
        !this.appliedUsageMonth ||
        this.monthKey(usage.lastUpdated) === this.appliedUsageMonth;
      return matchesPart && matchesMonth;
    });
    return this.sortUsageHistory(filtered);
  }

  get paginatedUsageHistory(): UsageHistory[] {
    const start = (this.usagePage - 1) * this.usageRowsPerPage;
    return this.filteredUsageHistory.slice(
      start,
      start + this.usageRowsPerPage,
    );
  }

  get usageTotalPages(): number {
    return Math.max(
      1,
      Math.ceil(this.filteredUsageHistory.length / this.usageRowsPerPage),
    );
  }

  openAddPart(): void {
    this.partForm = {
      partName: '',
      categoryId: 1,
      machineId: '',
      currentStock: 0,
      minStock: 0,
      manufactureDate: '',
      expiryDate: '',
      warrantyExpiryDate: '',
      shelfLifeDays: 0,
    };
    this.activeModal = 'part';
  }

  openRestock(part: InventoryItem): void {
    this.selectedPart = part;
    this.newStock = Number(part.currentStock || 0);
    this.activeModal = 'restock';
  }

  openRestockFromAlert(alert: any): void {
    const part = this.inventory.find((item) => {
      const partName = `${item.partName || ''}`.toLowerCase().trim();
      const alertPart = `${alert.partName || ''}`.toLowerCase().trim();
      return partName === alertPart || partName.includes(alertPart);
    });
    if (part) {
      this.openRestock(part);
    }
  }

  dismissReorderAlert(index: number): void {
    this.reorderRecommendations = this.reorderRecommendations.filter(
      (_, currentIndex) => currentIndex !== index,
    );
  }

  openUsage(): void {
    this.activeModal = 'usage';
  }

  closeModal(): void {
    if (this.submitting) return;
    this.activeModal = null;
    this.selectedPart = null;
  }

  savePart(): void {
    if (!this.partForm.partName.trim() || !this.partForm.machineId) {
      this.toastService.warning(
        'Missing part details',
        'Part name and machine are required.',
      );
      return;
    }

    if (this.partForm.currentStock < 0 || this.partForm.minStock < 0) {
      this.toastService.warning(
        'Invalid stock',
        'Stock values cannot be negative.',
      );
      return;
    }

    if (
      this.partForm.manufactureDate &&
      this.partForm.expiryDate &&
      new Date(this.partForm.expiryDate) <
        new Date(this.partForm.manufactureDate)
    ) {
      this.toastService.warning(
        'Invalid dates',
        'Expiry date must be on or after manufacture date.',
      );
      return;
    }

    if (Number(this.partForm.shelfLifeDays) < 0) {
      this.toastService.warning(
        'Invalid shelf life',
        'Shelf life must be 0 or greater.',
      );
      return;
    }

    this.submitting = true;
    this.inventoryService
      .createPart(this.partForm)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.toastService.success(
            'Part registered',
            'Inventory component has been added.',
          );
          this.closeModal();
          this.loadInventoryCenter();
        },
        error: (error) => {
          this.toastService.error(
            'Part creation failed',
            error?.error?.message || 'Unable to create this part.',
          );
        },
      });
  }

  updateStock(): void {
    if (!this.selectedPart) return;
    if (this.newStock < 0 || Number.isNaN(Number(this.newStock))) {
      this.toastService.warning(
        'Invalid stock',
        'Enter a valid stock quantity.',
      );
      return;
    }

    this.submitting = true;
    this.inventoryService
      .updateStock(this.selectedPart.partId, Number(this.newStock))
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.toastService.success(
            'Stock updated',
            `${this.selectedPart?.partName} stock has been updated.`,
          );
          this.closeModal();
          this.loadInventoryCenter();
        },
        error: (error) => {
          this.toastService.error(
            'Stock update failed',
            error?.error?.message || 'Unable to update stock.',
          );
        },
      });
  }

  sortBy(key: keyof InventoryItem): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
  }

  changePage(page: number): void {
    this.page = Math.min(Math.max(page, 1), this.totalPages);
  }

  pages(): PaginationItem[] {
    return buildPagination(this.page, this.totalPages);
  }

  usagePages(): PaginationItem[] {
    return buildPagination(this.usagePage, this.usageTotalPages);
  }

  changePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.changePage(item);
  }

  changeUsagePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.changeUsagePage(item);
  }

  changeUsagePage(page: number): void {
    this.usagePage = Math.min(Math.max(page, 1), this.usageTotalPages);
  }

  sortUsageBy(
    key: 'partId' | 'qtyAssigned' | 'scheduleId' | 'empId' | 'lastUpdated',
  ): void {
    if (this.usageSortKey === key) {
      this.usageSortDirection = this.usageSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.usageSortKey = key;
      this.usageSortDirection = 'asc';
    }
  }

  applyUsageFilters(): void {
    this.appliedUsageMonth = this.usageMonth;
    this.appliedUsagePartId = this.usagePartId;
    this.usagePage = 1;
    this.toastService.info(
      'Usage filters applied',
      `${this.filteredUsageHistory.length} usage record(s) matched.`,
    );
  }

  resetUsageFilters(): void {
    this.usageMonth = '';
    this.usagePartId = 'All';
    this.appliedUsageMonth = '';
    this.appliedUsagePartId = 'All';
    this.usagePage = 1;
    this.toastService.info('Usage filters reset', 'Showing all usage records.');
  }

  stockStatus(part: InventoryItem): string {
    if (Number(part.currentStock || 0) === 0) return 'Out of stock';
    if (this.isReorderRecommended(part)) return 'Reorder recommended';
    return 'Healthy';
  }

  stockClass(part: InventoryItem): string {
    if (Number(part.currentStock || 0) === 0)
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (this.isReorderRecommended(part))
      return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  lifecycleStatus(part: InventoryItem): string {
    return `${part.lifecycleStatus || 'ACTIVE'}`.toUpperCase();
  }

  lifecycleClass(part: InventoryItem): string {
    const status = this.lifecycleStatus(part);
    if (status === 'EXPIRED')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (status === 'OUT_OF_WARRANTY')
      return 'bg-orange-400/15 text-orange-300 border-orange-400/20';
    if (status === 'END_OF_LIFE')
      return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
    if (status === 'EXPIRING_SOON')
      return 'bg-yellow-400/15 text-yellow-200 border-yellow-300/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  conditionStatus(part: InventoryItem): string {
    return `${part.conditionStatus || this.stockStatus(part)}`.toUpperCase();
  }

  conditionClass(part: InventoryItem): string {
    const status = this.conditionStatus(part);
    if (status === 'DISCARDED')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (status === 'OUT_OF_STOCK')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (status === 'REORDER')
      return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  scheduleStatus(scheduleId: number | null): string {
    if (!scheduleId) return 'Unlinked';
    return (
      this.schedules.find(
        (schedule) => Number(schedule.scheduleId) === Number(scheduleId),
      )?.status || 'Unknown'
    );
  }

  partName(partId: number): string {
    return (
      this.inventory.find((part) => Number(part.partId) === Number(partId))
        ?.partName || `Part ${partId}`
    );
  }

  engineerDisplay(empId: number): string {
    const employee = this.employees.find(
      (emp) => Number(emp.empId) === Number(empId),
    );
    return employee ? `EMP${empId} - ${employee.name}` : `EMP${empId}`;
  }

  private isLowStock(part: InventoryItem): boolean {
    return Number(part.currentStock || 0) <= Number(part.minStock || 0);
  }

  private isReorderRecommended(part: InventoryItem): boolean {
    return (
      this.isLowStock(part) ||
      this.reorderRecommendations.some((item) =>
        this.partMatchesRecommendation(part, item),
      )
    );
  }

  private sortUsageHistory(list: UsageHistory[]): UsageHistory[] {
    const direction = this.usageSortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.usageSortValue(a);
      const right = this.usageSortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private usageSortValue(item: UsageHistory): string | number {
    switch (this.usageSortKey) {
      case 'partId':
        return Number(item.partId || 0);
      case 'qtyAssigned':
        return Number(item.qtyAssigned || 0);
      case 'scheduleId':
        return Number(item.scheduleId || 0);
      case 'empId':
        return Number(item.empId || 0);
      default:
        return `${item.lastUpdated || ''}`.toLowerCase();
    }
  }

  private partMatchesRecommendation(
    part: InventoryItem,
    recommendation: ReorderRecommendation,
  ): boolean {
    const partName = `${part.partName || ''}`.toLowerCase().trim();
    const recommendedPart = `${recommendation.partName || ''}`
      .toLowerCase()
      .trim();
    return partName === recommendedPart || partName.includes(recommendedPart);
  }

  private monthKey(value: string): string {
    if (!value) return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return '';
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
  }

  private unwrap(response: any): any[] {
    if (Array.isArray(response)) return response;
    if (Array.isArray(response?.data)) return response.data;
    return [];
  }

  private applyDrilldownParams(stock: string | null, lifecycle: string | null): void {
    if (stock !== null) {
      this.stockFilter = stock;
    }
    if (lifecycle !== null) {
      this.lifecycleFilter = lifecycle;
    }
    this.page = 1;
    this.scrollToTableIfDrilldown();
  }

  private scrollToTableIfDrilldown(): void {
    if (!this.route.snapshot.queryParamMap.get('drilldown')) return;
    setTimeout(() => {
      document.querySelector('.data-table')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    }, 120);
  }
}
