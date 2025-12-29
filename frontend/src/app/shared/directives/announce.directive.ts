import { Directive, ElementRef, OnInit, OnDestroy, Input } from '@angular/core';

@Directive({
  selector: '[appAnnounce]',
  standalone: true
})
export class AnnounceDirective implements OnInit, OnDestroy {
  @Input() appAnnounce = '';
  @Input() announcePolite = true; // true for 'polite', false for 'assertive'
  @Input() announceDelay = 100; // Delay in milliseconds
  
  private announcer?: HTMLElement;
  private timeoutId?: number;

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.createAnnouncer();
    this.setupObserver();
  }

  ngOnDestroy(): void {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    this.removeAnnouncer();
  }

  private createAnnouncer(): void {
    this.announcer = document.createElement('div');
    this.announcer.setAttribute('aria-live', this.announcePolite ? 'polite' : 'assertive');
    this.announcer.setAttribute('aria-atomic', 'true');
    this.announcer.setAttribute('class', 'sr-only');
    this.announcer.style.cssText = `
      position: absolute !important;
      width: 1px !important;
      height: 1px !important;
      padding: 0 !important;
      margin: -1px !important;
      overflow: hidden !important;
      clip: rect(0, 0, 0, 0) !important;
      white-space: nowrap !important;
      border: 0 !important;
    `;
    
    document.body.appendChild(this.announcer);
  }

  private removeAnnouncer(): void {
    if (this.announcer && this.announcer.parentNode) {
      this.announcer.parentNode.removeChild(this.announcer);
    }
  }

  private setupObserver(): void {
    const element = this.elementRef.nativeElement;
    
    // Initial announcement if there's content
    if (this.appAnnounce) {
      this.announce(this.appAnnounce);
    }
    
    // Watch for text content changes
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        if (mutation.type === 'childList' || mutation.type === 'characterData') {
          const text = element.textContent?.trim();
          if (text && text !== this.appAnnounce) {
            this.announce(text);
          }
        }
      });
    });
    
    observer.observe(element, {
      childList: true,
      subtree: true,
      characterData: true
    });
  }

  private announce(message: string): void {
    if (!this.announcer || !message.trim()) {
      return;
    }
    
    // Clear any existing timeout
    if (this.timeoutId) {
      clearTimeout(this.timeoutId);
    }
    
    // Clear the announcer first
    this.announcer.textContent = '';
    
    // Announce after a short delay to ensure screen readers pick it up
    this.timeoutId = window.setTimeout(() => {
      if (this.announcer) {
        this.announcer.textContent = message;
      }
    }, this.announceDelay);
  }
}