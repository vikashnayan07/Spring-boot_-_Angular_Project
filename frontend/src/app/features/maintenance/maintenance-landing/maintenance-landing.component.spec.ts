import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MaintenanceLandingComponent } from './maintenance-landing.component';

describe('MaintenanceLandingComponent', () => {
  let component: MaintenanceLandingComponent;
  let fixture: ComponentFixture<MaintenanceLandingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MaintenanceLandingComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MaintenanceLandingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
