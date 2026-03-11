import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Api } from '../../../api/api';
import { forgotPassword } from '../../../api/functions';
import { ErrorHandlerService } from '../../../core/error.handler.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.scss'
})
export class ForgotPasswordComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(Api);
  private readonly errorHandler = inject(ErrorHandlerService);

  forgotForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  formSubmitted = false;
  isSubmitting = false;
  submitted = false;
  sentToEmail = '';
  errorMessage: string | null = null;

  get f() { return this.forgotForm.controls; }

  async onSubmit() {
    this.formSubmitted = true;
    this.errorMessage = null;
    if (this.forgotForm.invalid) return;

    this.isSubmitting = true;
    this.sentToEmail = this.forgotForm.value.email;
    try {
      await this.api.invoke(forgotPassword, { body: this.forgotForm.value });
      this.submitted = true;
    } catch (error: any) {
      const parsed = this.errorHandler.parse(error);
      if (parsed.isServerError || parsed.isNetworkError) {
        this.errorMessage = parsed.message;
      } else {
        this.submitted = true; // email enumeration koruması
      }
    } finally {
      this.isSubmitting = false;
    }
  }

  onTryAgain() {
    this.submitted = false;
    this.formSubmitted = false;
    this.forgotForm.reset();
  }
}
