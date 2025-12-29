import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { SoutenanceResponse, SoutenanceStatus } from '../../../core/models/soutenance.model';

@Component({
  selector: 'app-soutenance-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  template: `
    <div class="soutenance-list">
      <header class="list-header">
        <div class="header-content">
          <h1>Mes demandes de soutenance</h1>
          <p class="subtitle">Gérez et suivez toutes vos demandes de soutenance</p>
        </div>
        <div class="header-actions">
          <a routerLink="../nouvelle" class="btn btn-primary">
            <i class="icon-plus"></i>
            Nouvelle demande
          </a>
        </div>
      </header>

      <!-- Filtres et recherche -->
      <div class="filters-section">
        <div class="filters-row">
          <div class="filter-group">
            <label for="statusFilter">Filtrer par statut</label>
            <select id="statusFilter" [(ngModel)]="selectedStatus" (change)="applyFilters()">
              <option value="">Tous les statuts</option>
              <option value="BROUILLON">Brouillon</option>
              <option value="SOUMISE">Soumise</option>
              <option value="EN_COURS_VALIDATION">En cours de validation</option>
              <option value="AUTORISEE">Autorisée</option>
              <option value="REJETEE">Rejetée</option>
              <option value="SOUTENUE">Soutenue</option>
            </select>
          </div>

          <div class="filter-group">
            <label for="searchInput">Rechercher</label>
            <input 
              type="text" 
              id="searchInput"
              [(ngModel)]="searchTerm" 
              (input)="applyFilters()"
              placeholder="Rechercher par titre de thèse...">
          </div>

          <div class="filter-actions">
            <button class="btn btn-outline" (click)="clearFilters()">
              <i class="icon-x"></i>
              Effacer les filtres
            </button>
          </div>
        </div>

        <div class="results-info" *ngIf="filteredSoutenances$ | async as soutenances">
          <span>{{ soutenances.length }} résultat(s) trouvé(s)</span>
        </div>
      </div>

      <!-- Liste des soutenances -->
      <div class="soutenances-content">
        <div class="soutenances-grid" *ngIf="filteredSoutenances$ | async as soutenances">
          <!-- État vide -->
          <div class="empty-state" *ngIf="soutenances.length === 0 && !searchTerm && !selectedStatus">
            <div class="empty-icon">
              <i class="icon-document-empty"></i>
            </div>
            <h3>Aucune demande de soutenance</h3>
            <p>Vous n'avez pas encore créé de demande de soutenance.</p>
            <a routerLink="../nouvelle" class="btn btn-primary">
              Créer ma première demande
            </a>
          </div>

          <!-- Aucun résultat de recherche -->
          <div class="empty-state" *ngIf="soutenances.length === 0 && (searchTerm || selectedStatus)">
            <div class="empty-icon">
              <i class="icon-search"></i>
            </div>
            <h3>Aucun résultat trouvé</h3>
            <p>Aucune demande ne correspond à vos critères de recherche.</p>
            <button class="btn btn-outline" (click)="clearFilters()">
              Effacer les filtres
            </button>
          </div>

          <!-- Cartes des soutenances -->
          <div class="soutenance-card" *ngFor="let soutenance of soutenances" [routerLink]="['../', soutenance.id]">
            <div class="card-header">
              <div class="card-title">
                <h3>{{ soutenance.titrethese | slice:0:80 }}{{ soutenance.titrethese.length > 80 ? '...' : '' }}</h3>
                <span class="status-badge" [class]="'status-' + soutenance.statut.toLowerCase()">
                  {{ getStatusLabel(soutenance.statut) }}
                </span>
              </div>
            </div>

            <div class="card-content">
              <div class="card-info">
                <div class="info-row">
                  <i class="icon-user"></i>
                  <span><strong>Directeur:</strong> {{ soutenance.directeur.FirstName }} {{ soutenance.directeur.LastName }}</span>
                </div>
                
                <div class="info-row" *ngIf="soutenance.dateSoutenance">
                  <i class="icon-calendar"></i>
                  <span><strong>Date prévue:</strong> {{ soutenance.dateSoutenance | date:'dd/MM/yyyy' }}</span>
                </div>
                
                <div class="info-row" *ngIf="soutenance.lieuSoutenance">
                  <i class="icon-map-pin"></i>
                  <span><strong>Lieu:</strong> {{ soutenance.lieuSoutenance }}</span>
                </div>

                <div class="info-row" *ngIf="soutenance.jury && soutenance.jury.length > 0">
                  <i class="icon-users"></i>
                  <span><strong>Jury:</strong> {{ soutenance.jury.length }} membre(s)</span>
                </div>
              </div>

              <!-- Indicateur des prérequis -->
              <div class="prerequis-indicator" *ngIf="soutenance.prerequis">
                <div class="prerequis-status" [class]="soutenance.prerequis.prerequisRemplis ? 'valid' : 'invalid'">
                  <i [class]="soutenance.prerequis.prerequisRemplis ? 'icon-check-circle' : 'icon-alert-circle'"></i>
                  <span>{{ soutenance.prerequis.prerequisRemplis ? 'Prérequis OK' : 'Prérequis manquants' }}</span>
                </div>
              </div>

              <!-- Indicateur de progression -->
              <div class="progress-indicator">
                <div class="progress-info">
                  <span class="progress-label">Progression: {{ getStatusProgress(soutenance.statut).step }}</span>
                  <span class="progress-percentage">{{ getStatusProgress(soutenance.statut).percentage | number:'1.0-0' }}%</span>
                </div>
                <div class="progress-bar-mini">
                  <div class="progress-fill-mini" [style.width.%]="getStatusProgress(soutenance.statut).percentage"></div>
                </div>
              </div>
            </div>

            <div class="card-actions">
              <div class="action-buttons">
                <button class="btn btn-sm btn-outline" (click)="editSoutenance($event, soutenance.id)" 
                        *ngIf="canEdit(soutenance.statut)">
                  <i class="icon-edit"></i>
                  Modifier
                </button>
                
                <button class="btn btn-sm btn-primary" (click)="viewDetails($event, soutenance.id)">
                  <i class="icon-eye"></i>
                  Voir détails
                </button>
              </div>
              
              <div class="card-meta">
                <span class="last-update">Mis à jour: {{ getLastUpdateDate(soutenance) | date:'dd/MM/yyyy' }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- État de chargement -->
        <div class="loading-state" *ngIf="!(filteredSoutenances$ | async)">
          <div class="spinner"></div>
          <p>Chargement de vos demandes de soutenance...</p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./soutenance-list.scss']
})
export class SoutenanceList implements OnInit {
  soutenances$!: Observable<SoutenanceResponse[]>;
  filteredSoutenances$!: Observable<SoutenanceResponse[]>;
  
  selectedStatus = '';
  searchTerm = '';

  constructor(private soutenanceService: SoutenanceService) {}

  ngOnInit(): void {
    this.soutenances$ = this.soutenanceService.getMySoutenances();
    this.filteredSoutenances$ = this.soutenances$;
  }

  applyFilters(): void {
    this.filteredSoutenances$ = this.soutenances$.pipe(
      map(soutenances => {
        let filtered = soutenances;

        // Filtre par statut
        if (this.selectedStatus) {
          filtered = filtered.filter(s => s.statut === this.selectedStatus);
        }

        // Filtre par recherche textuelle
        if (this.searchTerm) {
          const term = this.searchTerm.toLowerCase();
          filtered = filtered.filter(s => 
            s.titrethese.toLowerCase().includes(term) ||
            s.directeur.FirstName.toLowerCase().includes(term) ||
            s.directeur.LastName.toLowerCase().includes(term)
          );
        }

        return filtered;
      })
    );
  }

  clearFilters(): void {
    this.selectedStatus = '';
    this.searchTerm = '';
    this.filteredSoutenances$ = this.soutenances$;
  }

  getStatusLabel(status: SoutenanceStatus): string {
    return this.soutenanceService.getStatusLabel(status);
  }

  canEdit(status: SoutenanceStatus): boolean {
    return status === SoutenanceStatus.BROUILLON || status === SoutenanceStatus.REJETEE;
  }

  getLastUpdateDate(soutenance: SoutenanceResponse): Date {
    // Since we don't have a lastUpdate field, we'll use a placeholder
    // In a real implementation, this would come from the backend
    return new Date();
  }

  getStatusProgress(status: SoutenanceStatus): { percentage: number; step: string } {
    const statusMapping: Record<SoutenanceStatus, { step: number; label: string }> = {
      [SoutenanceStatus.BROUILLON]: { step: 1, label: 'Création' },
      [SoutenanceStatus.SOUMISE]: { step: 4, label: 'Soumise' },
      [SoutenanceStatus.EN_COURS_VALIDATION]: { step: 5, label: 'Validation' },
      [SoutenanceStatus.AUTORISEE]: { step: 6, label: 'Autorisée' },
      [SoutenanceStatus.REJETEE]: { step: 5, label: 'Rejetée' },
      [SoutenanceStatus.SOUTENUE]: { step: 8, label: 'Soutenue' }
    };

    const totalSteps = 8;
    const currentStatus = statusMapping[status] || { step: 1, label: 'Début' };
    
    return {
      percentage: (currentStatus.step / totalSteps) * 100,
      step: currentStatus.label
    };
  }

  editSoutenance(event: Event, soutenanceId: number): void {
    event.stopPropagation();
    // Navigation will be handled by the routerLink
  }

  viewDetails(event: Event, soutenanceId: number): void {
    event.stopPropagation();
    // Navigation will be handled by the routerLink
  }
}