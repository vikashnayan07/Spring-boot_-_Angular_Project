import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MaintenanceAlertComponent } from './maintenance-alert.component';

describe('MaintenanceAlertComponent', () => {
  let component: MaintenanceAlertComponent;
  let fixture: ComponentFixture<MaintenanceAlertComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaintenanceAlertComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MaintenanceAlertComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
