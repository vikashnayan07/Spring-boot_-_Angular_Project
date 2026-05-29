import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './admin-layout.component.html',
})
export class AdminLayoutComponent implements OnInit {
  adminName = '';
  isSidebarCollapsed = false;

  get isDashboardRoute(): boolean {
    return this.router.url === '/admin/dashboard';
  }

  navItems = [
    { name: 'Activity Center', path: '/admin/activity-center' },
    { name: 'Error Log', path: '/admin/fault-logs' },
    { name: 'Fault Analysis', path: '/admin/fault-analysis' },
    { name: 'Maintenance', path: '/admin/maintenance' },
    { name: 'Inventory', path: '/admin/inventory' },
    { name: 'Employees', path: '/admin/employees' },
    { name: 'Machines', path: '/admin/machines' },
  ];

  topNavLinks = [
    { label: 'Dashboard', path: '/admin/dashboard' },
    { label: 'Activity Center', path: '/admin/activity-center' },
    { label: 'Fault Logs', path: '/admin/fault-logs' },
    { label: 'Analysis', path: '/admin/fault-analysis' },
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.adminName = localStorage.getItem('name') || 'Admin';
    this.isSidebarCollapsed = window.innerWidth <= 1024;
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }
}
