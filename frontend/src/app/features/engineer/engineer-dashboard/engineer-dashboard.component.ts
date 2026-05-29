import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { EngineerService } from '../../../core/services/engineer.service';

@Component({
  selector: 'app-engineer-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './engineer-dashboard.component.html',
})
export class EngineerDashboardComponent implements OnInit {
  isLoading = true;
  engineerName = 'Engineer';
  tasks: any[] = [];
  stats = {
    activeTasks: 0,
    completedTasks: 0,
    overdueTasks: 0,
    inProgressTasks: 0,
    machineWarrantyExpiring: 0,
    machineWarrantyExpired: 0,
    partsExpiringSoon: 0,
    partsExpired: 0,
    replacementRequired: 0,
  };

  constructor(private engineerService: EngineerService) {}

  ngOnInit(): void {
    this.engineerName = localStorage.getItem('name') || 'Engineer';
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.isLoading = true;

    forkJoin({
      dashboard: this.engineerService
        .getDashboardStats()
        .pipe(catchError(() => of(null))),
      tasks: this.engineerService
        .getMyTasks()
        .pipe(catchError(() => of({ data: [] }))),
    })
      .pipe(finalize(() => (this.isLoading = false)))
      .subscribe(({ dashboard, tasks }: any) => {
        this.tasks = tasks?.data || tasks?.tasks || [];
        const dashboardData = dashboard?.dashboardData || {};

        this.stats.activeTasks =
          this.tasks.filter((task) => task.status !== 'Completed').length ||
          dashboardData.pendingTasks ||
          0;
        this.stats.completedTasks =
          this.tasks.filter((task) => task.status === 'Completed').length ||
          dashboardData.completedTasks ||
          0;
        this.stats.inProgressTasks = this.tasks.filter(
          (task) => task.status === 'In_progress',
        ).length;
        this.stats.overdueTasks = this.tasks.filter((task) =>
          this.isOverdue(task),
        ).length;
        this.stats.machineWarrantyExpiring =
          dashboardData.machineWarrantyExpiring || 0;
        this.stats.machineWarrantyExpired =
          dashboardData.machineWarrantyExpired || 0;
        this.stats.partsExpiringSoon = dashboardData.partsExpiringSoon || 0;
        this.stats.partsExpired = dashboardData.partsExpired || 0;
        this.stats.replacementRequired = dashboardData.replacementRequired || 0;
      });
  }

  get recentActivity(): any[] {
    return [...this.tasks].slice(0, 5);
  }

  get completionRate(): number {
    const total = this.stats.activeTasks + this.stats.completedTasks;
    return total === 0
      ? 0
      : Math.round((this.stats.completedTasks / total) * 100);
  }

  private isOverdue(task: any): boolean {
    if (!task.scheduleDate || task.status === 'Completed') return false;
    const scheduled = new Date(task.scheduleDate);
    const today = new Date();
    scheduled.setHours(0, 0, 0, 0);
    today.setHours(0, 0, 0, 0);
    return scheduled < today;
  }

  lifecycleBadgeClass(status: string | undefined | null): string {
    const normalized = `${status || ''}`.toUpperCase();
    if (normalized === 'EXPIRING_SOON')
      return 'bg-orange-400/15 text-orange-300 border-orange-400/20';
    if (normalized === 'EXPIRED' || normalized === 'SERVICE_OVERDUE')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (normalized === 'UNREGISTERED_ASSET')
      return 'bg-yellow-400/15 text-yellow-200 border-yellow-300/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
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
}
