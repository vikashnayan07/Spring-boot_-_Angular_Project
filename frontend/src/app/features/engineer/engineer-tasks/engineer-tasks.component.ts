import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, finalize } from 'rxjs/operators';
import { EngineerService } from '../../../core/services/engineer.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { InventoryService } from '../../../core/services/inventory.service';
import { InventoryItem } from '../../inventory/inventory.model';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

type TaskAction = 'start' | 'progress' | 'complete' | 'parts';

@Component({
  selector: 'app-engineer-tasks',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './engineer-tasks.component.html',
  styleUrls: ['./engineer-tasks.component.css'],
})
export class EngineerTasksComponent implements OnInit {
  tasks: any[] = [];
  parts: InventoryItem[] = [];
  loading = true;
  submitting = false;
  selectedTask: any | null = null;
  action: TaskAction = 'start';
  repairNotes = '';
  partRows = [{ partId: null as number | null, quantity: 1 }];
  allocationResult: { allocated: any[]; unavailable: any[] } | null = null;
  backRoute = '/engineer/dashboard';
  backLabel = 'Back to Dashboard';
  confirmVisible = false;
  page = 1;
  rowsPerPage = 6;
  sortKey: 'scheduleId' | 'machineId' | 'status' | 'scheduleDate' = 'scheduleId';
  sortDirection: 'asc' | 'desc' = 'desc';

  constructor(
    private engineerService: EngineerService,
    private toastService: ToastService,
    private inventoryService: InventoryService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.backRoute = '/engineer/dashboard';
    this.backLabel = 'Back to Dashboard';
    this.loadMyTasks();
  }

  goBack(): void {
    this.router.navigateByUrl(this.backRoute);
  }

  get visibleTasks(): any[] {
    const filtered = this.tasks.filter((task) => task.status !== 'Completed');
    return this.sortTasks(filtered);
  }

  get paginatedTasks(): any[] {
    const start = (this.page - 1) * this.rowsPerPage;
    return this.visibleTasks.slice(start, start + this.rowsPerPage);
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.visibleTasks.length / this.rowsPerPage));
  }

  pages(): PaginationItem[] {
    return buildPagination(this.page, this.totalPages);
  }

  changePage(page: number): void {
    this.page = Math.min(Math.max(page, 1), this.totalPages);
  }

  changePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.changePage(item);
  }

  sortBy(key: 'scheduleId' | 'machineId' | 'status' | 'scheduleDate'): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
  }

  loadMyTasks(): void {
    this.loading = true;

    forkJoin({
      tasks: this.engineerService
        .getMyTasks()
        .pipe(catchError(() => of({ data: [] }))),
      parts: this.inventoryService.getAllParts().pipe(catchError(() => of([]))),
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ tasks, parts }: any) => {
          this.tasks = tasks.data || tasks.tasks || [];
          this.page = 1;
          this.parts = Array.isArray(parts) ? parts : [];
        },
        error: () => {
          this.toastService.error(
            'Tasks unavailable',
            'Assigned maintenance tasks could not be loaded.',
          );
        },
      });
  }

  openTaskModal(task: any, action: TaskAction): void {
    if (action === 'parts' && task.status === 'Completed') {
      this.toastService.warning(
        'Task already completed',
        'This maintenance task is already completed. Spare parts can no longer be requested for closed tasks.',
      );
      return;
    }

    this.selectedTask = task;
    this.action = action;
    this.repairNotes = this.defaultNotes(action);
    this.partRows = [{ partId: null, quantity: 1 }];
    this.allocationResult = null;
  }

  closeTaskModal(): void {
    if (this.submitting) return;
    this.selectedTask = null;
    this.allocationResult = null;
    this.confirmVisible = false;
  }

  addPartRow(): void {
    this.partRows.push({ partId: null, quantity: 1 });
  }

  removePartRow(index: number): void {
    if (this.partRows.length === 1) return;
    this.partRows.splice(index, 1);
  }

  submitTaskAction(): void {
    if (!this.selectedTask || this.submitting) return;

    if (this.action === 'parts' || this.action === 'complete') {
      this.confirmVisible = true;
      return;
    }

    this.executeTaskAction();
  }

  confirmTaskAction(): void {
    this.confirmVisible = false;
    if (this.action === 'parts') {
      this.submitPartsRequest();
      return;
    }
    this.executeTaskAction();
  }

  cancelConfirm(): void {
    this.confirmVisible = false;
  }

  private executeTaskAction(): void {
    if (!this.selectedTask || this.submitting) return;

    const nextStatus = this.nextStatusForAction();
    if (!nextStatus) {
      this.toastService.warning(
        'Task already completed',
        'Completed work orders cannot be updated again.',
      );
      return;
    }

    this.submitting = true;
    this.engineerService
      .updateStatus(
        this.selectedTask.scheduleId,
        nextStatus,
        this.repairNotes || this.defaultNotes(this.action),
      )
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: () => {
          this.toastService.success(
            'Task updated',
            `Work order moved to ${nextStatus}.`,
          );
          if (nextStatus === 'Completed') {
            this.tasks = this.tasks.filter(
              (task) => task.scheduleId !== this.selectedTask?.scheduleId,
            );
          }
          this.closeTaskModal();
          this.loadMyTasks();
        },
        error: (error) => {
          this.toastService.error(
            'Update failed',
            error?.error?.message || 'Unable to update this work order.',
          );
        },
      });
  }

  statusClass(status: string): string {
    const normalized = `${status || ''}`.toLowerCase();
    if (normalized === 'blocked')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (normalized === 'completed')
      return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
    if (normalized === 'in_progress')
      return 'bg-cyan-400/15 text-cyan-300 border-cyan-400/20';
    return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
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

  partLabel(partId: number | null): string {
    const part = this.parts.find(
      (item) => Number(item.partId) === Number(partId),
    );
    return part ? part.partName : `Part ${partId}`;
  }

  private sortTasks(list: any[]): any[] {
    const direction = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.sortValue(a);
      const right = this.sortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private sortValue(task: any): string | number {
    switch (this.sortKey) {
      case 'machineId':
        return `${task?.machineId || ''}`.toLowerCase();
      case 'status':
        return `${task?.status || ''}`.toLowerCase();
      case 'scheduleDate':
        return `${task?.scheduleDate || ''}`.toLowerCase();
      default:
        return Number(task?.scheduleId || 0);
    }
  }

  private submitPartsRequest(): void {
    if (this.selectedTask?.status === 'Completed') {
      this.toastService.warning(
        'Task already completed',
        'This maintenance task is already completed. Spare parts can no longer be requested for closed tasks.',
      );
      return;
    }

    const parts = this.partRows
      .filter((row) => row.partId && row.quantity > 0)
      .map((row) => {
        const part = this.parts.find(
          (item) => Number(item.partId) === Number(row.partId),
        );
        return {
          partId: Number(row.partId),
          quantity: Number(row.quantity),
          partName: part?.partName || `Part ${row.partId}`,
          available: Number(part?.currentStock || 0),
        };
      });

    if (parts.length === 0) {
      this.toastService.warning(
        'Parts required',
        'Enter at least one valid part ID and quantity.',
      );
      return;
    }

    this.submitting = true;
    this.engineerService
      .requestParts(this.selectedTask.scheduleId, parts)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (response: any) => {
          const allocated = Array.isArray(response?.allocated)
            ? response.allocated
            : [];
          const unavailable = Array.isArray(response?.unavailable)
            ? response.unavailable
            : [];

          this.allocationResult = { allocated, unavailable };

          if (allocated.length) {
            this.toastService.success(
              'Parts allocation completed',
              response?.message ||
                `${allocated.length} part request(s) allocated successfully.`,
            );
          }

          if (unavailable.length) {
            const firstShortage = unavailable[0];
            this.toastService.warning(
              'Reorder recommended',
              `${firstShortage.partName} unavailable. Reorder escalation sent to inventory.`,
            );
          }

          this.loadMyTasks();
        },
        error: (error) => {
          this.toastService.error(
            'Parts request failed',
            error?.error?.message || 'Unable to process spare part request.',
          );
        },
      });
  }

  private nextStatusForAction(): string {
    if (`${this.selectedTask?.status}` === 'Completed') return '';
    if (this.action === 'complete') return 'Completed';
    return 'In_progress';
  }

  private defaultNotes(action: TaskAction): string {
    if (action === 'start') return 'Maintenance work started.';
    if (action === 'complete') return 'Maintenance work completed.';
    if (action === 'parts')
      return 'Parts requested for maintenance work order.';
    return 'Maintenance progress updated.';
  }
}
