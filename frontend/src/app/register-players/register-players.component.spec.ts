import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RegisterPlayersComponent } from './register-players.component';
import { FormsModule } from '@angular/forms';

describe('RegisterPlayersComponent', () => {
  let component: RegisterPlayersComponent;
  let fixture: ComponentFixture<RegisterPlayersComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [HttpClientTestingModule, FormsModule, RegisterPlayersComponent]
});
    fixture = TestBed.createComponent(RegisterPlayersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
