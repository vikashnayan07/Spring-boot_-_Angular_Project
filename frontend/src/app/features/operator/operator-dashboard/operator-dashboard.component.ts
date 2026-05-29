import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-operator-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './operator-dashboard.component.html',
})
export class OperatorDashboardComponent implements OnInit {
  isLoading = true;
  operatorName = '';
  stats = {
    totalMachines: 0,
    running: 0,
    faulty: 0,
    faultsRaisedToday: 0,
    criticalMaintenanceRequired: 0,
    machineWarrantyExpiring: 0,
    machineWarrantyExpired: 0,
  };
  machines: any[] = [];

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.operatorName = localStorage.getItem('name') || 'Operator';
    this.loadMachineStats();
  }

  loadMachineStats() {
    this.isLoading = true;
    forkJoin({
      machines: this.api.getAllMachines(),
      dashboard: this.api.getOperatorDashboard(),
    }).subscribe({
      next: ({ machines, dashboard }: any) => {
        const machList = machines.data || [];
        this.machines = machList;
        const dashboardData =
          dashboard?.dashboardData || dashboard?.data || dashboard || {};
        this.stats.totalMachines = machList.length;
        this.stats.running = machList.filter(
          (m: any) => m.status === 'Running',
        ).length;
        this.stats.faulty = machList.filter(
          (m: any) => m.status === 'Faulty' || m.status === 'Stopped',
        ).length;
        this.stats.faultsRaisedToday = Number(
          dashboardData.faultsRaisedToday || 0,
        );
        this.stats.criticalMaintenanceRequired = machList.filter((m: any) =>
          this.isCriticalMaintenance(m),
        ).length;
        this.stats.machineWarrantyExpiring = machList.filter(
          (m: any) =>
            `${m.warrantyStatus || ''}`.toUpperCase() === 'EXPIRING_SOON',
        ).length;
        this.stats.machineWarrantyExpired = machList.filter(
          (m: any) => `${m.warrantyStatus || ''}`.toUpperCase() === 'EXPIRED',
        ).length;
        this.isLoading = false;
      },
      error: () => (this.isLoading = false),
    });
  }

  healthBadgeClass(status: string | undefined | null): string {
    const normalized = `${status || ''}`.toUpperCase();
    if (normalized === 'MONITOR')
      return 'bg-yellow-400/15 text-yellow-200 border-yellow-300/20';
    if (
      normalized === 'AT_RISK' ||
      normalized === 'MAINTENANCE_DUE' ||
      normalized === 'OFFLINE'
    ) {
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    }
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  warrantyBadgeClass(status: string | undefined | null): string {
    const normalized = `${status || ''}`.toUpperCase();
    if (normalized === 'EXPIRING_SOON')
      return 'bg-orange-400/15 text-orange-300 border-orange-400/20';
    if (normalized === 'EXPIRED')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  criticalBadgeClass(machine: any): string {
    return this.isCriticalMaintenance(machine)
      ? 'bg-red-400/15 text-red-300 border-red-400/20'
      : 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  isCriticalMaintenance(machine: any): boolean {
    const health = `${machine?.healthStatus || ''}`.toUpperCase();
    const lifecycle = `${machine?.lifecycleStatus || ''}`.toUpperCase();
    return (
      health === 'MAINTENANCE_DUE' ||
      health === 'OFFLINE' ||
      lifecycle === 'SERVICE_OVERDUE'
    );
  }
}
