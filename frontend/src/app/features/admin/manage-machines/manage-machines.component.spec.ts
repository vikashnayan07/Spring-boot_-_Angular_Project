import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManageMachinesComponent } from './manage-machines.component';

describe('ManageMachinesComponent', () => {
  let component: ManageMachinesComponent;
  let fixture: ComponentFixture<ManageMachinesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageMachinesComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageMachinesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
