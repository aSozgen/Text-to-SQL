// register.component.ts

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../../api/api';
import { RegisterRequest } from '../../../api/models';
import { register } from '../../../api/functions';
import {ErrorHandlerService} from '../../../core/error.handler.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(Api);
  private readonly errorHandler = inject(ErrorHandlerService);

  registerForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  submitted = false;
  isSubmitting = false;
  errorMessage: string | null = null;
  registerSuccess = false;
  registeredEmail = '';

  get f() { return this.registerForm.controls; }

  async onSubmit() {
    this.submitted = true;
    this.errorMessage = null;

    if (this.registerForm.invalid) return;

    this.isSubmitting = true;

    try {
      const request: RegisterRequest = this.registerForm.value;
      await this.api.invoke(register, { body: request });
      
      // Clear guest session after successful registration & migration
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
      
      this.registeredEmail = this.registerForm.value.email;
      this.registerSuccess = true;
    } catch (error: any) {
      this.errorMessage = this.errorHandler.message(error);
    } finally {
      this.isSubmitting = false;
    }
  }
}
