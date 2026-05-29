import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EngineerLayoutComponent } from './engineer-layout.component';

describe('EngineerLayoutComponent', () => {
  let component: EngineerLayoutComponent;
  let fixture: ComponentFixture<EngineerLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EngineerLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(EngineerLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
