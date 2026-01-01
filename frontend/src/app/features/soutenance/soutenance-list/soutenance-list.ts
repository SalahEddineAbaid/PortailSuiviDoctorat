import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { Observable, Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { AuthService } from '../../../core/services/auth.service';
import { SoutenanceResponse, SoutenanceStatus } from '../../../core/models/soutenance.model';

interface FilterCriteria {
  searchTerm: string;
  status: SoutenanceStatus | 'ALL';
  dateFrom?: Date;
  dateTo?: Date;
}

@Component({
  selector: 'app-soutenance-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './soutenance-list.html',
  styleUrls: ['./soutenance-list.scss']
})
export class SoutenanceList implements OnInit {
  soutenances: SoutenanceResponse[] = [];
  filteredSoutenances: SoutenanceResponse[] = [];
  isLoading = true;
  errorMessage = '';
  currentUserRole: string | null = null;

  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions = [10, 25, 50];
  totalPages = 1;

  // Filters
  filters: FilterCriteria = {
    searchTerm: '',
    status: 'ALL',
    dateFrom: undefined,
    dateTo: undefined
  };

  private searchSubject = new Subject<string>();

  // Available statuses for filter
  availableStatuses = [
    { value: 'ALL', label: 'Tous les statuts' },
    { value: SoutenanceStatus.BROUILLON, label: 'Brouillon' },
    { value: SoutenanceStatus.SOUMISE, label: 'Soumise' },
    { value: SoutenanceStatus.EN_COURS_VALIDATION, label: 'En cours de validation' },
    { value: SoutenanceStatus.AUTORISEE, label: 'Autorisée' },
    { value: SoutenanceStatus.REJETEE, label: 'Rejetée' },
    { value: SoutenanceStatus.SOUTENUE, label: 'Soutenue' }
  ];

  constructor(
    private soutenanceService: SoutenanceService,
    private authService: AuthService,
    private router: Router
  ) {
    // Setup search debouncing
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(searchTerm => {
      this.filters.searchTerm = searchTerm;
      this.applyFilters();
    });
  }

  ngOnInit(): void {
    this.currentUserRole = this.authService.getUserRole();
    this.loadSoutenances();
  }

  loadSoutenances(): void {
    this.isLoading = true;
    this.errorMessage = '';

    // Use the appropriate method based on user role
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
        this.soutenances = data;
        this.applyFilters();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des soutenances:', error);
        this.errorMessage = 'Impossible de charger les soutenances';
        this.isLoading = false;
      }
    });
  }

  onSearchChange(searchTerm: string): void {
    this.searchSubject.next(searchTerm);
  }

  onFilterChange(): void {
    this.currentPage = 1;
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = [...this.soutenances];

    // Search filter
    if (this.filters.searchTerm) {
      const term = this.filters.searchTerm.toLowerCase();
      filtered = filtered.filter(s =>
        s.titrethese.toLowerCase().includes(term) ||
        s.doctorant.FirstName?.toLowerCase().includes(term) ||
        s.doctorant.LastName?.toLowerCase().includes(term) ||
        s.directeur.FirstName?.toLowerCase().includes(term) ||
        s.directeur.LastName?.toLowerCase().includes(term) ||
        s.specialite?.toLowerCase().includes(term) ||
        s.laboratoire?.toLowerCase().includes(term)
      );
    }

    // Status filter
    if (this.filters.status && this.filters.status !== 'ALL') {
      filtered = filtered.filter(s => s.statut === this.filters.status);
    }

    // Date range filter
    if (this.filters.dateFrom) {
      filtered = filtered.filter(s =>
        s.dateSoutenance && new Date(s.dateSoutenance) >= this.filters.dateFrom!
      );
    }
    if (this.filters.dateTo) {
      filtered = filtered.filter(s =>
        s.dateSoutenance && new Date(s.dateSoutenance) <= this.filters.dateTo!
      );
    }

    this.filteredSoutenances = filtered;
    this.calculatePagination();
  }

  calculatePagination(): void {
    this.totalPages = Math.ceil(this.filteredSoutenances.length / this.itemsPerPage);
    if (this.currentPage > this.totalPages) {
      this.currentPage = 1;
    }
  }

  getPaginatedSoutenances(): SoutenanceResponse[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredSoutenances.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  onPageSizeChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.calculatePagination();
  }

  clearFilters(): void {
    this.filters = {
      searchTerm: '',
      status: 'ALL',
      dateFrom: undefined,
      dateTo: undefined
    };
    this.applyFilters();
  }

  // Navigation
  viewDetails(soutenanceId: number): void {
    this.router.navigate(['/soutenance', soutenanceId]);
  }

  editSoutenance(soutenanceId: number): void {
    this.router.navigate(['/soutenance', soutenanceId, 'edit']);
  }

  deleteSoutenance(soutenanceId: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer cette demande ?')) {
      return;
    }

    this.soutenanceService.deleteDefenseRequest(soutenanceId).subscribe({
      next: () => {
        this.loadSoutenances();
      },
      error: (error) => {
        console.error('Erreur lors de la suppression:', error);
        this.errorMessage = 'Erreur lors de la suppression de la demande';
      }
    });
  }

  // Status and role-based actions
  canEdit(soutenance: SoutenanceResponse): boolean {
    return this.authService.isDoctorant() &&
      (soutenance.statut === SoutenanceStatus.BROUILLON ||
        soutenance.statut === SoutenanceStatus.REJETEE);
  }

  canDelete(soutenance: SoutenanceResponse): boolean {
    return this.authService.isDoctorant() &&
      soutenance.statut === SoutenanceStatus.BROUILLON;
  }

  canValidate(soutenance: SoutenanceResponse): boolean {
    return (this.authService.isDirecteur() || this.authService.isAdmin()) &&
      soutenance.statut === SoutenanceStatus.SOUMISE;
  }

  canAuthorize(soutenance: SoutenanceResponse): boolean {
    return this.authService.isAdmin() &&
      soutenance.statut === SoutenanceStatus.EN_COURS_VALIDATION;
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
}