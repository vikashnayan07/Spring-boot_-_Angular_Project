import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';

@Component({
  selector: 'app-maintenance-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule 
  ],
  templateUrl: './maintenance-landing.component.html',
  styleUrls: ['./maintenance-landing.component.css']
})
export class MaintenanceLandingComponent implements OnInit {
  
  // Default to admin, but we will update this in ngOnInit!
  baseRoute = '/admin'; 

  constructor(private router: Router) {}

  ngOnInit() {
    // 👉 Automatically detect who is logged in based on the current URL
    if (this.router.url.includes('/engineer')) {
      this.baseRoute = '/engineer';
    } else if (this.router.url.includes('/operator')) {
      this.baseRoute = '/operator';
    } else {
      this.baseRoute = '/admin';
    }
  }

  // 👉 The new dynamic Back Button logic
  goBack(): void {
    this.router.navigate([this.baseRoute, 'dashboard']);
  }

  // 👉 The dynamic Card navigation logic
  navigateTo(page: string): void {
    this.router.navigate([this.baseRoute, page]);
  }
}
