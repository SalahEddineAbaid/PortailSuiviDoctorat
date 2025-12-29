import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { JuryMember, JuryMemberRequest, JuryRole } from '../../../core/models/soutenance.model';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { CustomValidators } from '../../../core/validators/custom-validators';

@Component({
  selector: 'app-jury-proposal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="jury-proposal">
      <header class="section-header">
        <h3>Composition du jury</h3>
        <p class="section-description">
          Proposez la composition de votre jury de soutenance. Le jury doit comprendre au minimum 4 membres 
          dont au moins 2 rapporteurs externes à l'établissement.
        </p>
      </header>

      <form [formGroup]="juryForm" class="jury-form">
        <!-- Règles de composition -->
        <div class="jury-rules" *ngIf="showRules">
          <h4>Règles de composition du jury</h4>
          <ul class="rules-list">
            <li>Minimum 4 membres, maximum 8 membres</li>
            <li>Au moins 2 rapporteurs externes à l'établissement</li>
            <li>Un président de jury (différent du directeur de thèse)</li>
            <li>Le directeur de thèse fait partie du jury</li>
            <li>Au moins 50% de membres externes à l'établissement</li>
          </ul>
          <button type="button" class="btn btn-link" (click)="showRules = false">
            Masquer les règles
          </button>
        </div>

        <div class="rules-toggle" *ngIf="!showRules">
          <button type="button" class="btn btn-link" (click)="showRules = true">
            <i class="icon-info"></i> Voir les règles de composition
          </button>
        </div>

        <!-- Validation du jury -->
        <div class="jury-validation" *ngIf="juryMembers.length > 0">
          <div class="validation-item" [class.valid]="validationStatus.minMembers" [class.invalid]="!validationStatus.minMembers">
            <i class="icon" [class.icon-check]="validationStatus.minMembers" [class.icon-x]="!validationStatus.minMembers"></i>
            <span>Minimum 4 membres ({{ juryMembers.length }}/4)</span>
          </div>
          <div class="validation-item" [class.valid]="validationStatus.minRapporteurs" [class.invalid]="!validationStatus.minRapporteurs">
            <i class="icon" [class.icon-check]="validationStatus.minRapporteurs" [class.icon-x]="!validationStatus.minRapporteurs"></i>
            <span>Au moins 2 rapporteurs externes ({{ getExternalRapporteursCount() }}/2)</span>
          </div>
          <div class="validation-item" [class.valid]="validationStatus.hasPresident" [class.invalid]="!validationStatus.hasPresident">
            <i class="icon" [class.icon-check]="validationStatus.hasPresident" [class.icon-x]="!validationStatus.hasPresident"></i>
            <span>Un président de jury</span>
          </div>
          <div class="validation-item" [class.valid]="validationStatus.externalRatio" [class.invalid]="!validationStatus.externalRatio">
            <i class="icon" [class.icon-check]="validationStatus.externalRatio" [class.icon-x]="!validationStatus.externalRatio"></i>
            <span>Au moins 50% de membres externes ({{ getExternalMembersCount() }}/{{ Math.ceil(juryMembers.length / 2) }})</span>
          </div>
        </div>

        <!-- Liste des membres du jury -->
        <div class="jury-members" formArrayName="juryMembers">
          <div class="member-card" 
               *ngFor="let member of juryMembers.controls; let i = index" 
               [formGroupName]="i"
               [class.director]="member.get('role')?.value === 'DIRECTEUR'">
            
            <div class="member-header">
              <h4>Membre {{ i + 1 }}</h4>
              <div class="member-actions">
                <button type="button" 
                        class="btn btn-icon btn-danger" 
                        (click)="removeMember(i)"
                        [disabled]="member.get('role')?.value === 'DIRECTEUR'"
                        title="Supprimer ce membre">
                  <i class="icon-trash"></i>
                </button>
              </div>
            </div>

            <div class="member-form">
              <div class="form-row">
                <div class="form-group">
                  <label [for]="'nom-' + i">Nom *</label>
                  <input 
                    type="text"
                    [id]="'nom-' + i"
                    formControlName="nom"
                    placeholder="Nom de famille"
                    [class.error]="member.get('nom')?.invalid && member.get('nom')?.touched">
                  <div class="error-message" *ngIf="member.get('nom')?.invalid && member.get('nom')?.touched">
                    <span *ngIf="member.get('nom')?.errors?.['required']">Le nom est obligatoire</span>
                    <span *ngIf="member.get('nom')?.errors?.['minlength']">Le nom doit contenir au moins 2 caractères</span>
                  </div>
                </div>

                <div class="form-group">
                  <label [for]="'prenom-' + i">Prénom *</label>
                  <input 
                    type="text"
                    [id]="'prenom-' + i"
                    formControlName="prenom"
                    placeholder="Prénom"
                    [class.error]="member.get('prenom')?.invalid && member.get('prenom')?.touched">
                  <div class="error-message" *ngIf="member.get('prenom')?.invalid && member.get('prenom')?.touched">
                    <span *ngIf="member.get('prenom')?.errors?.['required']">Le prénom est obligatoire</span>
                    <span *ngIf="member.get('prenom')?.errors?.['minlength']">Le prénom doit contenir au moins 2 caractères</span>
                  </div>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label [for]="'etablissement-' + i">Établissement *</label>
                  <input 
                    type="text"
                    [id]="'etablissement-' + i"
                    formControlName="etablissement"
                    placeholder="Université, École, Entreprise..."
                    [class.error]="member.get('etablissement')?.invalid && member.get('etablissement')?.touched">
                  <div class="error-message" *ngIf="member.get('etablissement')?.invalid && member.get('etablissement')?.touched">
                    <span *ngIf="member.get('etablissement')?.errors?.['required']">L'établissement est obligatoire</span>
                    <span *ngIf="member.get('etablissement')?.errors?.['minlength']">L'établissement doit contenir au moins 3 caractères</span>
                  </div>
                </div>

                <div class="form-group">
                  <label [for]="'grade-' + i">Grade/Fonction *</label>
                  <input 
                    type="text"
                    [id]="'grade-' + i"
                    formControlName="grade"
                    placeholder="Professeur, Maître de conférences, Ingénieur..."
                    [class.error]="member.get('grade')?.invalid && member.get('grade')?.touched">
                  <div class="error-message" *ngIf="member.get('grade')?.invalid && member.get('grade')?.touched">
                    <span *ngIf="member.get('grade')?.errors?.['required']">Le grade est obligatoire</span>
                    <span *ngIf="member.get('grade')?.errors?.['minlength']">Le grade doit contenir au moins 3 caractères</span>
                  </div>
                </div>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label [for]="'role-' + i">Rôle dans le jury *</label>
                  <select 
                    [id]="'role-' + i"
                    formControlName="role"
                    [class.error]="member.get('role')?.invalid && member.get('role')?.touched"
                    [disabled]="member.get('role')?.value === 'DIRECTEUR'"
                    (change)="onRoleChange(i, $event)">
                    <option value="">Sélectionner un rôle</option>
                    <option *ngFor="let role of availableRoles" [value]="role.value">
                      {{ role.label }}
                    </option>
                  </select>
                  <div class="error-message" *ngIf="member.get('role')?.invalid && member.get('role')?.touched">
                    <span *ngIf="member.get('role')?.errors?.['required']">Le rôle est obligatoire</span>
                  </div>
                </div>

                <div class="form-group">
                  <label class="checkbox-label">
                    <input 
                      type="checkbox"
                      formControlName="externe"
                      [disabled]="member.get('role')?.value === 'DIRECTEUR'"
                      (change)="onExterneChange(i, $event)">
                    <span class="checkbox-text">Membre externe à l'établissement</span>
                  </label>
                  <div class="help-text">
                    Cochez si ce membre n'appartient pas à votre établissement de thèse
                  </div>
                </div>
              </div>

              <!-- Indicateurs visuels -->
              <div class="member-indicators">
                <span class="indicator" 
                      *ngIf="member.get('externe')?.value" 
                      class="external">
                  <i class="icon-external-link"></i> Externe
                </span>
                <span class="indicator" 
                      *ngIf="member.get('role')?.value === 'RAPPORTEUR'" 
                      class="rapporteur">
                  <i class="icon-file-text"></i> Rapporteur
                </span>
                <span class="indicator" 
                      *ngIf="member.get('role')?.value === 'PRESIDENT'" 
                      class="president">
                  <i class="icon-crown"></i> Président
                </span>
                <span class="indicator" 
                      *ngIf="member.get('role')?.value === 'DIRECTEUR'" 
                      class="director">
                  <i class="icon-user-check"></i> Directeur
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions pour ajouter des membres -->
        <div class="add-member-actions">
          <button type="button" 
                  class="btn btn-outline" 
                  (click)="addMember()"
                  [disabled]="juryMembers.length >= maxMembers">
            <i class="icon-plus"></i>
            Ajouter un membre
          </button>
          
          <div class="member-count">
            {{ juryMembers.length }} / {{ maxMembers }} membres
          </div>
        </div>

        <!-- Messages d'erreur globaux -->
        <div class="error-alert" *ngIf="errorMessage">
          <i class="icon-alert-circle"></i>
          <span>{{ errorMessage }}</span>
        </div>

        <!-- Actions du formulaire -->
        <div class="form-actions">
          <button type="button" 
                  class="btn btn-secondary" 
                  (click)="onCancel()">
            Annuler
          </button>
          <button type="button" 
                  class="btn btn-primary" 
                  (click)="onSave()"
                  [disabled]="!isJuryValid() || isSubmitting">
            {{ isSubmitting ? 'Sauvegarde...' : 'Sauvegarder la composition' }}
          </button>
        </div>
      </form>
    </div>
  `,
  styleUrls: ['./jury-proposal.component.scss']
})
export class JuryProposalComponent implements OnInit {
  @Input() soutenanceId?: number;
  @Input() existingJury: JuryMember[] = [];
  @Input() directeurInfo?: { nom: string; prenom: string; etablissement: string; grade: string };
  @Output() juryUpdated = new EventEmitter<JuryMemberRequest[]>();
  @Output() cancel = new EventEmitter<void>();

  juryForm: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  showRules = false;
  maxMembers = 8;
  minMembers = 4;

  // Validation status
  validationStatus = {
    minMembers: false,
    minRapporteurs: false,
    hasPresident: false,
    externalRatio: false
  };

  // Available roles for jury members
  availableRoles = [
    { value: JuryRole.PRESIDENT, label: 'Président du jury' },
    { value: JuryRole.RAPPORTEUR, label: 'Rapporteur' },
    { value: JuryRole.EXAMINATEUR, label: 'Examinateur' },
    { value: JuryRole.CO_DIRECTEUR, label: 'Co-directeur' }
  ];

  constructor(
    private fb: FormBuilder,
    private soutenanceService: SoutenanceService
  ) {
    this.juryForm = this.createForm();
  }

  ngOnInit(): void {
    this.initializeJury();
    this.validateJury();
  }

  private createForm(): FormGroup {
    return this.fb.group({
      juryMembers: this.fb.array([])
    });
  }

  get juryMembers(): FormArray {
    return this.juryForm.get('juryMembers') as FormArray;
  }

  private initializeJury(): void {
    // Add director as first member if provided
    if (this.directeurInfo) {
      this.addDirectorMember();
    }

    // Add existing jury members
    if (this.existingJury && this.existingJury.length > 0) {
      this.existingJury.forEach(member => {
        if (member.role !== JuryRole.DIRECTEUR) {
          this.addExistingMember(member);
        }
      });
    }

    // Ensure minimum members
    while (this.juryMembers.length < this.minMembers) {
      this.addMember();
    }
  }

  private addDirectorMember(): void {
    if (!this.directeurInfo) return;

    const directorGroup = this.fb.group({
      nom: [this.directeurInfo.nom, [Validators.required, Validators.minLength(2)]],
      prenom: [this.directeurInfo.prenom, [Validators.required, Validators.minLength(2)]],
      etablissement: [this.directeurInfo.etablissement, [Validators.required, Validators.minLength(3)]],
      grade: [this.directeurInfo.grade, [Validators.required, Validators.minLength(3)]],
      role: [{ value: JuryRole.DIRECTEUR, disabled: true }, [Validators.required]],
      externe: [{ value: false, disabled: true }]
    });

    this.juryMembers.push(directorGroup);
  }

  private addExistingMember(member: JuryMember): void {
    const memberGroup = this.fb.group({
      nom: [member.nom, [Validators.required, Validators.minLength(2)]],
      prenom: [member.prenom, [Validators.required, Validators.minLength(2)]],
      etablissement: [member.etablissement, [Validators.required, Validators.minLength(3)]],
      grade: [member.grade, [Validators.required, Validators.minLength(3)]],
      role: [member.role, [Validators.required]],
      externe: [member.externe]
    });

    this.juryMembers.push(memberGroup);
  }

  addMember(): void {
    if (this.juryMembers.length >= this.maxMembers) return;

    const memberGroup = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2)]],
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      etablissement: ['', [Validators.required, Validators.minLength(3)]],
      grade: ['', [Validators.required, Validators.minLength(3)]],
      role: ['', [Validators.required]],
      externe: [false]
    });

    this.juryMembers.push(memberGroup);
    this.validateJury();
  }

  removeMember(index: number): void {
    const member = this.juryMembers.at(index);
    if (member.get('role')?.value === JuryRole.DIRECTEUR) {
      return; // Cannot remove director
    }

    this.juryMembers.removeAt(index);
    this.validateJury();
  }

  onRoleChange(index: number, event: any): void {
    const role = event.target.value;
    const member = this.juryMembers.at(index);
    
    // If selecting RAPPORTEUR, suggest external
    if (role === JuryRole.RAPPORTEUR) {
      member.get('externe')?.setValue(true);
    }
    
    this.validateJury();
  }

  onExterneChange(index: number, event: any): void {
    this.validateJury();
  }

  private validateJury(): void {
    const members = this.juryMembers.controls;
    
    // Check minimum members
    this.validationStatus.minMembers = members.length >= this.minMembers;
    
    // Check minimum external rapporteurs
    const externalRapporteurs = members.filter((memberControl: any) => 
      memberControl.get('role')?.value === JuryRole.RAPPORTEUR && memberControl.get('externe')?.value
    );
    this.validationStatus.minRapporteurs = externalRapporteurs.length >= 2;
    
    // Check president
    const presidents = members.filter((memberControl: any) => memberControl.get('role')?.value === JuryRole.PRESIDENT);
    this.validationStatus.hasPresident = presidents.length === 1;
    
    // Check external ratio (at least 50%)
    const externalMembers = members.filter((memberControl: any) => memberControl.get('externe')?.value);
    const requiredExternal = Math.ceil(members.length / 2);
    this.validationStatus.externalRatio = externalMembers.length >= requiredExternal;
  }

  isJuryValid(): boolean {
    const formValid = this.juryForm.valid;
    const validationPassed = Object.values(this.validationStatus).every(status => status);
    return formValid && validationPassed;
  }

  getExternalRapporteursCount(): number {
    return this.juryMembers.controls.filter((memberControl: any) => 
      memberControl.get('role')?.value === JuryRole.RAPPORTEUR && memberControl.get('externe')?.value
    ).length;
  }

  getExternalMembersCount(): number {
    return this.juryMembers.controls.filter((memberControl: any) => memberControl.get('externe')?.value).length;
  }

  onSave(): void {
    if (!this.isJuryValid()) {
      this.errorMessage = 'Veuillez corriger les erreurs avant de sauvegarder';
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';

    const juryData: JuryMemberRequest[] = this.juryMembers.controls.map((memberControl: any) => ({
      nom: memberControl.get('nom')?.value,
      prenom: memberControl.get('prenom')?.value,
      etablissement: memberControl.get('etablissement')?.value,
      grade: memberControl.get('grade')?.value,
      role: memberControl.get('role')?.value,
      externe: memberControl.get('externe')?.value
    }));

    // Emit the jury data to parent component
    this.juryUpdated.emit(juryData);
    this.isSubmitting = false;
  }

  onCancel(): void {
    this.cancel.emit();
  }

  // Expose Math for template
  Math = Math;
}