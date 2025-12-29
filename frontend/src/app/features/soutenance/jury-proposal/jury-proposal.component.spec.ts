import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { JuryProposalComponent } from './jury-proposal.component';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { JuryRole, JuryMember } from '../../../core/models/soutenance.model';

describe('JuryProposalComponent', () => {
  let component: JuryProposalComponent;
  let fixture: ComponentFixture<JuryProposalComponent>;
  let mockSoutenanceService: jasmine.SpyObj<SoutenanceService>;

  beforeEach(async () => {
    const soutenanceServiceSpy = jasmine.createSpyObj('SoutenanceService', [
      'proposeJury'
    ]);

    await TestBed.configureTestingModule({
      imports: [JuryProposalComponent, ReactiveFormsModule],
      providers: [
        { provide: SoutenanceService, useValue: soutenanceServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(JuryProposalComponent);
    component = fixture.componentInstance;
    mockSoutenanceService = TestBed.inject(SoutenanceService) as jasmine.SpyObj<SoutenanceService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with director member when directeurInfo is provided', () => {
    component.directeurInfo = {
      nom: 'Dupont',
      prenom: 'Jean',
      etablissement: 'Université Test',
      grade: 'Professeur'
    };

    component.ngOnInit();

    expect(component.juryMembers.length).toBeGreaterThan(0);
    const directorMember = component.juryMembers.at(0);
    expect(directorMember.get('nom')?.value).toBe('Dupont');
    expect(directorMember.get('role')?.value).toBe(JuryRole.DIRECTEUR);
  });

  it('should add existing jury members on initialization', () => {
    const existingJury: JuryMember[] = [
      {
        id: 1,
        nom: 'Martin',
        prenom: 'Pierre',
        etablissement: 'Université B',
        grade: 'Professeur',
        role: JuryRole.RAPPORTEUR,
        externe: true
      }
    ];

    component.existingJury = existingJury;
    component.ngOnInit();

    expect(component.juryMembers.length).toBeGreaterThan(0);
    const addedMember = component.juryMembers.controls.find(
      control => control.get('nom')?.value === 'Martin'
    );
    expect(addedMember).toBeTruthy();
  });

  it('should validate minimum members requirement', () => {
    component.ngOnInit();
    
    // Should have minimum members after initialization
    expect(component.juryMembers.length).toBeGreaterThanOrEqual(component.minMembers);
    expect(component.validationStatus.minMembers).toBeTruthy();
  });

  it('should validate external rapporteurs requirement', () => {
    component.ngOnInit();
    
    // Add two external rapporteurs
    const member1 = component.juryMembers.at(1);
    member1.patchValue({
      nom: 'Test1',
      prenom: 'User1',
      etablissement: 'External Uni',
      grade: 'Prof',
      role: JuryRole.RAPPORTEUR,
      externe: true
    });

    const member2 = component.juryMembers.at(2);
    member2.patchValue({
      nom: 'Test2',
      prenom: 'User2',
      etablissement: 'External Uni 2',
      grade: 'Prof',
      role: JuryRole.RAPPORTEUR,
      externe: true
    });

    component['validateJury']();
    expect(component.validationStatus.minRapporteurs).toBeTruthy();
  });

  it('should validate president requirement', () => {
    component.ngOnInit();
    
    // Add a president
    const member = component.juryMembers.at(1);
    member.patchValue({
      nom: 'President',
      prenom: 'Test',
      etablissement: 'Uni',
      grade: 'Prof',
      role: JuryRole.PRESIDENT,
      externe: false
    });

    component['validateJury']();
    expect(component.validationStatus.hasPresident).toBeTruthy();
  });

  it('should validate external members ratio', () => {
    component.ngOnInit();
    
    // Make half of the members external
    const totalMembers = component.juryMembers.length;
    const requiredExternal = Math.ceil(totalMembers / 2);
    
    for (let i = 0; i < requiredExternal; i++) {
      const member = component.juryMembers.at(i);
      member.patchValue({
        externe: true
      });
    }

    component['validateJury']();
    expect(component.validationStatus.externalRatio).toBeTruthy();
  });

  it('should add new member when addMember is called', () => {
    component.ngOnInit();
    const initialCount = component.juryMembers.length;
    
    component.addMember();
    
    expect(component.juryMembers.length).toBe(initialCount + 1);
  });

  it('should not add member when max members reached', () => {
    component.ngOnInit();
    
    // Fill to max members
    while (component.juryMembers.length < component.maxMembers) {
      component.addMember();
    }
    
    const maxCount = component.juryMembers.length;
    component.addMember();
    
    expect(component.juryMembers.length).toBe(maxCount);
  });

  it('should remove member when removeMember is called', () => {
    component.ngOnInit();
    const initialCount = component.juryMembers.length;
    
    // Add a non-director member
    component.addMember();
    const newMemberIndex = component.juryMembers.length - 1;
    
    component.removeMember(newMemberIndex);
    
    expect(component.juryMembers.length).toBe(initialCount);
  });

  it('should not remove director member', () => {
    component.directeurInfo = {
      nom: 'Dupont',
      prenom: 'Jean',
      etablissement: 'Université Test',
      grade: 'Professeur'
    };
    component.ngOnInit();
    
    const initialCount = component.juryMembers.length;
    
    // Try to remove director (should be at index 0)
    component.removeMember(0);
    
    expect(component.juryMembers.length).toBe(initialCount);
  });

  it('should suggest external for rapporteur role', () => {
    component.ngOnInit();
    
    const mockEvent = { target: { value: JuryRole.RAPPORTEUR } };
    component.onRoleChange(1, mockEvent);
    
    const member = component.juryMembers.at(1);
    expect(member.get('externe')?.value).toBeTruthy();
  });

  it('should count external rapporteurs correctly', () => {
    component.ngOnInit();
    
    // Add external rapporteurs
    const member1 = component.juryMembers.at(1);
    member1.patchValue({
      role: JuryRole.RAPPORTEUR,
      externe: true
    });

    const member2 = component.juryMembers.at(2);
    member2.patchValue({
      role: JuryRole.RAPPORTEUR,
      externe: true
    });

    expect(component.getExternalRapporteursCount()).toBe(2);
  });

  it('should count external members correctly', () => {
    component.ngOnInit();
    
    // Make some members external
    component.juryMembers.at(1).patchValue({ externe: true });
    component.juryMembers.at(2).patchValue({ externe: true });

    expect(component.getExternalMembersCount()).toBe(2);
  });

  it('should emit juryUpdated when onSave is called with valid jury', () => {
    spyOn(component.juryUpdated, 'emit');
    
    // Setup valid jury
    component.ngOnInit();
    component.validationStatus = {
      minMembers: true,
      minRapporteurs: true,
      hasPresident: true,
      externalRatio: true
    };
    
    // Fill required fields for all members
    component.juryMembers.controls.forEach((member, index) => {
      member.patchValue({
        nom: `Nom${index}`,
        prenom: `Prenom${index}`,
        etablissement: `Etablissement${index}`,
        grade: `Grade${index}`,
        role: index === 0 ? JuryRole.DIRECTEUR : JuryRole.EXAMINATEUR
      });
    });

    component.onSave();

    expect(component.juryUpdated.emit).toHaveBeenCalled();
  });

  it('should emit cancel when onCancel is called', () => {
    spyOn(component.cancel, 'emit');
    
    component.onCancel();
    
    expect(component.cancel.emit).toHaveBeenCalled();
  });

  it('should return false for isJuryValid when validation fails', () => {
    component.ngOnInit();
    component.validationStatus = {
      minMembers: false,
      minRapporteurs: false,
      hasPresident: false,
      externalRatio: false
    };

    expect(component.isJuryValid()).toBeFalsy();
  });

  it('should return true for isJuryValid when all validations pass', () => {
    component.ngOnInit();
    component.validationStatus = {
      minMembers: true,
      minRapporteurs: true,
      hasPresident: true,
      externalRatio: true
    };

    // Fill required fields for all members
    component.juryMembers.controls.forEach((member, index) => {
      member.patchValue({
        nom: `Nom${index}`,
        prenom: `Prenom${index}`,
        etablissement: `Etablissement${index}`,
        grade: `Grade${index}`,
        role: index === 0 ? JuryRole.DIRECTEUR : JuryRole.EXAMINATEUR
      });
    });

    expect(component.isJuryValid()).toBeTruthy();
  });
});