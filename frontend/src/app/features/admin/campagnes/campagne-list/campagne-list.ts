import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { CampagneService } from '../../../../core/services/campagne.service';
import { CampagneResponse } from '../../../../core/models/campagne.model';

interface FilterCriteria {
    searchTerm: string;
    status: 'ALL' | string;
    year?: number;
}

@Component({
    selector: 'app-campagne-list',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './campagne-list.html',
    styleUrls: ['./campagne-list.scss']
})
export class CampagneList implements OnInit {
    campagnes: CampagneResponse[] = [];
    filteredCampagnes: CampagneResponse[] = [];
    isLoading = true;
    errorMessage = '';
    successMessage = '';

    // Pagination
    currentPage = 0;
    pageSize = 10;
    totalPages = 0;
    totalElements = 0;

    // Filters
    filters: FilterCriteria = {
        searchTerm: '',
        status: 'ALL',
        year: undefined
    };

    private searchSubject = new Subject<string>();
    availableYears: number[] = [];

    constructor(
        public campagneService: CampagneService,
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
        this.loadCampagnes();
    }

    loadCampagnes(): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.campagneService.getAllCampagnes().subscribe({
            next: (data) => {
                this.campagnes = data;
                this.availableYears = this.campagneService.getAvailableYears(data);
                this.applyFilters();
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading campagnes:', error);
                this.errorMessage = 'Impossible de charger les campagnes';
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
        let filtered = [...this.campagnes];

        // Search filter
        if (this.filters.searchTerm) {
            const term = this.filters.searchTerm.toLowerCase();
            filtered = filtered.filter(c =>
                c.libelle.toLowerCase().includes(term) ||
                (c.description && c.description.toLowerCase().includes(term))
            );
        }

        // Status filter - simplified
        if (this.filters.status && this.filters.status !== 'ALL') {
            filtered = filtered.filter(c => c.active === (this.filters.status === 'OUVERTE'));
        }

        // Year filter
        if (this.filters.year) {
            filtered = this.campagneService.getCampagnesByYear(filtered, this.filters.year);
        }

        this.filteredCampagnes = filtered;
        this.calculatePagination();
    }

    calculatePagination(): void {
        this.totalElements = this.filteredCampagnes.length;
        this.totalPages = Math.ceil(this.totalElements / this.pageSize);
        if (this.currentPage >= this.totalPages && this.totalPages > 0) {
            this.currentPage = 0;
        }
    }

    getPaginatedCampagnes(): CampagneResponse[] {
        const startIndex = this.currentPage * this.pageSize;
        const endIndex = startIndex + this.pageSize;
        return this.filteredCampagnes.slice(startIndex, endIndex);
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages) {
            this.currentPage = page;
        }
    }

    clearFilters(): void {
        this.filters = {
            searchTerm: '',
            status: 'ALL',
            year: undefined
        };
        this.applyFilters();
    }

    // Navigation
    viewCampagne(campagneId: number): void {
        this.router.navigate(['/admin/campagnes', campagneId]);
    }

    editCampagne(campagneId: number): void {
        this.router.navigate(['/admin/campagnes', campagneId, 'edit']);
    }

    // Actions
    closeCampagne(campagne: CampagneResponse): void {
        if (!confirm(`Êtes-vous sûr de vouloir fermer la campagne "${campagne.libelle}" ?`)) {
            return;
        }

        this.campagneService.fermerCampagne(campagne.id).subscribe({
            next: () => {
                this.successMessage = `Campagne "${campagne.libelle}" fermée avec succès`;
                this.loadCampagnes();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (error: any) => {
                console.error('Error closing campagne:', error);
                this.errorMessage = 'Erreur lors de la fermeture de la campagne';
                setTimeout(() => this.errorMessage = '', 3000);
            }
        });
    }

    cloneCampagne(campagne: CampagneResponse): void {
        // Navigate to clone form with campagne data
        this.router.navigate(['/admin/campagnes/new'], {
            queryParams: { cloneFrom: campagne.id }
        });
    }

    viewStatistics(campagneId: number): void {
        this.router.navigate(['/admin/campagnes', campagneId], {
            fragment: 'statistiques'
        });
    }

    // Helper methods
    formatDate(date: Date | string): string {
        if (!date) return 'N/A';
        return new Date(date).toLocaleDateString('fr-FR');
    }

    getStatusLabel(statut: string): string {
        const labels: { [key: string]: string } = {
            'OUVERTE': 'Ouverte',
            'FERMEE': 'Fermée',
            'BROUILLON': 'Brouillon'
        };
        return labels[statut] || statut;
    }

    getStatusColor(statut: string): string {
        const colors: { [key: string]: string } = {
            'OUVERTE': 'green',
            'FERMEE': 'gray',
            'BROUILLON': 'orange'
        };
        return colors[statut] || 'gray';
    }

    getDaysRemaining(campagne: CampagneResponse): number {
        return this.campagneService.getDaysRemaining(campagne);
    }

    isEndingSoon(campagne: CampagneResponse): boolean {
        return this.getDaysRemaining(campagne) <= 7 && this.getDaysRemaining(campagne) > 0;
    }

    isCurrentlyOpen(campagne: CampagneResponse): boolean {
        return campagne.active && campagne.ouverte;
    }
}
