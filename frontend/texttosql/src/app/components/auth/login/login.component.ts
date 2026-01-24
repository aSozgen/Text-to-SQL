import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

// API importları (ng-openapi-gen çıktılarına göre)
import { Api } from '../../../api/api';
import { LoginRequest, AuthenticationResponse } from '../../../api/models';
import { login } from '../../../api/functions';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly api = inject(Api);

  loginForm: FormGroup = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  submitted = false;
  isSubmitting = false;
  errorMessage: string | null = null;

  get f() {
    return this.loginForm.controls;
  }

  async onSubmit() {
    this.submitted = true;
    this.errorMessage = null;

    if (this.loginForm.invalid) {
      return;
    }

    this.isSubmitting = true;

    try {
      const request: LoginRequest = this.loginForm.value;
      
      const response = await this.api.invoke(login, { body: request });
      
      if (response && response.token) {
        localStorage.setItem('token', response.token);
        
        // Opsiyonel: Kullanıcı bilgisini de saklayabilir veya state management'a atabilirsiniz
        // localStorage.setItem('currentUser', JSON.stringify(response)); 
      }

      this.router.navigate(['/home']);

    } catch (error: any) {
      console.error('Login Error:', error);

      if (error.error) {
        if (typeof error.error === 'string') {
          try {
            const parsedError = JSON.parse(error.error);
            this.errorMessage = parsedError.message || error.error;
          } catch (e) {
            this.errorMessage = error.error;
          }
        } else if (typeof error.error === 'object' && error.error.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Login failed. Please check your credentials.';
        }
      } else if (error.message) {
        this.errorMessage = error.message;
      } else {
        this.errorMessage = 'An unexpected error occurred.';
      }
    } finally {
      this.isSubmitting = false;
    }
  }
}