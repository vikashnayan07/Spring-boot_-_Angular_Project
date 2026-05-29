import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})

export class HistoryService {

  private apiUrl =
    `${environment.apiUrl}/admin/history`;

  constructor(
    private http: HttpClient
  ) {}

  getAllHistory(): Observable<any> {

    const token =
      localStorage.getItem('token');

    return this.http.get(

      `${this.apiUrl}`,

      {
        headers: {
          Authorization:
            `Bearer ${token}`
        }
      }

    );

  }

}
