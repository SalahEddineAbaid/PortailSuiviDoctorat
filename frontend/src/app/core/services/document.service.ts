import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
    Document,
    DocumentRequest,
    DocumentResponse,
    DocumentUploadRequest,
    DocumentType
} from '../models/document.model';

@Injectable({
    providedIn: 'root'
})
export class DocumentService {
    private readonly API_URL = `${environment.apiUrl}/documents`;

    constructor(private http: HttpClient) { }

    // ===== DOCUMENT ENDPOINTS =====

    /**
     * 🔹 Uploader un document
     */
    uploadDocument(uploadRequest: DocumentUploadRequest, inscriptionId?: number): Observable<DocumentResponse> {
        const formData = new FormData();
        formData.append('file', uploadRequest.file);
        formData.append('type', uploadRequest.type);
        formData.append('obligatoire', uploadRequest.obligatoire.toString());

        if (inscriptionId) {
            formData.append('inscriptionId', inscriptionId.toString());
        }

        console.log('📤 [DOCUMENT SERVICE] Upload document:', uploadRequest.type, uploadRequest.file.name);
        return this.http.post<DocumentResponse>(this.API_URL, formData);
    }

    /**
     * 🔹 Récupérer un document par ID
     */
    getDocument(id: number): Observable<DocumentResponse> {
        console.log('📤 [DOCUMENT SERVICE] Récupération document:', id);
        return this.http.get<DocumentResponse>(`${this.API_URL}/${id}`);
    }

    /**
     * 🔹 Télécharger un document
     */
    downloadDocument(id: number): Observable<Blob> {
        console.log('📤 [DOCUMENT SERVICE] Téléchargement document:', id);
        return this.http.get(`${this.API_URL}/${id}/download`, {
            responseType: 'blob'
        });
    }

    /**
     * 🔹 Supprimer un document
     */
    deleteDocument(id: number): Observable<any> {
        console.log('📤 [DOCUMENT SERVICE] Suppression document:', id);
        return this.http.delete(`${this.API_URL}/${id}`);
    }

    /**
     * 🔹 Récupérer les documents d'une inscription
     */
    getDocumentsInscription(inscriptionId: number): Observable<DocumentResponse[]> {
        console.log('📤 [DOCUMENT SERVICE] Documents inscription:', inscriptionId);
        return this.http.get<DocumentResponse[]>(`${this.API_URL}/inscription/${inscriptionId}`);
    }

    /**
     * 🔹 Récupérer les documents d'une soutenance
     */
    getDocumentsSoutenance(soutenanceId: number): Observable<DocumentResponse[]> {
        console.log('📤 [DOCUMENT SERVICE] Documents soutenance:', soutenanceId);
        return this.http.get<DocumentResponse[]>(`${this.API_URL}/soutenance/${soutenanceId}`);
    }

    /**
     * 🔹 Récupérer mes documents (utilisateur connecté)
     */
    getMyDocuments(): Observable<DocumentResponse[]> {
        console.log('📤 [DOCUMENT SERVICE] Mes documents');
        return this.http.get<DocumentResponse[]>(`${this.API_URL}/me`);
    }

    /**
     * 🔹 Valider un document (ADMIN/DIRECTEUR)
     */
    validateDocument(id: number, valide: boolean, commentaire?: string): Observable<DocumentResponse> {
        console.log('📤 [DOCUMENT SERVICE] Validation document:', id, valide);
        return this.http.post<DocumentResponse>(`${this.API_URL}/${id}/validate`, {
            valide,
            commentaire
        });
    }

    /**
     * 🔹 Remplacer un document existant
     */
    replaceDocument(id: number, file: File): Observable<DocumentResponse> {
        const formData = new FormData();
        formData.append('file', file);

        console.log('📤 [DOCUMENT SERVICE] Remplacement document:', id, file.name);
        return this.http.put<DocumentResponse>(`${this.API_URL}/${id}/replace`, formData);
    }

    // ===== UTILITY METHODS =====

    /**
     * 🔹 Valider le format d'un fichier
     */
    validateFileFormat(file: File, allowedTypes: string[] = ['application/pdf', 'image/jpeg', 'image/png']): boolean {
        return allowedTypes.includes(file.type);
    }

    /**
     * 🔹 Valider la taille d'un fichier (en MB)
     */
    validateFileSize(file: File, maxSizeMB: number = 10): boolean {
        const maxSizeBytes = maxSizeMB * 1024 * 1024;
        return file.size <= maxSizeBytes;
    }

    /**
     * 🔹 Obtenir le libellé d'un type de document
     */
    getDocumentTypeLabel(type: DocumentType): string {
        const labels = {
            [DocumentType.CARTE_IDENTITE]: 'Carte d\'identité',
            [DocumentType.DIPLOME_MASTER]: 'Diplôme de Master',
            [DocumentType.RELEVES_NOTES]: 'Relevés de notes',
            [DocumentType.CV]: 'Curriculum Vitae',
            [DocumentType.LETTRE_MOTIVATION]: 'Lettre de motivation',
            [DocumentType.MANUSCRIT_THESE]: 'Manuscrit de thèse',
            [DocumentType.RESUME_THESE]: 'Résumé de thèse',
            [DocumentType.PUBLICATIONS]: 'Publications',
            [DocumentType.ATTESTATION_FORMATION]: 'Attestation de formation',
            [DocumentType.AUTORISATION_SOUTENANCE]: 'Autorisation de soutenance'
        };
        return labels[type] || type;
    }

    /**
     * 🔹 Obtenir l'icône d'un type de document
     */
    getDocumentTypeIcon(type: DocumentType): string {
        const icons = {
            [DocumentType.CARTE_IDENTITE]: 'badge',
            [DocumentType.DIPLOME_MASTER]: 'school',
            [DocumentType.RELEVES_NOTES]: 'assessment',
            [DocumentType.CV]: 'person',
            [DocumentType.LETTRE_MOTIVATION]: 'mail',
            [DocumentType.MANUSCRIT_THESE]: 'book',
            [DocumentType.RESUME_THESE]: 'description',
            [DocumentType.PUBLICATIONS]: 'library_books',
            [DocumentType.ATTESTATION_FORMATION]: 'verified',
            [DocumentType.AUTORISATION_SOUTENANCE]: 'gavel'
        };
        return icons[type] || 'insert_drive_file';
    }

    /**
     * 🔹 Formater la taille d'un fichier
     */
    formatFileSize(bytes: number): string {
        if (bytes === 0) return '0 Bytes';

        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));

        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }

    /**
     * 🔹 Obtenir les types de documents requis pour une inscription
     */
    getRequiredDocumentsForInscription(): DocumentType[] {
        return [
            DocumentType.CARTE_IDENTITE,
            DocumentType.DIPLOME_MASTER,
            DocumentType.RELEVES_NOTES,
            DocumentType.CV,
            DocumentType.LETTRE_MOTIVATION
        ];
    }

    /**
     * 🔹 Obtenir les types de documents requis pour une soutenance
     */
    getRequiredDocumentsForSoutenance(): DocumentType[] {
        return [
            DocumentType.MANUSCRIT_THESE,
            DocumentType.RESUME_THESE,
            DocumentType.ATTESTATION_FORMATION
        ];
    }

    /**
     * 🔹 Vérifier si tous les documents obligatoires sont présents
     */
    areRequiredDocumentsPresent(documents: DocumentResponse[], requiredTypes: DocumentType[]): boolean {
        const presentTypes = documents.map(doc => doc.type);
        return requiredTypes.every(type => presentTypes.includes(type));
    }

    /**
     * 🔹 Obtenir les documents manquants
     */
    getMissingDocuments(documents: DocumentResponse[], requiredTypes: DocumentType[]): DocumentType[] {
        const presentTypes = documents.map(doc => doc.type);
        return requiredTypes.filter(type => !presentTypes.includes(type));
    }

    /**
     * 🔹 Créer un nom de fichier sécurisé
     */
    sanitizeFileName(fileName: string): string {
        return fileName
            .replace(/[^a-zA-Z0-9.-]/g, '_')
            .replace(/_{2,}/g, '_')
            .toLowerCase();
    }

    // ===== DOCUMENT GENERATION ENDPOINTS =====

    /**
     * 🔹 Générer une attestation
     */
    generateAttestation(request: any): Observable<any> {
        console.log('📤 [DOCUMENT SERVICE] Génération attestation:', request);
        return this.http.post(`${this.API_URL}/generate/attestation`, request);
    }

    /**
     * 🔹 Générer une autorisation de soutenance
     */
    generateAutorisationSoutenance(request: any): Observable<any> {
        console.log('📤 [DOCUMENT SERVICE] Génération autorisation soutenance:', request);
        return this.http.post(`${this.API_URL}/generate/autorisation-soutenance`, request);
    }

    /**
     * 🔹 Générer un procès-verbal
     */
    generateProcesVerbal(request: any): Observable<any> {
        console.log('📤 [DOCUMENT SERVICE] Génération procès-verbal:', request);
        return this.http.post(`${this.API_URL}/generate/proces-verbal`, request);
    }
}