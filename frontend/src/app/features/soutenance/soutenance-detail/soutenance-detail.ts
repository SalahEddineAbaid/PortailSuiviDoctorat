import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { SoutenanceResponse, SoutenanceStatus, JuryRole } from '../../../core/models/soutenance.model';
import { StatusTrackingComponent } from '../../../shared/components/status-tracking/status-tracking.component';

@Component({
  selector: 'app-soutenance-detail',
  standalone: true,
  imports: [CommonModule, RouterLink, StatusTrackingComponent],
  template: `
    <div class="soutenance-detail" *ngIf="soutenance$ | async as soutenance">
      <header class="detail-header">
        <div class="header-content">
          <h1>{{ soutenance.titrethese }}</h1>
          <div class="status-info">
            <span class="status-badge" [class]="'status-' + soutenance.statut.toLowerCase()">
              {{ getStatusLabel(soutenance.statut) }}
            </span>
          </div>
        </div>
        <div class="header-actions">
          <a routerLink="../liste" class="btn btn-secondary">
            <i class="icon-arrow-left"></i>
            Retour à la liste
          </a>
          <a [routerLink]="['./', 'edit']" class="btn btn-outline" 
             *ngIf="canEdit(soutenance.statut)">
            <i class="icon-edit"></i>
            Modifier
          </a>
        </div>
      </header>

      <div class="detail-content">
        <!-- Suivi du statut -->
        <div class="info-section">
          <app-status-tracking 
            [soutenanceStatus]="soutenance.statut"
            [title]="'Suivi de votre demande de soutenance'"
            [subtitle]="'Suivez l avancement de votre demande etape par etape'">
          </app-status-tracking>
        </div>

        <!-- Informations générales -->
        <div class="info-section">
          <h2>Informations générales</h2>
          <div class="info-grid">
            <div class="info-item">
              <label>Doctorant</label>
              <span>{{ soutenance.doctorant.FirstName }} {{ soutenance.doctorant.LastName }}</span>
            </div>
            <div class="info-item">
              <label>Directeur de thèse</label>
              <span>{{ soutenance.directeur.FirstName }} {{ soutenance.directeur.LastName }}</span>
            </div>
            <div class="info-item">
              <label>Email doctorant</label>
              <span>{{ soutenance.doctorant.email }}</span>
            </div>
            <div class="info-item">
              <label>Email directeur</label>
              <span>{{ soutenance.directeur.email }}</span>
            </div>
          </div>
        </div>

        <!-- Détails de la soutenance -->
        <div class="info-section">
          <h2>Détails de la soutenance</h2>
          <div class="info-grid">
            <div class="info-item full-width">
              <label>Titre de la thèse</label>
              <span class="thesis-title">{{ soutenance.titrethese }}</span>
            </div>
            <div class="info-item" *ngIf="soutenance.dateSoutenance">
              <label>Date de soutenance</label>
              <span>{{ soutenance.dateSoutenance | date:'dd/MM/yyyy à HH:mm' }}</span>
            </div>
            <div class="info-item" *ngIf="soutenance.lieuSoutenance">
              <label>Lieu de soutenance</label>
              <span>{{ soutenance.lieuSoutenance }}</span>
            </div>
          </div>
        </div>

        <!-- Prérequis -->
        <div class="info-section" *ngIf="soutenance.prerequis">
          <h2>État des prérequis</h2>
          <div class="prerequis-status" [class]="soutenance.prerequis.prerequisRemplis ? 'valid' : 'invalid'">
            <div class="prerequis-header">
              <i [class]="soutenance.prerequis.prerequisRemplis ? 'icon-check-circle' : 'icon-alert-circle'"></i>
              <span>{{ soutenance.prerequis.prerequisRemplis ? 'Tous les prérequis sont remplis' : 'Certains prérequis ne sont pas remplis' }}</span>
            </div>
            
            <div class="prerequis-list">
              <div class="prerequis-item" [class]="soutenance.prerequis.publicationsValides ? 'valid' : 'invalid'">
                <i [class]="soutenance.prerequis.publicationsValides ? 'icon-check' : 'icon-x'"></i>
                <span>Publications scientifiques</span>
              </div>
              <div class="prerequis-item" [class]="soutenance.prerequis.heuresFormationValides ? 'valid' : 'invalid'">
                <i [class]="soutenance.prerequis.heuresFormationValides ? 'icon-check' : 'icon-x'"></i>
                <span>Heures de formation</span>
              </div>
              <div class="prerequis-item" [class]="soutenance.prerequis.dureeDoctoratValide ? 'valid' : 'invalid'">
                <i [class]="soutenance.prerequis.dureeDoctoratValide ? 'icon-check' : 'icon-x'"></i>
                <span>Durée du doctorat</span>
              </div>
              <div class="prerequis-item" [class]="soutenance.prerequis.documentsCompletsValides ? 'valid' : 'invalid'">
                <i [class]="soutenance.prerequis.documentsCompletsValides ? 'icon-check' : 'icon-x'"></i>
                <span>Documents complets</span>
              </div>
            </div>

            <div class="prerequis-details" *ngIf="soutenance.prerequis.details && soutenance.prerequis.details.length > 0">
              <h4>Détails des prérequis</h4>
              <div class="detail-list">
                <div class="detail-item" *ngFor="let detail of soutenance.prerequis.details" 
                     [class]="detail.valide ? 'valid' : 'invalid'">
                  <div class="detail-header">
                    <i [class]="detail.valide ? 'icon-check' : 'icon-x'"></i>
                    <span class="detail-title">{{ detail.critere }}</span>
                  </div>
                  <div class="detail-content" *ngIf="detail.commentaire || detail.valeurRequise">
                    <p *ngIf="detail.commentaire">{{ detail.commentaire }}</p>
                    <div class="detail-values" *ngIf="detail.valeurRequise">
                      <span class="required">Requis: {{ detail.valeurRequise }}</span>
                      <span class="current" *ngIf="detail.valeurActuelle">Actuel: {{ detail.valeurActuelle }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Composition du jury -->
        <div class="info-section" *ngIf="soutenance.jury && soutenance.jury.length > 0">
          <h2>Composition du jury</h2>
          <div class="jury-list">
            <div class="jury-member" *ngFor="let member of soutenance.jury">
              <div class="member-info">
                <div class="member-name">
                  <strong>{{ member.prenom }} {{ member.nom }}</strong>
                  <span class="member-type" [class]="member.externe ? 'externe' : 'interne'">
                    {{ member.externe ? 'Externe' : 'Interne' }}
                  </span>
                </div>
                <div class="member-details">
                  <span class="role-badge" [class]="'role-' + member.role.toLowerCase()">
                    {{ getRoleLabel(member.role) }}
                  </span>
                  <span class="establishment">{{ member.etablissement }}</span>
                  <span class="grade">{{ member.grade }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Actions selon le statut -->
        <div class="action-section" *ngIf="getAvailableActions(soutenance.statut).length > 0">
          <h2>Actions disponibles</h2>
          <div class="action-buttons">
            <button 
              *ngFor="let action of getAvailableActions(soutenance.statut)"
              class="btn"
              [class]="action.class"
              (click)="executeAction(action.type, soutenance.id)">
              <i [class]="action.icon"></i>
              {{ action.label }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading state -->
    <div class="loading-state" *ngIf="!(soutenance$ | async)">
      <div class="spinner"></div>
      <p>Chargement des détails de la soutenance...</p>
    </div>
  `,
  styleUrls: ['./soutenance-detail.scss']
})
export class SoutenanceDetail implements OnInit {
  soutenance$!: Observable<SoutenanceResponse>;
  soutenanceId!: number;

  constructor(
    private soutenanceService: SoutenanceService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.soutenanceId = +params['id'];
      this.soutenance$ = this.soutenanceService.getSoutenance(this.soutenanceId);
    });
  }

  getStatusLabel(status: SoutenanceStatus): string {
    return this.soutenanceService.getStatusLabel(status);
  }

  getRoleLabel(role: JuryRole): string {
    const labels: Record<JuryRole, string> = {
      [JuryRole.PRESIDENT]: 'Président',
      [JuryRole.RAPPORTEUR]: 'Rapporteur',
      [JuryRole.EXAMINATEUR]: 'Examinateur',
      [JuryRole.DIRECTEUR]: 'Directeur',
      [JuryRole.CO_DIRECTEUR]: 'Co-directeur'
    };
    return labels[role] || role;
  }

  canEdit(status: SoutenanceStatus): boolean {
    return status === SoutenanceStatus.BROUILLON || status === SoutenanceStatus.REJETEE;
  }

  getAvailableActions(status: SoutenanceStatus): Array<{type: string, label: string, class: string, icon: string}> {
    const actions = [];

    switch (status) {
      case SoutenanceStatus.BROUILLON:
        actions.push({
          type: 'submit',
          label: 'Soumettre la demande',
          class: 'btn-primary',
          icon: 'icon-send'
        });
        break;
      
      case SoutenanceStatus.AUTORISEE:
        actions.push({
          type: 'schedule',
          label: 'Planifier la soutenance',
          class: 'btn-primary',
          icon: 'icon-calendar'
        });
        break;
    }

    return actions;
  }

  executeAction(actionType: string, soutenanceId: number): void {
    switch (actionType) {
      case 'submit':
        this.submitSoutenance(soutenanceId);
        break;
      case 'schedule':
        this.scheduleSoutenance(soutenanceId);
        break;
    }
  }

  private submitSoutenance(soutenanceId: number): void {
    // TODO: Implement submit logic
    console.log('Submitting soutenance:', soutenanceId);
  }

  private scheduleSoutenance(soutenanceId: number): void {
    // TODO: Implement scheduling logic
    console.log('Scheduling soutenance:', soutenanceId);
  }
}