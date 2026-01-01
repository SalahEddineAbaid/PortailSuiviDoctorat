import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable, forkJoin } from 'rxjs';
import { map } from 'rxjs/operators';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { AuthService } from '../../../core/services/auth.service';
import { SoutenanceResponse, SoutenanceStatus } from '../../../core/models/soutenance.model';

interface DashboardStats {
  total: number;
  brouillon: number;
  soumises: number;
  enCoursValidation: number;
  autorisees: number;
  rejetees: number;
  soutenues: number;
}

@Component({
  selector: 'app-soutenance-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './soutenance-dashboard.html',
  styleUrls: ['./soutenance-dashboard.scss']
})
export class SoutenanceDashboard implements OnInit {
  isLoading = true;
  errorMessage = '';
  currentUserRole = '';

  // Statistics
  stats: DashboardStats = {
    total: 0,
    brouillon: 0,
    soumises: 0,
    enCoursValidation: 0,
    autorisees: 0,
    rejetees: 0,
    soutenues: 0
  };

  // Lists
  upcomingDefenses: SoutenanceResponse[] = [];
  pendingValidation: SoutenanceResponse[] = [];
  recentDefenses: SoutenanceResponse[] = [];

  constructor(
    private soutenanceService: SoutenanceService,
    public authService: AuthService
  ) { }

  ngOnInit(): void {
    this.currentUserRole = this.authService.getUserRole() || '';
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading = true;
    this.errorMessage = '';

    let observable: Observable<SoutenanceResponse[]>;

    if (this.authService.isDoctorant()) {
      observable = this.soutenanceService.getMySoutenances();
    } else if (this.authService.isDirecteur()) {
      observable = this.soutenanceService.getSoutenancesByDirecteur();
    } else if (this.authService.isAdmin()) {
      observable = this.soutenanceService.getAllDefenseRequests();
    } else {
      observable = this.soutenanceService.getAllDefenseRequests();
    }

    observable.subscribe({
      next: (data) => {
        this.calculateStatistics(data);
        this.filterDefenses(data);
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement du dashboard:', error);
        this.errorMessage = 'Impossible de charger les données du dashboard';
        this.isLoading = false;
      }
    });
  }

  calculateStatistics(soutenances: SoutenanceResponse[]): void {
    this.stats = {
      total: soutenances.length,
      brouillon: soutenances.filter(s => s.statut === SoutenanceStatus.BROUILLON).length,
      soumises: soutenances.filter(s => s.statut === SoutenanceStatus.SOUMISE).length,
      enCoursValidation: soutenances.filter(s => s.statut === SoutenanceStatus.EN_COURS_VALIDATION).length,
      autorisees: soutenances.filter(s => s.statut === SoutenanceStatus.AUTORISEE).length,
      rejetees: soutenances.filter(s => s.statut === SoutenanceStatus.REJETEE).length,
      soutenues: soutenances.filter(s => s.statut === SoutenanceStatus.SOUTENUE).length
    };
  }

  filterDefenses(soutenances: SoutenanceResponse[]): void {
    const now = new Date();

    // Upcoming defenses (authorized and in the future)
    this.upcomingDefenses = soutenances
      .filter(s =>
        s.statut === SoutenanceStatus.AUTORISEE &&
        s.dateSoutenance &&
        new Date(s.dateSoutenance) > now
      )
      .sort((a, b) => {
        const dateA = a.dateSoutenance ? new Date(a.dateSoutenance).getTime() : 0;
        const dateB = b.dateSoutenance ? new Date(b.dateSoutenance).getTime() : 0;
        return dateA - dateB;
      })
      .slice(0, 5);

    // Pending validation (for directeur/admin)
    if (this.authService.isDirecteur() || this.authService.isAdmin()) {
      this.pendingValidation = soutenances
        .filter(s =>
          s.statut === SoutenanceStatus.SOUMISE ||
          s.statut === SoutenanceStatus.EN_COURS_VALIDATION
        )
        .slice(0, 5);
    }

    // Recent defended theses
    this.recentDefenses = soutenances
      .filter(s => s.statut === SoutenanceStatus.SOUTENUE)
      .sort((a, b) => {
        const dateA = a.dateSoutenance ? new Date(a.dateSoutenance).getTime() : 0;
        const dateB = b.dateSoutenance ? new Date(b.dateSoutenance).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 5);
  }

  // Helper methods
  getStatusLabel(status: SoutenanceStatus): string {
    return this.soutenanceService.getStatusLabel(status);
  }

  getStatusColor(status: SoutenanceStatus): string {
    return this.soutenanceService.getStatusColor(status);
  }

  formatDate(date?: Date): string {
    if (!date) return 'Non définie';
    return new Date(date).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getFullName(user: { FirstName: string; LastName: string }): string {
    return `${user.FirstName} ${user.LastName}`;
  }

  getDaysUntil(date?: Date): number {
    if (!date) return 0;
    const now = new Date();
    const target = new Date(date);
    const diff = target.getTime() - now.getTime();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }

  getStatusPercentage(count: number): number {
    return this.stats.total > 0 ? Math.round((count / this.stats.total) * 100) : 0;
  }
}