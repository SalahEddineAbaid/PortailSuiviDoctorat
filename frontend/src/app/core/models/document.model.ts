// ============================================
// Enums
// ============================================

export enum TypeDocument {
  DIPLOME_MASTER = 'DIPLOME_MASTER',
  CV = 'CV',
  LETTRE_MOTIVATION = 'LETTRE_MOTIVATION',
  RELEVE_NOTES = 'RELEVE_NOTES',
  PROJET_THESE = 'PROJET_THESE',
  AUTORISATION_DIRECTEUR = 'AUTORISATION_DIRECTEUR',
  AUTRE = 'AUTRE'
}

// ============================================
// Request DTOs
// ============================================

export interface DocumentUploadRequest {
  file: File;
  typeDocument: TypeDocument;
  inscriptionId?: number;
}

// ============================================
// Response DTOs
// ============================================

export interface DocumentResponse {
  id: number;
  typeDocument: TypeDocument;
  nomFichier: string;
  tailleFichier: number;
  mimeType: string;
  dateUpload: string;
  valide: boolean;
  commentaire?: string;
}

// ============================================
// UI Models (Frontend only)
// ============================================

export interface DocumentConfig {
  type: TypeDocument;
  label: string;
  description: string;
  obligatoire: boolean;
  maxSizeMB: number;
  allowedTypes: string[];
  icon: string;
}

export interface DocumentUploadProgress {
  documentId?: number;
  type: TypeDocument;
  fileName: string;
  progress: number;
  status: 'uploading' | 'success' | 'error';
  error?: string;
}

export interface DocumentValidationError {
  type: 'size' | 'format' | 'required' | 'duplicate' | 'other';
  message: string;
}

// ============================================
// Constants
// ============================================

export const DOCUMENT_CONFIGS: DocumentConfig[] = [
  {
    type: TypeDocument.DIPLOME_MASTER,
    label: 'Diplôme de Master',
    description: 'Copie certifiée conforme du diplôme de Master ou équivalent',
    obligatoire: true,
    maxSizeMB: 10,
    allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
    icon: 'school'
  },
  {
    type: TypeDocument.CV,
    label: 'Curriculum Vitae',
    description: 'CV détaillé incluant le parcours académique et professionnel',
    obligatoire: true,
    maxSizeMB: 5,
    allowedTypes: ['application/pdf'],
    icon: 'description'
  },
  {
    type: TypeDocument.LETTRE_MOTIVATION,
    label: 'Lettre de Motivation',
    description: 'Lettre de motivation expliquant votre projet de recherche',
    obligatoire: true,
    maxSizeMB: 5,
    allowedTypes: ['application/pdf'],
    icon: 'mail'
  },
  {
    type: TypeDocument.RELEVE_NOTES,
    label: 'Relevé de Notes',
    description: 'Relevés de notes du Master (tous les semestres)',
    obligatoire: true,
    maxSizeMB: 10,
    allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
    icon: 'assessment'
  },
  {
    type: TypeDocument.PROJET_THESE,
    label: 'Projet de Thèse',
    description: 'Description détaillée du projet de recherche (5-10 pages)',
    obligatoire: true,
    maxSizeMB: 10,
    allowedTypes: ['application/pdf'],
    icon: 'article'
  },
  {
    type: TypeDocument.AUTORISATION_DIRECTEUR,
    label: 'Autorisation du Directeur',
    description: 'Lettre d\'acceptation signée par le directeur de thèse',
    obligatoire: true,
    maxSizeMB: 5,
    allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
    icon: 'verified'
  },
  {
    type: TypeDocument.AUTRE,
    label: 'Autre Document',
    description: 'Tout autre document justificatif',
    obligatoire: false,
    maxSizeMB: 10,
    allowedTypes: ['application/pdf', 'image/jpeg', 'image/png'],
    icon: 'attach_file'
  }
];

export const ALLOWED_FILE_TYPES = [
  'application/pdf',
  'image/jpeg',
  'image/png',
  'image/jpg'
];

export const MAX_FILE_SIZE_MB = 10;
export const MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024;

// ============================================
// Helper Functions
// ============================================

export function getDocumentConfig(type: TypeDocument): DocumentConfig | undefined {
  return DOCUMENT_CONFIGS.find(config => config.type === type);
}

export function getDocumentLabel(type: TypeDocument): string {
  const config = getDocumentConfig(type);
  return config?.label || type;
}

export function getDocumentIcon(type: TypeDocument): string {
  const config = getDocumentConfig(type);
  return config?.icon || 'insert_drive_file';
}

export function isDocumentRequired(type: TypeDocument): boolean {
  const config = getDocumentConfig(type);
  return config?.obligatoire || false;
}

export function getRequiredDocuments(): DocumentConfig[] {
  return DOCUMENT_CONFIGS.filter(config => config.obligatoire);
}

export function getOptionalDocuments(): DocumentConfig[] {
  return DOCUMENT_CONFIGS.filter(config => !config.obligatoire);
}

export function validateFileSize(file: File, maxSizeMB: number = MAX_FILE_SIZE_MB): DocumentValidationError | null {
  const maxSizeBytes = maxSizeMB * 1024 * 1024;
  if (file.size > maxSizeBytes) {
    return {
      type: 'size',
      message: `Le fichier est trop volumineux. Taille maximale: ${maxSizeMB} MB`
    };
  }
  return null;
}

export function validateFileType(file: File, allowedTypes: string[] = ALLOWED_FILE_TYPES): DocumentValidationError | null {
  if (!allowedTypes.includes(file.type)) {
    return {
      type: 'format',
      message: `Format de fichier non autorisé. Formats acceptés: ${getFormatsLabel(allowedTypes)}`
    };
  }
  return null;
}

export function validateFile(file: File, config?: DocumentConfig): DocumentValidationError | null {
  const maxSizeMB = config?.maxSizeMB || MAX_FILE_SIZE_MB;
  const allowedTypes = config?.allowedTypes || ALLOWED_FILE_TYPES;
  
  const sizeError = validateFileSize(file, maxSizeMB);
  if (sizeError) return sizeError;
  
  const typeError = validateFileType(file, allowedTypes);
  if (typeError) return typeError;
  
  return null;
}

export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 Bytes';
  
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  
  return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
}

export function getFormatsLabel(allowedTypes: string[]): string {
  const formatLabels: { [key: string]: string } = {
    'application/pdf': 'PDF',
    'image/jpeg': 'JPEG',
    'image/jpg': 'JPG',
    'image/png': 'PNG',
    'application/msword': 'DOC',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'DOCX'
  };
  
  return allowedTypes
    .map(type => formatLabels[type] || type)
    .join(', ');
}

export function getMimeTypeIcon(mimeType: string): string {
  if (mimeType.startsWith('image/')) {
    return 'image';
  }
  if (mimeType === 'application/pdf') {
    return 'picture_as_pdf';
  }
  if (mimeType.includes('word')) {
    return 'description';
  }
  return 'insert_drive_file';
}

export function canPreviewDocument(mimeType: string): boolean {
  return mimeType === 'application/pdf' || mimeType.startsWith('image/');
}

export function isImageFile(mimeType: string): boolean {
  return mimeType.startsWith('image/');
}

export function isPdfFile(mimeType: string): boolean {
  return mimeType === 'application/pdf';
}

export function getDocumentStatusColor(valide: boolean): string {
  return valide ? 'green' : 'orange';
}

export function getDocumentStatusLabel(valide: boolean): string {
  return valide ? 'Validé' : 'En attente';
}

export function getDocumentStatusIcon(valide: boolean): string {
  return valide ? 'check_circle' : 'pending';
}

export function areAllRequiredDocumentsUploaded(documents: DocumentResponse[]): boolean {
  const requiredTypes = getRequiredDocuments().map(config => config.type);
  const uploadedTypes = documents.map(doc => doc.typeDocument);
  
  return requiredTypes.every(type => uploadedTypes.includes(type));
}

export function getMissingRequiredDocuments(documents: DocumentResponse[]): DocumentConfig[] {
  const uploadedTypes = documents.map(doc => doc.typeDocument);
  return getRequiredDocuments().filter(config => !uploadedTypes.includes(config.type));
}

export function getDocumentsByType(documents: DocumentResponse[], type: TypeDocument): DocumentResponse[] {
  return documents.filter(doc => doc.typeDocument === type);
}

export function hasDocumentOfType(documents: DocumentResponse[], type: TypeDocument): boolean {
  return documents.some(doc => doc.typeDocument === type);
}

export function getUploadedDocumentsCount(documents: DocumentResponse[]): number {
  return documents.length;
}

export function getValidatedDocumentsCount(documents: DocumentResponse[]): number {
  return documents.filter(doc => doc.valide).length;
}

export function getPendingDocumentsCount(documents: DocumentResponse[]): number {
  return documents.filter(doc => !doc.valide).length;
}

export function getDocumentsProgress(documents: DocumentResponse[]): number {
  const required = getRequiredDocuments().length;
  const uploaded = getUploadedDocumentsCount(documents);
  return Math.round((uploaded / required) * 100);
}
