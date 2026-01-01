import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { DossierValidationService, InscriptionResponse, StatutInscription } from '../../../core/services/dossier-validation.service';
import { CampagneService, CampagneResponse } from '../../../core/services/campagne.service';

interface FilterCriteria {
    searchTerm: string;
    campagneId?: number;
    status: 'ALL' | StatutInscription;
}

@Component({
    selector: 'app-dossier-list',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './dossier-list.html',
    styleUrls: ['./dossier-list.scss']
})
export class DossierList implements OnInit {
    dossiers: InscriptionResponse[] = [];
    filteredDossiers: InscriptionResponse[] = [];
    campagnes: CampagneResponse[] = [];
    isLoading = true;
    errorMessage = '';
    successMessage = '';

    // Pagination
    currentPage = 0;
    pageSize = 15;
    totalPages = 0;
    totalElements = 0;

    // Filters
    filters: FilterCriteria = {
        searchTerm: '',
        campagneId: undefined,
        status: 'ALL'
    };

    private searchSubject = new Subject<string>();

    constructor(
        public dossierService: DossierValidationService,
        private campagneService: CampagneService,
        private router: Router
    ) {
        this.searchSubject.pipe(
            debounceTime(300),
            distinctUntilChanged()
        ).subscribe(searchTerm => {
            this.filters.searchTerm = searchTerm;
            this.applyFilters();
        });
    }

    ngOnInit(): void {
        this.loadData();
    }

    loadData(): void {
        this.isLoading = true;
        this.errorMessage = '';

        // Load campagnes for filter
        this.campagneService.getAllCampagnes().subscribe({
            next: (campagnes) => {
                this.campagnes = campagnes;
                this.loadDossiers();
            },
            error: (error) => {
                console.error('Error loading campagnes:', error);
                this.loadDossiers(); // Continue even if campagnes fail
            }
        });
    }

    loadDossiers(): void {
        this.dossierService.getPendingForAdmin().subscribe({
            next: (data) => {
                this.dossiers = data;
                this.applyFilters();
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading dossiers:', error);
                this.errorMessage = 'Impossible de charger les dossiers';
                this.isLoading = false;
            }
        });
    }

    onSearchChange(searchTerm: string): void {
        this.searchSubject.next(searchTerm);
    }

    onFilterChange(): void {
        this.currentPage = 0;
        this.applyFilters();
    }

    applyFilters(): void {
        let filtered = [...this.dossiers];

        // Search filter
        if (this.filters.searchTerm) {
            const term = this.filters.searchTerm.toLowerCase();
            filtered = filtered.filter(d =>
                (d.doctorant?.nom && d.doctorant.nom.toLowerCase().includes(term)) ||
                (d.doctorant?.prenom && d.doctorant.prenom.toLowerCase().includes(term)) ||
                (d.doctorant?.email && d.doctorant.email.toLowerCase().includes(term))
            );
        }

        // Campagne filter
        if (this.filters.campagneId) {
            filtered = filtered.filter(d => d.campagneId === this.filters.campagneId);
        }

        // Status filter
        if (this.filters.status && this.filters.status !== 'ALL') {
            filtered = filtered.filter(d => d.statut === this.filters.status);
        }

        this.filteredDossiers = filtered;
        this.calculatePagination();
    }

    calculatePagination(): void {
        this.totalElements = this.filteredDossiers.length;
        this.totalPages = Math.ceil(this.totalElements / this.pageSize);
        if (this.currentPage >= this.totalPages && this.totalPages > 0) {
            this.currentPage = 0;
        }
    }

    getPaginatedDossiers(): InscriptionResponse[] {
        const startIndex = this.currentPage * this.pageSize;
        const endIndex = startIndex + this.pageSize;
        return this.filteredDossiers.slice(startIndex, endIndex);
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages) {
            this.currentPage = page;
        }
    }

    clearFilters(): void {
        this.filters = {
            searchTerm: '',
            campagneId: undefined,
            status: 'ALL'
        };
        this.applyFilters();
    }

    // Navigation
    viewDossier(dossierId: number): void {
        this.router.navigate(['/admin/dossiers', dossierId]);
    }

    validateDossier(dossier: InscriptionResponse): void {
        this.router.navigate(['/admin/dossiers', dossier.id, 'validate']);
    }

    // Helper methods
    getDoctorantFullName(dossier: InscriptionResponse): string {
        if (!dossier.doctorant) return 'N/A';
        return `${dossier.doctorant.prenom} ${dossier.doctorant.nom}`;
    }

    getCampagneName(dossier: InscriptionResponse): string {
        if (dossier.campagne) return dossier.campagne.nom;
        const campagne = this.campagnes.find(c => c.id === dossier.campagneId);
        return campagne ? campagne.nom : 'N/A';
    }

    formatDate(date: Date | string): string {
        if (!date) return 'N/A';
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }

    getStatusLabel(statut: StatutInscription): string {
        return this.dossierService.getStatusLabel(statut);
    }

    getStatusColor(statut: StatutInscription): string {
        return this.dossierService.getStatusColor(statut);
    }

    isPending(dossier: InscriptionResponse): boolean {
        return this.dossierService.isPending(dossier);
    }

    getDaysSinceSubmission(dossier: InscriptionResponse): number {
        if (!dossier.createdAt) return 0;
        const now = new Date();
        const created = new Date(dossier.createdAt);
        const diff = now.getTime() - created.getTime();
        return Math.floor(diff / (1000 * 60 * 60 * 24));
    }

    isUrgent(dossier: InscriptionResponse): boolean {
        return this.getDaysSinceSubmission(dossier) > 7;
    }
}
