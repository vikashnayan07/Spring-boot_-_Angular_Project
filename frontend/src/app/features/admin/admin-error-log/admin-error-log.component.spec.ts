import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminErrorLogComponent } from './admin-error-log.component';

describe('AdminErrorLogComponent', () => {
  let component: AdminErrorLogComponent;
  let fixture: ComponentFixture<AdminErrorLogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminErrorLogComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminErrorLogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
