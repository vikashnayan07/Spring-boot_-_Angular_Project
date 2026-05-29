import { ComponentFixture, TestBed } from '@angular/core/testing';

import { OperatorLayoutComponent } from './operator-layout.component';

describe('OperatorLayoutComponent', () => {
  let component: OperatorLayoutComponent;
  let fixture: ComponentFixture<OperatorLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OperatorLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(OperatorLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
