import {
  Component,
  EventEmitter,
  HostListener,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ThemeService } from '../../../core/services/theme.service';
import { AuthService, UserProfile } from '../../../core/services/auth.service';
import {
  NotificationService,
  NotificationItem,
} from '../../../core/services/notification.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, AsyncPipe],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css'],
})
export class NavbarComponent implements OnInit, OnDestroy {
  @Input() showMenuToggle = false;
  @Input() portalTitle = 'Industrial Monitoring';
  @Input() navLinks: Array<{ label: string; path: string }> = [
    { label: 'Dashboard', path: '/fault-log' },
  ];
  @Output() menuToggle = new EventEmitter<void>();

  showProfileCard = false;
  showLogoutModal = false;
  showNotifications = false;

  profile: UserProfile | null = null;
  profileLoading = true;
  profileError = false;

  // Observables from NotificationService — assign in ngOnInit
  notifications$!: import('rxjs').Observable<NotificationItem[]>;
  unreadCount$!: import('rxjs').Observable<number>;

  private subscriptions = new Subscription();

  constructor(
    public themeService: ThemeService,
    private authService: AuthService,
    private notificationService: NotificationService,
  ) {}

  ngOnInit(): void {
    // initialize observables after injection
    this.notifications$ = this.notificationService.notifications$;
    this.unreadCount$ = this.notificationService.unreadCount$;
    this.profileLoading = true;
    this.subscriptions.add(
      this.authService.getProfile().subscribe({
        next: (profile) => {
          this.profile = profile;
          this.profileLoading = false;
        },
        error: () => {
          this.profileError = true;
          this.profileLoading = false;
        },
      }),
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  toggleProfileCard() {
    this.showProfileCard = !this.showProfileCard;
    if (this.showProfileCard) {
      this.showNotifications = false;
    }
  }

  onMenuToggle(event: Event): void {
    event.stopPropagation();
    this.menuToggle.emit();
  }

  toggleNotifications() {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications) {
      this.showProfileCard = false;
    }
  }

  openLogoutModal() {
    this.showLogoutModal = true;
  }

  closeLogoutModal() {
    this.showLogoutModal = false;
  }

  confirmLogout() {
    this.authService.logout();
    this.showLogoutModal = false;
    this.showProfileCard = false;
  }

  markNotificationRead(notification: NotificationItem): void {
    this.notificationService.markAsRead(notification.id);
  }

  markAllNotificationsRead(): void {
    this.notificationService.markAllRead();
  }

  getAvatarInitial(name?: string): string {
    return (name || 'U').trim().charAt(0).toUpperCase();
  }

  getRoleLabel(roleId?: number): string {
    if (roleId === 1) {
      return 'Administrator';
    }
    if (roleId === 2) {
      return 'Maintenance Engineer';
    }
    if (roleId === 3) {
      return 'Operator';
    }
    return 'User';
  }

  timeAgo(timestamp: string): string {
    const date = new Date(timestamp);
    const diffMs = Date.now() - date.getTime();
    const minutes = Math.floor(diffMs / 60000);
    if (minutes < 1) {
      return 'Just now';
    }
    if (minutes < 60) {
      return `${minutes}m ago`;
    }
    const hours = Math.floor(minutes / 60);
    if (hours < 24) {
      return `${hours}h ago`;
    }
    const days = Math.floor(hours / 24);
    return `${days}d ago`;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    const target = event.target as HTMLElement;
    if (
      !target.closest('.profile-trigger') &&
      !target.closest('.profile-dropdown')
    ) {
      this.showProfileCard = false;
    }
    if (
      !target.closest('.notification-trigger') &&
      !target.closest('.notification-dropdown')
    ) {
      this.showNotifications = false;
    }
  }
}
