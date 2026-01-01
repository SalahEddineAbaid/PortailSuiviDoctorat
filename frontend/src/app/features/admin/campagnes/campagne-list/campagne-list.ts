import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { CampagneService, CampagneResponse, StatutCampagne } from '../../../core/services/campagne.service';

interface FilterCriteria {
    searchTerm: string;
    status: 'ALL' | StatutCampagne;
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
                c.nom.toLowerCase().includes(term) ||
                (c.description && c.description.toLowerCase().includes(term))
            );
        }

        // Status filter
        if (this.filters.status && this.filters.status !== 'ALL') {
            filtered = filtered.filter(c => c.statut === this.filters.status);
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
        if (!confirm(`Êtes-vous sûr de vouloir fermer la campagne "${campagne.nom}" ?`)) {
            return;
        }

        this.campagneService.fermerCampagne(campagne.id).subscribe({
            next: () => {
                this.successMessage = `Campagne "${campagne.nom}" fermée avec succès`;
                this.loadCampagnes();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (error) => {
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
        return this.campagneService.formatDate(date);
    }

    getStatusLabel(statut: StatutCampagne): string {
        return this.campagneService.getStatusLabel(statut);
    }

    getStatusColor(statut: StatutCampagne): string {
        return this.campagneService.getStatusColor(statut);
    }

    getDaysRemaining(campagne: CampagneResponse): number {
        return this.campagneService.getDaysRemaining(campagne);
    }

    isEndingSoon(campagne: CampagneResponse): boolean {
        return this.campagneService.isEndingSoon(campagne);
    }

    isCurrentlyOpen(campagne: CampagneResponse): boolean {
        return this.campagneService.isCurrentlyOpen(campagne);
    }
}
