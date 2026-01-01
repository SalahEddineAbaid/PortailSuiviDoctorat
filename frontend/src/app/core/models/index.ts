// ✅ Core models
export * from './jwt-payload.model';
export * from './role.model';
export * from './user.model';

// ✅ Feature models
export * from './inscription.model';
export * from './soutenance.model';
export * from './notification.model';
export * from './dashboard.model';
export * from './parametrage.model';

// ✅ API models
export * from './api.model';

// ✅ Document model - export separately to avoid conflicts
export { TypeDocument, DOCUMENT_CONFIGS, ALLOWED_FILE_TYPES, MAX_FILE_SIZE_MB, MAX_FILE_SIZE_BYTES } from './document.model';
export { getDocumentConfig, getDocumentLabel, getDocumentIcon, isDocumentRequired } from './document.model';
export { getRequiredDocuments, getOptionalDocuments, validateFileSize, validateFileType, validateFile } from './document.model';
export { formatFileSize, getFormatsLabel, getMimeTypeIcon, canPreviewDocument, isImageFile, isPdfFile } from './document.model';
export { getDocumentStatusColor, getDocumentStatusLabel, getDocumentStatusIcon } from './document.model';
export { areAllRequiredDocumentsUploaded, getMissingRequiredDocuments, getDocumentsByType, hasDocumentOfType } from './document.model';
export { getUploadedDocumentsCount, getValidatedDocumentsCount, getPendingDocumentsCount, getDocumentsProgress } from './document.model';

// Re-export types with 'export type'
export type { DocumentUploadRequest, DocumentConfig, DocumentUploadProgress, DocumentValidationError } from './document.model';
export type { DocumentResponse as DocumentResponseType } from './document.model';
