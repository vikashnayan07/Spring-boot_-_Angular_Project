import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { NavbarComponent } from '../../shared/components/navbar/navbar.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@Component({
  selector: 'app-operator-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, NavbarComponent, FooterComponent],
  templateUrl: './operator-layout.component.html',
})
export class OperatorLayoutComponent implements OnInit {
  operatorName = '';
  isSidebarCollapsed = false;

  navItems = [
    { name: 'Error Log', path: '/operator/machines' },
    { name: 'Add Fault', path: '/operator/log-error' },
  ];

  topNavLinks = [
    { label: 'Dashboard', path: '/operator/dashboard' },
    { label: 'Machines', path: '/operator/machines' },
    { label: 'Add Fault', path: '/operator/log-error' },
  ];

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.operatorName = localStorage.getItem('name') || 'Operator';
    this.isSidebarCollapsed = window.innerWidth <= 1024;
  }

  get isDashboardRoute(): boolean {
    return this.router.url === '/operator/dashboard';
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }
}
