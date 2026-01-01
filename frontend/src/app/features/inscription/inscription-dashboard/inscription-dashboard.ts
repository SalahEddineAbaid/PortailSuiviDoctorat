import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';

import { InscriptionService } from '../../../core/services/inscription.service';
import { CampagneService } from '../../../core/services/campagne.service';
import { AuthService, UserInfo } from '../../../core/services/auth.service';

import { 
  DashboardResponse,
  InscriptionResponse,
  StatutInscription,
  getStatutLabel,
  getStatutColor
} from '../../../core/models/inscription.model';

@Component({
  selector: 'app-inscription-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatListModule,
    MatTooltipModule,
    MatBadgeModule
  ],
  templateUrl: './inscription-dashboard.html',
  styleUrls: ['./inscription-dashboard.scss']
})
export class InscriptionDashboard implements OnInit {
  dashboard: DashboardResponse | null = null;
  inscriptions: InscriptionResponse[] = [];
  loading = false;
  error: string | null = null;
  currentUser: UserInfo | null = null;

  // Statistics
  totalInscriptions = 0;
  inscriptionsValidees = 0;
  inscriptionsEnAttente = 0;
  inscriptionsRejetees = 0;

  constructor(
    private inscriptionService: InscriptionService,
    private campagneService: CampagneService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Subscribe to current user observable
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.loadDashboard();
      }
    });
  }

  loadDashboard(): void {
    if (!this.currentUser) {
      this.error = 'Utilisateur non connecté';
      return;
    }
    
    this.loading = true;
    const userId = this.currentUser.id;
    const role = this.currentUser.roles?.[0] || 'ROLE_DOCTORANT';

    // Load dashboard data
    this.inscriptionService.getDashboardDoctorant(userId, userId, role).subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement du dashboard';
        this.loading = false;
        console.error('Error loading dashboard:', error);
      }
    });

    // Load inscriptions for statistics
    this.inscriptionService.getInscriptionsDoctorant(userId).subscribe({
      next: (inscriptions) => {
        this.inscriptions = inscriptions;
        this.calculateStatistics(inscriptions);
      },
      error: (error) => {
        console.error('Error loading inscriptions:', error);
      }
    });
  }

  private calculateStatistics(inscriptions: InscriptionResponse[]): void {
    this.totalInscriptions = inscriptions.length;
    this.inscriptionsValidees = inscriptions.filter(i => i.statut === StatutInscription.VALIDE).length;
    this.inscriptionsEnAttente = inscriptions.filter(i => 
      [StatutInscription.SOUMIS, StatutInscription.EN_ATTENTE_DIRECTEUR, 
       StatutInscription.APPROUVE_DIRECTEUR, StatutInscription.EN_ATTENTE_ADMIN].includes(i.statut)
    ).length;
    this.inscriptionsRejetees = inscriptions.filter(i => i.statut === StatutInscription.REJETE).length;
  }

  onNewInscription(): void {
    this.router.navigate(['/inscription/new']);
  }

  onReinscription(): void {
    this.router.navigate(['/inscription/reinscription']);
  }

  onViewInscription(id: number): void {
    this.router.navigate(['/inscription', id]);
  }

  onViewAllInscriptions(): void {
    this.router.navigate(['/inscription/list']);
  }

  canCreateInscription(): boolean {
    return this.inscriptionService.canCreateInscription(this.inscriptions);
  }

  canReinscribe(): boolean {
    // Can reinscribe if has a validated inscription
    return this.inscriptions.some(i => i.statut === StatutInscription.VALIDE);
  }

  getStatutLabel(statut: StatutInscription): string {
    return getStatutLabel(statut);
  }

  getStatutColor(statut: StatutInscription): string {
    return getStatutColor(statut);
  }

  getMilestoneIcon(statut: string): string {
    switch (statut) {
      case 'COMPLETE': return 'check_circle';
      case 'EN_COURS': return 'pending';
      case 'EN_RETARD': return 'warning';
      case 'A_VENIR': return 'schedule';
      default: return 'help';
    }
  }

  getMilestoneColor(statut: string): string {
    switch (statut) {
      case 'COMPLETE': return 'primary';
      case 'EN_COURS': return 'accent';
      case 'EN_RETARD': return 'warn';
      case 'A_VENIR': return '';
      default: return '';
    }
  }

  getAlerteIcon(niveau: string): string {
    switch (niveau) {
      case 'INFO': return 'info';
      case 'WARNING': return 'warning';
      case 'DANGER': return 'error';
      default: return 'info';
    }
  }

  getAlerteColor(niveau: string): string {
    switch (niveau) {
      case 'INFO': return 'primary';
      case 'WARNING': return 'accent';
      case 'DANGER': return 'warn';
      default: return '';
    }
  }

  get progressionPercentage(): number {
    return this.dashboard?.progressionDoctorat || 0;
  }

  get dureeDoctoratAnnees(): number {
    if (!this.dashboard?.dureeDoctoratMois) return 0;
    return Math.floor(this.dashboard.dureeDoctoratMois / 12);
  }

  get dureeDoctoratMoisRestants(): number {
    if (!this.dashboard?.dureeDoctoratMois) return 0;
    return this.dashboard.dureeDoctoratMois % 12;
  }
}
