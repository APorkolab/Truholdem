import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RaiseInputComponent } from './raise-input.component';

describe('RaiseInputComponent', () => {
  let component: RaiseInputComponent;
  let fixture: ComponentFixture<RaiseInputComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RaiseInputComponent]
    });
    fixture = TestBed.createComponent(RaiseInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
