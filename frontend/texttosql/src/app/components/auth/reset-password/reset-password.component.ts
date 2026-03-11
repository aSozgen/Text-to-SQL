import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  AbstractControl,
  ValidationErrors
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Api } from '../../../api/api';
import { resetPassword } from '../../../api/functions';
import { ErrorHandlerService } from '../../../core/error.handler.service';

type TokenState = 'valid' | 'invalid';

interface PasswordStrength {
  class: string;
  label: string;
  width: string;
}

function passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
  const pass = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  return pass && confirm && pass !== confirm ? { mismatch: true } : null;
}

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss'
})
export class ResetPasswordComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(Api);
  private readonly fb = inject(FormBuilder);
  private readonly errorHandler = inject(ErrorHandlerService);

  tokenState: TokenState = 'valid';
  tokenError: string | null = null;
  private token = '';

  resetForm: FormGroup = this.fb.group(
    {
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', Validators.required]
    },
    { validators: passwordMatchValidator }
  );

  submitted = false;
  isSubmitting = false;
  resetSuccess = false;
  showPassword = false;
  errorMessage: string | null = null;

  get f() { return this.resetForm.controls; }

  get passwordStrength(): PasswordStrength {
    const val: string = this.f['newPassword'].value ?? '';
    const score =
      (val.length >= 8 ? 1 : 0) +
      (/[A-Z]/.test(val) ? 1 : 0) +
      (/[0-9]/.test(val) ? 1 : 0) +
      (/[^A-Za-z0-9]/.test(val) ? 1 : 0);

    if (score <= 1) return { class: 'weak',   label: 'Weak',   width: '25%' };
    if (score === 2) return { class: 'fair',   label: 'Fair',   width: '50%' };
    if (score === 3) return { class: 'good',   label: 'Good',   width: '75%' };
    return              { class: 'strong', label: 'Strong', width: '100%' };
  }

  ngOnInit() {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.tokenState = 'invalid';
      this.tokenError = 'Invalid reset link. No token found.';
    }
  }

  async onSubmit() {
    this.submitted = true;
    this.errorMessage = null;
    if (this.resetForm.invalid) return;

    this.isSubmitting = true;
    try {
      await this.api.invoke(resetPassword, {
        body: { token: this.token, newPassword: this.f['newPassword'].value }
      });
      this.resetSuccess = true;
    } catch (error: any) {
      const parsed = this.errorHandler.parse(error);
      if (parsed.isGone) {
        this.tokenState = 'invalid';
        this.tokenError = 'This reset link has expired. Please request a new one.';
      } else if (parsed.isConflict) {
        this.tokenState = 'invalid';
        this.tokenError = 'This reset link has already been used. Please request a new one.';
      } else {
        this.errorMessage = parsed.message;
      }
    } finally {
      this.isSubmitting = false;
    }
  }
}
