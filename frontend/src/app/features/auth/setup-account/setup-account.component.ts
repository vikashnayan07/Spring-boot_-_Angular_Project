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
import { Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

export function passwordPolicyValidator(): ValidatorFn {
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

@Component({
  selector: 'app-setup-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './setup-account.component.html',
})
export class SetupAccountComponent {
  setupForm: FormGroup;
  isSubmitting = false;

  constructor(
    private fb: FormBuilder,
    private api: ApiService,
    private router: Router,
    private toastService: ToastService,
  ) {
    this.setupForm = this.fb.group({
      newPassword: ['', [Validators.required, passwordPolicyValidator()]],
      q1: ["What is your mother's maiden name?", Validators.required],
      a1: ['', Validators.required],
      q2: ['What city were you born in?', Validators.required],
      a2: ['', Validators.required],
    });
  }

  get passwordChecks() {
    const value = `${this.setupForm.get('newPassword')?.value || ''}`;
    return [
      { label: '8 characters', valid: value.length >= 8 },
      { label: 'Uppercase', valid: /[A-Z]/.test(value) },
      { label: 'Lowercase', valid: /[a-z]/.test(value) },
      { label: 'Number', valid: /\d/.test(value) },
      { label: 'Special character', valid: /[^A-Za-z0-9]/.test(value) },
    ];
  }

  get passwordStrength(): number {
    return this.passwordChecks.filter((check) => check.valid).length * 20;
  }

  onSubmit(): void {
    this.setupForm.markAllAsTouched();
    if (this.setupForm.invalid) {
      this.toastService.warning('Secure password required', 'Complete the password checklist and security answers.');
      return;
    }

    this.isSubmitting = true;
    this.api.setupAccount(this.setupForm.value).subscribe({
      next: () => {
        this.toastService.success('Account secured', 'Please log in again with your new password.');
        localStorage.clear();
        this.router.navigate(['/login']);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.toastService.error('Setup failed', error?.error?.message || 'Unable to secure this account.');
      },
    });
  }
}
