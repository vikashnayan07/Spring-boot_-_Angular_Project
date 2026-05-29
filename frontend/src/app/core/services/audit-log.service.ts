import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AuditLogEntry {
  logId: number;
  empId: number | null;
  empName: string | null;
  email: string | null;
  roleId: number | null;
  action: string | null;
  status: string | null;
  message: string | null;
  createdAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private baseUrl = `${environment.apiUrl}/admin/audit-logs`;

  constructor(private http: HttpClient) {}

  getSummary(): Observable<any> {
    return this.http.get(`${this.baseUrl}/summary`, {
      headers: this.headers(),
    });
  }

  getLogs(filters: {
    date?: string;
    roleId?: string | number;
    status?: string;
    action?: string;
    page?: number;
    size?: number;
  }): Observable<any> {
    let params = new HttpParams();

    if (filters.date) {
      params = params.set('date', filters.date);
    }
    if (
      filters.roleId !== undefined &&
      filters.roleId !== null &&
      `${filters.roleId}` !== 'All'
    ) {
      params = params.set('roleId', `${filters.roleId}`);
    }
    if (filters.status && filters.status !== 'All') {
      params = params.set('status', filters.status);
    }
    if (filters.action && filters.action !== 'All') {
      params = params.set('action', filters.action);
    }

    params = params.set('page', `${filters.page ?? 1}`);
    params = params.set('size', `${filters.size ?? 10}`);

    return this.http.get(`${this.baseUrl}`, {
      headers: this.headers(),
      params,
    });
  }

  private headers(): HttpHeaders {
    return new HttpHeaders({
      Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
    });
  }
}
