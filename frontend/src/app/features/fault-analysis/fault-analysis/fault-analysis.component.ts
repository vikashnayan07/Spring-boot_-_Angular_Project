import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AnalysisService } from '../../../core/services/analysis.service';
import { EngineerService } from '../../../core/services/engineer.service';
import { FaultService } from '../../../core/services/fault.services';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

@Component({
  selector: 'app-fault-analysis',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fault-analysis.component.html',
  styleUrls: ['./fault-analysis.component.css'],
})
export class FaultAnalysisComponent implements OnInit {
  activeTab: 'pending' | 'completed' = 'pending';
  pendingFaults: any[] = [];
  completedAnalyses: any[] = [];
  alerts: any[] = [];

  isLoading = false;
  processingFaultId = '';
  loadError = '';
  searchTerm = '';
  selectedResult: any = null;
  pendingPage = 1;
  completedPage = 1;
  rowsPerPage = 10;
  pendingSortKey: 'faultId' | 'machineId' | 'severity' | 'priorityLevel' | 'faultDate' = 'faultId';
  pendingSortDirection: 'asc' | 'desc' = 'desc';
  completedSortKey: 'analysisId' | 'faultId' | 'healthScore' | 'healthStatus' | 'failureTrend' = 'analysisId';
  completedSortDirection: 'asc' | 'desc' = 'desc';
  pageSizes = [10, 20, 50];
  backRoute = '/admin/maintenance';
  backLabel = 'Back to Maintenance';

  constructor(
    private router: Router,
    private engineerService: EngineerService,
    private analysisService: AnalysisService,
    private faultService: FaultService,
  ) {}

  ngOnInit(): void {
    this.configureBackNavigation();
    this.loadWorkspace();
  }

  goBack(): void {
    this.router.navigateByUrl(this.backRoute);
  }

  get filteredPending(): any[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.pendingFaults.filter(
      (fault) =>
        !term ||
        `${fault.faultId || ''}`.toLowerCase().includes(term) ||
        `${fault.machineId || ''}`.toLowerCase().includes(term) ||
        `${fault.severity || ''}`.toLowerCase().includes(term),
    );
    return this.sortPending(filtered);
  }

  get filteredCompleted(): any[] {
    const term = this.searchTerm.toLowerCase().trim();
    const filtered = this.completedAnalyses.filter(
      (analysis) =>
        !term ||
        `${analysis.analysisId || ''}`.toLowerCase().includes(term) ||
        `${analysis.faultId || ''}`.toLowerCase().includes(term) ||
        `${analysis.healthStatus || ''}`.toLowerCase().includes(term),
    );
    return this.sortCompleted(filtered);
  }

  get criticalAlerts(): number {
    return this.alerts.filter(
      (alert) => alert.alertPriority === 'Critical Alert',
    ).length;
  }

  get criticalPriorityFaults(): number {
    return this.pendingFaults.filter(
      (fault) => `${fault.priorityLevel || ''}`.toUpperCase() === 'P1',
    ).length;
  }

  get averageHealth(): number {
    if (!this.completedAnalyses.length) return 0;
    const total = this.completedAnalyses.reduce(
      (sum, item) => sum + Number(item.healthScore || 0),
      0,
    );
    return Math.round(total / this.completedAnalyses.length);
  }

  loadWorkspace(): void {
    this.isLoading = true;
    this.loadError = '';

    const roleId = Number(localStorage.getItem('roleId'));
    if (roleId !== 2) {
      this.backRoute = '/admin/maintenance';
      this.backLabel = 'Back to Maintenance';
      this.loadAdminWorkspaceFallback();
      return;
    }

    this.backRoute = '/engineer/tasks';
    this.backLabel = 'Back to Tasks';

    forkJoin({
      pending: this.engineerService.getPendingFaultQueue(),
      completed: this.analysisService.getAllAnalysis(),
      alerts: this.engineerService.getAlerts(),
    }).subscribe({
      next: ({ pending, completed, alerts }: any) => {
        this.pendingFaults = this.sortFaultsLatestFirst(pending?.data || []);
        this.completedAnalyses = this.sortAnalysesLatestFirst(completed?.data || completed || []);
        this.alerts = alerts?.data || [];
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError =
          err.error?.message ||
          'Unable to load analysis workspace. Engineer access may be required.';
      },
    });
  }

  get paginatedPending(): any[] {
    return this.paginate(this.filteredPending, this.pendingPage);
  }

  get paginatedCompleted(): any[] {
    return this.paginate(this.filteredCompleted, this.completedPage);
  }

  get pendingTotalPages(): number {
    return Math.ceil(this.filteredPending.length / this.rowsPerPage) || 1;
  }

  get completedTotalPages(): number {
    return Math.ceil(this.filteredCompleted.length / this.rowsPerPage) || 1;
  }

  showingStart(total: number, page: number): number {
    return total === 0 ? 0 : (page - 1) * this.rowsPerPage + 1;
  }

  showingEnd(total: number, page: number): number {
    return Math.min(page * this.rowsPerPage, total);
  }

  pages(totalPages: number, currentPage: number): PaginationItem[] {
    return buildPagination(currentPage, totalPages);
  }

  changePageFromItem(
    kind: 'pending' | 'completed',
    item: PaginationItem,
  ): void {
    if (item === '...') return;
    this.changePage(kind, item);
  }

  sortPendingBy(
    key: 'faultId' | 'machineId' | 'severity' | 'priorityLevel' | 'faultDate',
  ): void {
    if (this.pendingSortKey === key) {
      this.pendingSortDirection = this.pendingSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.pendingSortKey = key;
      this.pendingSortDirection = 'asc';
    }
  }

  sortCompletedBy(
    key: 'analysisId' | 'faultId' | 'healthScore' | 'healthStatus' | 'failureTrend',
  ): void {
    if (this.completedSortKey === key) {
      this.completedSortDirection = this.completedSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.completedSortKey = key;
      this.completedSortDirection = 'asc';
    }
  }

  changeRowsPerPage(value: string): void {
    this.rowsPerPage = Number(value);
    this.pendingPage = 1;
    this.completedPage = 1;
  }

  changePage(kind: 'pending' | 'completed', page: number): void {
    const total =
      kind === 'pending' ? this.pendingTotalPages : this.completedTotalPages;
    if (page < 1 || page > total) return;
    if (kind === 'pending') this.pendingPage = page;
    else this.completedPage = page;
  }

  private loadAdminWorkspaceFallback(): void {
    forkJoin({
      faults: this.faultService.getFaults(),
      completed: this.analysisService.getAllAnalysis(),
      alerts: this.engineerService.getAlerts(),
    }).subscribe({
      next: ({ faults, completed, alerts }: any) => {
        const completedData = completed?.data || completed || [];
        const completedFaultIds = new Set(
          completedData.map((item: any) => item.faultId),
        );
        const faultData = faults?.data || faults || [];

        this.completedAnalyses = this.sortAnalysesLatestFirst(completedData);
        this.pendingFaults = faultData
          .filter((fault: any) => !completedFaultIds.has(fault.faultId))
          .map((fault: any) => ({
            ...fault,
            priorityLevel: fault.priorityLevel || 'P3',
            priorityScore: fault.priorityScore || 0,
            productionImpactScore: fault.productionImpactScore || 0,
          }))
          .sort((a: any, b: any) => this.faultIdNumber(b?.faultId) - this.faultIdNumber(a?.faultId));
        this.alerts = alerts?.data || [];
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.loadError =
          err.error?.message || 'Unable to load analysis workspace.';
      },
    });
  }

  analyzeFault(fault: any): void {
    if (!fault?.faultId || this.processingFaultId) return;

    this.processingFaultId = fault.faultId;
    this.selectedResult = null;

    this.engineerService.analyzeFault(fault.faultId).subscribe({
      next: (res: any) => {
        const analysis = res?.data || res;
        this.selectedResult = analysis;
        localStorage.setItem('selectedAnalysis', JSON.stringify(analysis));
        localStorage.setItem('selectedFault', JSON.stringify(fault));
        localStorage.setItem('analysisCountdown', 'true');
        this.processingFaultId = '';
        this.loadWorkspace();
        setTimeout(
          () => this.router.navigate([this.resultRoute(fault.faultId)]),
          650,
        );
      },
      error: (err) => {
        this.processingFaultId = '';
        this.loadError = err.error?.message || 'Failed to complete analysis.';
        setTimeout(() => (this.loadError = ''), 5000);
      },
    });
  }

  private paginate(items: any[], page: number): any[] {
    const start = (page - 1) * this.rowsPerPage;
    return items.slice(start, start + this.rowsPerPage);
  }

  private sortPending(list: any[]): any[] {
    const direction = this.pendingSortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.pendingSortValue(a);
      const right = this.pendingSortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private pendingSortValue(item: any): string | number {
    switch (this.pendingSortKey) {
      case 'machineId':
        return `${item?.machineId || ''}`.toLowerCase();
      case 'severity':
        return `${item?.severity || ''}`.toLowerCase();
      case 'priorityLevel':
        return `${item?.priorityLevel || ''}`.toLowerCase();
      case 'faultDate':
        return `${item?.faultDate || ''}`.toLowerCase();
      default:
        return `${item?.faultId || ''}`.toLowerCase();
    }
  }

  private sortCompleted(list: any[]): any[] {
    const direction = this.completedSortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.completedSortValue(a);
      const right = this.completedSortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private completedSortValue(item: any): string | number {
    switch (this.completedSortKey) {
      case 'faultId':
        return `${item?.faultId || ''}`.toLowerCase();
      case 'healthScore':
        return Number(item?.healthScore || 0);
      case 'healthStatus':
        return `${item?.healthStatus || ''}`.toLowerCase();
      case 'failureTrend':
        return `${item?.failureTrend || ''}`.toLowerCase();
      default:
        return `${item?.analysisId || ''}`.toLowerCase();
    }
  }

  viewAnalysis(analysis: any): void {
    if (!analysis?.faultId) return;
    localStorage.setItem('selectedAnalysis', JSON.stringify(analysis));
    localStorage.setItem('analysisCountdown', 'false');
    this.router.navigate([this.resultRoute(analysis.faultId)]);
  }

  priorityClass(level: string): string {
    const value = `${level || ''}`.toUpperCase();
    if (value === 'P1')
      return 'border-red-500/30 bg-red-500/10 text-red-300 shadow-[0_0_22px_rgba(239,68,68,.16)]';
    if (value === 'P2')
      return 'border-amber-500/30 bg-amber-500/10 text-amber-300';
    return 'border-blue-500/30 bg-blue-500/10 text-blue-300';
  }

  alertStatus(analysis: any): string {
    return this.alerts.some(
      (alert) =>
        alert.linkedAnalysisId === analysis.analysisId ||
        alert.analysisId === analysis.analysisId,
    )
      ? 'Alert Generated'
      : 'No Alert';
  }

  isEngineerRaised(item: any): boolean {
    return (
      item?.engineerRaised === true ||
      item?.requestSource === 'ENGINEER_RAISED' ||
      Number(item?.reporterRoleId || item?.reportedByRoleId) === 2 ||
      `${item?.reportedByRole || item?.roleName || ''}`.toLowerCase().includes('engineer')
    );
  }

  private resultRoute(faultId: string): string {
    const roleId = Number(localStorage.getItem('roleId'));
    return roleId === 2
      ? `/engineer/analysis/${faultId}`
      : `/admin/fault-analysis2/${faultId}`;
  }

  private configureBackNavigation(): void {
    const roleId = Number(localStorage.getItem('roleId'));
    if (roleId === 2) {
      this.backRoute = '/engineer/tasks';
      this.backLabel = 'Back to Tasks';
      return;
    }

    this.backRoute = '/admin/maintenance';
    this.backLabel = 'Back to Maintenance';
  }

  private sortFaultsLatestFirst(items: any[]): any[] {
    return [...items].sort((a, b) => {
      const dateDiff = this.timestampValue(b) - this.timestampValue(a);
      if (dateDiff) return dateDiff;
      return this.faultIdNumber(b?.faultId) - this.faultIdNumber(a?.faultId);
    });
  }

  private sortAnalysesLatestFirst(items: any[]): any[] {
    return [...items].sort((a, b) => Number(b?.analysisId || 0) - Number(a?.analysisId || 0));
  }

  private timestampValue(item: any): number {
    const raw = `${item?.faultDate || ''} ${item?.faultTime || ''}`.trim();
    const value = Date.parse(raw);
    return Number.isNaN(value) ? 0 : value;
  }

  private faultIdNumber(faultId: string | undefined): number {
    const match = String(faultId || '').match(/\d+/);
    return match ? Number(match[0]) : 0;
  }
}
