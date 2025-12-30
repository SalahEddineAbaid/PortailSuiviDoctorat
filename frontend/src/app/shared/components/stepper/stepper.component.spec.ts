import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StepperComponent, StepperStep } from './stepper.component';

describe('StepperComponent', () => {
  let component: StepperComponent;
  let fixture: ComponentFixture<StepperComponent>;

  const mockSteps: StepperStep[] = [
    { id: 'step1', label: 'Informations personnelles', completed: true, active: false },
    { id: 'step2', label: 'Documents', completed: false, active: true },
    { id: 'step3', label: 'Validation', completed: false, active: false }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StepperComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StepperComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have empty steps array by default', () => {
      expect(component.steps).toEqual([]);
    });

    it('should show content by default', () => {
      expect(component.showContent).toBe(true);
    });

    it('should allow click navigation by default', () => {
      expect(component.allowClickNavigation).toBe(true);
    });
  });

  describe('Step Display', () => {
    beforeEach(() => {
      component.steps = mockSteps;
      fixture.detectChanges();
    });

    it('should display all steps', () => {
      const stepElements = fixture.nativeElement.querySelectorAll('.stepper-step');
      expect(stepElements.length).toBe(mockSteps.length);
    });

    it('should display step labels', () => {
      const stepLabels = fixture.nativeElement.querySelectorAll('.step-label');
      expect(stepLabels[0].textContent.trim()).toBe('Informations personnelles');
      expect(stepLabels[1].textContent.trim()).toBe('Documents');
      expect(stepLabels[2].textContent.trim()).toBe('Validation');
    });

    it('should mark completed steps with correct class', () => {
      const completedStep = fixture.nativeElement.querySelector('.stepper-step.completed');
      expect(completedStep).toBeTruthy();
    });

    it('should mark active step with correct class', () => {
      const activeStep = fixture.nativeElement.querySelector('.stepper-step.active');
      expect(activeStep).toBeTruthy();
    });

    it('should show step numbers for non-completed steps', () => {
      const stepNumbers = fixture.nativeElement.querySelectorAll('.step-number');
      expect(stepNumbers.length).toBeGreaterThan(0);
    });

    it('should show check icon for completed steps', () => {
      const completedStepIcon = fixture.nativeElement.querySelector('.stepper-step.completed .material-icons');
      expect(completedStepIcon.textContent.trim()).toBe('check');
    });
  });

  describe('Step Navigation', () => {
    beforeEach(() => {
      component.steps = mockSteps;
      fixture.detectChanges();
    });

    it('should emit stepChange event when step is clicked', () => {
      spyOn(component.stepChange, 'emit');
      
      const firstStep = fixture.nativeElement.querySelector('.stepper-step');
      firstStep.click();
      
      expect(component.stepChange.emit).toHaveBeenCalledWith({ 
        step: mockSteps[0], 
        index: 0 
      });
    });

    it('should call onStepClick method when step is clicked', () => {
      spyOn(component, 'onStepClick');
      
      const firstStep = fixture.nativeElement.querySelector('.stepper-step');
      firstStep.click();
      
      expect(component.onStepClick).toHaveBeenCalledWith(mockSteps[0], 0);
    });

    it('should not emit event when disabled step is clicked', () => {
      const disabledSteps = [...mockSteps];
      disabledSteps[2].disabled = true;
      component.steps = disabledSteps;
      fixture.detectChanges();
      
      spyOn(component.stepChange, 'emit');
      
      const disabledStep = fixture.nativeElement.querySelectorAll('.stepper-step')[2];
      disabledStep.click();
      
      expect(component.stepChange.emit).not.toHaveBeenCalled();
    });

    it('should not emit event when click navigation is disabled', () => {
      component.allowClickNavigation = false;
      fixture.detectChanges();
      
      spyOn(component.stepChange, 'emit');
      
      const firstStep = fixture.nativeElement.querySelector('.stepper-step');
      firstStep.click();
      
      expect(component.stepChange.emit).not.toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      component.steps = mockSteps;
      fixture.detectChanges();
    });

    it('should have proper ARIA attributes on stepper', () => {
      const stepper = fixture.nativeElement.querySelector('.stepper');
      expect(stepper.getAttribute('aria-label')).toBe('Étapes du processus');
    });

    it('should have proper ARIA attributes on steps', () => {
      const stepElements = fixture.nativeElement.querySelectorAll('.stepper-step');
      
      stepElements.forEach((step, index) => {
        expect(step.getAttribute('role')).toBe('button');
        if (mockSteps[index].active) {
          expect(step.getAttribute('aria-current')).toBe('step');
        }
        if (mockSteps[index].disabled) {
          expect(step.getAttribute('aria-disabled')).toBe('true');
        }
      });
    });

    it('should have proper tabindex for keyboard navigation', () => {
      const stepElements = fixture.nativeElement.querySelectorAll('.stepper-step');
      
      stepElements.forEach((step, index) => {
        if (mockSteps[index].disabled) {
          expect(step.getAttribute('tabindex')).toBe('-1');
        } else {
          expect(step.getAttribute('tabindex')).toBe('0');
        }
      });
    });
  });

  describe('Keyboard Navigation', () => {
    beforeEach(() => {
      component.steps = mockSteps;
      fixture.detectChanges();
    });

    it('should handle Enter key to select step', () => {
      spyOn(component, 'onStepClick');
      
      const firstStep = fixture.nativeElement.querySelector('.stepper-step');
      const event = new KeyboardEvent('keydown', { key: 'Enter' });
      firstStep.dispatchEvent(event);
      
      expect(component.onStepClick).toHaveBeenCalledWith(mockSteps[0], 0);
    });

    it('should handle Space key to select step', () => {
      spyOn(component, 'onStepClick');
      
      const firstStep = fixture.nativeElement.querySelector('.stepper-step');
      const event = new KeyboardEvent('keydown', { key: ' ' });
      firstStep.dispatchEvent(event);
      
      expect(component.onStepClick).toHaveBeenCalledWith(mockSteps[0], 0);
    });
  });

  describe('Track By Function', () => {
    it('should track steps by id', () => {
      const step = mockSteps[0];
      const result = component.trackByStepId(0, step);
      expect(result).toBe(step.id);
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty steps array', () => {
      component.steps = [];
      fixture.detectChanges();
      
      const stepElements = fixture.nativeElement.querySelectorAll('.stepper-step');
      expect(stepElements.length).toBe(0);
    });

    it('should handle single step', () => {
      component.steps = [{ id: 'single', label: 'Single Step', completed: false, active: true }];
      fixture.detectChanges();
      
      const stepElements = fixture.nativeElement.querySelectorAll('.stepper-step');
      expect(stepElements.length).toBe(1);
      expect(stepElements[0]).toHaveClass('active');
    });

    it('should handle steps with descriptions', () => {
      const stepsWithDescription = [
        { id: 'step1', label: 'Step 1', description: 'Step description', completed: false, active: true }
      ];
      component.steps = stepsWithDescription;
      fixture.detectChanges();
      
      const description = fixture.nativeElement.querySelector('.step-description');
      expect(description).toBeTruthy();
      expect(description.textContent.trim()).toBe('Step description');
    });

    it('should handle steps with custom icons', () => {
      const stepsWithIcon = [
        { id: 'step1', label: 'Step 1', icon: 'person', completed: false, active: true }
      ];
      component.steps = stepsWithIcon;
      fixture.detectChanges();
      
      const icon = fixture.nativeElement.querySelector('.material-icons');
      expect(icon.textContent.trim()).toBe('person');
    });
  });

  describe('Content Projection', () => {
    it('should show content area when showContent is true', () => {
      component.showContent = true;
      fixture.detectChanges();
      
      const contentArea = fixture.nativeElement.querySelector('.stepper-content');
      expect(contentArea).toBeTruthy();
    });

    it('should hide content area when showContent is false', () => {
      component.showContent = false;
      fixture.detectChanges();
      
      const contentArea = fixture.nativeElement.querySelector('.stepper-content');
      expect(contentArea).toBeFalsy();
    });
  });
});
});