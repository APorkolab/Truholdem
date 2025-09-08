import { Component, OnInit, EventEmitter, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, LoginRequest } from '../services/auth.service';
import { ErrorHandlerService } from '../services/error-handler.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./auth.component.scss']
})
export class LoginComponent implements OnInit {
  @Output() switchMode = new EventEmitter<string>();
  
  loginForm: FormGroup;
  isLoading = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private errorHandler: ErrorHandlerService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      rememberMe: [false]
    });
  }

  ngOnInit(): void {
    // Check if already authenticated
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/game']);
    }
  }

  onLogin(): void {
    if (this.loginForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.isLoading = true;
    const loginRequest: LoginRequest = {
      username: this.loginForm.value.username,
      password: this.loginForm.value.password
    };

    this.authService.login(loginRequest).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.errorHandler.addSuccess('Login successful!', `Welcome back, ${response.username}!`);
        this.router.navigate(['/game']);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorHandler.handleHttpError(error);
        
        // Clear password on failed login
        this.loginForm.patchValue({ password: '' });
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  switchToRegister(): void {
    this.switchMode.emit('register');
  }

  private markFormGroupTouched(): void {
    Object.keys(this.loginForm.controls).forEach(key => {
      const control = this.loginForm.get(key);
      if (control) {
        control.markAsTouched();
      }
    });
  }

  // Social login methods (placeholder for future implementation)
  loginWithGoogle(): void {
    this.errorHandler.addInfo('Social login coming soon!');
  }

  loginWithGitHub(): void {
    this.errorHandler.addInfo('Social login coming soon!');
  }
}
