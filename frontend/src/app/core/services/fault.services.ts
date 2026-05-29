import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '../constants/api.config';

export interface FaultKpiData {
  totalFaults: number;
  activeFaults: number;
  resolvedToday: number;
  criticalAlerts: number;
}

@Injectable({
  providedIn: 'root',
})
export class FaultService {
  private apiUrl = `${API_BASE_URL}/faultlogs`;

  constructor(private http: HttpClient) {}

  getFaults(): Observable<any> {
    return this.http.get<any>(this.apiUrl);
  }

  getKPIData(): Observable<FaultKpiData> {
    return this.getFaults().pipe(
      map((response) => response.data || response || []),
      map((faults) => this.computeKpis(Array.isArray(faults) ? faults : [])),
    );
  }

  getFilteredFaults(filters: {
    severity?: string;
    machine?: string;
    startDate?: string;
    endDate?: string;
  }): Observable<any> {
    let params = new HttpParams();

    if (filters.severity) {
      params = params.set('severity', filters.severity);
    }
    if (filters.machine) {
      params = params.set('machine', filters.machine);
    }
    if (filters.startDate) {
      params = params.set('startDate', filters.startDate);
    }
    if (filters.endDate) {
      params = params.set('endDate', filters.endDate);
    }

    return this.http.get<any>(this.apiUrl, { params });
  }

  getMachines(): Observable<any> {
    return this.http.get<any>(`${API_BASE_URL}/machines`);
  }

  exportCSV(data: any[], filename: string): void {
    if (!data || data.length === 0) {
      return;
    }

    const headers = [
      'Machine Name',
      'Fault Type',
      'Severity',
      'Date',
      'Description',
      'Created By',
    ];

    const rows = data.map((fault) => [
      fault.machineName || fault.machineId || '',
      fault.faultType || '',
      fault.severity || '',
      fault.faultDate || '',
      fault.description || '',
      fault.reportedByName || '',
    ]);

    const csv = [headers, ...rows]
      .map((row) => row.map((value) => this.escapeCsv(value)).join(','))
      .join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  private computeKpis(faults: any[]): FaultKpiData {
    const today = this.toDateKey(new Date());
    const totalFaults = faults.length;
    const activeFaults = faults.filter((fault) => {
      const status = (fault.status || '').toLowerCase();
      return status ? status !== 'resolved' : true;
    }).length;
    const resolvedToday = faults.filter((fault) => {
      const dateKey = this.toDateKey(this.parseFaultDate(fault));
      return dateKey === today;
    }).length;
    const criticalAlerts = faults.filter(
      (fault) => (fault.severity || '').toLowerCase() === 'critical',
    ).length;

    return {
      totalFaults,
      activeFaults,
      resolvedToday,
      criticalAlerts,
    };
  }

  private parseFaultDate(fault: any): Date {
    if (fault?.faultDate) {
      return new Date(fault.faultDate);
    }
    return new Date();
  }

  private toDateKey(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  private escapeCsv(value: any): string {
    const stringValue = `${value ?? ''}`.replace(/\r?\n/g, ' ');
    if (stringValue.includes(',') || stringValue.includes('"')) {
      return `"${stringValue.replace(/"/g, '""')}"`;
    }
    return stringValue;
  }
}
