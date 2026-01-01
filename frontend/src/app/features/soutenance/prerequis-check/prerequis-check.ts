import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { Router } from '@angular/router';

import { SoutenanceService } from '../../../core/services/soutenance.service';

interface PrerequisitesCheck {
  id: number;
  doctorantId: number;
  doctorateStartDate: string;
  journalArticles: number;
  conferences: number;
  trainingHours: number;
  manuscriptUploaded: boolean;
  antiPlagiarismUploaded: boolean;
  publicationsReportUploaded: boolean;
  trainingCertsUploaded: boolean;
  authorizationLetterUploaded: boolean;
  isValid: boolean;
  publications: any[];
}

@Component({
  selector: 'app-prerequis-check',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatDividerModule,
    MatChipsModule
  ],
  templateUrl: './prerequis-check.html',
  styleUrls: ['./prerequis-check.scss']
})
export class PrerequisCheckComponent implements OnInit {
  @Input() defenseRequestId!: number;
  
  prerequisites: PrerequisitesCheck | null = null;
  loading = false;
  
  // Requirements
  readonly REQUIRED_JOURNAL_ARTICLES = 2;
  readonly REQUIRED_CONFERENCES = 1;
  readonly REQUIRED_TRAINING_HOURS = 60;

  constructor(
    private soutenanceService: SoutenanceService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPrerequisites();
  }

  private loadPrerequisites(): void {
    if (!this.defenseRequestId) return;

    this.loading = true;
    this.soutenanceService.getPrerequisitesByDoctorant(this.defenseRequestId).subscribe({
      next: (data: any) => {
        this.prerequisites = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  get statusClass(): string {
    if (!this.prerequisites) return 'pending';
    return this.prerequisites.isValid ? 'valid' : 'invalid';
  }

  get statusIcon(): string {
    if (!this.prerequisites) return 'hourglass_empty';
    return this.prerequisites.isValid ? 'check_circle' : 'cancel';
  }

  get statusTitle(): string {
    if (!this.prerequisites) return 'Vérification en cours...';
    return this.prerequisites.isValid 
      ? 'Prérequis Validés' 
      : 'Prérequis Non Satisfaits';
  }

  get statusMessage(): string {
    if (!this.prerequisites) return 'Chargement des prérequis...';
    return this.prerequisites.isValid
      ? 'Tous les prérequis sont satisfaits. Vous pouvez procéder à la demande de soutenance.'
      : 'Certains prérequis ne sont pas satisfaits. Veuillez compléter les éléments manquants.';
  }

  isPublicationsValid(): boolean {
    return this.prerequisites ? 
      this.prerequisites.journalArticles >= this.REQUIRED_JOURNAL_ARTICLES : false;
  }

  isConferencesValid(): boolean {
    return this.prerequisites ? 
      this.prerequisites.conferences >= this.REQUIRED_CONFERENCES : false;
  }

  isTrainingValid(): boolean {
    return this.prerequisites ? 
      this.prerequisites.trainingHours >= this.REQUIRED_TRAINING_HOURS : false;
  }

  getPublicationsProgress(): number {
    if (!this.prerequisites) return 0;
    return Math.min((this.prerequisites.journalArticles / this.REQUIRED_JOURNAL_ARTICLES) * 100, 100);
  }

  getConferencesProgress(): number {
    if (!this.prerequisites) return 0;
    return Math.min((this.prerequisites.conferences / this.REQUIRED_CONFERENCES) * 100, 100);
  }

  getTrainingProgress(): number {
    if (!this.prerequisites) return 0;
    return Math.min((this.prerequisites.trainingHours / this.REQUIRED_TRAINING_HOURS) * 100, 100);
  }

  getDocuments(): any[] {
    if (!this.prerequisites) return [];
    
    return [
      {
        name: 'Manuscrit de thèse',
        uploaded: this.prerequisites.manuscriptUploaded,
        icon: 'description'
      },
      {
        name: 'Rapport anti-plagiat',
        uploaded: this.prerequisites.antiPlagiarismUploaded,
        icon: 'verified_user'
      },
      {
        name: 'Rapport de publications',
        uploaded: this.prerequisites.publicationsReportUploaded,
        icon: 'article'
      },
      {
        name: 'Attestations de formation',
        uploaded: this.prerequisites.trainingCertsUploaded,
        icon: 'school'
      },
      {
        name: 'Lettre d\'autorisation',
        uploaded: this.prerequisites.authorizationLetterUploaded,
        icon: 'approval'
      }
    ];
  }

  refresh(): void {
    this.loadPrerequisites();
  }

  goToDocuments(): void {
    this.router.navigate(['/soutenance', this.defenseRequestId, 'documents']);
  }

  goToPublications(): void {
    this.router.navigate(['/soutenance', this.defenseRequestId, 'publications']);
  }
}
