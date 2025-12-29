import { Directive, ElementRef, OnInit, OnDestroy, Input } from '@angular/core';

@Directive({
  selector: '[appFocusTrap]',
  standalone: true
})
export class FocusTrapDirective implements OnInit, OnDestroy {
  @Input() appFocusTrap = true;
  @Input() restoreFocus = true;
  
  private focusableElements: HTMLElement[] = [];
  private firstFocusableElement?: HTMLElement;
  private lastFocusableElement?: HTMLElement;
  private previouslyFocusedElement?: HTMLElement;
  
  private readonly focusableSelectors = [
    'a[href]',
    'button:not([disabled])',
    'textarea:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
    '[contenteditable="true"]'
  ].join(', ');

  constructor(private elementRef: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    if (this.appFocusTrap) {
      this.setupFocusTrap();
    }
  }

  ngOnDestroy(): void {
    if (this.restoreFocus && this.previouslyFocusedElement) {
      this.previouslyFocusedElement.focus();
    }
    this.removeFocusTrap();
  }

  private setupFocusTrap(): void {
    // Store the previously focused element
    this.previouslyFocusedElement = document.activeElement as HTMLElement;
    
    // Find all focusable elements within the trap
    this.updateFocusableElements();
    
    // Focus the first focusable element
    if (this.firstFocusableElement) {
      this.firstFocusableElement.focus();
    }
    
    // Add event listeners
    this.elementRef.nativeElement.addEventListener('keydown', this.handleKeydown);
    document.addEventListener('focusin', this.handleFocusIn);
  }

  private removeFocusTrap(): void {
    this.elementRef.nativeElement.removeEventListener('keydown', this.handleKeydown);
    document.removeEventListener('focusin', this.handleFocusIn);
  }

  private updateFocusableElements(): void {
    const elements = this.elementRef.nativeElement.querySelectorAll(this.focusableSelectors);
    this.focusableElements = Array.from(elements) as HTMLElement[];
    this.firstFocusableElement = this.focusableElements[0];
    this.lastFocusableElement = this.focusableElements[this.focusableElements.length - 1];
  }

  private handleKeydown = (event: KeyboardEvent): void => {
    if (event.key !== 'Tab') {
      return;
    }

    // Update focusable elements in case DOM changed
    this.updateFocusableElements();

    if (this.focusableElements.length === 0) {
      event.preventDefault();
      return;
    }

    if (event.shiftKey) {
      // Shift + Tab (backward)
      if (document.activeElement === this.firstFocusableElement) {
        event.preventDefault();
        this.lastFocusableElement?.focus();
      }
    } else {
      // Tab (forward)
      if (document.activeElement === this.lastFocusableElement) {
        event.preventDefault();
        this.firstFocusableElement?.focus();
      }
    }
  };

  private handleFocusIn = (event: FocusEvent): void => {
    const target = event.target as HTMLElement;
    
    // If focus moves outside the trap, bring it back
    if (!this.elementRef.nativeElement.contains(target)) {
      event.preventDefault();
      this.firstFocusableElement?.focus();
    }
  };
}