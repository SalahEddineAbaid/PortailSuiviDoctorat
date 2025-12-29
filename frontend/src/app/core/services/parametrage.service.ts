import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  SystemConfiguration,
  ConfigurationCategory,
  SeuilConfiguration,
  DocumentTypeConfiguration,
  NotificationConfiguration,
  ParametrageRequest,
  ParametrageResponse,
  SeuilRequest,
  DocumentTypeRequest,
  NotificationConfigRequest
} from '../models/parametrage.model';
import { ApiResponse } from '../models/api.model';

@Injectable({
  providedIn: 'root'
})
export class ParametrageService {
  private readonly baseUrl = `${environment.apiUrl}/api/parametrage`;

  constructor(private http: HttpClient) {}

  // Configuration générale
  getAllConfigurations(): Observable<ApiResponse<SystemConfiguration[]>> {
    return this.http.get<ApiResponse<SystemConfiguration[]>>(`${this.baseUrl}/configurations`);
  }

  getConfigurationsByCategory(category: ConfigurationCategory): Observable<ApiResponse<SystemConfiguration[]>> {
    return this.http.get<ApiResponse<SystemConfiguration[]>>(`${this.baseUrl}/configurations/${category}`);
  }

  updateConfigurations(request: ParametrageRequest): Observable<ParametrageResponse> {
    return this.http.put<ParametrageResponse>(`${this.baseUrl}/configurations`, request);
  }

  // Gestion des seuils
  getAllSeuils(): Observable<ApiResponse<SeuilConfiguration[]>> {
    return this.http.get<ApiResponse<SeuilConfiguration[]>>(`${this.baseUrl}/seuils`);
  }

  updateSeuils(request: SeuilRequest): Observable<ApiResponse<SeuilConfiguration[]>> {
    return this.http.put<ApiResponse<SeuilConfiguration[]>>(`${this.baseUrl}/seuils`, request);
  }

  getSeuilById(id: number): Observable<ApiResponse<SeuilConfiguration>> {
    return this.http.get<ApiResponse<SeuilConfiguration>>(`${this.baseUrl}/seuils/${id}`);
  }

  // Gestion des types de documents
  getAllDocumentTypes(): Observable<ApiResponse<DocumentTypeConfiguration[]>> {
    return this.http.get<ApiResponse<DocumentTypeConfiguration[]>>(`${this.baseUrl}/document-types`);
  }

  updateDocumentTypes(request: DocumentTypeRequest): Observable<ApiResponse<DocumentTypeConfiguration[]>> {
    return this.http.put<ApiResponse<DocumentTypeConfiguration[]>>(`${this.baseUrl}/document-types`, request);
  }

  createDocumentType(documentType: DocumentTypeConfiguration): Observable<ApiResponse<DocumentTypeConfiguration>> {
    return this.http.post<ApiResponse<DocumentTypeConfiguration>>(`${this.baseUrl}/document-types`, documentType);
  }

  deleteDocumentType(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/document-types/${id}`);
  }

  // Gestion des paramètres de notification
  getAllNotificationConfigs(): Observable<ApiResponse<NotificationConfiguration[]>> {
    return this.http.get<ApiResponse<NotificationConfiguration[]>>(`${this.baseUrl}/notifications`);
  }

  updateNotificationConfigs(request: NotificationConfigRequest): Observable<ApiResponse<NotificationConfiguration[]>> {
    return this.http.put<ApiResponse<NotificationConfiguration[]>>(`${this.baseUrl}/notifications`, request);
  }

  toggleNotificationConfig(id: number, actif: boolean): Observable<ApiResponse<NotificationConfiguration>> {
    return this.http.patch<ApiResponse<NotificationConfiguration>>(`${this.baseUrl}/notifications/${id}/toggle`, { actif });
  }

  // Réinitialisation des paramètres
  resetToDefaults(category: ConfigurationCategory): Observable<ApiResponse<SystemConfiguration[]>> {
    return this.http.post<ApiResponse<SystemConfiguration[]>>(`${this.baseUrl}/reset/${category}`, {});
  }

  // Export/Import de configuration
  exportConfiguration(): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/export`, { responseType: 'blob' });
  }

  importConfiguration(file: File): Observable<ApiResponse<SystemConfiguration[]>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<SystemConfiguration[]>>(`${this.baseUrl}/import`, formData);
  }
}