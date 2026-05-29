import { Component, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ToastService } from '../../../../shared/components/toast/toast.service';
import { API_BASE_URL } from '../../../../core/constants/api.config';

@Component({
  selector: 'app-filter-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './filter-panel.component.html',
  styleUrls: ['./filter-panel.component.css'],
})
export class FilterPanelComponent implements OnInit {
  selectedSeverity = '';
  selectedMachine = '';
  startDate = '';
  endDate = '';
  dateError = '';

  machines: any[] = [];

  @Output() filtersChanged = new EventEmitter<any>();
  @Output() filtersReset = new EventEmitter<void>();

  constructor(
    private http: HttpClient,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.loadMachines();
  }

  applyFilters(): void {
    if (this.startDate && this.endDate && this.endDate < this.startDate) {
      this.dateError = 'End date cannot be before start date.';
      this.toastService.warning('Invalid Date Range', this.dateError);
      return;
    }

    this.dateError = '';
    this.filtersChanged.emit({
      severity: this.selectedSeverity,
      machine: this.selectedMachine,
      startDate: this.startDate,
      endDate: this.endDate,
    });
    this.toastService.success(
      'Filters Applied',
      'Fault list updated with filters.',
    );
  }

  clearFilters(): void {
    this.selectedSeverity = '';
    this.selectedMachine = '';
    this.startDate = '';
    this.endDate = '';
    this.dateError = '';
    this.filtersChanged.emit({
      severity: '',
      machine: '',
      startDate: '',
      endDate: '',
    });
    this.filtersReset.emit();
    this.toastService.info('Filters Reset', 'All filters have been cleared.');
  }

  onStartDateChange(): void {
    if (this.endDate && this.endDate < this.startDate) {
      this.dateError = 'End date cannot be before start date.';
    } else {
      this.dateError = '';
    }
  }

  onEndDateChange(): void {
    this.onStartDateChange();
  }

  private loadMachines(): void {
    this.http.get<any>(`${API_BASE_URL}/machines`).subscribe({
      next: (response) => {
        this.machines = response.data || response || [];
      },
      error: () => {
        this.machines = [];
      },
    });
  }
}
