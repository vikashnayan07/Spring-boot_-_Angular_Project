import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AnalysisService {
  baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllAnalysis() {
    return this.http.get(`${this.baseUrl}/fault-analysis`);
  }

  generateAnalysis(faultId: string) {
    return this.http.post(
      `${this.baseUrl}/fault-analysis/generate/${faultId}`,
      {},
      {
        headers: new HttpHeaders({
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        }),
      },
    );
  }
}
