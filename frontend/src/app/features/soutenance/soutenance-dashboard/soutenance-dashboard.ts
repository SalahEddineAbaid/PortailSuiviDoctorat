import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { AuthService } from '../../../core/services/auth.service';
import { SoutenanceResponse, SoutenanceStatus, PrerequisStatus } from '../../../core/models/soutenance.model';
import { PrerequisCheckComponent } from '../../../shared/components/prerequis-check/prerequis-check.component';

@Component({
  selector: 'app-soutenance-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, PrerequisCheckComponent],
  template: `
    <div class="soutenance-dashboard">
      <header class="dashboard-header">
        <h1>Tableau de bord - Soutenances</h1>
        <p class="subtitle">Gérez vos demandes de soutenance et suivez leur progression</p>
      </header>

      <div class="dashboard-stats">
        <div class="stat-card">
          <div class="stat-icon">
            <i class="icon-document"></i>
          </div>
          <div class="stat-content">
            <h3>{{ (soutenances$ | async)?.length || 0 }}</h3>
            <p>Demandes totales</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <i class="icon-clock"></i>
          </div>
          <div class="stat-content">
            <h3>{{ getCountByStatus(SoutenanceStatus.EN_COURS_VALIDATION) }}</h3>
            <p>En cours de validation</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <i class="icon-check"></i>
          </div>
          <div class="stat-content">
            <h3>{{ getCountByStatus(SoutenanceStatus.AUTORISEE) }}</h3>
            <p>Autorisées</p>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon">
            <i class="icon-calendar"></i>
          </div>
          <div class="stat-content">
            <h3>{{ getCountByStatus(SoutenanceStatus.SOUTENUE) }}</h3>
            <p>Soutenues</p>
          </div>
        </div>
      </div>

      <div class="dashboard-content">
        <div class="content-section">
          <h2>Actions rapides</h2>
          <div class="quick-actions">
            <a routerLink="../nouvelle" class="action-card primary">
              <i class="icon-plus"></i>
              <div>
                <h3>Nouvelle demande</h3>
                <p>Créer une nouvelle demande de soutenance</p>
              </div>
            </a>

            <a routerLink="../liste" class="action-card">
              <i class="icon-list"></i>
              <div>
                <h3>Mes demandes</h3>
                <p>Consulter toutes mes demandes de soutenance</p>
              </div>
            </a>
          </div>
        </div>

        <div class="content-section">
          <h2>Dernières demandes</h2>
          <div class="recent-soutenances" *ngIf="soutenances$ | async as soutenances">
            <div *ngIf="soutenances.length === 0" class="empty-state">
              <i class="icon-document-empty"></i>
              <h3>Aucune demande de soutenance</h3>
              <p>Vous n'avez pas encore créé de demande de soutenance.</p>
              <a routerLink="../nouvelle" class="btn btn-primary">
                Créer ma première demande
              </a>
            </div>

            <div *ngFor="let soutenance of getRecentSoutenances(soutenances)" 
                 class="soutenance-card"
                 [routerLink]="['../', soutenance.id]">
              <div class="soutenance-header">
                <h3>{{ soutenance.titrethese }}</h3>
                <span class="status-badge" [class]="'status-' + soutenance.statut.toLowerCase()">
                  {{ getStatusLabel(soutenance.statut) }}
                </span>
              </div>
              <div class="soutenance-info">
                <p><strong>Directeur:</strong> {{ soutenance.directeur.FirstName }} {{ soutenance.directeur.LastName }}</p>
                <p *ngIf="soutenance.dateSoutenance">
                  <strong>Date prévue:</strong> {{ soutenance.dateSoutenance | date:'dd/MM/yyyy' }}
                </p>
                <p *ngIf="soutenance.lieuSoutenance">
                  <strong>Lieu:</strong> {{ soutenance.lieuSoutenance }}
                </p>
              </div>
              <div class="soutenance-actions">
                <span class="view-link">Voir les détails →</span>
              </div>
            </div>
          </div>
        </div>

        <div class="content-section">
          <h2>Vérification des prérequis</h2>
          <app-prerequis-check
            [doctorantId]="currentUserId"
            [autoCheck]="true"
            [showTitle]="false"
            [compact]="true"
            (prerequisStatusChange)="onPrerequisStatusChange($event)"
            (canSubmitChange)="onCanSubmitChange($event)">
          </app-prerequis-check>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./soutenance-dashboard.scss']
})
export class SoutenanceDashboard implements OnInit {
  // Make enum available in template
  SoutenanceStatus = SoutenanceStatus;

  soutenances$: Observable<SoutenanceResponse[]>;
  soutenances: SoutenanceResponse[] = [];
  currentUserId: number = 0;
  prerequisStatus: PrerequisStatus | null = null;
  canSubmitBasedOnPrerequis = false;

  constructor(
    private soutenanceService: SoutenanceService,
    private authService: AuthService
  ) {
    this.soutenances$ = this.soutenanceService.getMySoutenances();
    
    // Get current user ID
    this.authService.getCurrentUser().subscribe(currentUser => {
      if (currentUser) {
        this.currentUserId = currentUser.id;
      }
    });
  }

  ngOnInit(): void {
    this.soutenances$.subscribe(soutenances => {
      this.soutenances = soutenances;
    });
  }

  getCountByStatus(status: SoutenanceStatus): number {
    return this.soutenances.filter(s => s.statut === status).length;
  }

  getRecentSoutenances(soutenances: SoutenanceResponse[]): SoutenanceResponse[] {
    return soutenances
      .sort((a, b) => new Date(b.id).getTime() - new Date(a.id).getTime()) // Sort by ID as proxy for creation date
      .slice(0, 3);
  }

  getStatusLabel(status: SoutenanceStatus): string {
    const labels: Record<SoutenanceStatus, string> = {
      [SoutenanceStatus.BROUILLON]: 'Brouillon',
      [SoutenanceStatus.SOUMISE]: 'Soumise',
      [SoutenanceStatus.EN_COURS_VALIDATION]: 'En cours de validation',
      [SoutenanceStatus.AUTORISEE]: 'Autorisée',
      [SoutenanceStatus.REJETEE]: 'Rejetée',
      [SoutenanceStatus.SOUTENUE]: 'Soutenue'
    };
    return labels[status] || status;
  }

  /**
   * Handler pour les changements de statut des prérequis
   */
  onPrerequisStatusChange(status: PrerequisStatus): void {
    console.log('📋 [SOUTENANCE DASHBOARD] Statut prérequis mis à jour:', status);
    this.prerequisStatus = status;
  }

  /**
   * Handler pour les changements de possibilité de soumission
   */
  onCanSubmitChange(canSubmit: boolean): void {
    console.log('📋 [SOUTENANCE DASHBOARD] Possibilité de soumission:', canSubmit);
    this.canSubmitBasedOnPrerequis = canSubmit;
  }
}