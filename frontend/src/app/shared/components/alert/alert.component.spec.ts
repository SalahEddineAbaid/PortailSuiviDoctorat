import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AlertComponent } from './alert.component';
import { DashboardAlert } from '../../../core/models/dashboard.model';

describe('AlertComponent', () => {
  let component: AlertComponent;
  let fixture: ComponentFixture<AlertComponent>;

  const mockAlert: DashboardAlert = {
    id: 'alert-1',
    type: 'warning',
    title: 'Document manquant',
    message: 'Veuillez télécharger votre CV',
    actionLabel: 'Télécharger',
    actionRoute: '/documents/upload',
    dismissible: true
  };

  const mockErrorAlert: DashboardAlert = {
    id: 'alert-2',
    type: 'error',
    title: 'Erreur de validation',
    message: 'Votre dossier contient des erreurs',
    dismissible: false
  };

  const mockSuccessAlert: DashboardAlert = {
    id: 'alert-3',
    type: 'success',
    title: 'Inscription validée',
    message: 'Votre inscription a été approuvée',
    dismissible: true
  };

  const mockInfoAlert: DashboardAlert = {
    id: 'alert-4',
    type: 'info',
    title: 'Information',
    message: 'Nouvelle campagne d\'inscription ouverte',
    dismissible: true
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AlertComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(AlertComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should require alert input', () => {
      expect(component.alert).toBeUndefined();
    });
  });

  describe('Alert Icon Methods', () => {
    it('should return correct icons for different alert types', () => {
      expect(component.getAlertIcon('warning')).toBe('fas fa-exclamation-triangle');
      expect(component.getAlertIcon('error')).toBe('fas fa-times-circle');
      expect(component.getAlertIcon('info')).toBe('fas fa-info-circle');
      expect(component.getAlertIcon('success')).toBe('fas fa-check-circle');
      expect(component.getAlertIcon('unknown')).toBe('fas fa-info-circle');
    });
  });

  describe('Alert Display', () => {
    beforeEach(() => {
      component.alert = mockAlert;
      fixture.detectChanges();
    });

    it('should display alert title', () => {
      const titleElement = fixture.nativeElement.querySelector('.alert-title');
      expect(titleElement.textContent.trim()).toBe('Document manquant');
    });

    it('should display alert message', () => {
      const messageElement = fixture.nativeElement.querySelector('.alert-message');
      expect(messageElement.textContent.trim()).toBe('Veuillez télécharger votre CV');
    });

    it('should display correct icon for warning type', () => {
      const iconElement = fixture.nativeElement.querySelector('.alert-icon i');
      expect(iconElement).toHaveClass('fa-exclamation-triangle');
    });

    it('should apply correct CSS class for alert type', () => {
      const alertElement = fixture.nativeElement.querySelector('.alert');
      expect(alertElement).toHaveClass('alert-warning');
    });
  });

  describe('Dismissible Alerts', () => {
    beforeEach(() => {
      component.alert = mockAlert;
      fixture.detectChanges();
    });

    it('should show dismiss button for dismissible alerts', () => {
      const dismissButton = fixture.nativeElement.querySelector('.alert-dismiss');
      expect(dismissButton).toBeTruthy();
    });

    it('should emit dismiss event when dismiss button is clicked', () => {
      spyOn(component.dismiss, 'emit');
      
      const dismissButton = fixture.nativeElement.querySelector('.alert-dismiss');
      dismissButton.click();
      
      expect(component.dismiss.emit).toHaveBeenCalledWith('alert-1');
    });

    it('should call onDismiss method when dismiss button is clicked', () => {
      spyOn(component, 'onDismiss');
      
      const dismissButton = fixture.nativeElement.querySelector('.alert-dismiss');
      dismissButton.click();
      
      expect(component.onDismiss).toHaveBeenCalled();
    });
  });

  describe('Non-dismissible Alerts', () => {
    beforeEach(() => {
      component.alert = mockErrorAlert;
      fixture.detectChanges();
    });

    it('should not show dismiss button for non-dismissible alerts', () => {
      const dismissButton = fixture.nativeElement.querySelector('.alert-dismiss');
      expect(dismissButton).toBeFalsy();
    });

    it('should not emit dismiss event for non-dismissible alerts', () => {
      spyOn(component.dismiss, 'emit');
      
      component.onDismiss();
      
      expect(component.dismiss.emit).not.toHaveBeenCalled();
    });
  });

  describe('Action Button', () => {
    beforeEach(() => {
      component.alert = mockAlert;
      fixture.detectChanges();
    });

    it('should show action button when actionLabel is provided', () => {
      const actionButton = fixture.nativeElement.querySelector('.alert-action-btn');
      expect(actionButton).toBeTruthy();
      expect(actionButton.textContent.trim()).toBe('Télécharger');
    });

    it('should have correct router link when actionRoute is provided', () => {
      const actionButton = fixture.nativeElement.querySelector('.alert-action-btn');
      // Check if the routerLink directive is properly set
      expect(actionButton.getAttribute('href')).toBe('/documents/upload');
    });

    it('should call onAction method when action button is clicked', () => {
      spyOn(component, 'onAction');
      
      const actionButton = fixture.nativeElement.querySelector('.alert-action-btn');
      actionButton.click();
      
      expect(component.onAction).toHaveBeenCalled();
    });
  });

  describe('Different Alert Types', () => {
    it('should display error alert correctly', () => {
      component.alert = mockErrorAlert;
      fixture.detectChanges();
      
      const alertElement = fixture.nativeElement.querySelector('.alert');
      const iconElement = fixture.nativeElement.querySelector('.alert-icon i');
      
      expect(alertElement).toHaveClass('alert-error');
      expect(iconElement).toHaveClass('fa-times-circle');
    });

    it('should display success alert correctly', () => {
      component.alert = mockSuccessAlert;
      fixture.detectChanges();
      
      const alertElement = fixture.nativeElement.querySelector('.alert');
      const iconElement = fixture.nativeElement.querySelector('.alert-icon i');
      
      expect(alertElement).toHaveClass('alert-success');
      expect(iconElement).toHaveClass('fa-check-circle');
    });

    it('should display info alert correctly', () => {
      component.alert = mockInfoAlert;
      fixture.detectChanges();
      
      const alertElement = fixture.nativeElement.querySelector('.alert');
      const iconElement = fixture.nativeElement.querySelector('.alert-icon i');
      
      expect(alertElement).toHaveClass('alert-info');
      expect(iconElement).toHaveClass('fa-info-circle');
    });
  });

  describe('Edge Cases', () => {
    it('should handle alert without actionLabel', () => {
      const alertWithoutAction: DashboardAlert = {
        ...mockAlert,
        actionLabel: undefined,
        actionRoute: undefined
      };
      
      component.alert = alertWithoutAction;
      fixture.detectChanges();
      
      const actionButton = fixture.nativeElement.querySelector('.alert-action-btn');
      expect(actionButton).toBeFalsy();
    });

    it('should handle alert without actionRoute', () => {
      const alertWithoutRoute: DashboardAlert = {
        ...mockAlert,
        actionRoute: undefined
      };
      
      component.alert = alertWithoutRoute;
      fixture.detectChanges();
      
      const actionButton = fixture.nativeElement.querySelector('.alert-action-btn');
      expect(actionButton).toBeFalsy();
    });
  });
});