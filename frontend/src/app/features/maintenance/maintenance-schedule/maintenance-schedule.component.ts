import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';

import { ScheduleService } from '../../../core/services/schedule.service';

@Component({
  selector: 'app-maintenance-schedule',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './maintenance-schedule.component.html',
  // styleUrls: ['./maintenance-schedule.component.css']
})

export class MaintenanceScheduleComponent
implements OnInit {

  schedules: any[] = [];
  role = '';

  loading = true;

  constructor(
    private scheduleService : ScheduleService
  ) {}

  ngOnInit(): void {

    this.loadSchedules();
    this.role=localStorage.getItem('role')||'';

  }

  loadSchedules(): void {

    this.loading = true;

    const role =
      localStorage.getItem('role');

    // =========================
    // ADMIN
    // =========================

    if(role === 'ADMIN') {

      this.scheduleService
        .getAllSchedules()
        .subscribe({

          next: (response: any) => {

            this.schedules = response;

            this.loading = false;

          },

          error: (error) => {

            console.log(error);

            this.loading = false;

          }

        });

    }

    // =========================
    // ENGINEER
    // =========================

    else {

      this.scheduleService
        .getMySchedules()
        .subscribe({

          next: (response: any) => {

            this.schedules = response;

            this.loading = false;

          },

          error: (error) => {

            console.log(error);

            this.loading = false;

          }

        });

    }

  }

}

