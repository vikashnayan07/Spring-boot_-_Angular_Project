import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { AdminLayoutComponent } from '../../layouts/admin-layout/admin-layout.component';
import { AdminDashboardComponent } from './admin-dashboard/admin-dashboard.component';
import { AddEmployeeComponent } from './add-employee/add-employee.component';
import { ManageMachinesComponent } from './manage-machines/manage-machines.component';
import { AdminErrorLogComponent } from './admin-error-log/admin-error-log.component';
import { AdminMaintenanceComponent } from './admin-maintenance/admin-maintenance.component';
import { MaintenanceLandingComponent } from '../maintenance/maintenance-landing/maintenance-landing.component';
import { MaintenanceScheduleComponent } from '../maintenance/maintenance-schedule/maintenance-schedule.component';
import { MaintenanceHistoryComponent } from '../maintenance/maintenance-history/maintenance-history.component';
import { MaintenanceAlertComponent } from '../maintenance/maintenance-alert/maintenance-alert.component';
import { FaultLogComponent } from '../fault-log/fault-log/fault-log.component';
import { FaultAnalysisComponent } from '../fault-analysis/fault-analysis/fault-analysis.component';
import { FaultAnalysis2Component } from '../fault-analysis/fault-analysis2/fault-analysis2.component';
import { ImportFaultLogsComponent } from './import-fault-logs/import-fault-logs.component';
import { InventoryComponent } from '../inventory/inventory/inventory.component';
import { SystemActivityCenterComponent } from './system-activity-center/system-activity-center.component';

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [1] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent },
      { path: 'employees', component: AddEmployeeComponent },
      { path: 'machines', component: ManageMachinesComponent },
      { path: 'error-log', component: AdminErrorLogComponent },
      { path: 'fault-logs', component: FaultLogComponent },
      { path: 'import-fault-logs', component: ImportFaultLogsComponent },
      { path: 'fault-analysis', component: FaultAnalysisComponent },
      { path: 'fault-analysis2/:faultId', component: FaultAnalysis2Component },
      { path: 'activity-center', component: SystemActivityCenterComponent },
      { path: 'maintenance', component: MaintenanceLandingComponent },
      { path: 'inventory', component: InventoryComponent },
      { path: 'alerts', component: MaintenanceAlertComponent },
      { path: 'maintenance-schedule', component: MaintenanceScheduleComponent },
      { path: 'maintenance-history', component: MaintenanceHistoryComponent },
      { path: 'maintenance-request', component: AdminMaintenanceComponent },
    ],
  },
];
