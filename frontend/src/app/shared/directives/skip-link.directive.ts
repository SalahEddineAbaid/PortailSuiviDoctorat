import { Directive, ElementRef, OnInit, Input } from '@angular/core';

@Directive({
  selector: '[appSkipLink]',
  standalone: true
})
export class SkipLinkDirective implements OnInit {
  @Input() appSkipLink = '';
  @Input() skipText = 'Aller au contenu principal';

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    this.setupSkipLink();
  }

  private setupSkipLink(): void {
    const element = this.elementRef.nativeElement;
    
    // Set default attributes
    element.setAttribute('href', `#${this.appSkipLink}`);
    element.setAttribute('class', 'skip-link');
    element.textContent = this.skipText;
    
    // Add click handler
    element.addEventListener('click', this.handleClick);
  }

  private handleClick = (event: Event): void => {
    event.preventDefault();
    
    const targetId = this.appSkipLink;
    const targetElement = document.getElementById(targetId);
    
    if (targetElement) {
      // Make the target focusable if it's not already
      if (!targetElement.hasAttribute('tabindex')) {
        targetElement.setAttribute('tabindex', '-1');
      }
      
      // Focus the target element
      targetElement.focus();
      
      // Scroll to the target element
      targetElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  };
}