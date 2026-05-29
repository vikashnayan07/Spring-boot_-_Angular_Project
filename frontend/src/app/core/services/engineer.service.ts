import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class EngineerService {

  baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  // ==========================================
  // DASHBOARD
  // ==========================================
  
  getDashboardStats(): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/dashboard`
    );
  }

  // ==========================================
  // TASK MANAGEMENT
  // ==========================================

  getMyTasks(): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/tasks`
    );
  }

  updateStatus(scheduleId: number, status: string, remarks: string): Observable<any> {
    // encodeURIComponent safely handles spaces and special characters in the remarks!
    const safeRemarks = encodeURIComponent(remarks);
    return this.http.put(
      `${this.baseUrl}/engineer/schedules/${scheduleId}/status?status=${status}&remarks=${safeRemarks}`,
      {}
    );
  }

  requestParts(scheduleId: number, parts: any[]): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/engineer/schedules/${scheduleId}/parts`,
      parts // Sends the array of PartRequestDTOs as the JSON body
    );
  }

  getRaiseRequestContext(scheduleId: number): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/schedules/${scheduleId}/raise-context`
    );
  }

  raiseFaultFromTask(scheduleId: number, payload: any): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/engineer/schedules/${scheduleId}/raise-fault`,
      payload
    );
  }

  requestAdditionalSupport(scheduleId: number, payload: any): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/engineer/schedules/${scheduleId}/support-request`,
      payload
    );
  }

  // ==========================================
  // FAULT LOGS & ALERTS
  // ==========================================

  getFaults(): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/faults`
    );
  }

  getPendingFaultQueue(): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/faults/pending`
    );
  }

  analyzeFault(faultId: string): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/engineer/faults/${faultId}/analyze`,
      {}
    );
  }

  getAlerts(): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/alerts`
    );
  }

  generateAlert(analysisId: number): Observable<any> {
    return this.http.post(
      `${this.baseUrl}/engineer/alerts/generate/${analysisId}`,
      {}
    );
  }

  // ==========================================
  // MAINTENANCE HISTORY
  // ==========================================

  getHistory(): Observable<any> {
    return this.http.get(
      `${this.baseUrl}/engineer/history`
    );
  }

  // ==========================================
  // ADMIN CROSSOVER METHODS
  // ==========================================

  getAllSchedules(): Observable<any> {
    // Note: Reaching into the /admin API route from the engineer service!
    return this.http.get(
      `${this.baseUrl}/admin/schedules/all`
    );
  }

}
