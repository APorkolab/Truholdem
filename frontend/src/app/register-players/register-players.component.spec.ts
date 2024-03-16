import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RegisterPlayersComponent } from './register-players.component';

describe('RegisterPlayersComponent', () => {
  let component: RegisterPlayersComponent;
  let fixture: ComponentFixture<RegisterPlayersComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [RegisterPlayersComponent]
    });
    fixture = TestBed.createComponent(RegisterPlayersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
