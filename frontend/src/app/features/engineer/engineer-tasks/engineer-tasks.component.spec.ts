import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngineerService } from '../../../core/services/engineer.service';

@Component({
  selector: 'app-engineer-tasks',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './engineer-tasks.component.html',
  styleUrls: ['./engineer-tasks.component.css']
})
export class EngineerTasksComponent implements OnInit {

  tasks: any[] = [];
  loading = true;

  constructor(private engineerService: EngineerService) {}

  ngOnInit(): void {
    this.loadMyTasks();
  }

  loadMyTasks(): void {
    this.loading = true;
    
    this.engineerService.getMyTasks().subscribe({
      next: (response: any) => {
        // Drop the secure data into your HTML array!
        this.tasks = response.data || [];
        this.loading = false;
      },
      error: (error) => {
        console.error('Failed to load tasks:', error);
        this.loading = false;
      }
    });
  }
}

