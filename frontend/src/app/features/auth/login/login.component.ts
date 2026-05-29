import { Component, OnInit, Renderer2 } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import {
  animate,
  query,
  stagger,
  style,
  transition,
  trigger,
} from '@angular/animations';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  animations: [
    trigger('pageIntro', [
      transition(':enter', [
        query(
          '.motion-item',
          [
            style({ opacity: 0, transform: 'translateY(22px) scale(0.98)' }),
            stagger(80, [
              animate(
                '620ms cubic-bezier(0.16, 1, 0.3, 1)',
                style({ opacity: 1, transform: 'translateY(0) scale(1)' }),
              ),
            ]),
          ],
          { optional: true },
        ),
      ]),
    ]),
  ],
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMsg = '';
  isLoading = false;
  isDark = true;
  showPassword = false;
  glowX = 50;
  glowY = 50;

  readonly stats = [
    { label: 'Downtime reduced', value: '38%' },
    { label: 'Assets monitored', value: '12.8k' },
    { label: 'Fault signals/day', value: '1.6M' },
  ];

  readonly telemetry = [
    { label: 'Hydraulic pressure', value: '94%', tone: 'cyan' },
    { label: 'Bearing vibration', value: 'Low', tone: 'emerald' },
    { label: 'Thermal anomaly', value: 'Watch', tone: 'orange' },
  ];

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private renderer: Renderer2,
    private api: ApiService,
    private toastService: ToastService,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required],
      remember: [true],
    });
  }

  ngOnInit(): void {
    const savedTheme =
      localStorage.getItem('theme') || localStorage.getItem('machcare-theme');
    this.isDark = savedTheme !== 'light';
    this.applyTheme();

    const rememberedEmail = localStorage.getItem('machcare-login-email');
    if (rememberedEmail) {
      this.loginForm.patchValue({ email: rememberedEmail, remember: true });
    }
  }

  get f() {
    return this.loginForm.controls;
  }

  get loginGreeting(): string {
    const hour = new Date().getHours();
    if (hour >= 5 && hour < 12) return 'Good Morning';
    if (hour >= 12 && hour < 18) return 'Good Afternoon';
    return 'Good Evening';
  }

  get loginSubtitle(): string {
    return 'Welcome to MachCare Industrial Intelligence Platform';
  }

  toggleTheme(): void {
    this.isDark = !this.isDark;
    localStorage.setItem('theme', this.isDark ? 'dark' : 'light');
    localStorage.setItem('machcare-theme', this.isDark ? 'dark' : 'light');
    this.applyTheme();
  }

  applyTheme(): void {
    this.renderer.setAttribute(
      document.documentElement,
      'data-theme',
      this.isDark ? 'dark' : 'light',
    );
    if (this.isDark) {
      this.renderer.addClass(document.documentElement, 'dark');
    } else {
      this.renderer.removeClass(document.documentElement, 'dark');
    }
  }

  onPointerMove(event: MouseEvent): void {
    const target = event.currentTarget as HTMLElement;
    const rect = target.getBoundingClientRect();
    this.glowX = ((event.clientX - rect.left) / rect.width) * 100;
    this.glowY = ((event.clientY - rect.top) / rect.height) * 100;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  goBack(): void {
    this.router.navigate(['/']);
  }

  onLogin(): void {
    this.loginForm.markAllAsTouched();
    if (this.loginForm.invalid) {
      this.toastService.warning(
        'Check your details',
        'Enter a valid email and password to continue.',
      );
      return;
    }

    this.isLoading = true;
    this.errorMsg = '';

    const { email, password, remember } = this.loginForm.value;

    this.api.login({ email, password }).subscribe({
      next: (res: any) => {
        this.isLoading = false;

        localStorage.setItem('token', res.token);
        if (res.name) {
          localStorage.setItem('name', res.name);
        }
        if (res.roleId !== undefined && res.roleId !== null) {
          localStorage.setItem('roleId', String(res.roleId));
        }

        if (remember) {
          localStorage.setItem('machcare-login-email', email);
        } else {
          localStorage.removeItem('machcare-login-email');
        }

        this.toastService.success(
          'Login successful',
          'MachCare workspace is ready.',
        );

        if (res.isFirstLogin === true || res.isFirstLogin === 'true') {
          this.router.navigate(['/setup-account']);
        } else if (res.roleId === 1) {
          this.router.navigate(['/admin/dashboard']);
        } else if (res.roleId === 2) {
          this.router.navigate(['/engineer/dashboard']);
        } else if (res.roleId === 3) {
          this.router.navigate(['/operator/dashboard']);
        } else {
          this.errorMsg = 'Unrecognized user role.';
          this.toastService.error(
            'Role unavailable',
            'Your account role is not configured for this workspace.',
          );
        }
      },
      error: (err: any) => {
        this.isLoading = false;
        const status = err?.status;
        this.errorMsg = err.error?.message || 'Invalid Email ID or Password';

        if (status === 0 || status >= 500) {
          this.toastService.error(
            'Server unavailable',
            'MachCare API is not reachable. Please try again.',
          );
        } else {
          this.toastService.error(
            'Invalid credentials',
            'Email or password is incorrect.',
          );
        }
      },
    });
  }
}
