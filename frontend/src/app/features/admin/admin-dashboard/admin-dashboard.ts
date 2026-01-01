import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import { UserManagementService, UserStatisticsResponse, ConnectionStatisticsResponse } from '../../../core/services/user-management.service';
import { CampagneService, CampagneResponse } from '../../../core/services/campagne.service';
import { DossierValidationService, InscriptionResponse } from '../../../core/services/dossier-validation.service';
import { AuthService } from '../../../core/services/auth.service';

interface GlobalStatistics {
    users: UserStatisticsResponse;
    connections: ConnectionStatisticsResponse;
    campagnes: {
        total: number;
        actives: number;
    };
    dossiers: {
        total: number;
        enAttente: number;
    };
}

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './admin-dashboard.html',
    styleUrls: ['./admin-dashboard.scss']
})
export class AdminDashboard implements OnInit {
    isLoading = true;
    errorMessage = '';

    statistics: GlobalStatistics | null = null;
    recentCampagnes: CampagneResponse[] = [];
    pendingDossiers: InscriptionResponse[] = [];

    constructor(
        private userManagementService: UserManagementService,
        private campagneService: CampagneService,
        private dossierService: DossierValidationService,
        public authService: AuthService
    ) { }

    ngOnInit(): void {
        this.loadDashboardData();
    }

    loadDashboardData(): void {
        this.isLoading = true;
        this.errorMessage = '';

        forkJoin({
            userStats: this.userManagementService.getUserStatistics(),
            connectionStats: this.userManagementService.getConnectionStatistics(),
            campagnes: this.campagneService.getAllCampagnes(),
            activeCampagnes: this.campagneService.getActiveCampagnes(),
            pendingDossiers: this.dossierService.getPendingForAdmin()
        }).subscribe({
            next: (data) => {
                this.statistics = {
                    users: data.userStats,
                    connections: data.connectionStats,
                    campagnes: {
                        total: data.campagnes.length,
                        actives: data.activeCampagnes.length
                    },
                    dossiers: {
                        total: data.pendingDossiers.length,
                        enAttente: data.pendingDossiers.filter(d => this.dossierService.isPending(d)).length
                    }
                };

                this.recentCampagnes = data.campagnes
                    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
                    .slice(0, 5);

                this.pendingDossiers = data.pendingDossiers.slice(0, 10);

                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading dashboard data:', error);
                this.errorMessage = 'Impossible de charger les données du dashboard';
                this.isLoading = false;
            }
        });
    }

    // Helper methods
    getUserRolePercentage(roleName: string): number {
        if (!this.statistics?.users.usersByRole) return 0;
        const total = this.statistics.users.totalUsers;
        const roleCount = this.statistics.users.usersByRole[roleName] || 0;
        return total > 0 ? Math.round((roleCount / total) * 100) : 0;
    }

    formatDate(date: Date | string): string {
        if (!date) return 'N/A';
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }
}
