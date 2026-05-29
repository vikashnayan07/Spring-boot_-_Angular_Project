import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

// 👉 1. Using YOUR AlertService
import { AlertService } from '../../../core/services/alert-service'; 
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-maintenance-alert',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink
  ],
  templateUrl: './maintenance-alert.component.html',
  styleUrls: ['./maintenance-alert.component.css']
})
export class MaintenanceAlertComponent implements OnInit {

  alerts: any[] = [];
  filteredAlerts: any[] = [];
  loading = true;
  searchText = '';
  severityFilter = 'All';
  fromDate = '';
  toDate = '';

  // 👉 2. Injecting YOUR AlertService
  constructor(
    private alertService: AlertService,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.loading = true;

    this.alertService.getAllAlerts().subscribe({
      next: (response: any) => {
        let data = response.data || [];

        // 👉 THE FIX: Filter out alerts that are already assigned!
        // (Adjust 'Assigned' to match whatever your backend actually sends)
        data = data.filter((alert: any) => alert.status !== 'Assigned' && alert.status !== 'Resolved');

        // SORT PRIORITY
        this.alerts = data.sort(
          (a: any, b: any) => Number(a.priority) - Number(b.priority)
        );

        this.filteredAlerts = [...this.alerts];
        this.loading = false;
      },
      error: (error) => {
        console.log(error);
        this.loading = false;
      }
    });
  }

  filterAlerts(): void {
    this.filteredAlerts = this.alerts.filter(alert => {
      const matchesSearch =
        alert.machineId?.toLowerCase().includes(this.searchText.toLowerCase()) ||
        alert.issueName?.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesSeverity =
        this.severityFilter === 'All' || alert.severity === this.severityFilter;

      return matchesSearch && matchesSeverity;
    });
  }

  autoAssign(alertId: number): void {
    // 👉 4. Using your exact method name
    this.alertService.autoAssign(alertId).subscribe({
      next: () => {
        // REMOVE CARD FROM ALERT PAGE
        this.alerts = this.alerts.filter(alert => alert.alertId !== alertId);
        this.filterAlerts(); // Updates the UI instantly
        this.toastService.success('Task assigned', 'Maintenance task assigned successfully.');
      },
      error: (error) => {
        console.log(error);
        this.toastService.error('Assignment failed', error?.error?.message || 'Unable to assign this alert.');
      }
    });
  }
}

