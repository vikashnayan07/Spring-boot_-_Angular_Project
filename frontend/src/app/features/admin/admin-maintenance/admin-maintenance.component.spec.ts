import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminMaintenanceComponent } from './admin-maintenance.component';

describe('AdminMaintenanceComponent', () => {
  let component: AdminMaintenanceComponent;
  let fixture: ComponentFixture<AdminMaintenanceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminMaintenanceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminMaintenanceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
