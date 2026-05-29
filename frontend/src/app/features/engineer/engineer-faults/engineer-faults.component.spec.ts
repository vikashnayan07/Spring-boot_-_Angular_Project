import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EngineerFaultsComponent } from './engineer-faults.component';

describe('EngineerFaultsComponent', () => {
  let component: EngineerFaultsComponent;
  let fixture: ComponentFixture<EngineerFaultsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EngineerFaultsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EngineerFaultsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
