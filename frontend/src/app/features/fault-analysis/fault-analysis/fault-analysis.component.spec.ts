import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FaultAnalysisComponent } from './fault-analysis.component';

describe('FaultAnalysisComponent', () => {
  let component: FaultAnalysisComponent;
  let fixture: ComponentFixture<FaultAnalysisComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FaultAnalysisComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FaultAnalysisComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
