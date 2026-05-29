import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

import { InventoryItem } from '../../features/inventory/inventory.model';

export interface UsageHistory {
  usageId: number;
  partId: number;
  empId: number;
  qtyAssigned: number;
  lastUpdated: string;
  scheduleId: number | null;
  historyId: number | null;
}

export interface ReorderRecommendation {
  machineId: string;
  machineName: string;
  partName: string;
  requestedQty: number;
  availableQty: number;
  engineerId: number;
  engineerName: string;
  requestTimestamp: string;
  status: string;
}

@Injectable({
  providedIn: 'root',
})
export class InventoryService {
  // ==========================
  // API URLS
  // ==========================
  private inventoryUrl = `${environment.apiUrl}/inventory`;

  private adminUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  // ==========================
  // GET ALL PARTS
  // ==========================
  getAllParts(): Observable<InventoryItem[]> {
    return this.http.get<InventoryItem[]>(`${this.inventoryUrl}/parts`);
  }

  // ==========================
  // GET PART BY ID
  // ==========================
  getPartById(partId: number): Observable<InventoryItem> {
    return this.http.get<InventoryItem>(`${this.inventoryUrl}/parts/${partId}`);
  }

  // ==========================
  // GET USAGE HISTORY
  // ==========================
  getUsageHistory(): Observable<UsageHistory[]> {
    return this.http.get<UsageHistory[]>(`${this.inventoryUrl}/usage-history`);
  }

  getReorderRecommendations(): Observable<ReorderRecommendation[]> {
    return this.http.get<ReorderRecommendation[]>(
      `${this.adminUrl}/inventory/reorder-recommendations`,
    );
  }

  // ==========================
  // ADMIN: CREATE PART
  // ==========================
  createPart(payload: any): Observable<any> {
    return this.http.post(`${this.adminUrl}/parts`, payload);
  }

  // ==========================
  // ADMIN: UPDATE STOCK
  // ==========================
  updateStock(partId: number, newStock: number): Observable<any> {
    return this.http.put(`${this.adminUrl}/parts/${partId}/stock`, {
      newStock,
    });
  }

  // ==========================
  // ADMIN: USE PART
  // (Later)
  // ==========================
  usePart(payload: any): Observable<any> {
    return this.http.post(`${this.adminUrl}/parts/use`, payload);
  }

  getAllMachines(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/admin/machines`);
  }
}
