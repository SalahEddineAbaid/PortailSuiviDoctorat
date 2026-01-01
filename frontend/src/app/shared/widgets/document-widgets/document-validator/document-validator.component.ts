import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface DocumentValidation {
    documentId: number;
    status: 'pending' | 'approved' | 'rejected';
    comment?: string;
    validator?: string;
    validationDate?: Date;
}

export interface Document {
    id: number;
    name: string;
    type: string;
    url: string;
    uploadDate?: Date;
    required?: boolean;
}

/**
 * Document Validator Component
 * Approve/reject documents with comments
 * 
 * @example
 * <app-document-validator
 *   [documents]="documents"
 *   [validations]="validationMap"
 *   (validate)="onDocumentValidate($event)">
 * </app-document-validator>
 */
@Component({
    selector: 'app-document-validator',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
    <div class="document-validator">
      <header class="validator-header">
        <h3>Documents à valider ({{ getPendingCount() }}/{{ documents.length }})</h3>
        <div class="header-actions" *ngIf="!readonly && hasDocuments()">
          <button 
            (click)="approveAll()"
            [disabled]="!canApproveAll()"
            class="btn btn-success"
            type="button">
            <i class="icon-check"></i>
            Tout approuver
          </button>
        </div>
      </header>

      <!-- Documents List -->
      <div class="documents-list" *ngIf="hasDocuments()">
        <div 
          *ngFor="let doc of documents"
          class="document-card"
          [class]="'status-' + getValidationStatus(doc.id)">
          
          <div class="doc-header">
            <div class="doc-info">
              <i class="doc-icon icon-{{getDocIcon(doc)}}"></i>
              <div class="doc-details">
                <div class="doc-name">
                  {{ doc.name }}
                  <span *ngIf="doc.required" class="required-badge">Requis</span>
                </div>
                <div class="doc-meta">
                  <span *ngIf="doc.uploadDate">
                    Uploadé le {{ formatDate(doc.uploadDate) }}
                  </span>
                </div>
              </div>
            </div>
            
            <!-- Status Badge -->
            <span class="status-badge" [class]="'badge-' + getValidationStatus(doc.id)">
              {{ getStatusLabel(getValidationStatus(doc.id)) }}
            </span>
          </div>

          <!-- Preview Button -->
          <button 
            (click)="selectDocument(doc)"
            class="btn btn-text"
            type="button">
            <i class="icon-eye"></i>
            Aperçu
          </button>

          <!-- Validation Form (if selected) -->
          <div *ngIf="selectedDocument?.id === doc.id && !readonly" class="validation-form">
            <div class="form-group">
              <label for="comment-{{doc.id}}">Commentaire</label>
              <textarea
                id="comment-{{doc.id}}"
                [(ngModel)]="validationComments[doc.id]"
                placeholder="Ajouter un commentaire (optionnel pour approbation, requis pour rejet)"
                rows="3"
                class="form-textarea">
              </textarea>
            </div>
            
            <div class="form-actions">
              <button 
                (click)="approve(doc)"
                class="btn btn-success"
                type="button">
                <i class="icon-check"></i>
                Approuver
              </button>
              <button 
                (click)="reject(doc)"
                class="btn btn-danger"
                type="button">
                <i class="icon-x"></i>
                Rejeter
              </button>
              <button 
                (click)="selectedDocument = undefined"
                class="btn btn-secondary"
                type="button">
                Annuler
              </button>
            </div>
          </div>

          <!-- Validation Info (if already validated) -->
          <div *ngIf="getValidation(doc.id)" class="validation-info">
            <div class="validation-details">
              <i class="icon-{{getValidationStatus(doc.id) === 'approved' ? 'check-circle' : 'x-circle'}}"></i>
              <div>
                <div class="validation-comment" *ngIf="getValidation(doc.id)?.comment">
                  "{{ getValidation(doc.id)?.comment }}"
                </div>
                <div class="validation-meta">
                  <span *ngIf="getValidation(doc.id)?.validator">
                    Par {{ getValidation(doc.id)?.validator }}
                  </span>
                  <span *ngIf="getValidation(doc.id)?.validationDate">
                    le {{ formatDate(getValidation(doc.id)!.validationDate!) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div class="empty-state" *ngIf="!hasDocuments()">
        <i class="icon-file-text"></i>
        <p>Aucun document à valider</p>
      </div>
    </div>
  `,
    styles: [`
    .document-validator {
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      overflow: hidden;
    }

    .validator-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid #e5e7eb;
      background-color: #f9fafb;
    }

    .validator-header h3 {
      margin: 0;
      font-size: 1.125rem;
      font-weight: 600;
      color: #111827;
    }

    .header-actions {
      display: flex;
      gap: 0.75rem;
    }

    .documents-list {
      padding: 1.5rem;
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .document-card {
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      padding: 1.25rem;
      transition: all 0.2s;
    }

    .document-card.status-approved {
      border-color: #10b981;
      background-color: #f0fdf4;
    }

    .document-card.status-rejected {
      border-color: #ef4444;
      background-color: #fef2f2;
    }

    .doc-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1rem;
    }

    .doc-info {
      display: flex;
      gap: 1rem;
      flex: 1;
    }

    .doc-icon {
      font-size: 2rem;
      color: #6b7280;
      flex-shrink: 0;
    }

    .doc-details {
      flex: 1;
      min-width: 0;
    }

    .doc-name {
      font-weight: 500;
      color: #111827;
      margin-bottom: 0.25rem;
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .required-badge {
      display: inline-block;
      padding: 0.125rem 0.5rem;
      background-color: #fef3c7;
      color: #92400e;
      font-size: 0.75rem;
      font-weight: 600;
      border-radius: 9999px;
    }

    .doc-meta {
      font-size: 0.875rem;
      color: #6b7280;
    }

    .status-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.875rem;
      font-weight: 500;
      flex-shrink: 0;
    }

    .badge-pending {
      background-color: #fef3c7;
      color: #92400e;
    }

    .badge-approved {
      background-color: #d1fae5;
      color: #065f46;
    }

    .badge-rejected {
      background-color: #fee2e2;
      color: #991b1b;
    }

    .validation-form {
      margin-top: 1rem;
      padding-top: 1rem;
      border-top: 1px solid #e5e7eb;
    }

    .form-group {
      margin-bottom: 1rem;
    }

    .form-group label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
      color: #374151;
      font-size: 0.875rem;
    }

    .form-textarea {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 0.375rem;
      font-size: 0.875rem;
      font-family: inherit;
      resize: vertical;
    }

    .form-textarea:focus {
      outline: none;
      border-color: #3b82f6;
      box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
    }

    .form-actions {
      display: flex;
      gap: 0.75rem;
    }

    .validation-info {
      margin-top: 1rem;
      padding: 1rem;
      background-color: #f9fafb;
      border-radius: 0.375rem;
    }

    .validation-details {
      display: flex;
      gap: 0.75rem;
      align-items: flex-start;
    }

    .validation-details i {
      font-size: 1.25rem;
      margin-top: 0.125rem;
    }

    .status-approved .validation-details i {
      color: #10b981;
    }

    .status-rejected .validation-details i {
      color: #ef4444;
    }

    .validation-comment {
      font-style: italic;
      color: #374151;
      margin-bottom: 0.5rem;
    }

    .validation-meta {
      font-size: 0.75rem;
      color: #6b7280;
      display: flex;
      gap: 0.5rem;
    }

    .btn {
      padding: 0.5rem 1rem;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
    }

    .btn-success {
      background-color: #10b981;
      color: white;
    }

    .btn-success:hover:not(:disabled) {
      background-color: #059669;
    }

    .btn-danger {
      background-color: #ef4444;
      color: white;
    }

    .btn-danger:hover {
      background-color: #dc2626;
    }

    .btn-secondary {
      background-color: #6b7280;
      color: white;
    }

    .btn-secondary:hover {
      background-color: #4b5563;
    }

    .btn-text {
      background: none;
      color: #3b82f6;
      padding: 0.5rem;
    }

    .btn-text:hover {
      background-color: #eff6ff;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
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

    @media (max-width: 640px) {
      .validator-header {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
      }

      .form-actions {
        flex-direction: column;
      }

      .form-actions .btn {
        width: 100%;
        justify-content: center;
      }
    }
  `]
})
export class DocumentValidatorComponent {
    @Input() documents: Document[] = [];
    @Input() validations: Map<number, DocumentValidation> = new Map();
    @Input() readonly: boolean = false;

    @Output() validate = new EventEmitter<{ documentId: number, approved: boolean, comment?: string }>();

    selectedDocument?: Document;
    validationComments: { [key: number]: string } = {};

    hasDocuments(): boolean {
        return this.documents.length > 0;
    }

    getPendingCount(): number {
        return this.documents.filter(d => this.getValidationStatus(d.id) === 'pending').length;
    }

    getValidation(docId: number): DocumentValidation | undefined {
        return this.validations.get(docId);
    }

    getValidationStatus(docId: number): string {
        return this.getValidation(docId)?.status || 'pending';
    }

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'pending': 'En attente',
            'approved': 'Approuvé',
            'rejected': 'Rejeté'
        };
        return labels[status] || status;
    }

    selectDocument(doc: Document): void {
        this.selectedDocument = doc;
    }

    approve(doc: Document): void {
        if (this.readonly) return;

        this.validate.emit({
            documentId: doc.id,
            approved: true,
            comment: this.validationComments[doc.id]
        });

        this.selectedDocument = undefined;
        this.validationComments[doc.id] = '';
    }

    reject(doc: Document): void {
        if (this.readonly) return;

        if (!this.validationComments[doc.id]) {
            alert('Un commentaire est requis pour rejeter un document');
            return;
        }

        this.validate.emit({
            documentId: doc.id,
            approved: false,
            comment: this.validationComments[doc.id]
        });

        this.selectedDocument = undefined;
        this.validationComments[doc.id] = '';
    }

    canApproveAll(): boolean {
        return this.documents.some(d => this.getValidationStatus(d.id) === 'pending');
    }

    approveAll(): void {
        if (this.readonly) return;

        this.documents
            .filter(d => this.getValidationStatus(d.id) === 'pending')
            .forEach(d => {
                this.validate.emit({
                    documentId: d.id,
                    approved: true
                });
            });
    }

    getDocIcon(doc: Document): string {
        const iconMap: Record<string, string> = {
            'pdf': 'file-text',
            'doc': 'file-text',
            'docx': 'file-text',
            'jpg': 'image',
            'jpeg': 'image',
            'png': 'image'
        };
        return iconMap[doc.type.toLowerCase()] || 'file';
    }

    formatDate(date: Date): string {
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }
}
