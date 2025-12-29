import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TimelineComponent } from './timeline.component';
import { TimelineEvent } from '../../../core/models/dashboard.model';

describe('TimelineComponent', () => {
  let component: TimelineComponent;
  let fixture: ComponentFixture<TimelineComponent>;

  const mockTimelineEvents: TimelineEvent[] = [
    {
      id: '1',
      date: new Date('2024-01-15'),
      title: 'Inscription soumise',
      description: 'Dossier d\'inscription soumis avec succès',
      status: 'completed',
      type: 'inscription'
    },
    {
      id: '2',
      date: new Date('2024-02-01'),
      title: 'Validation directeur',
      description: 'En attente de validation du directeur de thèse',
      status: 'current',
      type: 'validation'
    },
    {
      id: '3',
      date: new Date('2024-03-15'),
      title: 'Soutenance prévue',
      description: 'Date de soutenance à planifier',
      status: 'upcoming',
      type: 'soutenance'
    },
    {
      id: '4',
      date: new Date('2023-12-01'),
      title: 'Document manquant',
      description: 'Document requis non fourni',
      status: 'overdue',
      type: 'document'
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TimelineComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(TimelineComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default title', () => {
      expect(component.title).toBe('Timeline');
    });

    it('should have empty events array by default', () => {
      expect(component.events).toEqual([]);
    });
  });

  describe('Status Icon Methods', () => {
    it('should return correct status icons', () => {
      expect(component.getStatusIcon('completed')).toBe('fas fa-check-circle');
      expect(component.getStatusIcon('current')).toBe('fas fa-clock');
      expect(component.getStatusIcon('upcoming')).toBe('fas fa-circle');
      expect(component.getStatusIcon('overdue')).toBe('fas fa-exclamation-triangle');
      expect(component.getStatusIcon('unknown')).toBe('fas fa-circle');
    });

    it('should return correct status classes', () => {
      expect(component.getStatusClass('completed')).toBe('completed');
      expect(component.getStatusClass('current')).toBe('current');
      expect(component.getStatusClass('upcoming')).toBe('upcoming');
      expect(component.getStatusClass('overdue')).toBe('overdue');
      expect(component.getStatusClass('unknown')).toBe('upcoming');
    });

    it('should return correct status labels', () => {
      expect(component.getStatusLabel('completed')).toBe('Terminé');
      expect(component.getStatusLabel('current')).toBe('En cours');
      expect(component.getStatusLabel('upcoming')).toBe('À venir');
      expect(component.getStatusLabel('overdue')).toBe('En retard');
      expect(component.getStatusLabel('unknown')).toBe('À venir');
    });
  });

  describe('Type Icon Methods', () => {
    it('should return correct type icons', () => {
      expect(component.getTypeIcon('inscription')).toBe('fas fa-user-plus');
      expect(component.getTypeIcon('soutenance')).toBe('fas fa-graduation-cap');
      expect(component.getTypeIcon('document')).toBe('fas fa-file-alt');
      expect(component.getTypeIcon('validation')).toBe('fas fa-check');
      expect(component.getTypeIcon('unknown')).toBe('fas fa-info-circle');
    });
  });

  describe('Event Tracking', () => {
    it('should track events by id', () => {
      const event = mockTimelineEvents[0];
      const result = component.trackByEventId(0, event);
      expect(result).toBe(event.id);
    });
  });

  describe('Events Display', () => {
    beforeEach(() => {
      component.events = mockTimelineEvents;
      fixture.detectChanges();
    });

    it('should display all events', () => {
      const eventElements = fixture.nativeElement.querySelectorAll('.timeline-item');
      expect(eventElements.length).toBe(mockTimelineEvents.length);
    });

    it('should display event titles', () => {
      const titleElements = fixture.nativeElement.querySelectorAll('.timeline-title');
      expect(titleElements[0].textContent.trim()).toBe('Inscription soumise');
      expect(titleElements[1].textContent.trim()).toBe('Validation directeur');
    });

    it('should display event descriptions', () => {
      const descriptionElements = fixture.nativeElement.querySelectorAll('.timeline-description');
      expect(descriptionElements[0].textContent.trim()).toBe('Dossier d\'inscription soumis avec succès');
    });

    it('should apply correct status classes', () => {
      const eventElements = fixture.nativeElement.querySelectorAll('.timeline-item');
      expect(eventElements[0]).toHaveClass('completed');
      expect(eventElements[1]).toHaveClass('current');
      expect(eventElements[2]).toHaveClass('upcoming');
      expect(eventElements[3]).toHaveClass('overdue');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty events array', () => {
      component.events = [];
      fixture.detectChanges();
      
      const eventElements = fixture.nativeElement.querySelectorAll('.timeline-item');
      expect(eventElements.length).toBe(0);
      
      const emptyState = fixture.nativeElement.querySelector('.timeline-empty');
      expect(emptyState).toBeTruthy();
    });

    it('should handle events with missing properties gracefully', () => {
      const incompleteEvent: TimelineEvent = {
        id: '5',
        date: new Date(),
        title: 'Test Event',
        description: 'Test Description',
        status: 'completed',
        type: 'inscription'
      };
      
      component.events = [incompleteEvent];
      fixture.detectChanges();
      
      expect(component.getStatusIcon(incompleteEvent.status)).toBe('fas fa-check-circle');
      expect(component.getTypeIcon(incompleteEvent.type)).toBe('fas fa-user-plus');
    });
  });
});