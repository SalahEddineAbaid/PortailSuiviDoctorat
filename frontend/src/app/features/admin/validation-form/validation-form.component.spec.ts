import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ValidationFormComponent } from './validation-form.component';

describe('ValidationFormComponent', () => {
  let component: ValidationFormComponent;
  let fixture: ComponentFixture<ValidationFormComponent>;

  const mockDossier = {
    id: 1,
    type: 'inscription' as const,
    titre: 'Étude des algorithmes quantiques',
    doctorant: {
      nom: 'Dupont',
      prenom: 'Jean',
      email: 'jean.dupont@example.com'
    },
    directeur: {
      nom: 'Martin',
      prenom: 'Pierre'
    },
    statut: 'SOUMISE',
    dateCreation: new Date('2024-01-15'),
    documentsManquants: ['CV', 'Diplôme Master']
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ReactiveFormsModule,
        ValidationFormComponent
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ValidationFormComponent);
    component = fixture.componentInstance;
    component.dossier = mockDossier;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.validationForm.get('valide')?.value).toBe(true);
    expect(component.validationForm.get('commentaire')?.value).toBe('');
  });

  it('should display dossier information', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Étude des algorithmes quantiques');
    expect(compiled.textContent).toContain('Jean Dupont');
    expect(compiled.textContent).toContain('Pierre Martin');
  });

  it('should show documents manquants when present', () => {
    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Documents manquants');
    expect(compiled.textContent).toContain('CV');
    expect(compiled.textContent).toContain('Diplôme Master');
  });

  it('should require commentaire for rejection', () => {
    component.validationForm.patchValue({ valide: false });
    fixture.detectChanges();

    const commentaireControl = component.validationForm.get('commentaire');
    expect(commentaireControl?.hasError('required')).toBe(true);
  });

  it('should make commentaire optional for validation', () => {
    component.validationForm.patchValue({ valide: true });
    fixture.detectChanges();

    const commentaireControl = component.validationForm.get('commentaire');
    expect(commentaireControl?.hasError('required')).toBe(false);
  });

  it('should emit validation data on submit', () => {
    spyOn(component.validationSubmitted, 'emit');
    
    component.validationForm.patchValue({
      valide: true,
      commentaire: 'Dossier complet et conforme'
    });

    component.onSubmit();

    expect(component.validationSubmitted.emit).toHaveBeenCalledWith({
      validation: {
        valide: true,
        commentaire: 'Dossier complet et conforme'
      },
      options: {
        notifierDoctorant: true,
        notifierDirecteur: false
      }
    });
  });

  it('should emit cancelled event', () => {
    spyOn(component.cancelled, 'emit');
    
    component.onCancel();

    expect(component.cancelled.emit).toHaveBeenCalled();
  });

  it('should show rejection options when rejecting', () => {
    component.validationForm.patchValue({ valide: false });
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.rejection-options')).toBeTruthy();
    expect(compiled.textContent).toContain('Notifier le doctorant par email');
    expect(compiled.textContent).toContain('Notifier le directeur de thèse');
  });

  it('should hide rejection options when validating', () => {
    component.validationForm.patchValue({ valide: true });
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.querySelector('.rejection-options')).toBeFalsy();
  });

  it('should format date correctly', () => {
    const date = new Date('2024-01-15');
    const formatted = component.formatDate(date);
    expect(formatted).toBe('15/01/2024');
  });

  it('should return correct badge classes', () => {
    expect(component.getTypeBadgeClass('inscription')).toBe('primary');
    expect(component.getTypeBadgeClass('soutenance')).toBe('info');
    
    expect(component.getStatutBadgeClass('SOUMISE')).toBe('info');
    expect(component.getStatutBadgeClass('VALIDEE')).toBe('success');
    expect(component.getStatutBadgeClass('REJETEE')).toBe('danger');
  });

  it('should return correct status labels', () => {
    expect(component.getStatutLabel('SOUMISE')).toBe('Soumis');
    expect(component.getStatutLabel('VALIDEE')).toBe('Validé');
    expect(component.getStatutLabel('REJETEE')).toBe('Rejeté');
  });

  it('should update placeholder based on validation action', () => {
    component.validationForm.patchValue({ valide: true });
    expect(component.getCommentairePlaceholder()).toContain('optionnel');

    component.validationForm.patchValue({ valide: false });
    expect(component.getCommentairePlaceholder()).toContain('raisons du rejet');
  });
});