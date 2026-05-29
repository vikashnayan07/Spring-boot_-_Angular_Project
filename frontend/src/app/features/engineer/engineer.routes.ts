import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { EngineerLayoutComponent } from '../../layouts/engineer-layout/engineer-layout.component';
import { EngineerDashboardComponent } from './engineer-dashboard/engineer-dashboard.component';
import { EngineerTasksComponent } from './engineer-tasks/engineer-tasks.component';
import { EngineerFaultsComponent } from './engineer-faults/engineer-faults.component';
import { MaintenanceHistoryComponent } from '../maintenance/maintenance-history/maintenance-history.component';
import { FaultAnalysisComponent } from '../fault-analysis/fault-analysis/fault-analysis.component';
import { FaultAnalysis2Component } from '../fault-analysis/fault-analysis2/fault-analysis2.component';
import { EngineerRaiseRequestComponent } from './engineer-raise-request/engineer-raise-request.component';

export const engineerRoutes: Routes = [
  {
    path: '',
    component: EngineerLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [2] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: EngineerDashboardComponent },
      { path: 'tasks', component: EngineerTasksComponent },
      { path: 'raise-request', component: EngineerRaiseRequestComponent },
      { path: 'history', component: MaintenanceHistoryComponent },
      { path: 'faults', component: EngineerFaultsComponent },
      { path: 'alerts', component: FaultAnalysisComponent },
      { path: 'analysis', component: FaultAnalysisComponent },
      { path: 'analysis/:faultId', component: FaultAnalysis2Component }
    ]
  }
];
