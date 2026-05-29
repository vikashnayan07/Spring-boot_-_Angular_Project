import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@Component({
  selector: 'app-engineer-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './engineer-layout.component.html',
})
export class EngineerLayoutComponent implements OnInit {
  engineerName = '';
  isSidebarCollapsed = false;

  navItems = [
    { name: 'My Tasks', path: '/engineer/tasks' },
    { name: 'Fault Analysis', path: '/engineer/analysis' },
    { name: 'History', path: '/engineer/history' },
    { name: 'Raise Request', path: '/engineer/raise-request' },
  ];

  topNavLinks = [
    { label: 'Dashboard', path: '/engineer/dashboard' },
    { label: 'Tasks', path: '/engineer/tasks' },
    { label: 'Fault Analysis', path: '/engineer/analysis' },
    { label: 'Raise Request', path: '/engineer/raise-request' },
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.engineerName = localStorage.getItem('name') || 'Engineer';
    this.isSidebarCollapsed = window.innerWidth <= 1024;
  }

  get isDashboardRoute(): boolean {
    return this.router.url === '/engineer/dashboard';
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }
}
