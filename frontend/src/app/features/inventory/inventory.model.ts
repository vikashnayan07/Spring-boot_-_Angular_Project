export interface InventoryItem {
  partId: number;
  partName: string;
  currentStock: number;

  categoryId?: number;
  machineId?: string;
  minStock?: number;
  manufactureDate?: string;
  expiryDate?: string;
  warrantyExpiryDate?: string;
  shelfLifeDays?: number;
  conditionStatus?: string;
  lifecycleStatus?: string;
}
