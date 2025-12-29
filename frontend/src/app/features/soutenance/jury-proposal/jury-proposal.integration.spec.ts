import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { JuryProposalComponent } from './jury-proposal.component';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { JuryRole, JuryMemberRequest } from '../../../core/models/soutenance.model';

describe('JuryProposalComponent Integration', () => {
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

  it('should create a complete jury composition workflow', () => {
    // Setup director info
    component.directeurInfo = {
      nom: 'Dupont',
      prenom: 'Jean',
      etablissement: 'Université Test',
      grade: 'Professeur'
    };

    component.ngOnInit();
    fixture.detectChanges();

    // Verify director is added
    expect(component.juryMembers.length).toBeGreaterThan(0);
    const directorMember = component.juryMembers.at(0);
    expect(directorMember.get('role')?.value).toBe(JuryRole.DIRECTEUR);

    // Add a president
    const presidentIndex = 1;
    const presidentMember = component.juryMembers.at(presidentIndex);
    presidentMember.patchValue({
      nom: 'Martin',
      prenom: 'Pierre',
      etablissement: 'Université Externe',
      grade: 'Professeur',
      role: JuryRole.PRESIDENT,
      externe: true
    });

    // Add external rapporteurs
    const rapporteur1Index = 2;
    const rapporteur1Member = component.juryMembers.at(rapporteur1Index);
    rapporteur1Member.patchValue({
      nom: 'Durand',
      prenom: 'Marie',
      etablissement: 'Institut Externe 1',
      grade: 'Professeur',
      role: JuryRole.RAPPORTEUR,
      externe: true
    });

    const rapporteur2Index = 3;
    const rapporteur2Member = component.juryMembers.at(rapporteur2Index);
    rapporteur2Member.patchValue({
      nom: 'Bernard',
      prenom: 'Paul',
      etablissement: 'Institut Externe 2',
      grade: 'Maître de conférences',
      role: JuryRole.RAPPORTEUR,
      externe: true
    });

    // Validate jury composition
    component['validateJury']();

    expect(component.validationStatus.minMembers).toBeTruthy();
    expect(component.validationStatus.minRapporteurs).toBeTruthy();
    expect(component.validationStatus.hasPresident).toBeTruthy();
    expect(component.validationStatus.externalRatio).toBeTruthy();
    expect(component.isJuryValid()).toBeTruthy();
  });

  it('should emit correct jury data when saved', () => {
    spyOn(component.juryUpdated, 'emit');

    // Setup valid jury
    component.directeurInfo = {
      nom: 'Dupont',
      prenom: 'Jean',
      etablissement: 'Université Test',
      grade: 'Professeur'
    };

    component.ngOnInit();

    // Fill all required fields
    component.juryMembers.controls.forEach((member, index) => {
      member.patchValue({
        nom: `Nom${index}`,
        prenom: `Prenom${index}`,
        etablissement: `Etablissement${index}`,
        grade: `Grade${index}`,
        role: index === 0 ? JuryRole.DIRECTEUR : 
              index === 1 ? JuryRole.PRESIDENT :
              index <= 3 ? JuryRole.RAPPORTEUR : JuryRole.EXAMINATEUR,
        externe: index > 0 // All except director are external
      });
    });

    // Set validation status to valid
    component.validationStatus = {
      minMembers: true,
      minRapporteurs: true,
      hasPresident: true,
      externalRatio: true
    };

    component.onSave();

    expect(component.juryUpdated.emit).toHaveBeenCalled();
    const emittedData = (component.juryUpdated.emit as jasmine.Spy).calls.mostRecent().args[0] as JuryMemberRequest[];
    
    expect(emittedData.length).toBe(component.juryMembers.length);
    expect(emittedData[0].role).toBe(JuryRole.DIRECTEUR);
    expect(emittedData[1].role).toBe(JuryRole.PRESIDENT);
    expect(emittedData[2].role).toBe(JuryRole.RAPPORTEUR);
    expect(emittedData[3].role).toBe(JuryRole.RAPPORTEUR);
  });

  it('should handle role-based suggestions correctly', () => {
    component.ngOnInit();

    // Test rapporteur role suggestion
    const mockEvent = { target: { value: JuryRole.RAPPORTEUR } };
    component.onRoleChange(1, mockEvent);

    const member = component.juryMembers.at(1);
    expect(member.get('externe')?.value).toBeTruthy();
  });

  it('should prevent removal of director member', () => {
    component.directeurInfo = {
      nom: 'Dupont',
      prenom: 'Jean',
      etablissement: 'Université Test',
      grade: 'Professeur'
    };

    component.ngOnInit();
    const initialCount = component.juryMembers.length;

    // Try to remove director (index 0)
    component.removeMember(0);

    expect(component.juryMembers.length).toBe(initialCount);
  });

  it('should validate external members ratio correctly', () => {
    component.ngOnInit();

    // Make exactly half of the members external
    const totalMembers = component.juryMembers.length;
    const requiredExternal = Math.ceil(totalMembers / 2);

    for (let i = 0; i < requiredExternal; i++) {
      component.juryMembers.at(i).patchValue({ externe: true });
    }

    component['validateJury']();
    expect(component.validationStatus.externalRatio).toBeTruthy();
  });
});