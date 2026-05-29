import { Component, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { KpiCardComponent } from '../../../shared/components/kpi-card/kpi-card.component';
import { FilterPanelComponent } from '../components/filter-panel/filter-panel.component';
import { ChartsComponent } from '../components/charts/charts.component';
import { FilterTableComponent } from '../components/filter-table/filter-table.component';
import { FaultService } from '../../../core/services/fault.services';

@Component({
  selector: 'app-fault-log',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    KpiCardComponent,
    FilterPanelComponent,
    ChartsComponent,
    FilterTableComponent,
  ],
  templateUrl: './fault-log.component.html',
  styleUrls: ['./fault-log.component.css'],
})
export class FaultLogComponent implements OnInit {
  @ViewChild(FilterTableComponent) tableComponent?: FilterTableComponent;

  faults: any[] = [];
  filteredFaults: any[] = [];
  isLoading = true;
  searchQuery = '';

  filters = {
    severity: '',
    machine: '',
    startDate: '',
    endDate: '',
  };

  kpis = {
    totalFaults: '0',
    highAlerts: '0',
    todaysFaults: '0',
    criticalAlerts: '0',
  };

  get localStorageRoleId(): number {
    return Number(localStorage.getItem('roleId'));
  }

  constructor(private faultService: FaultService) {}

  ngOnInit(): void {
    this.loadFaults();
  }

  loadFaults(): void {
    this.isLoading = true;
    this.faultService.getFaults().subscribe({
      next: (response) => {
        this.faults = this.sortFaults(response.data || response || []);
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Fault log data error:', error);
        this.faults = [];
        this.filteredFaults = [];
        this.computeKpis();
        this.isLoading = false;
      },
    });
  }

  onFiltersChanged(filters: {
    severity?: string;
    machine?: string;
    startDate?: string;
    endDate?: string;
  }): void {
    this.filters = {
      severity: filters.severity || '',
      machine: filters.machine || '',
      startDate: filters.startDate || '',
      endDate: filters.endDate || '',
    };
    this.applyFilters();
  }

  onFiltersReset(): void {
    this.filters = {
      severity: '',
      machine: '',
      startDate: '',
      endDate: '',
    };
    this.applyFilters();
  }

  onSearchChange(event: Event): void {
    const target = event.target as HTMLInputElement;
    this.searchQuery = target.value;
  }

  exportCSV(): void {
    this.tableComponent?.exportCSV();
  }

  private applyFilters(): void {
    const { severity, machine, startDate, endDate } = this.filters;
    this.filteredFaults = this.sortFaults(this.faults.filter((fault) => {
      const severityMatch = severity
        ? (fault.severity || '').toLowerCase() === severity.toLowerCase()
        : true;
      const machineMatch = machine ? (fault.machineId || '') === machine : true;

      const faultDate = this.normalizeDate(fault.faultDate);
      const startMatch = startDate
        ? faultDate >= this.normalizeDate(startDate)
        : true;
      const endMatch = endDate
        ? faultDate <= this.normalizeDate(endDate)
        : true;

      return severityMatch && machineMatch && startMatch && endMatch;
    }));

    this.computeKpis();
  }

  private computeKpis(): void {
    const totalFaults = this.filteredFaults.length;
    const todayKey = this.normalizeDate(new Date());
    const highAlerts = this.filteredFaults.filter((fault) => {
      return (fault.severity || '').toLowerCase() === 'high';
    }).length;
    const todaysFaults = this.filteredFaults.filter((fault) => {
      return this.normalizeDate(fault.faultDate) === todayKey;
    }).length;
    const criticalAlerts = this.filteredFaults.filter((fault) => {
      return (fault.severity || '').toLowerCase() === 'critical';
    }).length;

    this.kpis = {
      totalFaults: totalFaults.toString(),
      highAlerts: highAlerts.toString(),
      todaysFaults: todaysFaults.toString(),
      criticalAlerts: criticalAlerts.toString(),
    };
  }

  private normalizeDate(dateValue: string | Date | undefined): string {
    if (!dateValue) {
      return '';
    }
    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
      return '';
    }
    return date.toISOString().split('T')[0];
  }

  private sortFaults(faults: any[]): any[] {
    const source = Array.isArray(faults) ? faults : [];
    return [...source].sort((a, b) => {
      const idDiff = this.faultIdNumber(b?.faultId) - this.faultIdNumber(a?.faultId);
      if (idDiff !== 0) {
        return idDiff;
      }

      const dateA = new Date(`${a?.faultDate || ''}T${a?.faultTime || '00:00:00'}`).getTime();
      const dateB = new Date(`${b?.faultDate || ''}T${b?.faultTime || '00:00:00'}`).getTime();
      return (Number.isNaN(dateB) ? 0 : dateB) - (Number.isNaN(dateA) ? 0 : dateA);
    });
  }

  private faultIdNumber(faultId: string | undefined): number {
    const match = String(faultId || '').match(/\d+/);
    return match ? Number(match[0]) : 0;
  }
}
