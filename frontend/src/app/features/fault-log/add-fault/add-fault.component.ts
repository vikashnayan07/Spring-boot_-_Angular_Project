import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { animate, style, transition, trigger } from '@angular/animations';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API_BASE_URL } from '../../../core/constants/api.config';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-add-fault',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
  ],
  templateUrl: './add-fault.component.html',
  styleUrls: ['./add-fault.component.css'],
  animations: [
    trigger('fadeUp', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(18px) scale(0.985)' }),
        animate(
          '420ms cubic-bezier(0.16, 1, 0.3, 1)',
          style({ opacity: 1, transform: 'translateY(0) scale(1)' }),
        ),
      ]),
      transition(':leave', [
        animate(
          '220ms ease',
          style({ opacity: 0, transform: 'translateY(-10px) scale(0.985)' }),
        ),
      ]),
    ]),
    trigger('successFade', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(18px) scale(0.94)' }),
        animate(
          '520ms cubic-bezier(0.16, 1, 0.3, 1)',
          style({ opacity: 1, transform: 'translateY(0) scale(1)' }),
        ),
      ]),
    ]),
  ],
})
export class AddFaultComponent implements OnInit {
  faultForm!: FormGroup;

  machines: any[] = [];

  faultTypes: string[] = [];

  showOtherFault = false;

  machineDropdownOpen = false;

  faultTypeDropdownOpen = false;

  selectedMachineLabel = 'Select Machine';

  selectedFaultTypeLabel = 'Select Fault Type';

  submitted = false;

  success = false;

  successFaultId = '';
  successMachineId = '';
  successTimestamp = '';
  successLoggedBy = '';

  readonly maxDescriptionLength = 500;

  loading = false;

  constructor(
    private fb: FormBuilder,
    private http: HttpClient,
    private toastService: ToastService,
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.loadCurrentTime();
    this.loadNextFaultId();
    this.loadMachines();
  }

  /*
  ======================================
  FORM
  ======================================
  */

  initializeForm() {
    this.faultForm = this.fb.group({
      faultId: [''],

      loggedTime: [''],

      machine: ['', Validators.required],

      faultType: ['', Validators.required],

      severity: ['', Validators.required],

      description: [''],

      otherFault: ['', [Validators.maxLength(this.maxDescriptionLength)]],
    });
  }

  /*
  ======================================
  NEXT FAULT ID
  ======================================
  */

  loadNextFaultId() {
    this.http.get<any>(`${API_BASE_URL}/faultlogs/next-id`).subscribe({
      next: (res) => {
        console.log('Next ID => ', res);

        /*
          Expected:
          {
            success:true,
            faultId:"F-1037"
          }
          */

        this.faultForm.patchValue({
          faultId: res.faultId || '',
        });
      },

      error: (err) => {
        console.log(err);
      },
    });
  }

  /*
  ======================================
  CURRENT TIME
  ======================================
  */

  loadCurrentTime() {
    this.http.get<any>(`${API_BASE_URL}/faultlogs/current-time`).subscribe({
      next: (res) => {
        console.log('Time => ', res);

        /*
          Expected:
          {
            success:true,
            timestamp:"2026-05-20T16:13:28"
          }
          */

        const rawTime = res.timestamp;

        if (rawTime) {
          const formatted = new Date(rawTime).toLocaleString();

          this.faultForm.patchValue({
            loggedTime: formatted,
          });
        }
      },

      error: (err) => {
        console.log(err);
      },
    });
  }

  /*
  ======================================
  LOAD MACHINES
  ======================================
  */

  loadMachines() {
    this.http.get<any>(`${API_BASE_URL}/machines`).subscribe({
      next: (res) => {
        console.log('Machines => ', res);

        this.machines = res.data || res;
      },

      error: (err) => {
        console.log(err);
      },
    });
  }

  /*
  ======================================
  MACHINE CHANGE
  ======================================
  */

  updateFaultTypes(machineId: string) {
    const selectedMachine = this.machines.find(
      (machine) => machine.machineId == machineId,
    );

    if (selectedMachine?.faultType) {
      this.faultTypes = selectedMachine.faultType
        .split(',')
        .map((item: string) => item.trim());
    } else {
      this.faultTypes = [];
    }

    if (!this.faultTypes.includes('Other')) {
      this.faultTypes = [...this.faultTypes, 'Other'];
    }

    this.faultForm.patchValue({
      faultType: '',
    });

    this.selectedFaultTypeLabel = 'Select Fault Type';

    this.onFaultTypeChange();
  }

  onFaultTypeChange() {
    const type = this.faultForm.get('faultType')?.value;

    this.showOtherFault = type === 'Other';

    const otherControl = this.faultForm.get('otherFault');

    if (this.showOtherFault) {
      otherControl?.setValidators([
        Validators.required,
        Validators.minLength(10),
        Validators.maxLength(this.maxDescriptionLength),
      ]);
    } else {
      otherControl?.clearValidators();
      otherControl?.setValue('');
    }

    otherControl?.updateValueAndValidity();
  }

  /*
  ======================================
  SUBMIT
  ======================================
  */

  submitFault() {
    this.submitted = true;

    if (this.faultForm.invalid) {
      this.faultForm.markAllAsTouched();
      this.toastService.warning(
        'Validation',
        'Please fill all required fields before submitting',
      );
      return;
    }

    this.loading = true;

    const payload = {
      machineId: this.faultForm.value.machine,
      description: this.showOtherFault
        ? this.faultForm.value.otherFault
        : this.faultForm.value.faultType,
      severity: this.faultForm.value.severity,
    };

    this.http
      .post<any>(`${API_BASE_URL}/operator/faults/log`, payload)
      .subscribe({
        next: (res) => {
          this.loading = false;
          const savedFault = res?.data || {};
          this.successFaultId =
            savedFault.faultId || this.faultForm.value.faultId;
          this.successMachineId =
            savedFault.machineId || this.faultForm.value.machine || 'Machine';
          this.successTimestamp = this.formatSuccessTimestamp(
            savedFault.faultDate,
            savedFault.faultTime,
          );
          this.successLoggedBy =
            savedFault.reportedByName || localStorage.getItem('name') || 'Operator';
          this.success = true;
          this.toastService.success(
            'Success',
            'Fault has been logged successfully!',
          );
        },
        error: (err) => {
          console.error(err);
          this.loading = false;
          this.toastService.error(
            'Error',
            'Failed to log fault. Please try again.',
          );
        },
      });
  }

  setSeverity(value: string) {
    this.faultForm.patchValue({
      severity: value,
    });

    this.faultForm.get('severity')?.markAsTouched();
  }

  startNewFault() {
    this.success = false;
    this.successFaultId = '';
    this.successMachineId = '';
    this.successTimestamp = '';
    this.successLoggedBy = '';
    this.submitted = false;
    this.showOtherFault = false;

    this.machineDropdownOpen = false;
    this.faultTypeDropdownOpen = false;

    this.selectedMachineLabel = 'Select Machine';
    this.selectedFaultTypeLabel = 'Select Fault Type';

    this.initializeForm();
    this.loadCurrentTime();
    this.loadNextFaultId();
    this.loadMachines();

    this.toastService.info(
      'Ready',
      'Form has been reset for a new fault report',
    );
  }

  isInvalid(controlName: string): boolean {
    const control = this.faultForm.get(controlName);

    return !!(this.submitted && control && control.invalid);
  }

  get otherFaultLength(): number {
    return this.faultForm.get('otherFault')?.value?.length || 0;
  }

  private formatSuccessTimestamp(faultDate?: string, faultTime?: string): string {
    const fallback = new Date();
    const parsed = faultDate
      ? new Date(`${faultDate}T${faultTime || '00:00:00'}`)
      : fallback;
    const date = Number.isNaN(parsed.getTime()) ? fallback : parsed;
    return date.toLocaleString();
  }

  toggleMachineDropdown(event: Event) {
    event.stopPropagation();

    this.machineDropdownOpen = !this.machineDropdownOpen;
    this.faultTypeDropdownOpen = false;
  }

  toggleFaultTypeDropdown(event: Event) {
    event.stopPropagation();

    this.faultTypeDropdownOpen = !this.faultTypeDropdownOpen;
    this.machineDropdownOpen = false;
  }

  selectMachine(machine: any) {
    this.faultForm.patchValue({
      machine: machine.machineId,
    });

    this.selectedMachineLabel = machine.machineName;
    this.machineDropdownOpen = false;

    this.updateFaultTypes(machine.machineId);
  }

  selectFaultType(type: string) {
    this.faultForm.patchValue({
      faultType: type,
    });

    this.selectedFaultTypeLabel = type;
    this.faultTypeDropdownOpen = false;

    this.onFaultTypeChange();
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;

    if (!target.closest('.dropdown-machine')) {
      this.machineDropdownOpen = false;
    }

    if (!target.closest('.dropdown-faulttype')) {
      this.faultTypeDropdownOpen = false;
    }
  }
}
