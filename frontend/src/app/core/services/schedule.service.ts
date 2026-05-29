import { Injectable } from '@angular/core';

import {
  HttpClient,
  HttpHeaders
} from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})

export class ScheduleService {

  private baseUrl =
    environment.apiUrl;

  constructor(
    private http: HttpClient
  ) {}

  // =========================
  // TOKEN
  // =========================

  private getHeaders() {

    const token =
      localStorage.getItem('token');

    return {

      headers: new HttpHeaders({

        Authorization:
          `Bearer ${token}`

      })

    };

  }

  // =========================
  // ADMIN -> ALL SCHEDULES
  // =========================

  getAllSchedules() {

    return this.http.get(

      `${this.baseUrl}/admin/schedules/all`,

      this.getHeaders()

    );

  }

  // =========================
  // ENGINEER -> MY TASKS
  // =========================

  getMySchedules() {

    return this.http.get(

      `${this.baseUrl}/engineer/my-tasks`,

      this.getHeaders()

    );

  }

}
