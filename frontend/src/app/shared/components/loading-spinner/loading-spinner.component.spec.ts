import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoadingSpinnerComponent } from './loading-spinner.component';

describe('LoadingSpinnerComponent', () => {
  let component: LoadingSpinnerComponent;
  let fixture: ComponentFixture<LoadingSpinnerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoadingSpinnerComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(LoadingSpinnerComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default size as medium', () => {
      expect(component.size).toBe('medium');
    });

    it('should have default type as primary', () => {
      expect(component.type).toBe('primary');
    });

    it('should not show overlay by default', () => {
      expect(component.overlay).toBe(false);
    });

    it('should not have message by default', () => {
      expect(component.message).toBe('');
    });

    it('should be visible by default', () => {
      expect(component.show).toBe(true);
    });
  });

  describe('Spinner Display', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display spinner when show is true', () => {
      component.show = true;
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toBeTruthy();
    });

    it('should not display spinner when show is false', () => {
      component.show = false;
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toBeFalsy();
    });

    it('should apply correct size class', () => {
      component.size = 'large';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-large');
    });

    it('should apply correct type class', () => {
      component.type = 'secondary';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-secondary');
    });

    it('should show overlay when overlay is true', () => {
      component.overlay = true;
      fixture.detectChanges();
      
      const overlay = fixture.nativeElement.querySelector('.spinner-overlay');
      expect(overlay).toBeTruthy();
    });

    it('should not show overlay when overlay is false', () => {
      component.overlay = false;
      fixture.detectChanges();
      
      const overlay = fixture.nativeElement.querySelector('.spinner-overlay');
      expect(overlay).toBeFalsy();
    });
  });

  describe('Message Display', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display message when provided', () => {
      component.message = 'Chargement en cours...';
      fixture.detectChanges();
      
      const messageElement = fixture.nativeElement.querySelector('.spinner-message');
      expect(messageElement).toBeTruthy();
      expect(messageElement.textContent.trim()).toBe('Chargement en cours...');
    });

    it('should not display message when empty', () => {
      component.message = '';
      fixture.detectChanges();
      
      const messageElement = fixture.nativeElement.querySelector('.spinner-message');
      expect(messageElement).toBeFalsy();
    });

    it('should update message dynamically', () => {
      component.message = 'Initial message';
      fixture.detectChanges();
      
      let messageElement = fixture.nativeElement.querySelector('.spinner-message');
      expect(messageElement.textContent.trim()).toBe('Initial message');
      
      component.message = 'Updated message';
      fixture.detectChanges();
      
      messageElement = fixture.nativeElement.querySelector('.spinner-message');
      expect(messageElement.textContent.trim()).toBe('Updated message');
    });
  });

  describe('Different Sizes', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display small spinner', () => {
      component.size = 'small';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-small');
    });

    it('should display medium spinner', () => {
      component.size = 'medium';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-medium');
    });

    it('should display large spinner', () => {
      component.size = 'large';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-large');
    });

    it('should display extra large spinner', () => {
      component.size = 'xl';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-xl');
    });
  });

  describe('Different Types', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should display primary spinner', () => {
      component.type = 'primary';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-primary');
    });

    it('should display secondary spinner', () => {
      component.type = 'secondary';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-secondary');
    });

    it('should display light spinner', () => {
      component.type = 'light';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-light');
    });

    it('should display dark spinner', () => {
      component.type = 'dark';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toHaveClass('spinner-dark');
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should have proper ARIA attributes', () => {
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner.getAttribute('role')).toBe('status');
      expect(spinner.getAttribute('aria-live')).toBe('polite');
    });

    it('should have aria-label when no message is provided', () => {
      component.message = '';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner.getAttribute('aria-label')).toBe('Chargement');
    });

    it('should have aria-label with message when message is provided', () => {
      component.message = 'Chargement des données';
      fixture.detectChanges();
      
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner.getAttribute('aria-label')).toBe('Chargement des données');
    });

    it('should have proper aria-hidden for decorative elements', () => {
      const spinnerIcon = fixture.nativeElement.querySelector('.spinner-icon');
      expect(spinnerIcon.getAttribute('aria-hidden')).toBe('true');
    });
  });

  describe('Overlay Behavior', () => {
    it('should center spinner in overlay', () => {
      component.overlay = true;
      fixture.detectChanges();
      
      const overlay = fixture.nativeElement.querySelector('.spinner-overlay');
      expect(overlay).toHaveClass('centered');
    });

    it('should make overlay full screen', () => {
      component.overlay = true;
      fixture.detectChanges();
      
      const overlay = fixture.nativeElement.querySelector('.spinner-overlay');
      expect(overlay).toHaveClass('fullscreen');
    });

    it('should have proper z-index for overlay', () => {
      component.overlay = true;
      fixture.detectChanges();
      
      const overlay = fixture.nativeElement.querySelector('.spinner-overlay');
      const computedStyle = window.getComputedStyle(overlay);
      expect(parseInt(computedStyle.zIndex)).toBeGreaterThan(1000);
    });
  });

  describe('Animation', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should have spinning animation class', () => {
      const spinnerIcon = fixture.nativeElement.querySelector('.spinner-icon');
      expect(spinnerIcon).toHaveClass('spinning');
    });

    it('should maintain animation during updates', () => {
      component.message = 'Loading...';
      fixture.detectChanges();
      
      const spinnerIcon = fixture.nativeElement.querySelector('.spinner-icon');
      expect(spinnerIcon).toHaveClass('spinning');
    });
  });

  describe('Edge Cases', () => {
    it('should handle very long messages', () => {
      component.message = 'This is a very long loading message that might overflow the container and should be handled properly by the component';
      fixture.detectChanges();
      
      const messageElement = fixture.nativeElement.querySelector('.spinner-message');
      expect(messageElement).toBeTruthy();
      expect(messageElement.textContent.trim()).toContain('This is a very long loading message');
    });

    it('should handle HTML in message safely', () => {
      component.message = '<script>alert("test")</script>Safe message';
      fixture.detectChanges();
      
      const messageElement = fixture.nativeElement.querySelector('.spinner-message');
      expect(messageElement.innerHTML).not.toContain('<script>');
      expect(messageElement.textContent).toContain('Safe message');
    });

    it('should handle rapid show/hide toggles', () => {
      component.show = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.loading-spinner')).toBeTruthy();
      
      component.show = false;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.loading-spinner')).toBeFalsy();
      
      component.show = true;
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.loading-spinner')).toBeTruthy();
    });
  });

  describe('Performance', () => {
    it('should not render DOM elements when show is false', () => {
      component.show = false;
      fixture.detectChanges();
      
      const spinnerElements = fixture.nativeElement.querySelectorAll('.loading-spinner, .spinner-overlay, .spinner-message');
      expect(spinnerElements.length).toBe(0);
    });

    it('should minimize DOM updates when properties change', () => {
      fixture.detectChanges();
      const initialHTML = fixture.nativeElement.innerHTML;
      
      // Change non-visual property
      component.ariaLabel = 'Custom loading';
      fixture.detectChanges();
      
      // Structure should remain the same
      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toBeTruthy();
    });
  });
});