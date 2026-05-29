import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

export function ageValidator(minAge: number): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) return null;
    const today = new Date();
    const birthDate = new Date(control.value);
    if (birthDate > today) return { futureDate: true };
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (
      monthDiff < 0 ||
      (monthDiff === 0 && today.getDate() < birthDate.getDate())
    )
      age--;
    return age < minAge ? { underAge: true } : null;
  };
}

export function enterprisePasswordValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value || ''}`;
    if (!value) return null;
    const valid =
      value.length >= 8 &&
      /[A-Z]/.test(value) &&
      /[a-z]/.test(value) &&
      /\d/.test(value) &&
      /[^A-Za-z0-9]/.test(value);
    return valid ? null : { enterprisePassword: true };
  };
}

@Component({
  selector: 'app-add-employee',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-employee.component.html',
})
export class AddEmployeeComponent implements OnInit {
  empForm: FormGroup;
  searchControl = new FormControl('');
  roleFilter = new FormControl('All');

  employees: any[] = [];
  filteredEmployees: any[] = [];
  currentPage = 1;
  pageSize = 10;
  sortKey: 'empId' | 'name' | 'email' | 'role' | 'status' = 'empId';
  sortDirection: 'asc' | 'desc' = 'asc';

  isSubmitting = false;
  isLoadingTable = false;
  showEmployeeModal = false;
  successMsg = '';
  errorMsg = '';
  pendingDeleteId: number | null = null;
  pendingSuspend: { empId: number; days: number } | null = null;

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private toastService: ToastService,
  ) {
    this.empForm = this.fb.group({
      name: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.pattern(/^[A-Za-z ]+$/),
        ],
      ],
      email: [
        '',
        [
          Validators.required,
          Validators.pattern(/^[^\s@]+@[^\s@]+\.(tcs|com|in)$/i),
        ],
      ],
      dob: ['', [Validators.required, ageValidator(21)]],
      password: ['', [Validators.required, enterprisePasswordValidator()]],
      role: ['Maintenance_engineer', Validators.required],
    });
  }

  ngOnInit(): void {
    this.loadEmployees();
    this.searchControl.valueChanges.subscribe(() => this.filterEmployees());
    this.roleFilter.valueChanges.subscribe(() => this.filterEmployees());
  }

  get f() {
    return this.empForm.controls;
  }

  get totalPages(): number {
    return Math.ceil(this.filteredEmployees.length / this.pageSize) || 1;
  }

  get paginatedEmployees(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredEmployees.slice(start, start + this.pageSize);
  }

  pages(): PaginationItem[] {
    return buildPagination(this.currentPage, this.totalPages);
  }

  changePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.currentPage = item;
  }

  get startIndex(): number {
    return this.filteredEmployees.length === 0
      ? 0
      : (this.currentPage - 1) * this.pageSize + 1;
  }

  get endIndex(): number {
    return Math.min(
      this.currentPage * this.pageSize,
      this.filteredEmployees.length,
    );
  }

  get activeEmployees(): number {
    return this.employees.filter((emp) => this.isActive(emp)).length;
  }

  get engineerCount(): number {
    return this.employees.filter(
      (emp) => this.roleLabel(emp) === 'Maintenance Engineer',
    ).length;
  }

  get operatorCount(): number {
    return this.employees.filter((emp) => this.roleLabel(emp) === 'Operator')
      .length;
  }

  get passwordChecks() {
    const value = `${this.empForm.get('password')?.value || ''}`;
    return [
      { label: '8 characters', valid: value.length >= 8 },
      { label: 'Uppercase', valid: /[A-Z]/.test(value) },
      { label: 'Lowercase', valid: /[a-z]/.test(value) },
      { label: 'Number', valid: /\d/.test(value) },
      { label: 'Special character', valid: /[^A-Za-z0-9]/.test(value) },
    ];
  }

  loadEmployees(): void {
    this.isLoadingTable = true;
    this.api.getAllEmployees().subscribe({
      next: (res) => {
        this.isLoadingTable = false;
        this.employees = Array.isArray(res?.data)
          ? res.data
          : Array.isArray(res)
            ? res
            : [];
        this.filterEmployees();
      },
      error: () => {
        this.isLoadingTable = false;
        this.toastService.error(
          'Employee data unavailable',
          'Failed to load employee list.',
        );
      },
    });
  }

  filterEmployees(): void {
    const term = `${this.searchControl.value || ''}`.toLowerCase().trim();
    const role = `${this.roleFilter.value || 'All'}`;
    this.currentPage = 1;
    const filtered = this.employees.filter((emp) => {
      const matchesTerm =
        !term ||
        `${emp.name || ''}`.toLowerCase().includes(term) ||
        `${emp.email || ''}`.toLowerCase().includes(term) ||
        `${emp.empId || ''}`.includes(term);
      const matchesRole = role === 'All' || this.roleLabel(emp) === role;
      return matchesTerm && matchesRole;
    });
    this.filteredEmployees = this.sortEmployees(filtered);
  }

  sortBy(key: 'empId' | 'name' | 'email' | 'role' | 'status'): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.filteredEmployees = this.sortEmployees(this.filteredEmployees);
  }

  openEmployeeModal(): void {
    this.successMsg = '';
    this.errorMsg = '';
    this.showEmployeeModal = true;
  }

  closeEmployeeModal(): void {
    if (this.isSubmitting) return;
    this.showEmployeeModal = false;
    this.empForm.reset({ role: 'Maintenance_engineer' });
  }

  onSubmit(): void {
    this.empForm.markAllAsTouched();
    if (this.empForm.invalid) {
      this.toastService.warning(
        'Validation required',
        'Complete all required employee fields.',
      );
      return;
    }

    this.isSubmitting = true;
    this.api.addEmployee(this.empForm.value).subscribe({
      next: (res) => {
        this.isSubmitting = false;
        this.toastService.success(
          'Employee registered',
          res?.message || 'Employee securely added.',
        );
        this.closeEmployeeModal();
        this.loadEmployees();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.toastService.error(
          'Registration failed',
          err.error?.message || 'Failed to register employee.',
        );
      },
    });
  }

  disableEmployee(empId: number, daysString: string): void {
    const days = parseInt(daysString, 10);
    if (Number.isNaN(days) || days <= 0) {
      this.toastService.warning(
        'Invalid duration',
        'Enter a valid suspension duration.',
      );
      return;
    }
    this.pendingSuspend = { empId, days };
  }

  confirmSuspend(): void {
    if (!this.pendingSuspend) return;
    const { empId, days } = this.pendingSuspend;
    this.api.disableEmployee(empId, days).subscribe({
      next: (res) => {
        this.toastService.success(
          'Employee suspended',
          res?.message || 'Suspended successfully.',
        );
        this.pendingSuspend = null;
        this.loadEmployees();
      },
      error: (err) =>
        this.toastService.error(
          'Suspend failed',
          err.error?.message || 'Failed to disable employee.',
        ),
    });
  }

  deleteEmployee(empId: number): void {
    this.pendingDeleteId = empId;
  }

  confirmDelete(): void {
    if (!this.pendingDeleteId) return;
    const empId = this.pendingDeleteId;
    this.api.deleteEmployee(empId).subscribe({
      next: () => {
        this.toastService.success(
          'Employee deactivated',
          'The employee account was preserved for audit history.',
        );
        this.pendingDeleteId = null;
        this.loadEmployees();
      },
      error: (err) =>
        this.toastService.error(
          'Deactivate failed',
          err.error?.message || 'Failed to deactivate employee.',
        ),
    });
  }

  cancelConfirm(): void {
    this.pendingDeleteId = null;
    this.pendingSuspend = null;
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) this.currentPage++;
  }

  prevPage(): void {
    if (this.currentPage > 1) this.currentPage--;
  }

  isActive(emp: any): boolean {
    return !(
      emp.disabled === true ||
      emp.active === false ||
      emp.isActive === false
    );
  }

  roleLabel(emp: any): string {
    const raw = `${emp.roleName || emp.role || ''}`.toLowerCase();
    if (raw.includes('operator') || Number(emp.roleId) === 3) return 'Operator';
    if (raw.includes('engineer') || Number(emp.roleId) === 2)
      return 'Maintenance Engineer';
    if (raw.includes('admin') || Number(emp.roleId) === 1) return 'Admin';
    return 'Employee';
  }

  private sortEmployees(list: any[]): any[] {
    const direction = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.sortValue(a);
      const right = this.sortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private sortValue(emp: any): string | number {
    switch (this.sortKey) {
      case 'name':
        return `${emp?.name || ''}`.toLowerCase();
      case 'email':
        return `${emp?.email || ''}`.toLowerCase();
      case 'role':
        return this.roleLabel(emp).toLowerCase();
      case 'status':
        return this.isActive(emp) ? 'active' : 'inactive';
      default:
        return Number(emp?.empId || 0);
    }
  }
}
