import { Injectable } from '@angular/core';

import {
  HttpClient,
  HttpHeaders
} from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})

export class AlertService {

  private baseUrl =
    `${environment.apiUrl}/admin`;

  constructor(
    private http: HttpClient
  ) {}

  // ==========================================
  // GET ALERTS
  // ==========================================

  getAllAlerts() {

    const token =
      localStorage.getItem('token');

    const headers =
      new HttpHeaders({

        Authorization:
        `Bearer ${token}`

      });

    return this.http.get(

      `${this.baseUrl}/alerts`,

      { headers }

    );

  }

  // ==========================================
  // AUTO ASSIGN
  // ==========================================

  autoAssign(alertId: number) {

    const token =
      localStorage.getItem('token');

    const headers =
      new HttpHeaders({

        Authorization:
        `Bearer ${token}`

      });

    return this.http.post(

      `${this.baseUrl}/alerts/${alertId}/auto-assign`,

      {},

      { headers }

    );

  }

}
