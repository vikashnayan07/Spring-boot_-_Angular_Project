import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

@Component({
  selector: 'app-manage-machines',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './manage-machines.component.html',
  styleUrls: ['./manage-machines.component.css'],
})
export class ManageMachinesComponent implements OnInit {
  machineForm: FormGroup;
  searchControl = new FormControl('');
  criticalityFilter = new FormControl('');
  lifecycleFilter = new FormControl('');
  healthFilter = new FormControl('');

  isSubmitting = false;
  isLoadingTable = false;
  showMachineRegistered = false;
  workspaceMode: 'list' | 'form' | 'success' = 'list';
  formMode: 'add' | 'edit' = 'add';
  editModalOpen = false;
  successMsg = '';
  errorMsg = '';
  registeredMachine: any = null;
  selectedMachine: any = null;

  machines: any[] = [];
  filteredMachines: any[] = [];
  currentPage = 1;
  pageSize = 8;
  sortKey:
    | 'machineId'
    | 'machineName'
    | 'machineType'
    | 'status'
    | 'productionCriticality'
    | 'lifecycleStatus'
    | 'healthStatus'
    | 'currentOperationalLoad' = 'machineId';
  sortDirection: 'asc' | 'desc' = 'asc';

  criticalityOptions = ['Low', 'Medium', 'High', 'Critical'];
  statusOptions = ['Idle', 'Running', 'Stopped'];
  lifecycleOptions = [
    'ACTIVE',
    'SERVICE_OVERDUE',
    'OUT_OF_WARRANTY',
    'UNREGISTERED_ASSET',
  ];
  healthOptions = [
    'HEALTHY',
    'MONITOR',
    'AT_RISK',
    'MAINTENANCE_DUE',
    'OFFLINE',
  ];

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private route: ActivatedRoute,
  ) {
    this.machineForm = this.fb.group({
      machineName: ['', [Validators.required, Validators.minLength(3)]],
      machineType: ['', Validators.required],
      status: ['Idle', Validators.required],
      faultType: [''],
      description: [''],
      productionCriticality: ['Medium', Validators.required],
      currentOperationalLoad: [
        55,
        [Validators.required, Validators.min(0), Validators.max(100)],
      ],
      productionBottleneck: [false, Validators.required],
      expectedMtbf: [240, [Validators.required, Validators.min(1)]],
      expectedMttr: [120, [Validators.required, Validators.min(1)]],
      purchaseDate: [''],
      warrantyExpiryDate: [''],
      lastServiceDate: [''],
      nextServiceDueDate: [''],
    });
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.applyDrilldownParams(params.get('lifecycle'), params.get('health'));
    });
    this.loadMachines();
    this.searchControl.valueChanges.subscribe(() => this.applyFilters());
    this.criticalityFilter.valueChanges.subscribe(() => this.applyFilters());
    this.lifecycleFilter.valueChanges.subscribe(() => this.applyFilters());
    this.healthFilter.valueChanges.subscribe(() => this.applyFilters());
  }

  get f() {
    return this.machineForm.controls;
  }
  get currentLoad(): number {
    return Number(this.machineForm.get('currentOperationalLoad')?.value || 0);
  }

  get loadLabel(): string {
    const load = this.currentLoad;
    if (load <= 25) return 'Backup';
    if (load <= 50) return 'Normal';
    if (load <= 75) return 'High Load';
    return 'Critical';
  }

  get loadColorClass(): string {
    const load = this.currentLoad;
    if (load <= 25) return 'from-blue-600 to-blue-400 text-white';
    if (load <= 50) return 'from-emerald-500 to-green-400 text-emerald-950';
    if (load <= 75) return 'from-orange-500 to-amber-400 text-orange-950';
    return 'from-red-600 to-rose-500 text-white';
  }

  get sliderGradient(): string {
    const load = this.currentLoad;
    const color =
      load <= 25
        ? '#2563eb'
        : load <= 50
          ? '#10b981'
          : load <= 75
            ? '#f97316'
            : '#ef4444';
    return `linear-gradient(90deg, ${color} ${load}%, rgba(148,163,184,.25) ${load}%)`;
  }

  get totalPages(): number {
    return Math.ceil(this.filteredMachines.length / this.pageSize) || 1;
  }

  get paginatedMachines(): any[] {
    const start = (this.currentPage - 1) * this.pageSize;
    return this.filteredMachines.slice(start, start + this.pageSize);
  }

  pages(): PaginationItem[] {
    return buildPagination(this.currentPage, this.totalPages);
  }

  changePageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.currentPage = item;
  }

  get totalMachines(): number {
    return this.machines.length;
  }
  get activeMachines(): number {
    return this.machines.filter((m) => m.status === 'Running').length;
  }
  get criticalLoadMachines(): number {
    return this.machines.filter(
      (m) => Number(m.currentOperationalLoad || 0) > 80,
    ).length;
  }
  get maintenanceDue(): number {
    return this.machines.filter(
      (m) =>
        m.healthStatus === 'MAINTENANCE_DUE' ||
        m.lifecycleStatus === 'SERVICE_OVERDUE',
    ).length;
  }

  loadMachines(): void {
    this.isLoadingTable = true;
    this.errorMsg = '';

    this.api.getAllMachines().subscribe({
      next: (res: any) => {
        this.isLoadingTable = false;
        this.machines = res.data || [];
        this.applyFilters();
        this.scrollToTableIfDrilldown();
      },
      error: (err) => {
        this.isLoadingTable = false;
        this.errorMsg =
          err.error?.message || 'Failed to load machine roster from database.';
      },
    });
  }

  applyFilters(): void {
    const term = `${this.searchControl.value || ''}`.toLowerCase().trim();
    const criticality = this.criticalityFilter.value;
    const lifecycle = this.normalizeLifecycleFilter(this.lifecycleFilter.value || '');
    const health = this.healthFilter.value;
    this.currentPage = 1;

    const filtered = this.machines.filter((machine) => {
      const matchesSearch =
        !term ||
        `${machine.machineId || ''}`.toLowerCase().includes(term) ||
        `${machine.machineName || ''}`.toLowerCase().includes(term) ||
        `${machine.machineType || ''}`.toLowerCase().includes(term);
      const matchesCriticality =
        !criticality || machine.productionCriticality === criticality;
      const matchesLifecycle =
        !lifecycle ||
        `${machine.lifecycleStatus || ''}`.toUpperCase() === lifecycle;
      const matchesHealth =
        !health || `${machine.healthStatus || ''}`.toUpperCase() === health;
      return (
        matchesSearch && matchesCriticality && matchesLifecycle && matchesHealth
      );
    });
    this.filteredMachines = this.sortMachines(filtered);
  }

  private applyDrilldownParams(lifecycle: string | null, health: string | null): void {
    if (lifecycle !== null) {
      this.lifecycleFilter.setValue(this.normalizeLifecycleFilter(lifecycle), { emitEvent: false });
    }
    if (health !== null) {
      this.healthFilter.setValue(health, { emitEvent: false });
    }
    if (this.machines.length) {
      this.applyFilters();
      this.scrollToTableIfDrilldown();
    }
  }

  private normalizeLifecycleFilter(value: string): string {
    const normalized = `${value || ''}`.toUpperCase();
    if (normalized === 'WARRANTY_EXPIRED') return 'OUT_OF_WARRANTY';
    return normalized;
  }

  private scrollToTableIfDrilldown(): void {
    if (!this.route.snapshot.queryParamMap.get('drilldown')) return;
    setTimeout(() => {
      document.querySelector('.data-table')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start',
      });
    }, 120);
  }

  sortBy(
    key:
      | 'machineId'
      | 'machineName'
      | 'machineType'
      | 'status'
      | 'productionCriticality'
      | 'lifecycleStatus'
      | 'healthStatus'
      | 'currentOperationalLoad',
  ): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.filteredMachines = this.sortMachines(this.filteredMachines);
  }

  onSubmit(): void {
    this.machineForm.markAllAsTouched();
    if (this.machineForm.invalid) return;

    this.isSubmitting = true;
    this.successMsg = '';
    this.errorMsg = '';

    const request$ =
      this.formMode === 'edit' && this.selectedMachine?.machineId
        ? this.api.updateMachine(
            this.selectedMachine.machineId,
            this.machineForm.value,
          )
        : this.api.addMachine(this.machineForm.value);

    request$.subscribe({
      next: (res: any) => {
        this.isSubmitting = false;
        this.registeredMachine = res.data;
        this.successMsg =
          res.message ||
          (this.formMode === 'edit'
            ? 'Machine updated successfully.'
            : 'Machine registered successfully.');
        if (this.formMode === 'edit') {
          this.editModalOpen = false;
          this.workspaceMode = 'list';
        } else {
          this.showMachineRegistered = true;
          this.workspaceMode = 'success';
        }
        this.loadMachines();
      },
      error: (err) => {
        this.isSubmitting = false;
        this.errorMsg = err.error?.message || 'Failed to register machine.';
      },
    });
  }

  addAnother(): void {
    this.formMode = 'add';
    this.workspaceMode = 'form';
    this.showMachineRegistered = false;
    this.editModalOpen = false;
    this.selectedMachine = null;
    this.registeredMachine = null;
    this.machineForm.reset({
      status: 'Idle',
      productionCriticality: 'Medium',
      currentOperationalLoad: 55,
      productionBottleneck: false,
      expectedMtbf: 240,
      expectedMttr: 120,
      purchaseDate: '',
      warrantyExpiryDate: '',
      lastServiceDate: '',
      nextServiceDueDate: '',
    });
  }

  viewMachines(): void {
    this.workspaceMode = 'list';
    this.showMachineRegistered = false;
    this.editModalOpen = false;
    this.selectedMachine = null;
    this.formMode = 'add';
  }

  openAddMachine(): void {
    this.addAnother();
  }

  editMachine(machine: any): void {
    this.formMode = 'edit';
    this.workspaceMode = 'list';
    this.editModalOpen = true;
    this.showMachineRegistered = false;
    this.selectedMachine = machine;
    this.errorMsg = '';
    this.successMsg = '';
    this.machineForm.reset({
      machineName: machine.machineName || '',
      machineType: machine.machineType || '',
      status: machine.status || 'Idle',
      faultType: machine.faultType || '',
      description: machine.description || '',
      productionCriticality: machine.productionCriticality || 'Medium',
      currentOperationalLoad: machine.currentOperationalLoad ?? 55,
      productionBottleneck: !!machine.productionBottleneck,
      expectedMtbf: machine.expectedMtbf || 240,
      expectedMttr: machine.expectedMttr || 120,
      purchaseDate: machine.purchaseDate || '',
      warrantyExpiryDate: machine.warrantyExpiryDate || '',
      lastServiceDate: machine.lastServiceDate || '',
      nextServiceDueDate: machine.nextServiceDueDate || '',
    });
  }

  closeEditModal(): void {
    this.editModalOpen = false;
    this.formMode = 'add';
    this.selectedMachine = null;
    this.addAnother();
    this.workspaceMode = 'list';
  }

  viewMachine(machine: any): void {
    this.selectedMachine = machine;
    this.errorMsg = '';
    this.successMsg = `Viewing ${machine.machineName || machine.machineId}. Use Edit to update machine intelligence.`;
  }

  backToList(): void {
    this.viewMachines();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) this.currentPage++;
  }

  prevPage(): void {
    if (this.currentPage > 1) this.currentPage--;
  }

  exportMachines(): void {
    const headers = [
      'Machine ID',
      'Machine Name',
      'Type',
      'Production Criticality',
      'Current Load',
      'Bottleneck',
      'Expected MTBF',
      'Expected MTTR',
      'Status',
      'Lifecycle Status',
      'Health Status',
      'Next Service Due',
      'Warranty Expiry',
    ];
    const rows = this.filteredMachines.map((machine) => [
      machine.machineId || '',
      machine.machineName || '',
      machine.machineType || '',
      machine.productionCriticality || '',
      machine.currentOperationalLoad ?? '',
      machine.productionBottleneck ? 'Yes' : 'No',
      machine.expectedMtbf ?? '',
      machine.expectedMttr ?? '',
      machine.status || '',
      machine.lifecycleStatus || '',
      machine.healthStatus || '',
      machine.nextServiceDueDate || '',
      machine.warrantyExpiryDate || '',
    ]);
    const csv = [headers, ...rows]
      .map((row) =>
        row.map((value) => `"${`${value}`.replace(/"/g, '""')}"`).join(','),
      )
      .join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'machcare-machines.csv';
    link.click();
    URL.revokeObjectURL(url);
  }

  lifecycleClass(machine: any): string {
    const status = `${machine.lifecycleStatus || ''}`.toUpperCase();
    if (status === 'SERVICE_OVERDUE')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (status === 'OUT_OF_WARRANTY')
      return 'bg-orange-400/15 text-orange-300 border-orange-400/20';
    if (status === 'UNREGISTERED_ASSET')
      return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  healthClass(machine: any): string {
    const status = `${machine.healthStatus || ''}`.toUpperCase();
    if (status === 'OFFLINE')
      return 'bg-red-400/15 text-red-300 border-red-400/20';
    if (status === 'MAINTENANCE_DUE')
      return 'bg-amber-400/15 text-amber-300 border-amber-400/20';
    if (status === 'AT_RISK')
      return 'bg-orange-400/15 text-orange-300 border-orange-400/20';
    if (status === 'MONITOR')
      return 'bg-cyan-400/15 text-cyan-300 border-cyan-400/20';
    return 'bg-emerald-400/15 text-emerald-300 border-emerald-400/20';
  }

  private sortMachines(list: any[]): any[] {
    const direction = this.sortDirection === 'asc' ? 1 : -1;
    return [...list].sort((a, b) => {
      const left = this.sortValue(a);
      const right = this.sortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  private sortValue(machine: any): string | number {
    switch (this.sortKey) {
      case 'machineName':
        return `${machine?.machineName || ''}`.toLowerCase();
      case 'machineType':
        return `${machine?.machineType || ''}`.toLowerCase();
      case 'status':
        return `${machine?.status || ''}`.toLowerCase();
      case 'productionCriticality':
        return `${machine?.productionCriticality || ''}`.toLowerCase();
      case 'lifecycleStatus':
        return `${machine?.lifecycleStatus || ''}`.toLowerCase();
      case 'healthStatus':
        return `${machine?.healthStatus || ''}`.toLowerCase();
      case 'currentOperationalLoad':
        return Number(machine?.currentOperationalLoad || 0);
      default:
        return `${machine?.machineId || ''}`.toLowerCase();
    }
  }
}
