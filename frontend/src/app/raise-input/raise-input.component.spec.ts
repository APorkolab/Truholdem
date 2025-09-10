import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RaiseInputComponent } from './raise-input.component';

describe('RaiseInputComponent', () => {
  let component: RaiseInputComponent;
  let fixture: ComponentFixture<RaiseInputComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [HttpClientTestingModule, RaiseInputComponent]
});
    fixture = TestBed.createComponent(RaiseInputComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
