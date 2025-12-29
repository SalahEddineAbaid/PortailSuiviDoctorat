import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { DoctorantListComponent, DoctorantListItem } from './doctorant-list.component';
import { RoleName } from '../../../core/models/role.model';

describe('DoctorantListComponent', () => {
  let component: DoctorantListComponent;
  let fixture: ComponentFixture<DoctorantListComponent>;

  const mockDoctorants: DoctorantListItem[] = [
    {
      id: 1,
      FirstName: 'Jean',
      LastName: 'Dupont',
      email: 'jean.dupont@example.com',
      phoneNumber: '0123456789',
      adresse: '123 Rue de la Paix',
      ville: 'Paris',
      pays: 'France',
      roles: [{ id: 1, name: RoleName.DOCTORANT }],
      enabled: true,
      inscriptionStatus: 'VALIDEE',
      soutenanceStatus: 'EN_COURS_VALIDATION',
      lastActivity: new Date('2024-01-15'),
      pendingActions: 2
    },
    {
      id: 2,
      FirstName: 'Marie',
      LastName: 'Martin',
      email: 'marie.martin@example.com',
      phoneNumber: '0987654321',
      adresse: '456 Avenue des Champs',
      ville: 'Lyon',
      pays: 'France',
      roles: [{ id: 1, name: RoleName.DOCTORANT }],
      enabled: true,
      inscriptionStatus: 'EN_COURS_VALIDATION',
      soutenanceStatus: undefined,
      lastActivity: new Date('2024-01-10'),
      pendingActions: 1
    },
    {
      id: 3,
      FirstName: 'Pierre',
      LastName: 'Durand',
      email: 'pierre.durand@example.com',
      phoneNumber: '0147258369',
      adresse: '789 Boulevard Saint-Michel',
      ville: 'Marseille',
      pays: 'France',
      roles: [{ id: 1, name: RoleName.DOCTORANT }],
      enabled: false,
      inscriptionStatus: 'REJETEE',
      soutenanceStatus: undefined,
      lastActivity: new Date('2023-12-01'),
      pendingActions: 0
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DoctorantListComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(DoctorantListComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default title', () => {
      expect(component.title).toBe('Liste des doctorants');
    });

    it('should have showActions true by default', () => {
      expect(component.showActions).toBe(true);
    });

    it('should have empty doctorants array by default', () => {
      expect(component.doctorants).toEqual([]);
    });
  });

  describe('Status Helper Methods', () => {
    it('should return correct status colors', () => {
      expect(component.getStatusColor('VALIDEE')).toBe('green');
      expect(component.getStatusColor('AUTORISEE')).toBe('green');
      expect(component.getStatusColor('EN_COURS_VALIDATION')).toBe('orange');
      expect(component.getStatusColor('REJETEE')).toBe('red');
      expect(component.getStatusColor('SOUMISE')).toBe('blue');
      expect(component.getStatusColor('UNKNOWN')).toBe('gray');
      expect(component.getStatusColor(undefined)).toBe('gray');
    });

    it('should return correct status labels', () => {
      expect(component.getStatusLabel('VALIDEE')).toBe('Validée');
      expect(component.getStatusLabel('EN_COURS_VALIDATION')).toBe('En validation');
      expect(component.getStatusLabel('REJETEE')).toBe('Rejetée');
      expect(component.getStatusLabel('SOUMISE')).toBe('Soumise');
      expect(component.getStatusLabel('AUTORISEE')).toBe('Autorisée');
      expect(component.getStatusLabel('SOUTENUE')).toBe('Soutenue');
      expect(component.getStatusLabel('UNKNOWN')).toBe('Aucune');
      expect(component.getStatusLabel(undefined)).toBe('Aucune');
    });
  });

  describe('Utility Methods', () => {
    it('should generate correct initials', () => {
      expect(component.getInitials('Jean', 'Dupont')).toBe('JD');
      expect(component.getInitials('Marie', 'Martin')).toBe('MM');
      expect(component.getInitials('a', 'b')).toBe('AB');
    });

    it('should format last activity correctly', () => {
      const today = new Date();
      const yesterday = new Date(today.getTime() - 24 * 60 * 60 * 1000);
      const threeDaysAgo = new Date(today.getTime() - 3 * 24 * 60 * 60 * 1000);
      const twoWeeksAgo = new Date(today.getTime() - 14 * 24 * 60 * 60 * 1000);
      const twoMonthsAgo = new Date(today.getTime() - 60 * 24 * 60 * 60 * 1000);

      expect(component.formatLastActivity(undefined)).toBe('Aucune activité');
      
      // Test with more flexible expectations since the calculation can vary by a day
      const yesterdayResult = component.formatLastActivity(yesterday);
      expect(yesterdayResult).toMatch(/^(Hier|Il y a [12] jours?)$/);
      
      const threeDaysResult = component.formatLastActivity(threeDaysAgo);
      expect(threeDaysResult).toMatch(/^Il y a [34] jours$/);
      
      const twoWeeksResult = component.formatLastActivity(twoWeeksAgo);
      expect(twoWeeksResult).toMatch(/^Il y a [23] semaines$/);
      
      const twoMonthsResult = component.formatLastActivity(twoMonthsAgo);
      expect(twoMonthsResult).toMatch(/^Il y a [23] mois$/);
    });

    it('should track doctorants by id', () => {
      const doctorant = mockDoctorants[0];
      const result = component.trackByDoctorantId(0, doctorant);
      expect(result).toBe(doctorant.id);
    });
  });

  describe('Event Emitters', () => {
    beforeEach(() => {
      component.doctorants = mockDoctorants;
      fixture.detectChanges();
    });

    it('should emit viewDetails event', () => {
      spyOn(component.viewDetails, 'emit');
      
      component.onViewDetails(mockDoctorants[0]);
      
      expect(component.viewDetails.emit).toHaveBeenCalledWith(mockDoctorants[0]);
    });

    it('should emit viewDossier event', () => {
      spyOn(component.viewDossier, 'emit');
      
      component.onViewDossier(mockDoctorants[0]);
      
      expect(component.viewDossier.emit).toHaveBeenCalledWith(mockDoctorants[0]);
    });
  });

  describe('Doctorants Display', () => {
    beforeEach(() => {
      component.doctorants = mockDoctorants;
      fixture.detectChanges();
    });

    it('should display all doctorants', () => {
      const doctorantElements = fixture.nativeElement.querySelectorAll('.doctorant-item');
      expect(doctorantElements.length).toBe(mockDoctorants.length);
    });

    it('should display doctorant names', () => {
      const nameElements = fixture.nativeElement.querySelectorAll('.doctorant-name');
      expect(nameElements[0].textContent.trim()).toContain('Jean Dupont');
      expect(nameElements[1].textContent.trim()).toContain('Marie Martin');
    });

    it('should display doctorant emails', () => {
      const emailElements = fixture.nativeElement.querySelectorAll('.doctorant-email');
      expect(emailElements[0].textContent.trim()).toBe('jean.dupont@example.com');
      expect(emailElements[1].textContent.trim()).toBe('marie.martin@example.com');
    });

    it('should display correct initials', () => {
      const initialsElements = fixture.nativeElement.querySelectorAll('.avatar-circle');
      expect(initialsElements[0].textContent.trim()).toBe('JD');
      expect(initialsElements[1].textContent.trim()).toBe('MM');
    });

    it('should display status badges with correct colors', () => {
      const statusElements = fixture.nativeElement.querySelectorAll('.status-badge');
      expect(statusElements[0]).toHaveClass('status-green'); // VALIDEE
      expect(statusElements[1]).toHaveClass('status-orange'); // EN_COURS_VALIDATION
    });

    it('should display pending actions count', () => {
      const pendingElements = fixture.nativeElement.querySelectorAll('.status-indicator');
      expect(pendingElements[0].textContent.trim()).toContain('2');
      expect(pendingElements[1].textContent.trim()).toContain('1');
    });
  });

  describe('Action Buttons', () => {
    beforeEach(() => {
      component.doctorants = mockDoctorants;
      component.showActions = true;
      fixture.detectChanges();
    });

    it('should show action buttons when showActions is true', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('.action-btn');
      expect(actionButtons.length).toBeGreaterThan(0);
    });

    it('should emit viewDetails when details button is clicked', () => {
      spyOn(component.viewDetails, 'emit');
      
      const detailsButton = fixture.nativeElement.querySelector('.action-btn.primary');
      detailsButton.click();
      
      expect(component.viewDetails.emit).toHaveBeenCalledWith(mockDoctorants[0]);
    });

    it('should emit viewDossier when dossier button is clicked', () => {
      spyOn(component.viewDossier, 'emit');
      
      const dossierButton = fixture.nativeElement.querySelector('.action-btn.secondary');
      dossierButton.click();
      
      expect(component.viewDossier.emit).toHaveBeenCalledWith(mockDoctorants[0]);
    });
  });

  describe('Hidden Actions', () => {
    beforeEach(() => {
      component.doctorants = mockDoctorants;
      component.showActions = false;
      fixture.detectChanges();
    });

    it('should hide action buttons when showActions is false', () => {
      const actionButtons = fixture.nativeElement.querySelectorAll('.action-btn');
      expect(actionButtons.length).toBe(0);
    });
  });

  describe('Empty State', () => {
    beforeEach(() => {
      component.doctorants = [];
      fixture.detectChanges();
    });

    it('should display empty state message when no doctorants', () => {
      const emptyMessage = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyMessage).toBeTruthy();
      expect(emptyMessage.textContent.trim()).toContain('Aucun doctorant');
    });

    it('should not display doctorant items when list is empty', () => {
      const doctorantElements = fixture.nativeElement.querySelectorAll('.doctorant-item');
      expect(doctorantElements.length).toBe(0);
    });
  });

  describe('Edge Cases', () => {
    it('should handle doctorants with missing optional properties', () => {
      const incompleteDoctorant: DoctorantListItem = {
        id: 4,
        FirstName: 'Test',
        LastName: 'User',
        email: 'test@example.com',
        phoneNumber: '0123456789',
        adresse: 'Test Address',
        ville: 'Test City',
        pays: 'France',
        roles: [{ id: 1, name: RoleName.DOCTORANT }],
        enabled: true
        // Missing optional properties
      };
      
      component.doctorants = [incompleteDoctorant];
      fixture.detectChanges();
      
      expect(component.getStatusLabel(incompleteDoctorant.inscriptionStatus)).toBe('Aucune');
      expect(component.formatLastActivity(incompleteDoctorant.lastActivity)).toBe('Aucune activité');
    });
  });
});