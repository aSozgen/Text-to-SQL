import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { Router } from '@angular/router';
import { Api } from '../../../api/api';
import { updateProfile, changePassword, getMe, deleteAccount } from '../../../api/functions';
import { UpdateProfileRequest, ChangePasswordRequest, UserDto } from '../../../api/models';
import { AuthService } from '../../../core/auth.service';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private api = inject(Api);
  private router = inject(Router);
  private authService = inject(AuthService);

  profileForm: FormGroup;
  passwordForm: FormGroup;

  isLoading = true;
  isSaving = false;
  showDeleteConfirm = false;

  successMessage: string | null = null;
  errorMessage: string | null = null;

  constructor() {
    this.profileForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: [{ value: '', disabled: true }, [Validators.required, Validators.email]],
    });

    this.passwordForm = this.fb.group({
      currentPassword: [''],
      newPassword: ['', [Validators.minLength(6)]],
      confirmPassword: ['']
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadUserData();
  }

  passwordMatchValidator(control: AbstractControl) {
    const newPassword = control.get('newPassword')?.value;
    const confirmPassword = control.get('confirmPassword')?.value;

    if (newPassword && newPassword !== confirmPassword) {
      control.get('confirmPassword')?.setErrors({ mismatch: true });
      return { mismatch: true };
    }

    const confirmControl = control.get('confirmPassword');
    if (confirmControl?.hasError('mismatch')) {
      const errors = { ...confirmControl.errors };
      delete errors['mismatch'];
      confirmControl.setErrors(Object.keys(errors).length ? errors : null);
    }

    return null;
  }

  async loadUserData() {
    this.isLoading = true;
    this.errorMessage = null;

    try {
      const user: UserDto = await this.api.invoke(getMe);

      this.profileForm.patchValue({
        username: user.username,
        email: user.email
      });
    } catch (error) {
      this.handleError(error, 'Failed to load user data.');
    } finally {
      this.isLoading = false;
    }
  }

  async onSaveAll() {
    this.successMessage = null;
    this.errorMessage = null;
    this.isSaving = true;

    let shouldLogout = false;

    try {
      if (this.profileForm.dirty && this.profileForm.valid) {
        const profileRequest: UpdateProfileRequest = {
          username: this.profileForm.value.username,
        };
        await this.api.invoke(updateProfile, { body: profileRequest });
        await this.authService.refreshUser();
      }

      const currentPass = this.passwordForm.get('currentPassword')?.value;
      const newPass = this.passwordForm.get('newPassword')?.value;

      if (currentPass && newPass) {
        if (this.passwordForm.invalid) {
          throw new Error('Please check password fields.');
        }

        const passRequest: ChangePasswordRequest = {
          currentPassword: currentPass,
          newPassword: newPass,
        };

        await this.api.invoke(changePassword, { body: passRequest });
        shouldLogout = true;
      }

      if (shouldLogout) {
        this.authService.logout();
      } else {
        this.successMessage = "Profile updated successfully.";
        this.profileForm.markAsPristine();
        this.passwordForm.reset();
      }

    } catch (error: any) {
      this.handleError(error, 'Update failed. Please try again.');
    } finally {
      this.isSaving = false;
    }
  }

  initiateAccountDeletion() {
    this.showDeleteConfirm = true;
  }

  cancelAccountDeletion() {
    this.showDeleteConfirm = false;
  }

  async confirmDeleteAccount() {
    this.isSaving = true;
    this.errorMessage = null;
    try {
      await this.api.invoke(deleteAccount);
      this.authService.logout();
    } catch (e: any) {
      this.handleError(e, 'Failed to delete account. Please try again.');
      this.showDeleteConfirm = false;
      this.isSaving = false;
    }
  }

  private handleError(error: any, defaultMessage: string) {
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
        this.errorMessage = defaultMessage;
      }
    } else if (error.message) {
      this.errorMessage = error.message;
    } else {
      this.errorMessage = 'An unexpected error occurred.';
    }
  }

  get p() { return this.profileForm.controls; }
  get pw() { return this.passwordForm.controls; }

  get isSaveDisabled(): boolean {
    if (this.isSaving || this.isLoading) return true;
    if (this.profileForm.invalid || this.passwordForm.invalid) return true;
    if (this.profileForm.pristine && this.passwordForm.pristine) return true;

    const current = this.passwordForm.get('currentPassword')?.value;
    const newP = this.passwordForm.get('newPassword')?.value;
    const confirmP = this.passwordForm.get('confirmPassword')?.value;

    if ((current && !newP) || (!current && newP)) return true;
    return !!(newP && !confirmP);
  }
}
