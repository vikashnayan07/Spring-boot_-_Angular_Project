import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  OnInit,
  ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './landing.component.html',
})
export class LandingComponent implements OnInit, AfterViewInit, OnDestroy {
  readonly logoSrc = '/landing/logo.png';
  readonly aboutCards = [
    {
      icon: '◆',
      title: 'Operational Clarity',
      description:
        'See faults, maintenance tasks, and machine health in one elegant enterprise view.',
    },
    {
      icon: '◉',
      title: 'Predictive Readiness',
      description:
        'Surface maintenance risks early with industrial intelligence designed to reduce downtime.',
    },
    {
      icon: '✦',
      title: 'Responsive Execution',
      description:
        'Coordinate engineers and keep operational flow smooth across the maintenance lifecycle.',
    },
  ];
  readonly serviceCards = [
    {
      title: 'Predictive Maintenance',
      image: '/landing/service_image1.png',
      phrases: [
        'Prevent Before Failure',
        'Predict Early. Act Early.',
        'Uptime Starts Here',
      ],
      description:
        'Use proactive intelligence to spot failure risk before it disrupts production.',
    },
    {
      title: 'Real-Time Monitoring',
      image: '/landing/service_image2.png',
      phrases: [
        'Monitor Every Machine',
        'Live Signals. Clear Action.',
        'Visibility Without Lag',
      ],
      description:
        'Track live machine health with a cinematic, always-on industrial view.',
    },
    {
      title: 'Fault Analytics',
      image: '/landing/service_image3.png',
      phrases: [
        'Predict. Detect. Resolve.',
        'Turn Data Into Decisions',
        'Resolve Faster, Smarter',
      ],
      description:
        'Transform fault data into clear operational insight and faster response.',
    },
  ];
  readonly heroMessages = [
    'Predicting Failures',
    'Optimizing Maintenance',
    'Powering Industry',
    'Real-Time  Monitoring',
    'Preventing Downtime ',
  ];
  readonly serviceShowcase = [
    {
      src: '/landing/service_image1.png',
      title: 'Real-Time Machine Monitoring',
      description:
        'Unified live visibility across machines, alerts, and uptime signals for operational control.',
    },
    {
      src: '/landing/service_image2.png',
      title: 'Predictive Fault Intelligence',
      description:
        'Identify maintenance risks early with intelligent analysis that highlights failure patterns.',
    },
    {
      src: '/landing/service_image3.png',
      title: 'Smart Engineer Assignment',
      description:
        'Route issues to the right engineer faster with guided task assignment and response flow.',
    },
  ];

  heroLoaded = false;
  logoFailed = false;
  navbarSolid = false;
  aboutVisible = false;
  servicesVisible = false;
  footerVisible = false;
  currentMessageIndex = 0;
  currentMessage = this.heroMessages[0];
  messageTransitioning = false;
  servicePhraseIndex = 0;
  servicePhrasesTransitioning = false;

  private rotationTimerId?: number;
  private swapTimerId?: number;
  private servicePhraseTimerId?: number;
  private servicePhraseSwapTimerId?: number;
  private revealObserver?: IntersectionObserver;
  private navbarScrollPending = false;

  @ViewChild('heroVideo') heroVideo?: ElementRef<HTMLVideoElement>;
  @ViewChild('aboutSection') aboutSection?: ElementRef<HTMLElement>;
  @ViewChild('servicesSection') servicesSection?: ElementRef<HTMLElement>;
  @ViewChild('footerSection') footerSection?: ElementRef<HTMLElement>;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.updateNavbarState();

    requestAnimationFrame(() => {
      this.heroLoaded = true;
    });

    this.startMessageRotation();
    this.startServicePhraseRotation();
  }

  ngAfterViewInit(): void {
    requestAnimationFrame(() => {
      const videoElement = this.heroVideo?.nativeElement;

      if (!videoElement) {
        return;
      }

      videoElement.muted = true;
      videoElement.playsInline = true;
      videoElement.load();
      void videoElement.play().catch(() => undefined);
    });

    this.setupSectionReveals();
  }

  goToDashboard(): void {
    const roleId = Number(localStorage.getItem('roleId'));
    const token = localStorage.getItem('token');

    if (!token) {
      this.router.navigate(['/login']);
      return;
    }

    if (roleId === 1) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    if (roleId === 2) {
      this.router.navigate(['/engineer/dashboard']);
      return;
    }

    if (roleId === 3) {
      this.router.navigate(['/operator/dashboard']);
      return;
    }

    this.router.navigate(['/login']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  scrollToSection(sectionId: string): void {
    document
      .getElementById(sectionId)
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  goToAbout(): void {
    this.scrollToSection('about');
  }

  goToServices(): void {
    this.scrollToSection('services');
  }

  goToContact(): void {
    this.scrollToSection('contact');
  }

  @HostListener('window:scroll')
  onWindowScroll(): void {
    if (this.navbarScrollPending) {
      return;
    }

    this.navbarScrollPending = true;

    requestAnimationFrame(() => {
      this.navbarScrollPending = false;
      this.updateNavbarState();
    });
  }

  ngOnDestroy(): void {
    this.revealObserver?.disconnect();

    if (this.rotationTimerId) {
      clearInterval(this.rotationTimerId);
    }

    if (this.swapTimerId) {
      clearTimeout(this.swapTimerId);
    }

    if (this.servicePhraseTimerId) {
      clearInterval(this.servicePhraseTimerId);
    }

    if (this.servicePhraseSwapTimerId) {
      clearTimeout(this.servicePhraseSwapTimerId);
    }
  }

  get navbarClass(): string {
    return this.navbarSolid
      ? 'bg-slate-950/90 backdrop-blur-xl border-b border-white/10 shadow-[0_1px_0_rgba(255,255,255,0.04)]'
      : 'bg-transparent border-b border-transparent shadow-none';
  }

  get messageVisibleClass(): string {
    return this.messageTransitioning
      ? 'opacity-0 translate-y-4 blur-md'
      : this.heroLoaded
        ? 'opacity-100 translate-y-0 blur-0'
        : 'opacity-0 translate-y-6 blur-md';
  }

  get heroSurfaceClass(): string {
    return this.heroLoaded
      ? 'opacity-100 translate-y-0 scale-100'
      : 'opacity-0 translate-y-6 scale-[0.985]';
  }

  get aboutSectionClass(): string {
    return this.aboutVisible
      ? 'opacity-100 translate-y-0 blur-0'
      : 'opacity-0 translate-y-10 blur-md';
  }

  get servicesSectionClass(): string {
    return this.servicesVisible
      ? 'opacity-100 translate-y-0 blur-0'
      : 'opacity-0 translate-y-10 blur-md';
  }

  get footerSectionClass(): string {
    return this.footerVisible
      ? 'opacity-100 translate-y-0 blur-0'
      : 'opacity-0 translate-y-10 blur-md';
  }

  get serviceCardTextClass(): string {
    return this.servicePhrasesTransitioning
      ? 'opacity-0 translate-y-3 blur-sm'
      : 'opacity-100 translate-y-0 blur-0';
  }

  getServicePhrase(cardIndex: number): string {
    const phrases = this.serviceCards[cardIndex]?.phrases ?? [];

    if (phrases.length === 0) {
      return '';
    }

    return phrases[this.servicePhraseIndex % phrases.length];
  }

  onLogoError(): void {
    this.logoFailed = true;
  }

  footerColumnClass(): string {
    return `transition-all duration-800 ease-out ${this.footerVisible ? 'opacity-100 translate-y-0 blur-0' : 'opacity-0 translate-y-8 blur-md'}`;
  }

  private startMessageRotation(): void {
    this.rotationTimerId = window.setInterval(() => {
      this.messageTransitioning = true;

      if (this.swapTimerId) {
        clearTimeout(this.swapTimerId);
      }

      this.swapTimerId = window.setTimeout(() => {
        this.currentMessageIndex =
          (this.currentMessageIndex + 1) % this.heroMessages.length;
        this.currentMessage = this.heroMessages[this.currentMessageIndex];
        this.messageTransitioning = false;
      }, 280);
    }, 2800);
  }

  private startServicePhraseRotation(): void {
    this.servicePhraseTimerId = window.setInterval(() => {
      this.servicePhrasesTransitioning = true;

      if (this.servicePhraseSwapTimerId) {
        clearTimeout(this.servicePhraseSwapTimerId);
      }

      this.servicePhraseSwapTimerId = window.setTimeout(() => {
        this.servicePhraseIndex = this.servicePhraseIndex + 1;
        this.servicePhrasesTransitioning = false;
      }, 1200);
    }, 3000);
  }

  private setupSectionReveals(): void {
    if (typeof IntersectionObserver === 'undefined') {
      this.aboutVisible = true;
      this.servicesVisible = true;
      this.footerVisible = true;
      return;
    }

    this.revealObserver = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) {
            continue;
          }

          if (entry.target === this.aboutSection?.nativeElement) {
            this.aboutVisible = true;
          } else if (entry.target === this.servicesSection?.nativeElement) {
            this.servicesVisible = true;
          } else if (entry.target === this.footerSection?.nativeElement) {
            this.footerVisible = true;
          }

          this.revealObserver?.unobserve(entry.target);
        }
      },
      {
        threshold: 0.2,
        rootMargin: '0px 0px -10% 0px',
      },
    );

    const targets = [
      this.aboutSection?.nativeElement,
      this.servicesSection?.nativeElement,
      this.footerSection?.nativeElement,
    ].filter((element): element is HTMLElement => Boolean(element));

    for (const target of targets) {
      this.revealObserver.observe(target);
    }
  }

  private updateNavbarState(): void {
    this.navbarSolid = window.scrollY > 24;
  }
}
