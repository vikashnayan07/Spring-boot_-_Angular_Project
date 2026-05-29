import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-kpi-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './kpi-card.component.html',
  styleUrls: ['./kpi-card.component.css']
})
export class KpiCardComponent {
  @Input() title: string = 'Active Faults';
  @Input() value: string = '0';
  @Input() icon: string = '⚠️';
  @Input() trend: string = '0%';
  @Input() trendUp: boolean = true;
  @Input() color: string = 'violet';
}