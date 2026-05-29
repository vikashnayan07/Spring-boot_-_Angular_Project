import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminFaultAnalysisComponent } from './admin-fault-analysis.component';

describe('AdminFaultAnalysisComponent', () => {
  let component: AdminFaultAnalysisComponent;
  let fixture: ComponentFixture<AdminFaultAnalysisComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminFaultAnalysisComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminFaultAnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
