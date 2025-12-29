import { Injectable } from '@angular/core';
import { Observable, combineLatest, map, of } from 'rxjs';
import { InscriptionService } from './inscription.service';
import { SoutenanceService } from './soutenance.service';
import { NotificationService } from './notification.service';
import { AuthService } from './auth.service';
import {
  DoctorantDashboardData,
  DirecteurDashboardData,
  AdminDashboardData,
  DashboardAlert,
  TimelineEvent,
  ProgressIndicator,
  DashboardStats
} from '../models/dashboard.model';
import { StatusWidgetData } from '../../shared/components/status-widget/status-widget.component';
import { InscriptionStatus } from '../models/inscription.model';
import { SoutenanceStatus } from '../models/soutenance.model';

@Injectable({
  providedIn: 'root'
})
export class DashboardService {

  constructor(
    private inscriptionService: InscriptionService,
    private soutenanceService: SoutenanceService,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  // ===== DOCTORANT DASHBOARD =====

  /**
   * 🔹 Récupérer les données du dashboard doctorant
   */
  getDoctorantDashboardData(): Observable<DoctorantDashboardData> {
    return combineLatest([
      this.inscriptionService.getMyInscriptions(),
      this.soutenanceService.getMySoutenances(),
      this.notificationService.getMyNotifications()
    ]).pipe(
      map(([inscriptions, soutenances, notifications]) => {
        const inscriptionActuelle = inscriptions.find(i => 
          i.statut === InscriptionStatus.VALIDEE || 
          i.statut === InscriptionStatus.EN_COURS_VALIDATION
        );

        const prochaineSoutenance = soutenances.find(s => 
          s.statut === SoutenanceStatus.EN_COURS_VALIDATION ||
          s.statut === SoutenanceStatus.AUTORISEE
        );

        return {
          inscriptionActuelle,
          prochaineSoutenance,
          notifications: notifications.slice(0, 5), // Dernières 5 notifications
          documentsManquants: this.getDocumentsManquants(inscriptionActuelle, prochaineSoutenance),
          alertes: this.generateDoctorantAlerts(inscriptionActuelle, prochaineSoutenance),
          timeline: this.generateDoctorantTimeline(inscriptions, soutenances)
        };
      })
    );
  }

  /**
   * 🔹 Récupérer les widgets de statut pour le doctorant
   */
  getDoctorantStatusWidgets(): Observable<StatusWidgetData[]> {
    return combineLatest([
      this.inscriptionService.getMyInscriptions(),
      this.soutenanceService.getMySoutenances(),
      this.notificationService.getMyNotifications()
    ]).pipe(
      map(([inscriptions, soutenances, notifications]) => {
        const widgets: StatusWidgetData[] = [];

        // Widget Inscription
        const inscriptionActuelle = inscriptions.find(i => 
          i.statut === InscriptionStatus.VALIDEE || 
          i.statut === InscriptionStatus.EN_COURS_VALIDATION
        );

        widgets.push({
          title: 'Inscription',
          value: inscriptionActuelle ? this.inscriptionService.getStatusLabel(inscriptionActuelle.statut) : 'Aucune',
          subtitle: inscriptionActuelle ? `Année ${inscriptionActuelle.campagne.anneeUniversitaire}` : undefined,
          icon: 'fas fa-user-graduate',
          color: this.getInscriptionWidgetColor(inscriptionActuelle?.statut),
          actionLabel: 'Voir détails',
          actionRoute: '/inscription'
        });

        // Widget Soutenance
        const prochaineSoutenance = soutenances.find(s => 
          s.statut !== SoutenanceStatus.SOUTENUE
        );

        widgets.push({
          title: 'Soutenance',
          value: prochaineSoutenance ? this.soutenanceService.getStatusLabel(prochaineSoutenance.statut) : 'Aucune',
          subtitle: prochaineSoutenance?.dateSoutenance ? 
            `Prévue le ${new Date(prochaineSoutenance.dateSoutenance).toLocaleDateString('fr-FR')}` : undefined,
          icon: 'fas fa-graduation-cap',
          color: this.getSoutenanceWidgetColor(prochaineSoutenance?.statut),
          actionLabel: 'Voir détails',
          actionRoute: '/soutenance'
        });

        // Widget Notifications
        const notificationsNonLues = notifications.filter(n => !n.lue).length;
        widgets.push({
          title: 'Notifications',
          value: notificationsNonLues,
          subtitle: `${notifications.length} au total`,
          icon: 'fas fa-bell',
          color: notificationsNonLues > 0 ? 'orange' : 'gray',
          actionLabel: 'Voir toutes',
          actionRoute: '/notifications'
        });

        return widgets;
      })
    );
  }

  /**
   * 🔹 Récupérer les indicateurs de progression pour le doctorant
   */
  getDoctorantProgressIndicators(): Observable<ProgressIndicator[]> {
    return combineLatest([
      this.inscriptionService.getMyInscriptions(),
      this.soutenanceService.getMySoutenances()
    ]).pipe(
      map(([inscriptions, soutenances]) => {
        const indicators: ProgressIndicator[] = [];

        // Progression générale du doctorat
        const etapesCompletees = this.calculateCompletedSteps(inscriptions, soutenances);
        indicators.push({
          current: etapesCompletees,
          total: 5, // Inscription, Formation, Recherche, Rédaction, Soutenance
          label: 'Progression générale',
          percentage: Math.round((etapesCompletees / 5) * 100)
        });

        // Documents soumis
        const totalDocuments = this.getTotalRequiredDocuments(inscriptions, soutenances);
        const documentsValides = this.getValidatedDocuments(inscriptions, soutenances);
        indicators.push({
          current: documentsValides,
          total: totalDocuments,
          label: 'Documents validés',
          percentage: totalDocuments > 0 ? Math.round((documentsValides / totalDocuments) * 100) : 0
        });

        return indicators;
      })
    );
  }

  // ===== DIRECTEUR DASHBOARD =====

  /**
   * 🔹 Récupérer les données du dashboard directeur
   */
  getDirecteurDashboardData(): Observable<DirecteurDashboardData> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      return of({
        doctorants: [],
        dossiersEnAttente: [],
        soutenancesAPlanifier: [],
        statistiques: this.getEmptyStats()
      });
    }

    return combineLatest([
      this.inscriptionService.getInscriptionsEnAttenteDirecteur(currentUser.id),
      this.soutenanceService.getSoutenancesEnAttenteDirecteur(currentUser.id)
    ]).pipe(
      map(([inscriptionsEnAttente, soutenancesEnAttente]) => {
        // Extraire les doctorants uniques
        const doctorantsMap = new Map();
        inscriptionsEnAttente.forEach(inscription => {
          if (inscription.doctorant) {
            doctorantsMap.set(inscription.doctorant.id, inscription.doctorant);
          }
        });
        soutenancesEnAttente.forEach(soutenance => {
          if (soutenance.doctorant) {
            doctorantsMap.set(soutenance.doctorant.id, soutenance.doctorant);
          }
        });

        return {
          doctorants: Array.from(doctorantsMap.values()),
          dossiersEnAttente: inscriptionsEnAttente,
          soutenancesAPlanifier: soutenancesEnAttente,
          statistiques: this.calculateDirecteurStats(inscriptionsEnAttente, soutenancesEnAttente)
        };
      })
    );
  }

  /**
   * 🔹 Récupérer les statistiques du directeur
   */
  getDirecteurStats(): Observable<DashboardStats> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      return of(this.getEmptyDirecteurStats());
    }

    return combineLatest([
      this.inscriptionService.getDoctorantsByDirecteur(),
      this.inscriptionService.getInscriptionsEnAttenteDirecteur(currentUser.id),
      this.soutenanceService.getSoutenancesByDirecteur(),
      this.soutenanceService.getSoutenancesEnAttenteDirecteur(currentUser.id)
    ]).pipe(
      map(([doctorants, inscriptionsEnAttente, soutenances, soutenancesEnAttente]) => ({
        totalDoctorants: doctorants.length,
        inscriptionsEnAttente: inscriptionsEnAttente.length,
        soutenancesEnAttente: soutenancesEnAttente.length,
        soutenancesValidees: soutenances.filter(s => s.statut === SoutenanceStatus.AUTORISEE).length,
        totalInscriptions: 0, // Not needed for directeur
        inscriptionsValidees: 0, // Not needed for directeur
        inscriptionsRejetees: 0, // Not needed for directeur
        totalSoutenances: soutenances.length,
        soutenancesAutorisees: soutenances.filter(s => s.statut === SoutenanceStatus.AUTORISEE).length
      }))
    );
  }

  /**
   * 🔹 Récupérer les alertes du directeur
   */
  getDirecteurAlerts(): Observable<DashboardAlert[]> {
    const currentUser = this.authService.getCurrentUser();
    if (!currentUser) {
      return of([]);
    }

    return combineLatest([
      this.inscriptionService.getInscriptionsEnAttenteDirecteur(currentUser.id),
      this.soutenanceService.getSoutenancesEnAttenteDirecteur(currentUser.id)
    ]).pipe(
      map(([inscriptionsEnAttente, soutenancesEnAttente]) => {
        const alerts: DashboardAlert[] = [];

        // Alerte pour inscriptions en attente
        if (inscriptionsEnAttente.length > 0) {
          alerts.push({
            id: 'inscriptions-attente',
            type: 'warning',
            title: 'Inscriptions en attente',
            message: `Vous avez ${inscriptionsEnAttente.length} inscription(s) en attente de votre avis.`,
            actionLabel: 'Voir les dossiers',
            actionRoute: '/directeur/inscriptions',
            dismissible: true
          });
        }

        // Alerte pour soutenances en attente
        if (soutenancesEnAttente.length > 0) {
          alerts.push({
            id: 'soutenances-attente',
            type: 'info',
            title: 'Demandes de soutenance',
            message: `Vous avez ${soutenancesEnAttente.length} demande(s) de soutenance à examiner.`,
            actionLabel: 'Voir les demandes',
            actionRoute: '/directeur/soutenances',
            dismissible: true
          });
        }

        return alerts;
      })
    );
  }

  // ===== ADMIN DASHBOARD =====

  /**
   * 🔹 Récupérer les données du dashboard admin
   */
  getAdminDashboardData(): Observable<AdminDashboardData> {
    return combineLatest([
      this.inscriptionService.getCampagneActive(),
      this.inscriptionService.getInscriptionsEnAttenteAdmin(),
      this.soutenanceService.getSoutenancesEnAttenteAdmin()
    ]).pipe(
      map(([campagneActive, inscriptionsEnAttente, soutenancesEnAttente]) => {
        // Transformer les données pour correspondre à DossierValidationItem
        const dossiersEnAttente = [
          ...inscriptionsEnAttente.map(inscription => ({
            id: inscription.id,
            type: 'inscription' as const,
            doctorant: {
              id: inscription.doctorant.id,
              firstName: inscription.doctorant.FirstName,
              lastName: inscription.doctorant.LastName,
              email: inscription.doctorant.email
            },
            sujetThese: inscription.sujetThese,
            dateCreation: new Date(inscription.dateInscription),
            statut: inscription.statut,
            priorite: this.calculatePriorite(inscription.dateInscription),
            documentsCount: inscription.documents?.length || 0,
            directeur: inscription.directeur ? {
              firstName: inscription.directeur.FirstName,
              lastName: inscription.directeur.LastName
            } : undefined
          })),
          ...soutenancesEnAttente.map(soutenance => ({
            id: soutenance.id,
            type: 'soutenance' as const,
            doctorant: {
              id: soutenance.doctorant.id,
              firstName: soutenance.doctorant.FirstName,
              lastName: soutenance.doctorant.LastName,
              email: soutenance.doctorant.email
            },
            titre: soutenance.titrethese,
            dateCreation: new Date(), // Use current date as fallback
            statut: soutenance.statut,
            priorite: this.calculatePriorite(new Date()),
            documentsCount: 0, // SoutenanceResponse doesn't have documents
            directeur: soutenance.directeur ? {
              firstName: soutenance.directeur.FirstName,
              lastName: soutenance.directeur.LastName
            } : undefined
          }))
        ];

        return {
          statistiques: this.calculateAdminStats(inscriptionsEnAttente, soutenancesEnAttente),
          campagneActive,
          dossiersEnAttente,
          utilisateursRecents: [] // À implémenter si nécessaire
        };
      })
    );
  }

  // ===== UTILITY METHODS =====

  private getDocumentsManquants(inscription: any, soutenance: any): string[] {
    const manquants: string[] = [];
    
    if (inscription && inscription.documents) {
      const requiredDocs = ['CARTE_IDENTITE', 'DIPLOME_MASTER', 'CV'];
      requiredDocs.forEach(docType => {
        if (!inscription.documents.find((d: any) => d.type === docType)) {
          manquants.push(this.getDocumentLabel(docType));
        }
      });
    }

    if (soutenance && soutenance.documents) {
      const requiredDocs = ['MANUSCRIT_THESE', 'RESUME_THESE'];
      requiredDocs.forEach(docType => {
        if (!soutenance.documents.find((d: any) => d.type === docType)) {
          manquants.push(this.getDocumentLabel(docType));
        }
      });
    }

    return manquants;
  }

  private generateDoctorantAlerts(inscription: any, soutenance: any): DashboardAlert[] {
    const alerts: DashboardAlert[] = [];

    // Alerte inscription en attente
    if (inscription && inscription.statut === InscriptionStatus.EN_COURS_VALIDATION) {
      alerts.push({
        id: 'inscription-validation',
        type: 'info',
        title: 'Inscription en cours de validation',
        message: 'Votre dossier d\'inscription est en cours de validation par l\'administration.',
        dismissible: true
      });
    }

    // Alerte soutenance proche
    if (soutenance && soutenance.dateSoutenance) {
      const dateSoutenance = new Date(soutenance.dateSoutenance);
      const maintenant = new Date();
      const joursRestants = Math.ceil((dateSoutenance.getTime() - maintenant.getTime()) / (1000 * 60 * 60 * 24));

      if (joursRestants <= 30 && joursRestants > 0) {
        alerts.push({
          id: 'soutenance-proche',
          type: 'warning',
          title: 'Soutenance approche',
          message: `Votre soutenance est prévue dans ${joursRestants} jours. Assurez-vous d'avoir finalisé tous les préparatifs.`,
          actionLabel: 'Voir détails',
          actionRoute: '/soutenance',
          dismissible: true
        });
      }
    }

    return alerts;
  }

  private generateDoctorantTimeline(inscriptions: any[], soutenances: any[]): TimelineEvent[] {
    const events: TimelineEvent[] = [];

    // Événements d'inscription
    inscriptions.forEach(inscription => {
      events.push({
        id: `inscription-${inscription.id}`,
        date: new Date(inscription.dateInscription),
        title: `Inscription ${inscription.campagne.anneeUniversitaire}`,
        description: `Inscription pour l'année universitaire ${inscription.campagne.anneeUniversitaire}`,
        status: this.getTimelineStatus(inscription.statut),
        type: 'inscription'
      });
    });

    // Événements de soutenance
    soutenances.forEach(soutenance => {
      if (soutenance.dateSoutenance) {
        events.push({
          id: `soutenance-${soutenance.id}`,
          date: new Date(soutenance.dateSoutenance),
          title: 'Soutenance de thèse',
          description: soutenance.titrethese || 'Soutenance de thèse',
          status: this.getTimelineStatus(soutenance.statut),
          type: 'soutenance'
        });
      }
    });

    // Trier par date
    return events.sort((a, b) => a.date.getTime() - b.date.getTime());
  }

  private getTimelineStatus(statut: string): 'completed' | 'current' | 'upcoming' | 'overdue' {
    switch (statut) {
      case InscriptionStatus.VALIDEE:
      case SoutenanceStatus.SOUTENUE:
        return 'completed';
      case InscriptionStatus.EN_COURS_VALIDATION:
      case SoutenanceStatus.EN_COURS_VALIDATION:
        return 'current';
      case InscriptionStatus.REJETEE:
      case SoutenanceStatus.REJETEE:
        return 'overdue';
      default:
        return 'upcoming';
    }
  }

  private getInscriptionWidgetColor(statut?: InscriptionStatus): 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'gray' {
    switch (statut) {
      case InscriptionStatus.VALIDEE:
        return 'green';
      case InscriptionStatus.EN_COURS_VALIDATION:
        return 'orange';
      case InscriptionStatus.REJETEE:
        return 'red';
      case InscriptionStatus.SOUMISE:
        return 'blue';
      default:
        return 'gray';
    }
  }

  private getSoutenanceWidgetColor(statut?: SoutenanceStatus): 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'gray' {
    switch (statut) {
      case SoutenanceStatus.AUTORISEE:
        return 'green';
      case SoutenanceStatus.EN_COURS_VALIDATION:
        return 'orange';
      case SoutenanceStatus.REJETEE:
        return 'red';
      case SoutenanceStatus.SOUTENUE:
        return 'purple';
      default:
        return 'gray';
    }
  }

  private calculateCompletedSteps(inscriptions: any[], soutenances: any[]): number {
    let steps = 0;
    
    // Étape 1: Inscription validée
    if (inscriptions.some(i => i.statut === InscriptionStatus.VALIDEE)) {
      steps++;
    }
    
    // Étape 2: Formation (simulé)
    if (steps > 0) steps++;
    
    // Étape 3: Recherche (simulé)
    if (steps > 1) steps++;
    
    // Étape 4: Rédaction (si soutenance créée)
    if (soutenances.length > 0) {
      steps++;
    }
    
    // Étape 5: Soutenance
    if (soutenances.some(s => s.statut === SoutenanceStatus.SOUTENUE)) {
      steps++;
    }
    
    return steps;
  }

  private getTotalRequiredDocuments(inscriptions: any[], soutenances: any[]): number {
    let total = 0;
    
    if (inscriptions.length > 0) {
      total += 3; // Documents d'inscription de base
    }
    
    if (soutenances.length > 0) {
      total += 2; // Documents de soutenance de base
    }
    
    return total;
  }

  private getValidatedDocuments(inscriptions: any[], soutenances: any[]): number {
    let validated = 0;
    
    inscriptions.forEach(inscription => {
      if (inscription.documents) {
        validated += inscription.documents.filter((d: any) => d.valide).length;
      }
    });
    
    soutenances.forEach(soutenance => {
      if (soutenance.documents) {
        validated += soutenance.documents.filter((d: any) => d.valide).length;
      }
    });
    
    return validated;
  }

  private calculateDirecteurStats(inscriptions: any[], soutenances: any[]): DashboardStats {
    return {
      totalInscriptions: inscriptions.length,
      inscriptionsEnAttente: inscriptions.filter(i => i.statut === InscriptionStatus.EN_COURS_VALIDATION).length,
      inscriptionsValidees: inscriptions.filter(i => i.statut === InscriptionStatus.VALIDEE).length,
      inscriptionsRejetees: inscriptions.filter(i => i.statut === InscriptionStatus.REJETEE).length,
      totalSoutenances: soutenances.length,
      soutenancesEnAttente: soutenances.filter(s => s.statut === SoutenanceStatus.EN_COURS_VALIDATION).length,
      soutenancesAutorisees: soutenances.filter(s => s.statut === SoutenanceStatus.AUTORISEE).length
    };
  }

  private calculateAdminStats(inscriptions: any[], soutenances: any[]): DashboardStats {
    return {
      totalInscriptions: inscriptions.length,
      inscriptionsEnAttente: inscriptions.filter(i => i.statut === InscriptionStatus.EN_COURS_VALIDATION).length,
      inscriptionsValidees: inscriptions.filter(i => i.statut === InscriptionStatus.VALIDEE).length,
      inscriptionsRejetees: inscriptions.filter(i => i.statut === InscriptionStatus.REJETEE).length,
      totalSoutenances: soutenances.length,
      soutenancesEnAttente: soutenances.filter(s => s.statut === SoutenanceStatus.EN_COURS_VALIDATION).length,
      soutenancesAutorisees: soutenances.filter(s => s.statut === SoutenanceStatus.AUTORISEE).length
    };
  }

  private getEmptyStats(): DashboardStats {
    return {
      totalInscriptions: 0,
      inscriptionsEnAttente: 0,
      inscriptionsValidees: 0,
      inscriptionsRejetees: 0,
      totalSoutenances: 0,
      soutenancesEnAttente: 0,
      soutenancesAutorisees: 0
    };
  }

  private getEmptyDirecteurStats(): DashboardStats {
    return {
      totalDoctorants: 0,
      inscriptionsEnAttente: 0,
      soutenancesEnAttente: 0,
      soutenancesValidees: 0,
      totalInscriptions: 0,
      inscriptionsValidees: 0,
      inscriptionsRejetees: 0,
      totalSoutenances: 0,
      soutenancesAutorisees: 0
    };
  }

  private getDocumentLabel(type: string): string {
    const labels: { [key: string]: string } = {
      'CARTE_IDENTITE': 'Carte d\'identité',
      'DIPLOME_MASTER': 'Diplôme de Master',
      'CV': 'Curriculum Vitae',
      'MANUSCRIT_THESE': 'Manuscrit de thèse',
      'RESUME_THESE': 'Résumé de thèse'
    };
    return labels[type] || type;
  }

  private calculatePriorite(dateCreation: string | number | Date): 'haute' | 'normale' | 'basse' {
    const now = new Date();
    const creationDate = new Date(dateCreation);
    const daysDiff = Math.floor((now.getTime() - creationDate.getTime()) / (1000 * 60 * 60 * 24));

    if (daysDiff > 7) {
      return 'haute'; // Plus de 7 jours = priorité haute
    } else if (daysDiff > 3) {
      return 'normale'; // Entre 3 et 7 jours = priorité normale
    } else {
      return 'basse'; // Moins de 3 jours = priorité basse
    }
  }
}