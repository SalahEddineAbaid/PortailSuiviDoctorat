import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { JuryService } from '../../../core/services/jury.service';
import { AuthService } from '../../../core/services/auth.service';
import { SoutenanceResponse, SoutenanceStatus, JuryMember, PrerequisStatus } from '../../../core/models/soutenance.model';

@Component({
  selector: 'app-soutenance-detail',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './soutenance-detail.html',
  styleUrls: ['./soutenance-detail.scss']
})
export class SoutenanceDetail implements OnInit {
  soutenance: SoutenanceResponse | null = null;
  isLoading = true;
  errorMessage = '';
  successMessage = '';
  isProcessing = false;

  // Current user info
  currentUser Role = '';
  canEdit = false;
  canValidate = false;
  canAuthorize = false;

  // Enum for template use
  SoutenanceStatus = SoutenanceStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private soutenanceService: SoutenanceService,
    private juryService: JuryService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.currentUserRole = this.authService.getUserRole() || '';
    this.route.params.subscribe(params => {
      const id = +params['id'];
      if (id) {
        this.loadSoutenance(id);
      }
    });
  }

  loadSoutenance(id: number): void {
    this.isLoading = true;
    this.errorMessage = '';

    this.soutenanceService.getDefenseRequestById(id).subscribe({
      next: (data) => {
        this.soutenance = data;
        this.updatePermissions();
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la soutenance:', error);
        this.errorMessage = 'Impossible de charger les détails de la soutenance';
        this.isLoading = false;
      }
    });
  }

  updatePermissions(): void {
    if (!this.soutenance) return;

    const status = this.soutenance.statut;

    // Edit permissions
    this.canEdit = this.authService.isDoctorant() &&
      (status === SoutenanceStatus.BROUILLON || status === SoutenanceStatus.REJETEE);

    // Validation permissions
    this.canValidate = (this.authService.isDirecteur() || this.authService.isAdmin()) &&
      status === SoutenanceStatus.SOUMISE;

    // Authorization permissions
    this.canAuthorize = this.authService.isAdmin() &&
      status === SoutenanceStatus.EN_COURS_VALIDATION;
  }

  // Action methods
  editSoutenance(): void {
    if (this.soutenance) {
      this.router.navigate(['/soutenance', this.soutenance.id, 'edit']);
    }
  }

  validateDefense(): void {
    if (!this.soutenance || this.isProcessing) return;

    if (!confirm('Êtes-vous sûr de vouloir valider cette demande de soutenance ?')) {
      return;
    }

    this.isProcessing = true;
    this.errorMessage = '';

    // Update status to EN_COURS_VALIDATION
    this.soutenanceService.updateDefenseRequestStatus(this.soutenance.id, 'EN_COURS_VALIDATION').subscribe({
      next: () => {
        this.successMessage = 'Soutenance validée avec succès';
        this.loadSoutenance(this.soutenance!.id);
        this.isProcessing = false;
      },
      error: (error) => {
        console.error('Erreur lors de la validation:', error);
        this.errorMessage = 'Erreur lors de la validation de la soutenance';
        this.isProcessing = false;
      }
    });
  }

  authorizeDefense(): void {
    if (!this.soutenance || this.isProcessing) return;

    if (!confirm('Êtes-vous sûr de vouloir autoriser cette soutenance ?')) {
      return;
    }

    this.isProcessing = true;
    this.errorMessage = '';

    this.soutenanceService.autoriserSoutenance(this.soutenance.id).subscribe({
      next: () => {
        this.successMessage = 'Soutenance autorisée avec succès';
        this.loadSoutenance(this.soutenance!.id);
        this.isProcessing = false;
      },
      error: (error) => {
        console.error('Erreur lors de l\'autorisation:', error);
        this.errorMessage = 'Erreur lors de l\'autorisation de la soutenance';
        this.isProcessing = false;
      }
    });
  }

  rejectDefense(): void {
    if (!this.soutenance || this.isProcessing) return;

    const motif = prompt('Motif du rejet:');
    if (!motif) return;

    this.isProcessing = true;
    this.errorMessage = '';

    this.soutenanceService.rejeterSoutenance(this.soutenance.id, motif).subscribe({
      next: () => {
        this.successMessage = 'Soutenance rejetée';
        this.loadSoutenance(this.soutenance!.id);
        this.isProcessing = false;
      },
      error: (error) => {
        console.error('Erreur lors du rejet:', error);
        this.errorMessage = 'Erreur lors du rejet de la soutenance';
        this.isProcessing = false;
      }
    });
  }

  deleteSoutenance(): void {
    if (!this.soutenance || this.isProcessing) return;

    if (!confirm('Êtes-vous sûr de vouloir supprimer cette demande ?')) {
      return;
    }

    this.isProcessing = true;
    this.errorMessage = '';

    this.soutenanceService.deleteDefenseRequest(this.soutenance.id).subscribe({
      next: () => {
        this.router.navigate(['/soutenance/liste']);
      },
      error: (error) => {
        console.error('Erreur lors de la suppression:', error);
        this.errorMessage = 'Erreur lors de la suppression de la demande';
        this.isProcessing = false;
      }
    });
  }

  // Helper methods
  getStatusLabel(): string {
    if (!this.soutenance) return '';
    return this.soutenanceService.getStatusLabel(this.soutenance.statut);
  }

  getStatusColor(): string {
    if (!this.soutenance) return 'gray';
    return this.soutenanceService.getStatusColor(this.soutenance.statut);
  }

  formatDate(date?: Date): string {
    if (!date) return 'Non définie';
    return new Date(date).toLocaleDateString('fr-FR', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  getFullName(user: { FirstName: string; LastName: string }): string {
    return `${user.FirstName} ${user.LastName}`;
  }

  getRoleLabel(role: string): string {
    return this.juryService.getRoleLabel(role as any);
  }

  getPrerequisPercentage(): number {
    if (!this.soutenance || !this.soutenance.prerequis) return 0;

    const total = this.soutenance.prerequis.details?.length || 0;
    if (total === 0) return 0;

    const validated = this.soutenance.prerequis.details.filter(d => d.valide).length;
    return Math.round((validated / total) * 100);
  }

  goBack(): void {
    this.router.navigate(['/soutenance/liste']);
  }
}