// login.component.ts

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../../api/api';
import { AuthenticationResponse, LoginRequest } from '../../../api/models';
import { login, getMe, resendVerification } from '../../../api/functions';
import { AuthService } from '../../../core/auth.service';
import {ErrorHandlerService} from '../../../core/error.handler.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(Api);
  private readonly authService = inject(AuthService);
  private readonly errorHandler = inject(ErrorHandlerService);

  loginForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  submitted = false;
  isSubmitting = false;
  errorMessage: string | null = null;
  showResendVerification = false;
  isResending = false;
  resendSuccess = false;

  get f() { return this.loginForm.controls; }

  async onSubmit() {
    this.submitted = true;
    this.errorMessage = null;
    this.showResendVerification = false;
    this.resendSuccess = false;

    if (this.loginForm.invalid) return;

    this.isSubmitting = true;

    try {
      const request: LoginRequest = this.loginForm.value;
      const authResponse: AuthenticationResponse = await this.api.invoke(login, { body: request });

      if (authResponse?.token) {
        localStorage.setItem('token', authResponse.token);
        const userDto: any = await this.api.invoke(getMe, { Authorization: '' });
        this.authService.login(userDto, authResponse);
      }

    } catch (error: any) {
      localStorage.removeItem('token');
      const parsed = this.errorHandler.parse(error);

      if (parsed.isUnauthorized && parsed.message.toLowerCase().includes('email not verified')) {
        this.errorMessage = null;
        this.showResendVerification = true;
      } else {
        this.errorMessage = parsed.message;
      }
    } finally {
      this.isSubmitting = false;
    }
  }

  async onResendVerification() {
    const email = this.f['email'].value;
    if (!email) return;

    this.isResending = true;
    this.resendSuccess = false;

    try {
      await this.api.invoke(resendVerification, { body: { email } });
      this.resendSuccess = true;
    } catch {
      // Silently fail
    } finally {
      this.isResending = false;
    }
  }
}
