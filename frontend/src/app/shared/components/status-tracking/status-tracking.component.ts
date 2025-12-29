import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { SoutenanceStatus } from '../../../core/models/soutenance.model';

export interface StatusStep {
  id: string;
  label: string;
  description: string;
  status: 'completed' | 'current' | 'upcoming' | 'blocked';
  date?: Date;
  icon: string;
  details?: string[];
}

@Component({
  selector: 'app-status-tracking',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="status-tracking">
      <div class="tracking-header" *ngIf="title">
        <h3>{{ title }}</h3>
        <p class="tracking-subtitle" *ngIf="subtitle">{{ subtitle }}</p>
      </div>

      <div class="tracking-progress">
        <div class="progress-bar">
          <div class="progress-fill" [style.width.%]="getProgressPercentage()"></div>
        </div>
        <div class="progress-text">
          <span>{{ getCompletedSteps() }} / {{ steps.length }} étapes complétées</span>
        </div>
      </div>

      <div class="tracking-steps">
        <div 
          class="step-item" 
          *ngFor="let step of steps; let i = index; trackBy: trackByStepId"
          [class]="getStepClass(step)"
          [class.last]="i === steps.length - 1">
          
          <div class="step-connector" *ngIf="i < steps.length - 1"></div>
          
          <div class="step-icon">
            <i [class]="step.icon" *ngIf="step.status !== 'current'"></i>
            <div class="spinner-small" *ngIf="step.status === 'current'"></div>
          </div>
          
          <div class="step-content">
            <div class="step-header">
              <h4 class="step-title">{{ step.label }}</h4>
              <span class="step-date" *ngIf="step.date">
                {{ step.date | date:'dd/MM/yyyy' }}
              </span>
            </div>
            
            <p class="step-description">{{ step.description }}</p>
            
            <div class="step-details" *ngIf="step.details && step.details.length > 0">
              <ul class="details-list">
                <li *ngFor="let detail of step.details">{{ detail }}</li>
              </ul>
            </div>
            
            <div class="step-status">
              <span class="status-badge" [class]="'status-' + step.status">
                {{ getStatusLabel(step.status) }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions section -->
      <div class="tracking-actions" *ngIf="showActions && availableActions.length > 0">
        <h4>Actions disponibles</h4>
        <div class="action-buttons">
          <button 
            *ngFor="let action of availableActions"
            class="btn"
            [class]="action.class"
            (click)="onActionClick(action.type)"
            [disabled]="action.disabled">
            <i [class]="action.icon"></i>
            {{ action.label }}
          </button>
        </div>
      </div>

      <!-- Next steps section -->
      <div class="tracking-next-steps" *ngIf="showNextSteps">
        <h4>Prochaines étapes</h4>
        <div class="next-steps-list">
          <div class="next-step" *ngFor="let step of getUpcomingSteps()">
            <i [class]="step.icon"></i>
            <div class="next-step-content">
              <span class="next-step-title">{{ step.label }}</span>
              <span class="next-step-description">{{ step.description }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./status-tracking.component.scss']
})
export class StatusTrackingComponent implements OnInit {
  @Input() soutenanceStatus!: SoutenanceStatus;
  @Input() title: string = 'Suivi de la demande';
  @Input() subtitle?: string;
  @Input() showActions: boolean = true;
  @Input() showNextSteps: boolean = true;
  @Input() customSteps?: StatusStep[];

  steps: StatusStep[] = [];
  availableActions: Array<{
    type: string;
    label: string;
    class: string;
    icon: string;
    disabled: boolean;
  }> = [];

  ngOnInit(): void {
    this.initializeSteps();
    this.updateStepsStatus();
    this.initializeActions();
  }

  private initializeSteps(): void {
    if (this.customSteps) {
      this.steps = [...this.customSteps];
      return;
    }

    this.steps = [
      {
        id: 'creation',
        label: 'Création du dossier',
        description: 'Création et préparation de votre demande de soutenance',
        status: 'completed',
        icon: 'fas fa-file-plus',
        details: [
          'Informations générales saisies',
          'Titre de thèse défini'
        ]
      },
      {
        id: 'prerequis',
        label: 'Vérification des prérequis',
        description: 'Validation des conditions nécessaires à la soutenance',
        status: 'upcoming',
        icon: 'fas fa-check-circle',
        details: [
          'Publications scientifiques',
          'Heures de formation',
          'Durée du doctorat',
          'Documents obligatoires'
        ]
      },
      {
        id: 'jury',
        label: 'Composition du jury',
        description: 'Proposition et validation de la composition du jury',
        status: 'upcoming',
        icon: 'fas fa-users',
        details: [
          'Membres du jury proposés',
          'Rôles attribués',
          'Validation par l\'administration'
        ]
      },
      {
        id: 'submission',
        label: 'Soumission de la demande',
        description: 'Envoi officiel de votre demande de soutenance',
        status: 'upcoming',
        icon: 'fas fa-paper-plane'
      },
      {
        id: 'validation',
        label: 'Validation administrative',
        description: 'Examen et validation par l\'administration',
        status: 'upcoming',
        icon: 'fas fa-stamp',
        details: [
          'Vérification des documents',
          'Validation du jury',
          'Contrôle des prérequis'
        ]
      },
      {
        id: 'authorization',
        label: 'Autorisation de soutenance',
        description: 'Autorisation officielle de soutenir votre thèse',
        status: 'upcoming',
        icon: 'fas fa-certificate'
      },
      {
        id: 'scheduling',
        label: 'Planification',
        description: 'Organisation de la date et du lieu de soutenance',
        status: 'upcoming',
        icon: 'fas fa-calendar-alt'
      },
      {
        id: 'defense',
        label: 'Soutenance',
        description: 'Présentation et défense de votre thèse',
        status: 'upcoming',
        icon: 'fas fa-graduation-cap'
      }
    ];
  }

  private updateStepsStatus(): void {
    const statusMapping = this.getStatusMapping();
    const currentStepIndex = statusMapping[this.soutenanceStatus] || 0;

    this.steps.forEach((step, index) => {
      if (index < currentStepIndex) {
        step.status = 'completed';
        step.date = new Date(); // In real implementation, this would come from backend
      } else if (index === currentStepIndex) {
        step.status = 'current';
      } else {
        step.status = 'upcoming';
      }
    });

    // Handle special cases
    if (this.soutenanceStatus === SoutenanceStatus.REJETEE) {
      const rejectedStepIndex = Math.max(0, currentStepIndex - 1);
      this.steps[rejectedStepIndex].status = 'blocked';
      this.steps[rejectedStepIndex].details = [
        ...(this.steps[rejectedStepIndex].details || []),
        'Demande rejetée - corrections nécessaires'
      ];
    }
  }

  private getStatusMapping(): Record<SoutenanceStatus, number> {
    return {
      [SoutenanceStatus.BROUILLON]: 0,
      [SoutenanceStatus.SOUMISE]: 3,
      [SoutenanceStatus.EN_COURS_VALIDATION]: 4,
      [SoutenanceStatus.AUTORISEE]: 5,
      [SoutenanceStatus.REJETEE]: 4,
      [SoutenanceStatus.SOUTENUE]: 7
    };
  }

  private initializeActions(): void {
    this.availableActions = [];

    switch (this.soutenanceStatus) {
      case SoutenanceStatus.BROUILLON:
        this.availableActions.push({
          type: 'edit',
          label: 'Modifier le dossier',
          class: 'btn-outline',
          icon: 'fas fa-edit',
          disabled: false
        });
        this.availableActions.push({
          type: 'submit',
          label: 'Soumettre la demande',
          class: 'btn-primary',
          icon: 'fas fa-paper-plane',
          disabled: false
        });
        break;

      case SoutenanceStatus.REJETEE:
        this.availableActions.push({
          type: 'edit',
          label: 'Corriger et modifier',
          class: 'btn-primary',
          icon: 'fas fa-edit',
          disabled: false
        });
        break;

      case SoutenanceStatus.AUTORISEE:
        this.availableActions.push({
          type: 'schedule',
          label: 'Planifier la soutenance',
          class: 'btn-primary',
          icon: 'fas fa-calendar-alt',
          disabled: false
        });
        break;
    }
  }

  getStepClass(step: StatusStep): string {
    return `step-${step.status}`;
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'completed': 'Terminé',
      'current': 'En cours',
      'upcoming': 'À venir',
      'blocked': 'Bloqué'
    };
    return labels[status] || status;
  }

  getProgressPercentage(): number {
    const completedSteps = this.steps.filter(step => step.status === 'completed').length;
    return (completedSteps / this.steps.length) * 100;
  }

  getCompletedSteps(): number {
    return this.steps.filter(step => step.status === 'completed').length;
  }

  getUpcomingSteps(): StatusStep[] {
    return this.steps.filter(step => step.status === 'upcoming').slice(0, 3);
  }

  trackByStepId(index: number, step: StatusStep): string {
    return step.id;
  }

  onActionClick(actionType: string): void {
    // Emit event to parent component
    console.log('Action clicked:', actionType);
    // In a real implementation, this would emit an event or call a service
  }
}