import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private baseUrl = environment.apiUrl;
  constructor(private http: HttpClient) {}

  getAdminDashboard(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/dashboard`);
  }
  addEmployee(data: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/add-employee`, data);
  }
  getEngineerDashboard(): Observable<any> {
    return this.http.get(`${this.baseUrl}/engineer/dashboard`);
  }
  getOperatorDashboard(): Observable<any> {
    const headers = new HttpHeaders({
      Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
    });
    return this.http.get(`${this.baseUrl}/operator/faults/dashboard`, {
      headers,
    });
  }

  // Add these right below your existing admin APIs:
  getAllEmployees(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/employees`);
  }

  getActiveEngineers(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/employees/active`);
  }

  deleteEmployee(empId: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/admin/employees/${empId}`);
  }

  disableEmployee(empId: number, days: number): Observable<any> {
    // Assuming your Spring Boot endpoint uses @RequestParam like we saw in your Postman errors earlier!
    return this.http.put(
      `${this.baseUrl}/admin/disable-account?empId=${empId}&days=${days}`,
      {},
    );
  }

  // ==========================================
  // AUTHENTICATION
  // ==========================================
  login(credentials: any): Observable<any> {
    // 👉 NOTE: Check your Spring Boot AuthController!
    // If your login endpoint is just "/login", remove the "/api/auth" part.
    return this.http.post(`${this.baseUrl}/auth/login`, credentials);
  }

  // ==========================================
  // 👉 MACHINE MANAGEMENT APIs
  // ==========================================
  private machineUrl = `${this.baseUrl}/admin/machines`;

  // 1. Get all machines
  getAllMachines() {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.get(this.machineUrl, { headers });
  }

  // 2. Add a new machine
  addMachine(machineData: any) {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.post(this.machineUrl, machineData, { headers });
  }

  updateMachine(id: string, machineData: any) {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.put(`${this.machineUrl}/${id}`, machineData, { headers });
  }

  // 3. Get machine by ID (for future use)
  getMachineById(id: string) {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.get(`${this.machineUrl}/${id}`, { headers });
  }

  // ==========================================
  // 👉 MAINTENANCE (ENGINEER) APIs
  // ==========================================

  private engineerUrl = `${this.baseUrl}/engineer`;

  // 1. Get assigned tasks for the logged-in engineer
  getMyTasks() {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.get(`${this.engineerUrl}/my-tasks`, { headers });
  }

  // 2. Get all fault logs (For future use when you build the Faults page!)
  getFaultLogs() {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.get(`${this.engineerUrl}/faults`, { headers });
  }

  setupAccount(data: any) {
    const headers = {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
    };
    return this.http.post(`${this.baseUrl}/auth/setup-account`, data, {
      headers,
    });
  }

  getSecurityQuestions(email: string) {
    return this.http.get(
      `${this.baseUrl}/auth/security-questions?email=${email}`,
    );
  }

  resetPassword(data: any) {
    return this.http.post(`${this.baseUrl}/auth/reset-password`, data);
  }
}
