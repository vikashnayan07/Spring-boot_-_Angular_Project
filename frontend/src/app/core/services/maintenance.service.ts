import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})

export class MaintenanceService {

  baseUrl = `${environment.apiUrl}/admin`;

  constructor(
    private http: HttpClient
  ) {}

  // ==========================================
  // GET ALERTS
  // ==========================================

  getAlerts(): Observable<any> {

    return this.http.get(
      `${this.baseUrl}/alerts`
    );

  }

  // ==========================================
  // AUTO ASSIGN
  // ==========================================

  autoAssign(alertId: number): Observable<any> {

    return this.http.post(
      `${this.baseUrl}/alerts/${alertId}/auto-assign`,
      {}
    );

  }

  // ==========================================
  // GET SCHEDULES
  // ==========================================

  getSchedules(): Observable<any> {

    return this.http.get(
      `${this.baseUrl}/schedules/all`
    );

  }

  // ==========================================
  // GET HISTORY
  // ==========================================

  getHistory(): Observable<any> {

    return this.http.get(
      `${this.baseUrl}/history`
    );

  }

}
