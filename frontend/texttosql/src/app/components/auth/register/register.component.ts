import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Api } from '../../../api/api';
import { RegisterRequest } from '../../../api/models';
import { register } from '../../../api/functions';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly api = inject(Api);

  registerForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  submitted = false;
  isSubmitting = false;
  errorMessage: string | null = null;

  get f() {
    return this.registerForm.controls;
  }

  async onSubmit() {
    this.submitted = true;
    this.errorMessage = null;

    if (this.registerForm.invalid) {
      return;
    }

    this.isSubmitting = true;

    try {
      const request: RegisterRequest = this.registerForm.value;
      
      await this.api.invoke(register, { body: request });
      
      this.router.navigate(['/login']);

    } catch (error: any) {
      console.error('API Error:', error);

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
          this.errorMessage = 'Registration failed. Please try again.';
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