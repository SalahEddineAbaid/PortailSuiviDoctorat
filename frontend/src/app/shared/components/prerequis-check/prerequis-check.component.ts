import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PrerequisStatus, PrerequisDetail } from '../../../core/models/soutenance.model';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { Observable, BehaviorSubject } from 'rxjs';

@Component({
  selector: 'app-prerequis-check',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './prerequis-check.component.html',
  styleUrls: ['./prerequis-check.component.scss']
})
export class PrerequisCheckComponent implements OnInit, OnChanges {
  @Input() doctorantId!: number;
  @Input() autoCheck: boolean = true;
  @Input() showTitle: boolean = true;
  @Input() compact: boolean = false;
  @Output() prerequisStatusChange = new EventEmitter<PrerequisStatus>();
  @Output() canSubmitChange = new EventEmitter<boolean>();

  prerequisStatus$ = new BehaviorSubject<PrerequisStatus | null>(null);
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  constructor(private soutenanceService: SoutenanceService) {}

  ngOnInit(): void {
    if (this.autoCheck && this.doctorantId) {
      this.checkPrerequis();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['doctorantId'] && this.doctorantId && this.autoCheck) {
      this.checkPrerequis();
    }
  }

  /**
   * Vérifier les prérequis via l'API backend
   */
  checkPrerequis(): void {
    if (!this.doctorantId) {
      this.error$.next('ID doctorant requis pour vérifier les prérequis');
      return;
    }

    this.loading$.next(true);
    this.error$.next(null);

    this.soutenanceService.checkPrerequis(this.doctorantId).subscribe({
      next: (status: PrerequisStatus) => {
        console.log('✅ [PREREQUIS CHECK] Prérequis récupérés:', status);
        this.prerequisStatus$.next(status);
        this.prerequisStatusChange.emit(status);
        this.canSubmitChange.emit(status.prerequisRemplis);
        this.loading$.next(false);
      },
      error: (error) => {
        console.error('❌ [PREREQUIS CHECK] Erreur vérification prérequis:', error);
        this.error$.next('Erreur lors de la vérification des prérequis');
        this.loading$.next(false);
        this.canSubmitChange.emit(false);
      }
    });
  }

  /**
   * Rafraîchir les prérequis
   */
  refreshPrerequis(): void {
    this.checkPrerequis();
  }

  /**
   * Obtenir l'icône pour un prérequis
   */
  getPrerequisIcon(valide: boolean): string {
    return valide ? '✅' : '❌';
  }

  /**
   * Obtenir la classe CSS pour un prérequis
   */
  getPrerequisClass(valide: boolean): string {
    return valide ? 'prerequis-valid' : 'prerequis-invalid';
  }

  /**
   * Obtenir le libellé d'un critère
   */
  getCritereLabel(critere: string): string {
    const labels: { [key: string]: string } = {
      'publications': 'Publications scientifiques',
      'heures_formation': 'Heures de formation',
      'duree_doctorat': 'Durée du doctorat',
      'documents_complets': 'Documents complets',
      'inscription_valide': 'Inscription valide',
      'directeur_valide': 'Directeur de thèse validé',
      'laboratoire_valide': 'Laboratoire validé',
      'sujet_valide': 'Sujet de thèse validé'
    };
    return labels[critere] || critere;
  }

  /**
   * Obtenir le message d'aide pour un critère
   */
  getCritereHelp(detail: PrerequisDetail): string {
    if (detail.commentaire) {
      return detail.commentaire;
    }
    
    if (detail.valeurRequise && detail.valeurActuelle) {
      return `Requis: ${detail.valeurRequise}, Actuel: ${detail.valeurActuelle}`;
    }
    
    return '';
  }

  /**
   * Vérifier si tous les prérequis sont remplis
   */
  get allPrerequisMet(): boolean {
    const status = this.prerequisStatus$.value;
    return status ? status.prerequisRemplis : false;
  }

  /**
   * Obtenir le nombre de prérequis valides
   */
  get validPrerequisCount(): number {
    const status = this.prerequisStatus$.value;
    if (!status) return 0;
    return status.details.filter(detail => detail.valide).length;
  }

  /**
   * Obtenir le nombre total de prérequis
   */
  get totalPrerequisCount(): number {
    const status = this.prerequisStatus$.value;
    return status ? status.details.length : 0;
  }

  /**
   * Obtenir le pourcentage de completion
   */
  get completionPercentage(): number {
    if (this.totalPrerequisCount === 0) return 0;
    return Math.round((this.validPrerequisCount / this.totalPrerequisCount) * 100);
  }

  /**
   * Obtenir la classe CSS globale selon l'état
   */
  get globalStatusClass(): string {
    if (this.loading$.value) return 'prerequis-loading';
    if (this.error$.value) return 'prerequis-error';
    if (this.allPrerequisMet) return 'prerequis-success';
    return 'prerequis-warning';
  }

  /**
   * Obtenir le message global
   */
  get globalMessage(): string {
    if (this.loading$.value) return 'Vérification des prérequis en cours...';
    if (this.error$.value) return this.error$.value;
    if (this.allPrerequisMet) return 'Tous les prérequis sont remplis. Vous pouvez soumettre votre demande.';
    return `${this.validPrerequisCount}/${this.totalPrerequisCount} prérequis remplis. Veuillez compléter les éléments manquants.`;
  }

  /**
   * TrackBy function pour optimiser le rendu de la liste
   */
  trackByPrerequisDetail(index: number, detail: PrerequisDetail): string {
    return detail.critere;
  }
}