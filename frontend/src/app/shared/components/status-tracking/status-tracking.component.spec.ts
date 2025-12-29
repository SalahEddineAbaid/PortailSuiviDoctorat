import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { StatusTrackingComponent, StatusStep } from './status-tracking.component';
import { SoutenanceStatus } from '../../../core/models/soutenance.model';

describe('StatusTrackingComponent', () => {
  let component: StatusTrackingComponent;
  let fixture: ComponentFixture<StatusTrackingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        CommonModule,
        RouterTestingModule,
        StatusTrackingComponent
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StatusTrackingComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with default steps when no custom steps provided', () => {
    component.soutenanceStatus = SoutenanceStatus.BROUILLON;
    component.ngOnInit();

    expect(component.steps).toBeDefined();
    expect(component.steps.length).toBe(8);
    expect(component.steps[0].id).toBe('creation');
    expect(component.steps[0].status).toBe('current'); // BROUILLON status means we're at step 0 (current)
  });

  it('should use custom steps when provided', () => {
    const customSteps: StatusStep[] = [
      {
        id: 'custom1',
        label: 'Custom Step 1',
        description: 'Custom description',
        status: 'completed',
        icon: 'fas fa-test'
      },
      {
        id: 'custom2',
        label: 'Custom Step 2',
        description: 'Custom description 2',
        status: 'current',
        icon: 'fas fa-test2'
      }
    ];

    component.customSteps = customSteps;
    component.soutenanceStatus = SoutenanceStatus.BROUILLON;
    component.ngOnInit();

    expect(component.steps).toEqual(customSteps);
  });

  it('should update steps status based on soutenance status', () => {
    component.soutenanceStatus = SoutenanceStatus.SOUMISE;
    component.ngOnInit();

    // First 3 steps should be completed, 4th should be current
    expect(component.steps[0].status).toBe('completed');
    expect(component.steps[1].status).toBe('completed');
    expect(component.steps[2].status).toBe('completed');
    expect(component.steps[3].status).toBe('current');
    expect(component.steps[4].status).toBe('upcoming');
  });

  it('should handle rejected status correctly', () => {
    component.soutenanceStatus = SoutenanceStatus.REJETEE;
    component.ngOnInit();

    // Should have blocked status for the rejected step
    const blockedStep = component.steps.find(step => step.status === 'blocked');
    expect(blockedStep).toBeDefined();
    expect(blockedStep?.details).toContain('Demande rejetée - corrections nécessaires');
  });

  it('should calculate progress percentage correctly', () => {
    component.soutenanceStatus = SoutenanceStatus.SOUMISE;
    component.ngOnInit();

    const percentage = component.getProgressPercentage();
    const completedSteps = component.getCompletedSteps();
    
    expect(completedSteps).toBe(3);
    expect(percentage).toBe((3 / 8) * 100);
  });

  it('should return correct status label', () => {
    expect(component.getStatusLabel('completed')).toBe('Terminé');
    expect(component.getStatusLabel('current')).toBe('En cours');
    expect(component.getStatusLabel('upcoming')).toBe('À venir');
    expect(component.getStatusLabel('blocked')).toBe('Bloqué');
  });

  it('should return upcoming steps correctly', () => {
    component.soutenanceStatus = SoutenanceStatus.BROUILLON;
    component.ngOnInit();

    const upcomingSteps = component.getUpcomingSteps();
    expect(upcomingSteps.length).toBeLessThanOrEqual(3);
    upcomingSteps.forEach(step => {
      expect(step.status).toBe('upcoming');
    });
  });

  it('should initialize actions based on status', () => {
    // Test BROUILLON status
    component.soutenanceStatus = SoutenanceStatus.BROUILLON;
    component.ngOnInit();

    expect(component.availableActions.length).toBe(2);
    expect(component.availableActions.some(action => action.type === 'edit')).toBeTruthy();
    expect(component.availableActions.some(action => action.type === 'submit')).toBeTruthy();

    // Test REJETEE status
    component.soutenanceStatus = SoutenanceStatus.REJETEE;
    component.ngOnInit();

    expect(component.availableActions.length).toBe(1);
    expect(component.availableActions[0].type).toBe('edit');

    // Test AUTORISEE status
    component.soutenanceStatus = SoutenanceStatus.AUTORISEE;
    component.ngOnInit();

    expect(component.availableActions.length).toBe(1);
    expect(component.availableActions[0].type).toBe('schedule');
  });

  it('should handle action clicks', () => {
    spyOn(console, 'log');
    
    component.onActionClick('test-action');
    
    expect(console.log).toHaveBeenCalledWith('Action clicked:', 'test-action');
  });

  it('should track steps by id', () => {
    const step: StatusStep = {
      id: 'test-step',
      label: 'Test Step',
      description: 'Test description',
      status: 'upcoming',
      icon: 'fas fa-test'
    };

    const result = component.trackByStepId(0, step);
    expect(result).toBe('test-step');
  });

  it('should return correct step class', () => {
    const step: StatusStep = {
      id: 'test',
      label: 'Test',
      description: 'Test',
      status: 'completed',
      icon: 'fas fa-test'
    };

    expect(component.getStepClass(step)).toBe('step-completed');
  });
});