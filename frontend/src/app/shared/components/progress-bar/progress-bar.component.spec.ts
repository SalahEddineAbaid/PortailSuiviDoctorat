import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ProgressBarComponent } from './progress-bar.component';

describe('ProgressBarComponent', () => {
  let component: ProgressBarComponent;
  let fixture: ComponentFixture<ProgressBarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProgressBarComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ProgressBarComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default value of 0', () => {
      expect(component.value).toBe(0);
    });

    it('should have default max of 100', () => {
      expect(component.max).toBe(100);
    });

    it('should have default type as primary', () => {
      expect(component.type).toBe('primary');
    });

    it('should have default size as medium', () => {
      expect(component.size).toBe('medium');
    });

    it('should not show label by default', () => {
      expect(component.showLabel).toBe(false);
    });

    it('should not be striped by default', () => {
      expect(component.striped).toBe(false);
    });

    it('should not be animated by default', () => {
      expect(component.animated).toBe(false);
    });
  });

  describe('Progress Calculation', () => {
    it('should calculate correct percentage', () => {
      component.value = 50;
      component.max = 100;
      expect(component.getPercentage()).toBe(50);
    });

    it('should calculate percentage with different max value', () => {
      component.value = 25;
      component.max = 50;
      expect(component.getPercentage()).toBe(50);
    });

    it('should handle zero max value', () => {
      component.value = 50;
      component.max = 0;
      expect(component.getPercentage()).toBe(0);
    });

    it('should handle value greater than max', () => {
      component.value = 150;
      component.max = 100;
      expect(component.getPercentage()).toBe(100);
    });

    it('should handle negative values', () => {
      component.value = -10;
      component.max = 100;
      expect(component.getPercentage()).toBe(0);
    });
  });

  describe('Progress Bar Display', () => {
    beforeEach(() => {
      component.value = 75;
      component.max = 100;
      fixture.detectChanges();
    });

    it('should display progress bar with correct width', () => {
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('75%');
    });

    it('should apply correct type class', () => {
      component.type = 'success';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-success');
    });

    it('should apply correct size class', () => {
      component.size = 'large';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-large');
    });

    it('should show label when showLabel is true', () => {
      component.showLabel = true;
      fixture.detectChanges();
      
      const label = fixture.nativeElement.querySelector('.progress-label');
      expect(label).toBeTruthy();
      expect(label.textContent.trim()).toBe('75%');
    });

    it('should not show label when showLabel is false', () => {
      component.showLabel = false;
      fixture.detectChanges();
      
      const label = fixture.nativeElement.querySelector('.progress-label');
      expect(label).toBeFalsy();
    });

    it('should apply striped class when striped is true', () => {
      component.striped = true;
      fixture.detectChanges();
      
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill).toHaveClass('striped');
    });

    it('should apply animated class when animated is true', () => {
      component.animated = true;
      fixture.detectChanges();
      
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill).toHaveClass('animated');
    });
  });

  describe('Different Progress Types', () => {
    beforeEach(() => {
      component.value = 60;
      fixture.detectChanges();
    });

    it('should display primary progress bar', () => {
      component.type = 'primary';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-primary');
    });

    it('should display success progress bar', () => {
      component.type = 'success';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-success');
    });

    it('should display warning progress bar', () => {
      component.type = 'warning';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-warning');
    });

    it('should display error progress bar', () => {
      component.type = 'error';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-error');
    });
  });

  describe('Different Sizes', () => {
    beforeEach(() => {
      component.value = 40;
      fixture.detectChanges();
    });

    it('should display small progress bar', () => {
      component.size = 'small';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-small');
    });

    it('should display medium progress bar', () => {
      component.size = 'medium';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-medium');
    });

    it('should display large progress bar', () => {
      component.size = 'large';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar).toHaveClass('progress-large');
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      component.value = 65;
      component.max = 100;
      fixture.detectChanges();
    });

    it('should have proper ARIA attributes', () => {
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar.getAttribute('role')).toBe('progressbar');
      expect(progressBar.getAttribute('aria-valuenow')).toBe('65');
      expect(progressBar.getAttribute('aria-valuemin')).toBe('0');
      expect(progressBar.getAttribute('aria-valuemax')).toBe('100');
    });

    it('should update ARIA attributes when value changes', () => {
      component.value = 80;
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar.getAttribute('aria-valuenow')).toBe('80');
    });

    it('should have aria-label when provided', () => {
      component.ariaLabel = 'Upload progress';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar.getAttribute('aria-label')).toBe('Upload progress');
    });

    it('should have aria-labelledby when provided', () => {
      component.ariaLabelledBy = 'progress-label-id';
      fixture.detectChanges();
      
      const progressBar = fixture.nativeElement.querySelector('.progress-bar');
      expect(progressBar.getAttribute('aria-labelledby')).toBe('progress-label-id');
    });
  });

  describe('Custom Label', () => {
    beforeEach(() => {
      component.value = 30;
      component.max = 100;
      component.showLabel = true;
      fixture.detectChanges();
    });

    it('should display custom label when provided', () => {
      component.customLabel = '30 of 100 files';
      fixture.detectChanges();
      
      const label = fixture.nativeElement.querySelector('.progress-label');
      expect(label.textContent.trim()).toBe('30 of 100 files');
    });

    it('should display percentage when no custom label', () => {
      component.customLabel = '';
      fixture.detectChanges();
      
      const label = fixture.nativeElement.querySelector('.progress-label');
      expect(label.textContent.trim()).toBe('30%');
    });
  });

  describe('Edge Cases', () => {
    it('should handle 0% progress', () => {
      component.value = 0;
      component.max = 100;
      fixture.detectChanges();
      
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('0%');
    });

    it('should handle 100% progress', () => {
      component.value = 100;
      component.max = 100;
      fixture.detectChanges();
      
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('100%');
    });

    it('should handle decimal values', () => {
      component.value = 33.33;
      component.max = 100;
      fixture.detectChanges();
      
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('33.33%');
    });

    it('should handle very small max values', () => {
      component.value = 1;
      component.max = 3;
      fixture.detectChanges();
      
      const progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('33.333333333333336%');
    });
  });

  describe('Dynamic Updates', () => {
    it('should update progress when value changes', () => {
      component.value = 25;
      fixture.detectChanges();
      
      let progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('25%');
      
      component.value = 75;
      fixture.detectChanges();
      
      progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('75%');
    });

    it('should update progress when max changes', () => {
      component.value = 50;
      component.max = 100;
      fixture.detectChanges();
      
      let progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('50%');
      
      component.max = 200;
      fixture.detectChanges();
      
      progressFill = fixture.nativeElement.querySelector('.progress-fill');
      expect(progressFill.style.width).toBe('25%');
    });
  });
});