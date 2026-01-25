import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../../api/api';
import {AuthenticationResponse, LoginRequest} from '../../../api/models';
import { login, getMe } from '../../../api/functions';
import { AuthService } from '../../../core/auth.service';

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

      const authResponse: AuthenticationResponse = await this.api.invoke(login, { body: request });

      if (authResponse && authResponse.token) {
        const token: string = authResponse.token;

        localStorage.setItem('token', token);

        const userDto: any = await this.api.invoke(getMe);

        this.authService.login(userDto, token);
      }

    } catch (error: any) {
      localStorage.removeItem('token');

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
