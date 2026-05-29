import {
  Component,
  AfterViewInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ViewChild,
  ElementRef,
  Input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { ThemeService } from '../../../../core/services/theme.service';
import {
  Chart,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  LineController,
  DoughnutController,
  Tooltip,
  Legend,
  Filler,
} from 'chart.js';

Chart.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  ArcElement,
  LineController,
  DoughnutController,
  Tooltip,
  Legend,
  Filler,
);

@Component({
  selector: 'app-charts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './charts.component.html',
  styleUrls: ['./charts.component.css'],
})
export class ChartsComponent implements AfterViewInit, OnDestroy, OnChanges {
  @Input() data: any[] = [];
  @Input() isLoading = false;

  @ViewChild('lineChart', { static: false })
  lineChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('doughnutChart', { static: false })
  doughnutChartRef!: ElementRef<HTMLCanvasElement>;

  lineChart: Chart | null = null;
  doughnutChart: Chart | null = null;
  totalFaults = 0;
  selectedPeriod = 10;
  periodOptions = [5, 10, 15];

  private themeSub!: Subscription;
  private isDark = true;
  private viewReady = false;

  constructor(public themeService: ThemeService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['data'] || changes['isLoading']) && this.viewReady) {
      this.queueRender();
    }
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.themeSub = this.themeService.isDark$.subscribe((dark: boolean) => {
      this.isDark = dark;
      this.queueRender();
    });

    this.queueRender();
  }

  ngOnDestroy(): void {
    if (this.themeSub) {
      this.themeSub.unsubscribe();
    }
    this.destroyCharts();
  }

  setPeriod(days: number): void {
    this.selectedPeriod = days;
    this.queueRender();
  }

  private queueRender(): void {
    window.requestAnimationFrame(() => this.renderCharts());
  }

  private renderCharts(): void {
    const data = Array.isArray(this.data) ? this.data : [];
    if (this.isLoading) {
      return;
    }
    if (!this.lineChartRef || !this.doughnutChartRef) {
      return;
    }
    if (!data.length) {
      this.totalFaults = 0;
      this.destroyCharts();
      return;
    }

    this.totalFaults = data.length;
    this.createLineChart(data);
    this.createDoughnutChart(data);
  }

  private destroyCharts(): void {
    if (this.lineChart) {
      this.lineChart.destroy();
      this.lineChart = null;
    }
    if (this.doughnutChart) {
      this.doughnutChart.destroy();
      this.doughnutChart = null;
    }
  }

  createLineChart(data: any[]): void {
    const { labels, values } = this.buildLineSeries(data, this.selectedPeriod);
    const canvas = this.lineChartRef.nativeElement;
    const ctx = canvas.getContext('2d')!;

    const gradient = ctx.createLinearGradient(0, 0, 0, 350);
    gradient.addColorStop(0, 'rgba(139, 92, 246, 0.25)');
    gradient.addColorStop(1, 'rgba(139, 92, 246, 0.0)');

    const lineGradient = ctx.createLinearGradient(0, 0, canvas.width, 0);
    lineGradient.addColorStop(0, '#8b5cf6');
    lineGradient.addColorStop(1, '#06b6d4');

    const gridColor = this.isDark
      ? 'rgba(255, 255, 255, 0.05)'
      : 'rgba(0, 0, 0, 0.05)';
    const tickColor = this.isDark ? '#94a3b8' : '#64748b';
    const legendColor = this.isDark ? '#e2e8f0' : '#334155';

    if (this.lineChart) {
      this.lineChart.destroy();
    }

    this.lineChart = new Chart(canvas, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Faults',
            data: values,
            borderColor: lineGradient,
            backgroundColor: gradient,
            fill: true,
            tension: 0.4,
            pointRadius: 5,
            pointHoverRadius: 8,
            pointBackgroundColor: '#8b5cf6',
            pointBorderColor: this.isDark ? '#0f172a' : '#ffffff',
            pointBorderWidth: 2,
            borderWidth: 3,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: {
          intersect: false,
          mode: 'index',
        },
        plugins: {
          legend: {
            display: true,
            labels: {
              color: legendColor,
              font: { family: 'Inter, sans-serif', size: 13 },
              usePointStyle: true,
              pointStyle: 'circle',
              padding: 20,
            },
          },
          tooltip: {
            backgroundColor: this.isDark
              ? 'rgba(15, 23, 42, 0.9)'
              : 'rgba(255, 255, 255, 0.95)',
            titleColor: this.isDark ? '#e2e8f0' : '#1e293b',
            bodyColor: this.isDark ? '#94a3b8' : '#475569',
            borderColor: this.isDark
              ? 'rgba(139, 92, 246, 0.3)'
              : 'rgba(139, 92, 246, 0.2)',
            borderWidth: 1,
            cornerRadius: 12,
            padding: 12,
            titleFont: {
              family: 'Segoe UI, Arial, sans-serif',
              weight: 'bold' as const,
            },
            bodyFont: { family: 'Inter, sans-serif' },
          },
        },
        scales: {
          x: {
            ticks: {
              color: tickColor,
              font: { family: 'Segoe UI, Arial, sans-serif', size: 11 },
              maxRotation: 45,
            },
            grid: { color: gridColor },
            border: { display: false },
          },
          y: {
            ticks: {
              color: tickColor,
              font: { family: 'Segoe UI, Arial, sans-serif', size: 11 },
            },
            grid: { color: gridColor },
            border: { display: false },
          },
        },
      },
    });
  }

  createDoughnutChart(data: any[]): void {
    let low = 0;
    let medium = 0;
    let high = 0;
    let critical = 0;

    data.forEach((item) => {
      switch ((item.severity || '').toLowerCase()) {
        case 'low':
          low++;
          break;
        case 'medium':
          medium++;
          break;
        case 'high':
          high++;
          break;
        case 'critical':
          critical++;
          break;
      }
    });

    const canvas = this.doughnutChartRef.nativeElement;
    const total = data.length;
    const legendColor = this.isDark ? '#e2e8f0' : '#334155';

    const centerTextPlugin = {
      id: 'centerText',
      beforeDraw: (chart: Chart) => {
        const ctx = chart.ctx;
        const { width, height } = chart;
        ctx.save();

        ctx.font = `bold 36px 'Segoe UI', Arial, sans-serif`;
        ctx.fillStyle = this.isDark ? '#f1f5f9' : '#1e293b';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(`${total}`, width / 2, height / 2 - 10);

        ctx.font = `13px 'Inter', sans-serif`;
        ctx.fillStyle = this.isDark ? '#64748b' : '#94a3b8';
        ctx.fillText('Total Faults', width / 2, height / 2 + 18);

        ctx.restore();
      },
    };

    if (this.doughnutChart) {
      this.doughnutChart.destroy();
    }

    this.doughnutChart = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: ['Low', 'Medium', 'High', 'Critical'],
        datasets: [
          {
            data: [low, medium, high, critical],
            backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#8b5cf6'],
            borderWidth: 0,
            hoverOffset: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: {
          legend: {
            display: true,
            position: 'bottom',
            labels: {
              color: legendColor,
              padding: 16,
              font: { family: 'Inter, sans-serif', size: 12 },
            },
          },
          tooltip: {
            backgroundColor: this.isDark
              ? 'rgba(15, 23, 42, 0.9)'
              : 'rgba(255, 255, 255, 0.95)',
            titleColor: this.isDark ? '#e2e8f0' : '#1e293b',
            bodyColor: this.isDark ? '#94a3b8' : '#475569',
            borderColor: this.isDark
              ? 'rgba(139, 92, 246, 0.3)'
              : 'rgba(139, 92, 246, 0.2)',
            borderWidth: 1,
            cornerRadius: 12,
            padding: 12,
            titleFont: {
              family: 'Segoe UI, Arial, sans-serif',
              weight: 'bold' as const,
            },
            bodyFont: { family: 'Inter, sans-serif' },
          },
        },
        animation: {
          duration: 1200,
        },
      } as any,
      plugins: [centerTextPlugin],
    } as any);
  }

  private buildLineSeries(
    data: any[],
    days: number,
  ): { labels: string[]; values: number[] } {
    const end = new Date();
    end.setHours(0, 0, 0, 0);
    const start = new Date(end);
    start.setDate(end.getDate() - (days - 1));

    const labels: string[] = [];
    const values: number[] = [];

    for (let i = 0; i < days; i += 1) {
      const current = new Date(start);
      current.setDate(start.getDate() + i);
      const key = this.toDateKey(current);
      const count = data.filter(
        (fault) => this.toDateKey(this.parseFaultDate(fault)) === key,
      ).length;
      labels.push(
        current.toLocaleDateString('en-US', { month: 'short', day: '2-digit' }),
      );
      values.push(count);
    }

    return { labels, values };
  }

  private parseFaultDate(fault: any): Date {
    if (fault?.faultDate && fault?.faultTime) {
      return new Date(`${fault.faultDate}T${fault.faultTime}`);
    }
    if (fault?.faultDate) {
      return new Date(fault.faultDate);
    }
    return new Date();
  }

  private toDateKey(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
