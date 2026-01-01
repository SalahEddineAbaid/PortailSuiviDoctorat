import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';

import { InscriptionService } from '../../../core/services/inscription.service';
import { DocumentService } from '../../../core/services/document.service';
import { AuthService } from '../../../core/services/auth.service';

import { 
  InscriptionResponse,
  getStatutLabel,
  getStatutColor,
  getTypeInscriptionLabel,
  canEditInscription
} from '../../../core/models/inscription.model';

@Component({
  selector: 'app-inscription-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatExpansionModule,
    MatTooltipModule
  ],
  templateUrl: './inscription-detail.html',
  styleUrls: ['./inscription-detail.scss']
})
export class InscriptionDetail implements OnInit {
  inscription: InscriptionResponse | null = null;
  loading = false;
  error: string | null = null;
  currentUser: any;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private inscriptionService: InscriptionService,
    private documentService: DocumentService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadInscription();
  }

  private loadInscription(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/inscription']);
      return;
    }

    this.loading = true;
    this.inscriptionService.getInscription(+id).subscribe({
      next: (inscription) => {
        this.inscription = inscription;
        this.loading = false;
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement de l\'inscription';
        this.loading = false;
        console.error('Error loading inscription:', error);
      }
    });
  }

  onEdit(): void {
    if (this.inscription) {
      this.router.navigate(['/inscription', this.inscription.id, 'edit']);
    }
  }

  onDownloadAttestation(): void {
    if (!this.inscription) return;

    this.inscriptionService.downloadAttestation(
      this.inscription.id,
      this.currentUser.id,
      this.currentUser.role?.name
    ).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `attestation_${this.inscription!.id}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('Error downloading attestation:', error);
      }
    });
  }

  onDownloadDocument(documentId: number, fileName: string): void {
    this.documentService.downloadAndSave(documentId, fileName);
  }

  onBack(): void {
    this.router.navigate(['/inscription']);
  }

  canEdit(): boolean {
    return this.inscription ? canEditInscription(this.inscription.statut) : false;
  }

  canDownloadAttestation(): boolean {
    return this.inscription?.statut === 'VALIDE';
  }

  getStatutLabel(statut: string): string {
    return getStatutLabel(statut as any);
  }

  getStatutColor(statut: string): string {
    return getStatutColor(statut as any);
  }

  getTypeLabel(type: string): string {
    return getTypeInscriptionLabel(type as any);
  }

  getValidationIcon(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return 'check_circle';
      case 'REJETE': return 'cancel';
      default: return 'pending';
    }
  }

  getValidationColor(statut: string): string {
    switch (statut) {
      case 'APPROUVE': return 'primary';
      case 'REJETE': return 'warn';
      default: return 'accent';
    }
  }
}
