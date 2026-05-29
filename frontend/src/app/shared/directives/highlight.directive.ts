import { Directive, ElementRef, HostListener } from '@angular/core';

@Directive({
  selector: '[appHighlight]',
  standalone: true
})
export class HighlightDirective {

  constructor(private el: ElementRef) {
    const row = this.el.nativeElement as HTMLElement;

    row.style.transition =
      'background-color 0.35s ease, box-shadow 0.35s ease, transform 0.25s ease';

    row.style.color = 'inherit';
  }

  @HostListener('mouseenter')
  onMouseEnter() {
    const row = this.el.nativeElement as HTMLElement;

    // ✅ dark highlight only, not light background
    row.style.backgroundColor = 'rgba(8, 145, 178, 0.12)';
    row.style.boxShadow = 'inset 4px 0 0 rgba(34, 211, 238, 0.95)';
    row.style.transform = 'translateX(2px)';
  }

  @HostListener('mouseleave')
  onMouseLeave() {
    const row = this.el.nativeElement as HTMLElement;

    row.style.backgroundColor = '';
    row.style.boxShadow = '';
    row.style.transform = 'translateX(0)';
  }
}