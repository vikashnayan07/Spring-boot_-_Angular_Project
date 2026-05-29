import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin-dashboard.component.html',
})
export class AdminDashboardComponent implements OnInit {
  isLoading = true;

  // 👉 Real-time stats object
  stats = {
    totalEmployees: 0,
    totalMachines: 0,
    operationalMachines: 0,
    faultyMachines: 0,
    maintenanceMachines: 0,
    machinesServiceOverdue: 0,
    machinesOutOfWarranty: 0,
    machinesMaintenanceDue: 0,
    machinesAtRisk: 0,
    partsExpired: 0,
    partsExpiringSoon: 0,
    partsOutOfWarranty: 0,
    partsEndOfLife: 0,
    partsOutOfStock: 0,
    partsReorder: 0,
  };

  adminName = '';

  activeLifecycleDrilldown = '';

  constructor(
    private api: ApiService,
    private router: Router,
  ) {}

  ngOnInit() {
    this.adminName = localStorage.getItem('name') || 'Admin';
    this.loadRealTimeData();
  }

  // 👉 Fetches data from existing APIs and calculates the KPIs
  loadRealTimeData() {
    this.isLoading = true;
    let requestsCompleted = 0;

    const checkDone = () => {
      requestsCompleted++;
      if (requestsCompleted === 3) this.isLoading = false;
    };

    // 1. Fetch Employees API
    this.api.getAllEmployees().subscribe({
      next: (res: any) => {
        // Handles both raw arrays and wrapped { data: [...] } responses safely
        const empList = res.data ? res.data : Array.isArray(res) ? res : [];
        this.stats.totalEmployees = empList.length;
        checkDone();
      },
      error: () => checkDone(),
    });

    // 2. Fetch Machines API
    this.api.getAllMachines().subscribe({
      next: (res: any) => {
        const machList = res.data ? res.data : Array.isArray(res) ? res : [];
        this.stats.totalMachines = machList.length;

        // Calculate specific statuses
        this.stats.operationalMachines = machList.filter((m: any) =>
          ['HEALTHY', 'MONITOR'].includes(m.healthStatus),
        ).length;
        this.stats.maintenanceMachines = machList.filter(
          (m: any) => m.healthStatus === 'MAINTENANCE_DUE',
        ).length;
        this.stats.faultyMachines = machList.filter(
          (m: any) => m.healthStatus === 'AT_RISK' || m.status === 'Stopped',
        ).length;

        checkDone();
      },
      error: () => checkDone(),
    });

    // 3. Fetch Lifecycle Alerts
    this.api.getAdminDashboard().subscribe({
      next: (res: any) => {
        const data = res?.dashboardData || {};
        this.stats.machinesServiceOverdue = data.machinesServiceOverdue || 0;
        this.stats.machinesOutOfWarranty = data.machinesOutOfWarranty || 0;
        this.stats.machinesMaintenanceDue = data.machinesMaintenanceDue || 0;
        this.stats.machinesAtRisk = data.machinesAtRisk || 0;
        this.stats.partsExpired = data.partsExpired || 0;
        this.stats.partsExpiringSoon = data.partsExpiringSoon || 0;
        this.stats.partsOutOfWarranty = data.partsOutOfWarranty || 0;
        this.stats.partsEndOfLife = data.partsEndOfLife || 0;
        this.stats.partsOutOfStock = data.partsOutOfStock || 0;
        this.stats.partsReorder = data.partsReorder || 0;
        checkDone();
      },
      error: () => checkDone(),
    });
  }

  openLifecycleDrilldown(kind: string): void {
    this.activeLifecycleDrilldown = kind;

    const drilldowns: Record<string, { route: string; queryParams: any }> = {
      serviceOverdue: {
        route: '/admin/machines',
        queryParams: { lifecycle: 'SERVICE_OVERDUE', drilldown: 'serviceOverdue' },
      },
      warrantyExpired: {
        route: '/admin/machines',
        queryParams: { lifecycle: 'WARRANTY_EXPIRED', drilldown: 'warrantyExpired' },
      },
      maintenanceDue: {
        route: '/admin/machines',
        queryParams: { health: 'MAINTENANCE_DUE', drilldown: 'maintenanceDue' },
      },
      machineAtRisk: {
        route: '/admin/machines',
        queryParams: { health: 'AT_RISK', drilldown: 'machineAtRisk' },
      },
      partsExpiringSoon: {
        route: '/admin/inventory',
        queryParams: { lifecycle: 'EXPIRING_SOON', drilldown: 'partsExpiringSoon' },
      },
      partsExpired: {
        route: '/admin/inventory',
        queryParams: { lifecycle: 'EXPIRED', drilldown: 'partsExpired' },
      },
      partsOutOfStock: {
        route: '/admin/inventory',
        queryParams: { stock: 'Out', drilldown: 'partsOutOfStock' },
      },
      partsReorder: {
        route: '/admin/inventory',
        queryParams: { stock: 'Reorder', drilldown: 'partsReorder' },
      },
    };

    const target = drilldowns[kind];
    if (!target) return;
    this.router.navigate([target.route], { queryParams: target.queryParams });
  }
}
