import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

function passwordPolicyValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value || ''}`;
    if (!value) return null;
    const valid =
      value.length >= 8 &&
      /[A-Z]/.test(value) &&
      /[a-z]/.test(value) &&
      /\d/.test(value) &&
      /[^A-Za-z0-9]/.test(value);
    return valid ? null : { passwordPolicy: true };
  };
}

function passwordMatchValidator(): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const password = group.get('newPassword')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return password && confirm && password !== confirm ? { passwordMismatch: true } : null;
  };
}

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
})
export class ForgotPasswordComponent {
  step = 1;
  emailForm: FormGroup;
  resetForm: FormGroup;

  questions = { q1: '', q2: '' };
  errorMsg = '';
  successMsg = '';
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private router: Router,
    private toastService: ToastService,
  ) {
    this.emailForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
    });

    this.resetForm = this.fb.group(
      {
        a1: ['', Validators.required],
        a2: ['', Validators.required],
        newPassword: ['', [Validators.required, passwordPolicyValidator()]],
        confirmPassword: ['', Validators.required],
      },
      { validators: passwordMatchValidator() },
    );
  }

  get passwordChecks() {
    const value = `${this.resetForm.get('newPassword')?.value || ''}`;
    return [
      { label: '8 characters', valid: value.length >= 8 },
      { label: 'Uppercase', valid: /[A-Z]/.test(value) },
      { label: 'Lowercase', valid: /[a-z]/.test(value) },
      { label: 'Number', valid: /\d/.test(value) },
      { label: 'Special character', valid: /[^A-Za-z0-9]/.test(value) },
      { label: 'Passwords match', valid: !this.resetForm.errors?.['passwordMismatch'] && !!this.resetForm.get('confirmPassword')?.value },
    ];
  }

  onEmailSubmit(): void {
    this.emailForm.markAllAsTouched();
    if (this.emailForm.invalid) return;
    this.isLoading = true;
    this.errorMsg = '';

    this.api.getSecurityQuestions(this.emailForm.value.email).subscribe({
      next: (res: any) => {
        this.questions = res.data || res;
        this.step = 2;
        this.isLoading = false;
        this.toastService.info('Account verified', 'Answer your security questions to continue.');
      },
      error: (err) => {
        this.errorMsg = err.error?.message || 'Email not found.';
        this.toastService.error('Verification failed', this.errorMsg);
        this.isLoading = false;
      },
    });
  }

  onResetSubmit(): void {
    this.resetForm.markAllAsTouched();
    if (this.resetForm.invalid) {
      this.toastService.warning('Validation required', 'Check answers and password requirements.');
      return;
    }

    this.isLoading = true;
    this.errorMsg = '';

    const payload = {
      email: this.emailForm.value.email,
      a1: this.resetForm.value.a1,
      a2: this.resetForm.value.a2,
      newPassword: this.resetForm.value.newPassword,
    };

    this.api.resetPassword(payload).subscribe({
      next: (res: any) => {
        this.successMsg = res.message || 'Password reset successfully.';
        this.toastService.success('Password reset', 'Redirecting to login.');
        this.isLoading = false;
        setTimeout(() => this.router.navigate(['/login']), 1600);
      },
      error: (err) => {
        this.errorMsg = err.error?.message || 'Failed to reset password.';
        this.toastService.error('Reset failed', this.errorMsg);
        this.isLoading = false;
      },
    });
  }
}
