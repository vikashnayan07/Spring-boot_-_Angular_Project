import { Routes } from '@angular/router';
import { authGuard } from '../../core/guards/auth.guard';
import { roleGuard } from '../../core/guards/role.guard';
import { OperatorLayoutComponent } from '../../layouts/operator-layout/operator-layout.component';
import { OperatorDashboardComponent } from './operator-dashboard/operator-dashboard.component';
import { AddFaultComponent } from '../fault-log/add-fault/add-fault.component';
import { FaultLogComponent } from '../fault-log/fault-log/fault-log.component';

export const operatorRoutes: Routes = [
  {
    path: '',
    component: OperatorLayoutComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: [3] },
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: OperatorDashboardComponent },
      { path: 'log-error', component: AddFaultComponent },
      { path: 'machines', component: FaultLogComponent }
    ]
  }
];
