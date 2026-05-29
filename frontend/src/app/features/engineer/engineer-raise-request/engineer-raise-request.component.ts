import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { EngineerService } from '../../../core/services/engineer.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

type RaiseMode = 'fault' | 'support';

@Component({
  selector: 'app-engineer-raise-request',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './engineer-raise-request.component.html',
})
export class EngineerRaiseRequestComponent implements OnInit {
  tasks: any[] = [];
  loading = true;
  submitting = false;
  contextLoading = false;
  selectedTask: any | null = null;
  context: any | null = null;
  mode: RaiseMode = 'fault';
  confirmVisible = false;

  faultForm = {
    faultType: '',
    severity: 'Medium',
    remark: '',
  };

  supportForm = {
    reason: '',
    requiredEngineerCount: 1,
    urgency: 'Medium',
  };

  constructor(
    private engineerService: EngineerService,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.loadTasks();
  }

  get startedTasks(): any[] {
    return this.tasks.filter((task) => `${task.status}` === 'In_progress');
  }

  loadTasks(): void {
    this.loading = true;
    this.engineerService
      .getMyTasks()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (response: any) => {
          this.tasks = response?.data || response?.tasks || [];
        },
        error: () => {
          this.toastService.error(
            'Raise Request unavailable',
            'Started maintenance tasks could not be loaded.',
          );
        },
      });
  }

  openModal(task: any, mode: RaiseMode): void {
    this.selectedTask = task;
    this.mode = mode;
    this.context = null;
    this.contextLoading = true;
    this.resetForms();

    this.engineerService
      .getRaiseRequestContext(task.scheduleId)
      .pipe(finalize(() => (this.contextLoading = false)))
      .subscribe({
        next: (response: any) => {
          this.context = response?.data || response;
          const firstType = this.context?.faultTypes?.[0] || 'Other';
          this.faultForm.faultType = firstType;
        },
        error: (error) => {
          this.toastService.error(
            'Context unavailable',
            error?.error?.message ||
              'Raise request actions are available only after Start Work.',
          );
          this.closeModal();
        },
      });
  }

  closeModal(): void {
    if (this.submitting) return;
    this.selectedTask = null;
    this.context = null;
  }

  submit(): void {
    if (!this.selectedTask || this.submitting) return;
    this.confirmVisible = true;
  }

  confirmSubmit(): void {
    this.confirmVisible = false;
    if (this.mode === 'fault') {
      this.submitFault();
    } else {
      this.submitSupport();
    }
  }

  cancelConfirm(): void {
    this.confirmVisible = false;
  }

  private submitFault(): void {
    if (!this.faultForm.faultType) {
      this.toastService.warning(
        'Fault type required',
        'Select a machine fault type.',
      );
      return;
    }

    if (this.faultForm.faultType === 'Other' && !this.faultForm.remark.trim()) {
      this.toastService.warning(
        'Remark required',
        'Describe the observed fault when selecting Other.',
      );
      return;
    }

    this.submitting = true;
    this.engineerService
      .raiseFaultFromTask(this.selectedTask.scheduleId, this.faultForm)
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (response: any) => {
          this.toastService.success(
            'Fault raised',
            response?.message || 'Fault sent to analysis queue.',
          );
          this.closeModal();
        },
        error: (error) => {
          this.toastService.error(
            'Fault raise failed',
            error?.error?.message || 'Unable to raise this fault.',
          );
        },
      });
  }

  private submitSupport(): void {
    const count = Number(this.supportForm.requiredEngineerCount);
    if (!this.supportForm.reason.trim()) {
      this.toastService.warning(
        'Reason required',
        'Enter why support is needed.',
      );
      return;
    }
    if (count < 1 || count > 3) {
      this.toastService.warning(
        'Invalid engineer count',
        'Required engineer count must be between 1 and 3.',
      );
      return;
    }

    this.submitting = true;
    this.engineerService
      .requestAdditionalSupport(this.selectedTask.scheduleId, {
        ...this.supportForm,
        requiredEngineerCount: count,
      })
      .pipe(finalize(() => (this.submitting = false)))
      .subscribe({
        next: (response: any) => {
          this.toastService.success(
            'Support escalation created',
            response?.message || 'Admin can now auto assign support alerts.',
          );
          this.closeModal();
        },
        error: (error) => {
          this.toastService.error(
            'Support request failed',
            error?.error?.message || 'Unable to create support escalation.',
          );
        },
      });
  }

  private resetForms(): void {
    this.faultForm = {
      faultType: '',
      severity: 'Medium',
      remark: '',
    };
    this.supportForm = {
      reason: '',
      requiredEngineerCount: 1,
      urgency: 'Medium',
    };
  }
}
