import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FaultAnalysis2Component } from './fault-analysis2.component';

describe('FaultAnalysis2Component', () => {
  let component: FaultAnalysis2Component;
  let fixture: ComponentFixture<FaultAnalysis2Component>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FaultAnalysis2Component]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FaultAnalysis2Component);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
