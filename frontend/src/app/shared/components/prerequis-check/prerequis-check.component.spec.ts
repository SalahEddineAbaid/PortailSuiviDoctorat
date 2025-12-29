import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';

import { PrerequisCheckComponent } from './prerequis-check.component';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { PrerequisStatus, PrerequisDetail } from '../../../core/models/soutenance.model';

describe('PrerequisCheckComponent', () => {
  let component: PrerequisCheckComponent;
  let fixture: ComponentFixture<PrerequisCheckComponent>;
  let soutenanceService: jasmine.SpyObj<SoutenanceService>;

  const mockPrerequisStatus: PrerequisStatus = {
    publicationsValides: true,
    heuresFormationValides: false,
    dureeDoctoratValide: true,
    documentsCompletsValides: true,
    prerequisRemplis: false,
    details: [
      {
        critere: 'publications',
        valide: true,
        commentaire: '2 publications validées',
        valeurRequise: '2',
        valeurActuelle: '2'
      },
      {
        critere: 'heures_formation',
        valide: false,
        commentaire: 'Heures insuffisantes',
        valeurRequise: '100',
        valeurActuelle: '80'
      },
      {
        critere: 'duree_doctorat',
        valide: true,
        commentaire: 'Durée conforme',
        valeurRequise: '3 ans',
        valeurActuelle: '3 ans'
      }
    ]
  };

  beforeEach(async () => {
    const soutenanceServiceSpy = jasmine.createSpyObj('SoutenanceService', ['checkPrerequis']);

    await TestBed.configureTestingModule({
      imports: [PrerequisCheckComponent, HttpClientTestingModule],
      providers: [
        { provide: SoutenanceService, useValue: soutenanceServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PrerequisCheckComponent);
    component = fixture.componentInstance;
    soutenanceService = TestBed.inject(SoutenanceService) as jasmine.SpyObj<SoutenanceService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should check prerequis on init when autoCheck is true', () => {
    component.doctorantId = 1;
    component.autoCheck = true;
    soutenanceService.checkPrerequis.and.returnValue(of(mockPrerequisStatus));

    component.ngOnInit();

    expect(soutenanceService.checkPrerequis).toHaveBeenCalledWith(1);
  });

  it('should not check prerequis on init when autoCheck is false', () => {
    component.doctorantId = 1;
    component.autoCheck = false;

    component.ngOnInit();

    expect(soutenanceService.checkPrerequis).not.toHaveBeenCalled();
  });

  it('should emit prerequisStatusChange when prerequis are loaded', () => {
    spyOn(component.prerequisStatusChange, 'emit');
    spyOn(component.canSubmitChange, 'emit');
    component.doctorantId = 1;
    soutenanceService.checkPrerequis.and.returnValue(of(mockPrerequisStatus));

    component.checkPrerequis();

    expect(component.prerequisStatusChange.emit).toHaveBeenCalledWith(mockPrerequisStatus);
    expect(component.canSubmitChange.emit).toHaveBeenCalledWith(false);
  });

  it('should handle error when checking prerequis', () => {
    spyOn(component.canSubmitChange, 'emit');
    component.doctorantId = 1;
    soutenanceService.checkPrerequis.and.returnValue(throwError(() => new Error('API Error')));

    component.checkPrerequis();

    expect(component.error$.value).toBe('Erreur lors de la vérification des prérequis');
    expect(component.canSubmitChange.emit).toHaveBeenCalledWith(false);
  });

  it('should calculate completion percentage correctly', () => {
    component.prerequisStatus$.next(mockPrerequisStatus);

    expect(component.completionPercentage).toBe(67); // 2/3 * 100 = 66.67 rounded to 67
  });

  it('should return correct prerequis counts', () => {
    component.prerequisStatus$.next(mockPrerequisStatus);

    expect(component.validPrerequisCount).toBe(2);
    expect(component.totalPrerequisCount).toBe(3);
  });

  it('should return correct global status class', () => {
    component.loading$.next(true);
    expect(component.globalStatusClass).toBe('prerequis-loading');

    component.loading$.next(false);
    component.error$.next('Error');
    expect(component.globalStatusClass).toBe('prerequis-error');

    component.error$.next(null);
    component.prerequisStatus$.next({ ...mockPrerequisStatus, prerequisRemplis: true });
    expect(component.globalStatusClass).toBe('prerequis-success');

    component.prerequisStatus$.next(mockPrerequisStatus);
    expect(component.globalStatusClass).toBe('prerequis-warning');
  });

  it('should get correct critere label', () => {
    expect(component.getCritereLabel('publications')).toBe('Publications scientifiques');
    expect(component.getCritereLabel('heures_formation')).toBe('Heures de formation');
    expect(component.getCritereLabel('unknown')).toBe('unknown');
  });

  it('should get correct critere help', () => {
    const detail: PrerequisDetail = {
      critere: 'test',
      valide: false,
      commentaire: 'Test comment'
    };
    expect(component.getCritereHelp(detail)).toBe('Test comment');

    const detailWithValues: PrerequisDetail = {
      critere: 'test',
      valide: false,
      valeurRequise: '100',
      valeurActuelle: '80'
    };
    expect(component.getCritereHelp(detailWithValues)).toBe('Requis: 100, Actuel: 80');
  });

  it('should track prerequis details by critere', () => {
    const detail: PrerequisDetail = {
      critere: 'test_critere',
      valide: true
    };

    expect(component.trackByPrerequisDetail(0, detail)).toBe('test_critere');
  });
});