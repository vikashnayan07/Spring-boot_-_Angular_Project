import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import Chart from 'chart.js/auto';
import { forkJoin } from 'rxjs';
import { AnalysisService } from '../../../core/services/analysis.service';
import { EngineerService } from '../../../core/services/engineer.service';
import { FaultService } from '../../../core/services/fault.services';
import { buildPagination, PaginationItem } from '../../../shared/utils/pagination';

@Component({
  selector: 'app-fault-analysis2',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './fault-analysis2.component.html',
  styleUrls: ['./fault-analysis2.component.css'],
})
export class FaultAnalysis2Component implements OnInit {
  isDataLoaded = false;
  showProcessing = true;
  countdown = 5;
  faultId = '';
  analysis: any = null;
  alert: any = null;
  fault: any = null;
  relatedFaults: any[] = [];
  faultPage = 1;
  faultPageSize = 5;
  faultSortKey: 'faultId' | 'severity' | 'faultDate' | 'analysisStatus' = 'faultDate';
  faultSortDirection: 'asc' | 'desc' = 'desc';
  charts: Chart[] = [];
  showCalculationGuide = false;
  selectedMetric = 'Health Score';

  metricGuides: Record<string, { formula: string; explanation: string }> = {
    'Health Score': {
      formula:
        '100 - (Severity x 0.30 + Frequency x 0.25 + MTBF x 0.25 + MTTR x 0.20)',
      explanation:
        'Combines technical severity, recent recurrence, failure spacing and repair complexity into a machine condition score.',
    },
    'Failure Trend': {
      formula: 'Last 7 days faults compared with previous 7 days faults',
      explanation:
        'Increasing means the latest week has more than one additional fault compared with the previous week.',
    },
    'Production Impact': {
      formula:
        '(Criticality x 0.50) + (Current Load x 0.30) + Bottleneck Bonus',
      explanation:
        'Shows how much this machine affects production flow and business continuity.',
    },
    MTBF: {
      formula: 'Average hours between consecutive machine faults',
      explanation: 'Lower MTBF means the machine is failing more often.',
    },
    MTTR: {
      formula: 'Average repair duration in minutes',
      explanation:
        'Higher MTTR means the machine takes longer to recover after failure.',
    },
    'Priority Score': {
      formula:
        'Severity x 0.25 + Frequency x 0.20 + MTBF x 0.15 + MTTR x 0.10 + Production Impact x 0.30',
      explanation:
        'Ranks faults for engineers using both technical seriousness and production dependency.',
    },
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private analysisService: AnalysisService,
    private engineerService: EngineerService,
    private faultService: FaultService,
  ) {}

  ngOnInit(): void {
    this.faultId = this.route.snapshot.paramMap.get('faultId') || '';
    this.loadAnalysisData();
  }

  loadAnalysisData(): void {
    this.isDataLoaded = false;
    const cachedAnalysis = this.readCachedAnalysis();

    forkJoin({
      analyses: this.analysisService.getAllAnalysis(),
      alerts: this.engineerService.getAlerts(),
      faults: this.faultService.getFaults(),
    }).subscribe({
      next: ({ analyses, alerts, faults }: any) => {
        const allAnalyses = analyses?.data || analyses || [];
        const allAlerts = alerts?.data || [];
        const allFaults = faults?.data || faults || [];
        this.analysis =
          allAnalyses.find((item: any) => item.faultId === this.faultId) ||
          cachedAnalysis;
        this.fault = this.readCachedFault();
        const machineId =
          this.fault?.machineId ||
          allAlerts.find((item: any) => item.linkedFaultId === this.faultId)
            ?.machineId;
        this.relatedFaults = allFaults.filter(
          (item: any) => item.machineId === machineId,
        );
        this.fault =
          this.relatedFaults.find(
            (item: any) => item.faultId === this.faultId,
          ) || this.fault;
        this.alert = allAlerts.find(
          (item: any) =>
            item.linkedAnalysisId === this.analysis?.analysisId ||
            item.analysisId === this.analysis?.analysisId ||
            item.linkedFaultId === this.faultId,
        );
        this.isDataLoaded = true;
        this.startCountdown();
      },
      error: () => {
        this.analysis = cachedAnalysis;
        this.fault = this.readCachedFault();
        this.isDataLoaded = true;
        this.startCountdown();
      },
    });
  }

  get healthScore(): number {
    return Number(this.analysis?.healthScore || 0);
  }

  get priorityScore(): number {
    const cachedFault = this.readCachedFault();
    return Number(cachedFault?.priorityScore || this.analysis?.priority || 0);
  }

  get productionImpact(): number {
    return Number(this.analysis?.productionImpactScore || 0);
  }

  get alertRisk(): string {
    return this.alert?.alertPriority || 'No Alert';
  }

  get machineHeading(): string {
    const machineId =
      this.fault?.machineId || this.alert?.machineId || 'Machine';
    const machineName =
      this.fault?.machineName || this.fault?.machine || 'Industrial Asset';
    return `Machine ${machineId} - ${machineName}`;
  }

  get recommendedAction(): string {
    if (this.alert)
      return 'Create maintenance action and assign engineer based on workload.';
    if (this.healthScore < 60)
      return 'Monitor closely and schedule inspection during next production window.';
    return 'Continue standard monitoring cycle.';
  }

  get lastFaultDate(): string {
    return (
      this.relatedFaults
        .map((item) => item.faultDate)
        .filter(Boolean)
        .sort()
        .reverse()[0] ||
      this.fault?.faultDate ||
      'N/A'
    );
  }

  get paginatedRelatedFaults(): any[] {
    const start = (this.faultPage - 1) * this.faultPageSize;
    return this.sortedRelatedFaults.slice(start, start + this.faultPageSize);
  }

  get sortedRelatedFaults(): any[] {
    const direction = this.faultSortDirection === 'asc' ? 1 : -1;
    return [...this.relatedFaults].sort((a, b) => {
      const left = this.sortValue(a);
      const right = this.sortValue(b);
      if (left > right) return direction;
      if (left < right) return -direction;
      return 0;
    });
  }

  get faultTotalPages(): number {
    return Math.ceil(this.relatedFaults.length / this.faultPageSize) || 1;
  }

  pages(): PaginationItem[] {
    return buildPagination(this.faultPage, this.faultTotalPages);
  }

  changeFaultPageFromItem(item: PaginationItem): void {
    if (item === '...') return;
    this.changeFaultPage(item);
  }

  sortBy(key: 'faultId' | 'severity' | 'faultDate' | 'analysisStatus'): void {
    if (this.faultSortKey === key) {
      this.faultSortDirection = this.faultSortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.faultSortKey = key;
      this.faultSortDirection = 'asc';
    }
  }

  changeFaultPage(page: number): void {
    if (page < 1 || page > this.faultTotalPages) return;
    this.faultPage = page;
  }

  goBack(): void {
    const roleId = Number(localStorage.getItem('roleId'));
    this.router.navigate([
      roleId === 2 ? '/engineer/analysis' : '/admin/fault-analysis',
    ]);
  }

  healthPanelClass(): string {
    if (this.alert) return 'border-red-500/30 bg-red-500/10 text-red-200';
    return 'border-emerald-500/30 bg-emerald-500/10 text-emerald-200';
  }

  healthColor(): string {
    if (this.healthScore < 40) return '#ef4444';
    if (this.healthScore < 60) return '#f97316';
    if (this.healthScore < 80) return '#f59e0b';
    return '#10b981';
  }

  toggleCalculationGuide(): void {
    this.showCalculationGuide = !this.showCalculationGuide;
  }

  selectMetric(metric: string): void {
    this.selectedMetric = metric;
  }

  private startCountdown(): void {
    const shouldCountdown =
      localStorage.getItem('analysisCountdown') === 'true';
    localStorage.setItem('analysisCountdown', 'false');
    if (!shouldCountdown) {
      this.showProcessing = false;
      setTimeout(() => this.renderCharts(), 80);
      return;
    }

    this.showProcessing = true;
    this.countdown = 5;
    const timer = setInterval(() => {
      this.countdown--;
      if (this.countdown <= 0) {
        clearInterval(timer);
        this.showProcessing = false;
        setTimeout(() => this.renderCharts(), 80);
      }
    }, 1000);
  }

  renderCharts(): void {
    if (!this.analysis) return;
    this.charts.forEach((chart) => chart.destroy());
    this.charts = [];

    this.renderChart('faultTrendChart', 'bar', this.relatedSeries());
    this.renderChart('severityDistributionChart', 'bar', this.severitySeries());
    this.renderChart('mtbfTrendChart', 'line', this.mtbfSeries());
    this.renderChart('healthTimelineChart', 'line', this.healthSeries());
  }

  private sortValue(item: any): string | number {
    switch (this.faultSortKey) {
      case 'faultId':
        return `${item?.faultId || ''}`.toLowerCase();
      case 'severity':
        return `${item?.severity || ''}`.toLowerCase();
      case 'analysisStatus':
        return `${item?.analysisStatus || ''}`.toLowerCase();
      default:
        return `${item?.faultDate || ''}`.toLowerCase();
    }
  }

  private renderChart(
    canvasId: string,
    type: 'line' | 'bar',
    values: number[],
  ): void {
    const canvas = document.getElementById(
      canvasId,
    ) as HTMLCanvasElement | null;
    if (!canvas) return;

    const chart = new Chart(canvas, {
      type,
      data: {
        labels: ['D-6', 'D-5', 'D-4', 'D-3', 'D-2', 'D-1', 'Today'],
        datasets: [
          {
            label: canvasId,
            data: values,
            borderColor: this.healthColor(),
            backgroundColor: `${this.healthColor()}22`,
            tension: 0.42,
            fill: true,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          y: { min: 0, max: 100, grid: { color: 'rgba(148,163,184,.14)' } },
          x: { grid: { display: false } },
        },
      },
    });
    this.charts.push(chart);
  }

  private healthSeries(): number[] {
    const health = this.healthScore;
    return [
      Math.max(0, health - 18),
      Math.max(0, health - 10),
      Math.max(0, health - 4),
      health,
      Math.min(100, health + 3),
      Math.max(0, health - 2),
      health,
    ];
  }

  private productionSeries(): number[] {
    const impact = this.productionImpact;
    return [
      Math.max(0, impact - 18),
      Math.max(0, impact - 12),
      Math.max(0, impact - 6),
      impact,
      Math.min(100, impact + 4),
      Math.max(0, impact - 3),
      impact,
    ];
  }

  private relatedSeries(): number[] {
    const base = Math.max(1, this.relatedFaults.length);
    return [
      1,
      2,
      Math.max(1, base - 2),
      Math.max(1, base - 1),
      base,
      Math.max(1, base - 1),
      base,
    ];
  }

  private severitySeries(): number[] {
    const counts = ['Low', 'Medium', 'High', 'Critical'].map(
      (severity) =>
        this.relatedFaults.filter((fault) => fault.severity === severity)
          .length,
    );
    return counts.length ? counts : [0, 0, 0, 0];
  }

  private mtbfSeries(): number[] {
    const mtbf = Number(
      this.analysis?.mtbfHours || this.fault?.expectedMtbf || 0,
    );
    return [
      mtbf + 8,
      mtbf + 5,
      mtbf + 2,
      mtbf,
      Math.max(0, mtbf - 2),
      mtbf,
      mtbf,
    ];
  }

  private readCachedAnalysis(): any {
    try {
      return JSON.parse(localStorage.getItem('selectedAnalysis') || 'null');
    } catch {
      return null;
    }
  }

  private readCachedFault(): any {
    try {
      return JSON.parse(localStorage.getItem('selectedFault') || 'null');
    } catch {
      return null;
    }
  }
}
