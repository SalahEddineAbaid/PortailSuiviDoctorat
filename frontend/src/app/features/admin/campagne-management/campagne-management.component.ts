import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CampagneService } from '../../../core/services/campagne.service';
import { 
  CampagneResponse, 
  CampagneRequest, 
  TypeCampagne,
  StatistiquesCampagne,
  getCampagneStatus,
  getCampagneStatusLabel,
  getTypeCampagneLabel,
  getDaysRemaining,
  canCloseCampagne
} from '../../../core/models/campagne.model';

@Component({
  selector: 'app-campagne-management',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
  templateUrl: './campagne-management.component.html',
  styleUrls: ['./campagne-management.component.css']
})
export class CampagneManagementComponent implements OnInit {
  // Data
  campagnes: CampagneResponse[] = [];
  filteredCampagnes: CampagneResponse[] = [];
  selectedCampagne: CampagneResponse | null = null;
  statistiques: StatistiquesCampagne | null = null;

  // UI State
  isLoading = true;
  isSaving = false;
  errorMessage = '';
  successMessage = '';
  
  // Modal State
  showFormModal = false;
  showStatsModal = false;
  showCloneModal = false;
  showDeleteConfirm = false;
  isEditMode = false;

  // Forms
  campagneForm: FormGroup;
  cloneForm: FormGroup;

  // Filters
  filterType: TypeCampagne | '' = '';
  filterStatus: string = '';
  filterYear: number | '' = '';
  searchTerm = '';
  availableYears: number[] = [];

  // Enum for template
  TypeCampagne = TypeCampagne;

  constructor(
    private campagneService: CampagneService,
    private fb: FormBuilder,
    private route: ActivatedRoute
  ) {
    this.campagneForm = this.createCampagneForm();
    this.cloneForm = this.createCloneForm();
  }

  ngOnInit(): void {
    this.loadCampagnes();
    
    // Check for action query param to auto-open create modal
    this.route.queryParams.subscribe(params => {
      if (params['action'] === 'create') {
        setTimeout(() => this.openCreateModal(), 500);
      }
    });
  }

  // ============================================
  // Form Creation
  // ============================================

  private createCampagneForm(): FormGroup {
    return this.fb.group({
      libelle: ['', [Validators.required, Validators.minLength(3)]],
      type: [TypeCampagne.INSCRIPTION, Validators.required],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required],
      anneeUniversitaire: [new Date().getFullYear(), [Validators.required, Validators.min(2020)]]
    });
  }

  private createCloneForm(): FormGroup {
    return this.fb.group({
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required]
    });
  }

  // ============================================
  // Data Loading
  // ============================================

  loadCampagnes(): void {
    this.isLoading = true;
    this.clearMessages();
    
    this.campagneService.getAllCampagnes().subscribe({
      next: (campagnes: CampagneResponse[]) => {
        this.campagnes = campagnes.sort((a, b) => 
          new Date(b.dateDebut).getTime() - new Date(a.dateDebut).getTime()
        );
        this.availableYears = this.campagneService.getAvailableYears(campagnes);
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err: { message: string }) => {
        this.errorMessage = err.message || 'Erreur lors du chargement des campagnes';
        this.isLoading = false;
      }
    });
  }

  // ============================================
  // CRUD Operations
  // ============================================

  openCreateModal(): void {
    this.isEditMode = false;
    this.campagneForm.reset({
      type: TypeCampagne.INSCRIPTION,
      anneeUniversitaire: new Date().getFullYear()
    });
    this.showFormModal = true;
  }

  openEditModal(campagne: CampagneResponse): void {
    this.isEditMode = true;
    this.selectedCampagne = campagne;
    this.campagneForm.patchValue({
      libelle: campagne.libelle,
      type: campagne.type,
      dateDebut: campagne.dateDebut,
      dateFin: campagne.dateFin,
      anneeUniversitaire: campagne.anneeUniversitaire
    });
    this.showFormModal = true;
  }

  saveCampagne(): void {
    if (this.campagneForm.invalid) {
      this.campagneForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.clearMessages();

    const formValue = this.campagneForm.value;
    
    // Format dates to yyyy-MM-dd for backend LocalDate
    const request: CampagneRequest = {
      libelle: formValue.libelle,
      type: formValue.type,
      dateDebut: formValue.dateDebut, // HTML date input already gives yyyy-MM-dd
      dateFin: formValue.dateFin,
      anneeUniversitaire: formValue.anneeUniversitaire
    };

    const operation = this.isEditMode && this.selectedCampagne
      ? this.campagneService.updateCampagne(this.selectedCampagne.id, request)
      : this.campagneService.createCampagne(request);

    operation.subscribe({
      next: () => {
        this.successMessage = this.isEditMode 
          ? 'Campagne modifiée avec succès' 
          : 'Campagne créée avec succès';
        this.closeFormModal();
        this.loadCampagnes();
        this.isSaving = false;
      },
      error: (err: { message: string }) => {
        this.errorMessage = err.message || 'Erreur lors de la sauvegarde';
        this.isSaving = false;
      }
    });
  }

  confirmDelete(campagne: CampagneResponse): void {
    this.selectedCampagne = campagne;
    this.showDeleteConfirm = true;
  }

  deleteCampagne(): void {
    if (!this.selectedCampagne) return;

    this.isSaving = true;
    // Note: Si le backend a une méthode delete, l'utiliser ici
    // Pour l'instant, on ferme la campagne
    this.campagneService.fermerCampagne(this.selectedCampagne.id).subscribe({
      next: () => {
        this.successMessage = 'Campagne fermée avec succès';
        this.showDeleteConfirm = false;
        this.selectedCampagne = null;
        this.loadCampagnes();
        this.isSaving = false;
      },
      error: (err: { message: string }) => {
        this.errorMessage = err.message || 'Erreur lors de la fermeture';
        this.isSaving = false;
      }
    });
  }

  // ============================================
  // Campagne Actions
  // ============================================

  fermerCampagne(campagne: CampagneResponse): void {
    if (!confirm(`Voulez-vous vraiment fermer la campagne "${campagne.libelle}" ?`)) {
      return;
    }

    this.campagneService.fermerCampagne(campagne.id).subscribe({
      next: () => {
        this.successMessage = 'Campagne fermée avec succès';
        this.loadCampagnes();
      },
      error: (err: { message: string }) => {
        this.errorMessage = err.message || 'Erreur lors de la fermeture';
      }
    });
  }

  openCloneModal(campagne: CampagneResponse): void {
    this.selectedCampagne = campagne;
    this.cloneForm.reset();
    this.showCloneModal = true;
  }

  cloneCampagne(): void {
    if (!this.selectedCampagne || this.cloneForm.invalid) {
      this.cloneForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    this.campagneService.clonerCampagne(this.selectedCampagne.id, this.cloneForm.value).subscribe({
      next: () => {
        this.successMessage = 'Campagne clonée avec succès';
        this.showCloneModal = false;
        this.selectedCampagne = null;
        this.loadCampagnes();
        this.isSaving = false;
      },
      error: (err: { message: string }) => {
        this.errorMessage = err.message || 'Erreur lors du clonage';
        this.isSaving = false;
      }
    });
  }

  // ============================================
  // Statistics
  // ============================================

  openStatsModal(campagne: CampagneResponse): void {
    this.selectedCampagne = campagne;
    this.statistiques = null;
    this.showStatsModal = true;

    this.campagneService.getStatistiques(campagne.id).subscribe({
      next: (stats: StatistiquesCampagne) => {
        this.statistiques = stats;
      },
      error: (err: { message: string }) => {
        this.errorMessage = err.message || 'Erreur lors du chargement des statistiques';
      }
    });
  }

  // ============================================
  // Filtering
  // ============================================

  applyFilters(): void {
    let filtered = [...this.campagnes];

    if (this.filterType) {
      filtered = filtered.filter(c => c.type === this.filterType);
    }

    if (this.filterStatus) {
      filtered = filtered.filter(c => getCampagneStatus(c) === this.filterStatus);
    }

    if (this.filterYear) {
      filtered = filtered.filter(c => c.anneeUniversitaire === this.filterYear);
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(c => c.libelle.toLowerCase().includes(term));
    }

    this.filteredCampagnes = filtered;
  }

  resetFilters(): void {
    this.filterType = '';
    this.filterStatus = '';
    this.filterYear = '';
    this.searchTerm = '';
    this.applyFilters();
  }

  // ============================================
  // Helper Methods
  // ============================================

  getStatus(campagne: CampagneResponse): string {
    return getCampagneStatus(campagne);
  }

  getStatusLabel(campagne: CampagneResponse): string {
    return getCampagneStatusLabel(getCampagneStatus(campagne));
  }

  getTypeLabel(type: TypeCampagne): string {
    return getTypeCampagneLabel(type);
  }

  getDaysRemaining(campagne: CampagneResponse): number {
    return getDaysRemaining(campagne);
  }

  canClose(campagne: CampagneResponse): boolean {
    return canCloseCampagne(campagne);
  }

  canEdit(campagne: CampagneResponse): boolean {
    const status = getCampagneStatus(campagne);
    return status === 'future' || status === 'inactive';
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR');
  }

  // ============================================
  // Modal Management
  // ============================================

  closeFormModal(): void {
    this.showFormModal = false;
    this.selectedCampagne = null;
    this.campagneForm.reset();
  }

  closeStatsModal(): void {
    this.showStatsModal = false;
    this.selectedCampagne = null;
    this.statistiques = null;
  }

  closeCloneModal(): void {
    this.showCloneModal = false;
    this.selectedCampagne = null;
    this.cloneForm.reset();
  }

  closeDeleteConfirm(): void {
    this.showDeleteConfirm = false;
    this.selectedCampagne = null;
  }

  // ============================================
  // Messages
  // ============================================

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }

  dismissSuccess(): void {
    this.successMessage = '';
  }

  dismissError(): void {
    this.errorMessage = '';
  }
}
