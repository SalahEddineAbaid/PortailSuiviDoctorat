import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable } from 'rxjs';
import { InscriptionService } from '../../../core/services/inscription.service';
import { AuthService } from '../../../core/services/auth.service';
import { InscriptionResponse, CampagneResponse } from '../../../core/models/inscription.model';

@Component({
  selector: 'app-inscription-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './inscription-dashboard.html',
  styleUrls: ['./inscription-dashboard.scss']
})
export class InscriptionDashboard implements OnInit {
  inscriptions$!: Observable<InscriptionResponse[]>;
  campagneActive$!: Observable<CampagneResponse | null>;
  loading = true;
  error: string | null = null;

  constructor(
    private inscriptionService: InscriptionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    try {
      this.inscriptions$ = this.inscriptionService.getMyInscriptions();
      this.campagneActive$ = this.inscriptionService.getCampagneActive();
      this.loading = false;
    } catch (error) {
      this.error = 'Erreur lors du chargement des données';
      this.loading = false;
      console.error('Erreur dashboard inscription:', error);
    }
  }

  onRefresh(): void {
    this.loading = true;
    this.error = null;
    this.loadDashboardData();
  }

  getStatusClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'BROUILLON': 'status-draft',
      'SOUMISE': 'status-submitted',
      'EN_COURS_VALIDATION': 'status-pending',
      'VALIDEE': 'status-approved',
      'REJETEE': 'status-rejected'
    };
    return statusClasses[status] || 'status-default';
  }

  getStatusLabel(status: string): string {
    return this.inscriptionService.getStatusLabel(status as any);
  }
}