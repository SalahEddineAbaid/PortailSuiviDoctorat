import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

export interface Dossier {
    id: number;
    doctorantId: number;
    doctorantName: string;
    campagneId: number;
    campagneName: string;
    status: string;
    submissionDate?: Date;
    lastUpdate?: Date;
    directeurName?: string;
    documents?: DossierDocument[];
    validations?: DossierValidation[];
}

export interface DossierDocument {
    id: number;
    name: string;
    type: string;
    url: string;
    required: boolean;
    validated?: boolean;
}

export interface DossierValidation {
    id: number;
    type: string; // 'directeur', 'admin'
    status: string;
    date: Date;
    validator: string;
    comment?: string;
}

/**
 * Dossier Consultation Component
 * Displays detailed view of a dossier with documents and validation history
 * 
 * @example
 * <app-dossier-consultation
 *   [dossier]="currentDossier"
 *   [editable]="canEdit"
 *   (validate)="onValidate($event)"
 *   (reject)="onReject($event)">
 * </app-dossier-consultation>
 */
@Component({
    selector: 'app-dossier-consultation',
    standalone: true,
    imports: [CommonModule, RouterModule],
    template: `
    <div class="dossier-consultation" *ngIf="dossier">
      <!-- Header -->
      <div class="dossier-header">
        <div class="header-content">
          <h2>Dossier d'inscription</h2>
          <div class="dossier-meta">
            <span class="meta-item">
              <i class="icon-user"></i>
              {{ dossier.doctorantName }}
            </span>
            <span class="meta-item">
              <i class="icon-calendar"></i>
              Campagne: {{ dossier.campagneName }}
            </span>
            <span *ngIf="dossier.directeurName" class="meta-item">
              <i class="icon-user-check"></i>
              Directeur: {{ dossier.directeurName }}
            </span>
          </div>
        </div>
        
        <div class="header-actions">
          <span class="status-badge" [class]="'badge-' + getStatusColor(dossier.status)">
            {{ getStatusLabel(dossier.status) }}
          </span>
        </div>
      </div>

      <!-- Tabs -->
      <div class="dossier-tabs">
        <button 
          *ngFor="let tab of tabs"
          (click)="activeTab = tab.id"
          [class.active]="activeTab === tab.id"
          class="tab-button"
          type="button">
          <i class="icon-{{tab.icon}}"></i>
          {{ tab.label }}
        </button>
      </div>

      <!-- Tab Content -->
      <div class="dossier-content">
        <!-- Informations Tab -->
        <div *ngIf="activeTab === 'info'" class="tab-panel">
          <div class="info-grid">
            <div class="info-item">
              <label>Date de soumission</label>
              <span>{{ formatDate(dossier.submissionDate) || 'Non soumis' }}</span>
            </div>
            <div class="info-item">
              <label>Dernière mise à jour</label>
              <span>{{ formatDate(dossier.lastUpdate) || '-' }}</span>
            </div>
            <div class="info-item">
              <label>Statut actuel</label>
              <span>{{ getStatusLabel(dossier.status) }}</span>
            </div>
            <div class="info-item">
              <label>ID Dossier</label>
              <span>#{{ dossier.id }}</span>
            </div>
          </div>
          
          <div class="info-section" *ngIf="showDoctorantInfo">
            <h3>Informations Doctorant</h3>
            <a [routerLink]="['/doctorants', dossier.doctorantId]" class="link-button">
              Voir profil complet →
            </a>
          </div>
        </div>

        <!-- Documents Tab -->
        <div *ngIf="activeTab === 'documents'" class="tab-panel">
          <div class="documents-section">
            <h3>Documents ({{ dossier.documents?.length || 0 }})</h3>
            
            <div *ngIf="!dossier.documents || dossier.documents.length === 0" class="empty-state">
              <i class="icon-file"></i>
              <p>Aucun document</p>
            </div>
            
            <div *ngIf="dossier.documents && dossier.documents.length > 0" class="documents-list">
              <div 
                *ngFor="let doc of dossier.documents"
                class="document-item"
                [class.required]="doc.required">
                
                <div class="doc-info">
                  <i class="icon-{{getDocIcon(doc.type)}}"></i>
                  <div class="doc-details">
                    <div class="doc-name">
                      {{ doc.name }}
                      <span *ngIf="doc.required" class="required-label">Requis</span>
                    </div>
                    <div class="doc-meta">Type: {{ doc.type.toUpperCase() }}</div>
                  </div>
                </div>
                
                <div class="doc-actions">
                  <span *ngIf="doc.validated !== undefined" class="validation-badge" [class.validated]="doc.validated">
                    <i class="icon-{{doc.validated ? 'check-circle' : 'x-circle'}}"></i>
                    {{ doc.validated ? 'Validé' : 'Rejeté' }}
                  </span>
                  <button (click)="onDocumentView(doc)" class="btn-icon" type="button" title="View">
                    <i class="icon-eye"></i>
                  </button>
                  <button (click)="onDocumentDownload(doc)" class="btn-icon" type="button" title="Download">
                    <i class="icon-download"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Validations Tab -->
        <div *ngIf="activeTab === 'validations'" class="tab-panel">
          <div class="validations-section">
            <h3>Historique des validations</h3>
            
            <div *ngIf="!dossier.validations || dossier.validations.length === 0" class="empty-state">
              <i class="icon-clock"></i>
              <p>Aucune validation</p>
            </div>
            
            <div *ngIf="dossier.validations && dossier.validations.length > 0" class="timeline">
              <div 
                *ngFor="let validation of dossier.validations; let last = last"
                class="timeline-item"
                [class.last]="last">
                
                <div class="timeline-marker" [class]="'marker-' + getValidationColor(validation.status)">
                  <i class="icon-{{getValidationIcon(validation.status)}}"></i>
                </div>
                
                <div class="timeline-content">
                  <div class="validation-header">
                    <strong>{{ getValidationType(validation.type) }}</strong>
                    <span class="validation-date">{{ formatDate(validation.date) }}</span>
                  </div>
                  <div class="validation-body">
                    <p class="validation-status">
                      <span [class]="'status-' + validation.status">{{ validation.status }}</span>
                      par {{ validation.validator }}
                    </p>
                    <p *ngIf="validation.comment" class="validation-comment">
                      "{{ validation.comment }}"
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Actions Footer -->
      <div class="dossier-footer" *ngIf="editable && canValidate()">
        <button 
          (click)="onRejectClick()"
          class="btn btn-danger"
          type="button">
          <i class="icon-x"></i>
          Rejeter
        </button>
        <button 
          (click)="onValidateClick()"
          class="btn btn-success"
          type="button">
          <i class="icon-check"></i>
          Valider
        </button>
      </div>
    </div>
  `,
    styles: [`
    .dossier-consultation {
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      overflow: hidden;
    }

    .dossier-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 1.5rem;
      background-color: #f9fafb;
      border-bottom: 1px solid #e5e7eb;
      gap: 1rem;
    }

    .header-content h2 {
      margin: 0 0 0.75rem 0;
      font-size: 1.5rem;
      font-weight: 600;
      color: #111827;
    }

    .dossier-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .meta-item {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      font-size: 0.875rem;
      color: #6b7280;
    }

    .meta-item i {
      color: #9ca3af;
    }

    .status-badge {
      padding: 0.5rem 1rem;
      border-radius: 9999px;
      font-weight: 500;
      font-size: 0.875rem;
    }

    .badge-success { background-color: #d1fae5; color: #065f46; }
    .badge-warning { background-color: #fef3c7; color: #92400e; }
    .badge-error { background-color: #fee2e2; color: #991b1b; }
    .badge-info { background-color: #dbeafe; color: #1e40af; }

    .dossier-tabs {
      display: flex;
      border-bottom: 1px solid #e5e7eb;
      background-color: white;
    }

    .tab-button {
      flex: 1;
      padding: 1rem 1.5rem;
      background: none;
      border: none;
      border-bottom: 2px solid transparent;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      color: #6b7280;
      font-weight: 500;
      transition: all 0.2s;
    }

    .tab-button:hover {
      background-color: #f9fafb;
      color: #111827;
    }

    .tab-button.active {
      color: #3b82f6;
      border-bottom-color: #3b82f6;
      background-color: #eff6ff;
    }

    .dossier-content {
      padding: 1.5rem;
    }

    .tab-panel {
      animation: fadeIn 0.2s;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2rem;
    }

    .info-item label {
      display: block;
      font-size: 0.875rem;
      color: #6b7280;
      margin-bottom: 0.375rem;
    }

    .info-item span {
      font-weight: 500;
      color: #111827;
    }

    .info-section {
      padding: 1.5rem;
      background-color: #f9fafb;
      border-radius: 0.5rem;
    }

    .info-section h3 {
      margin: 0 0 1rem 0;
      font-size: 1rem;
      font-weight: 600;
    }

    .link-button {
      color: #3b82f6;
      text-decoration: none;
      font-weight: 500;
    }

    .link-button:hover {
      text-decoration: underline;
    }

    .documents-section h3,
    .validations-section h3 {
      margin: 0 0 1.5rem 0;
      font-size: 1.125rem;
      font-weight: 600;
      color: #111827;
    }

    .documents-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .document-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      gap: 1rem;
    }

    .document-item.required {
      border-left: 3px solid #f59e0b;
    }

    .doc-info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex: 1;
      min-width: 0;
    }

    .doc-info i {
      font-size: 1.5rem;
      color: #6b7280;
    }

    .doc-details {
      flex: 1;
      min-width: 0;
    }

    .doc-name {
      font-weight: 500;
      color: #111827;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .required-label {
      padding: 0.125rem 0.5rem;
      background-color: #fef3c7;
      color: #92400e;
      font-size: 0.75rem;
      border-radius: 9999px;
    }

    .doc-meta {
      font-size: 0.75rem;
      color: #9ca3af;
    }

    .doc-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .validation-badge {
      display: flex;
      align-items: center;
      gap: 0.25rem;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 500;
      background-color: #fee2e2;
      color: #991b1b;
    }

    .validation-badge.validated {
      background-color: #d1fae5;
      color: #065f46;
    }

    .btn-icon {
      background: none;
      border: none;
      cursor: pointer;
      padding: 0.375rem;
      border-radius: 0.25rem;
      color: #6b7280;
      transition: all 0.2s;
    }

    .btn-icon:hover {
      background-color: #f3f4f6;
      color: #111827;
    }

    .timeline {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .timeline-item {
      display: flex;
      gap: 1rem;
    }

    .timeline-marker {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }

    .marker-success { background-color: #d1fae5; color: #059669; }
    .marker-error { background-color: #fee2e2; color: #dc2626; }
    .marker-warning { background-color: #fef3c7; color: #d97706; }

    .timeline-content {
      flex: 1;
      padding-top: 0.25rem;
    }

    .validation-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 0.5rem;
    }

    .validation-date {
      font-size: 0.875rem;
      color: #6b7280;
    }

    .validation-status {
      font-size: 0.875rem;
      margin: 0 0 0.5rem 0;
    }

    .status-VALIDEE { color: #059669; font-weight: 500; }
    .status-REJETEE { color: #dc2626; font-weight: 500; }

    .validation-comment {
      font-size: 0.875rem;
      font-style: italic;
      color: #6b7280;
      margin: 0;
      padding: 0.75rem;
      background-color: #f9fafb;
      border-radius: 0.375rem;
    }

    .dossier-footer {
      display: flex;
      justify-content: flex-end;
      gap: 0.75rem;
      padding: 1.5rem;
      border-top: 1px solid #e5e7eb;
      background-color: #f9fafb;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
    }

    .btn-success {
      background-color: #10b981;
      color: white;
    }

    .btn-success:hover {
      background-color: #059669;
    }

    .btn-danger {
      background-color: #ef4444;
      color: white;
    }

    .btn-danger:hover {
      background-color: #dc2626;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem;
      color: #9ca3af;
    }

    .empty-state i {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    @media (max-width: 768px) {
      .dossier-header {
        flex-direction: column;
      }

      .dossier-tabs {
        overflow-x: auto;
      }

      .tab-button {
        flex: 0 0 auto;
        min-width: 120px;
      }

      .info-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DossierConsultationComponent {
    @Input() dossier!: Dossier;
    @Input() editable: boolean = false;
    @Input() showDoctorantInfo: boolean = true;

    @Output() validate = new EventEmitter<Dossier>();
    @Output() reject = new EventEmitter<Dossier>();
    @Output() documentView = new EventEmitter<DossierDocument>();
    @Output() documentDownload = new EventEmitter<DossierDocument>();

    activeTab: string = 'info';

    tabs = [
        { id: 'info', label: 'Informations', icon: 'info' },
        { id: 'documents', label: 'Documents', icon: 'file-text' },
        { id: 'validations', label: 'Validations', icon: 'check-circle' }
    ];

    canValidate(): boolean {
        return ['SOUMISE', 'VALIDEE_DIRECTEUR'].includes(this.dossier.status);
    }

    onValidateClick(): void {
        this.validate.emit(this.dossier);
    }

    onRejectClick(): void {
        this.reject.emit(this.dossier);
    }

    onDocumentView(doc: DossierDocument): void {
        this.documentView.emit(doc);
    }

    onDocumentDownload(doc: DossierDocument): void {
        this.documentDownload.emit(doc);
    }

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'BROUILLON': 'Brouillon',
            'SOUMISE': 'Soumise',
            'VALIDEE_DIRECTEUR': 'Validée Directeur',
            'VALIDEE': 'Validée',
            'REJETEE': 'Rejetée'
        };
        return labels[status] || status;
    }

    getStatusColor(status: string): string {
        if (status === 'VALIDEE') return 'success';
        if (status === 'REJETEE') return 'error';
        if (status.includes('VALIDEE')) return 'info';
        return 'warning';
    }

    getValidationType(type: string): string {
        return type === 'directeur' ? 'Validation Directeur' : 'Validation Administrateur';
    }

    getValidationColor(status: string): string {
        if (status === 'VALIDEE') return 'success';
        if (status === 'REJETEE') return 'error';
        return 'warning';
    }

    getValidationIcon(status: string): string {
        if (status === 'VALIDEE') return 'check-circle';
        if (status === 'REJETEE') return 'x-circle';
        return 'clock';
    }

    getDocIcon(type: string): string {
        const iconMap: Record<string, string> = {
            'pdf': 'file-text',
            'doc': 'file-text',
            'docx': 'file-text',
            'jpg': 'image',
            'jpeg': 'image',
            'png': 'image'
        };
        return iconMap[type.toLowerCase()] || 'file';
    }

    formatDate(date: Date | undefined): string {
        if (!date) return '';
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }
}
