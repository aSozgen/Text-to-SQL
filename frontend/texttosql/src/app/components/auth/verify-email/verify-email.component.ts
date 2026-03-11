import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Api } from '../../../api/api';
import { verifyEmail, resendVerification } from '../../../api/functions';
import { ErrorHandlerService } from '../../../core/error.handler.service';

type VerifyState = 'loading' | 'success' | 'error';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss'
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(Api);
  private readonly fb = inject(FormBuilder);
  private readonly errorHandler = inject(ErrorHandlerService);

  state: VerifyState = 'loading';
  errorMessage: string | null = null;

  resendForm: FormGroup = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });
  resendSubmitted = false;
  isResending = false;
  resendSuccess = false;

  get r() { return this.resendForm.controls; }

  async ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state = 'error';
      this.errorMessage = 'Invalid verification link. No token found.';
      return;
    }
    try {
      await this.api.invoke(verifyEmail, { token });
      this.state = 'success';
    } catch (error: any) {
      this.state = 'error';
      this.errorMessage = this.errorHandler.message(error);
    }
  }

  async onResend() {
    this.resendSubmitted = true;
    if (this.resendForm.invalid) return;

    this.isResending = true;
    try {
      await this.api.invoke(resendVerification, { body: this.resendForm.value });
      this.resendSuccess = true;
    } catch (error: any) {
      this.errorMessage = this.errorHandler.message(error);
    } finally {
      this.isResending = false;
    }
  }
}
