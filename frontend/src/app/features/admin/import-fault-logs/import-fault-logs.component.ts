import { CommonModule } from '@angular/common';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { environment } from '../../../../environments/environment';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-import-fault-logs',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './import-fault-logs.component.html',
  styleUrls: ['./import-fault-logs.component.css'],
})
export class ImportFaultLogsComponent {
  selectedFile: File | null = null;
  previewRows: string[][] = [];
  headers: string[] = [];
  validationError = '';
  isDragging = false;
  isUploading = false;
  uploadProgress = 0;

  constructor(
    private http: HttpClient,
    private toastService: ToastService,
    private router: Router,
  ) {}

  get recordCount(): number {
    return this.previewRows.length;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = true;
  }

  onDragLeave(): void {
    this.isDragging = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging = false;
    const file = event.dataTransfer?.files?.[0];
    if (file) {
      this.handleFile(file);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.handleFile(file);
    }
    input.value = '';
  }

  clearFile(): void {
    this.selectedFile = null;
    this.previewRows = [];
    this.headers = [];
    this.validationError = '';
    this.uploadProgress = 0;
  }

  upload(): void {
    if (!this.selectedFile || this.validationError || !this.recordCount) {
      this.toastService.warning('CSV not ready', 'Choose a valid CSV file before importing.');
      return;
    }

    const formData = new FormData();
    formData.append('file', this.selectedFile);
    this.isUploading = true;
    this.uploadProgress = 5;

    this.http
      .post(`${environment.apiUrl}/admin/faults/upload`, formData, {
        observe: 'events',
        reportProgress: true,
      })
      .subscribe({
        next: (event: HttpEvent<any>) => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            this.uploadProgress = Math.round((event.loaded / event.total) * 100);
          }
          if (event.type === HttpEventType.Response) {
            this.isUploading = false;
            this.uploadProgress = 100;
            const body = event.body || {};
            const imported = Number(body.imported ?? this.recordCount);
            const skipped = Number(body.skipped ?? 0);
            const failed = Number(body.failed ?? 0);
            const summary = failed > 0 || skipped > 0
              ? `${imported} imported, ${failed} failed, ${skipped} skipped.`
              : `${imported} fault logs imported successfully.`;
            this.toastService.success(
              'Fault logs imported',
              summary,
            );
            setTimeout(() => {
              this.router.navigate(['/admin/fault-logs']);
            }, 700);
          }
        },
        error: (error) => {
          console.error('CSV upload failed', error);
          this.isUploading = false;
          const missingHeaders = error?.error?.missingHeaders?.length
            ? ` Missing: ${error.error.missingHeaders.join(', ')}.`
            : '';
          this.toastService.error(
            'Import failed',
            `${error?.error?.message || 'Unable to import fault logs.'}${missingHeaders}`,
          );
        },
      });
  }

  private handleFile(file: File): void {
    this.clearFile();

    if (!file.name.toLowerCase().endsWith('.csv')) {
      this.validationError = 'Only .csv files are supported.';
      this.toastService.error('Invalid file type', this.validationError);
      return;
    }

    if (file.size === 0) {
      this.validationError = 'The selected CSV file is empty.';
      this.toastService.error('Empty file', this.validationError);
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const content = String(reader.result || '').trim();
      this.validateAndPreview(file, content);
    };
    reader.onerror = () => {
      this.validationError = 'Could not read the selected file.';
      this.toastService.error('Read failed', this.validationError);
    };
    reader.readAsText(file);
  }

  private validateAndPreview(file: File, content: string): void {
    if (!content) {
      this.validationError = 'The selected CSV file is empty.';
      return;
    }

    const rows = this.parseCsv(content);
    if (rows.length < 2) {
      this.validationError = 'CSV must include a header row and at least one data row.';
      return;
    }

    const [headers, ...dataRows] = rows;
    const width = headers.length;
    const malformed = dataRows.some((row) => row.length !== width);
    if (width < 3 || malformed) {
      this.validationError = 'Malformed CSV detected. Please check missing commas or broken rows.';
      return;
    }

    this.selectedFile = file;
    this.headers = headers;
    this.previewRows = dataRows.filter((row) => row.some((cell) => cell.trim())).slice(0, 50);
    if (!this.previewRows.length) {
      this.validationError = 'CSV does not contain any data records.';
      return;
    }
  }

  private parseCsv(content: string): string[][] {
    return content
      .split(/\r?\n/)
      .map((line) => line.split(',').map((cell) => cell.trim().replace(/^"|"$/g, '')))
      .filter((row) => row.some((cell) => cell.length > 0));
  }
}
