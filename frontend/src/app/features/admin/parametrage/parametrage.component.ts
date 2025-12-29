import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { ParametrageService } from '../../../core/services/parametrage.service';
import {
  SystemConfiguration,
  ConfigurationCategory,
  SeuilConfiguration,
  DocumentTypeConfiguration,
  NotificationConfiguration,
  DocumentCategory,
  NotificationDestinataire,
  SeuilRequest,
  DocumentTypeRequest,
  NotificationConfigRequest
} from '../../../core/models/parametrage.model';

@Component({
  selector: 'app-parametrage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './parametrage.component.html',
  styleUrls: ['./parametrage.component.scss']
})
export class ParametrageComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  // State management
  loading = false;
  activeTab = 'seuils';
  
  // Data arrays
  seuils: SeuilConfiguration[] = [];
  documentTypes: DocumentTypeConfiguration[] = [];
  notifications: NotificationConfiguration[] = [];
  generalConfigurations: SystemConfiguration[] = [];
  
  // Forms
  seuilsForm: FormGroup;
  documentTypeForm: FormGroup;
  generalForm: FormGroup;
  
  // Modal state
  showDocumentTypeModal = false;
  editingDocumentType: DocumentTypeConfiguration | null = null;
  
  // Enums for template
  DocumentCategory = DocumentCategory;
  NotificationDestinataire = NotificationDestinataire;

  constructor(
    private parametrageService: ParametrageService,
    private fb: FormBuilder
  ) {
    this.seuilsForm = this.fb.group({});
    this.documentTypeForm = this.createDocumentTypeForm();
    this.generalForm = this.fb.group({});
  }

  ngOnInit(): void {
    this.loadAllConfigurations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Tab management
  setActiveTab(tab: string): void {
    this.activeTab = tab;
    
    // Load data for the active tab if not already loaded
    switch (tab) {
      case 'seuils':
        if (this.seuils.length === 0) {
          this.loadSeuils();
        }
        break;
      case 'documents':
        if (this.documentTypes.length === 0) {
          this.loadDocumentTypes();
        }
        break;
      case 'notifications':
        if (this.notifications.length === 0) {
          this.loadNotifications();
        }
        break;
      case 'general':
        if (this.generalConfigurations.length === 0) {
          this.loadGeneralConfigurations();
        }
        break;
    }
  }

  // Data loading methods
  private loadAllConfigurations(): void {
    this.loadSeuils();
  }

  private loadSeuils(): void {
    this.loading = true;
    this.parametrageService.getAllSeuils()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.seuils = response.data;
          this.buildSeuilsForm();
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement des seuils:', error);
          this.showError('Erreur lors du chargement des seuils');
        }
      });
  }

  private loadDocumentTypes(): void {
    this.loading = true;
    this.parametrageService.getAllDocumentTypes()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.documentTypes = response.data;
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement des types de documents:', error);
          this.showError('Erreur lors du chargement des types de documents');
        }
      });
  }

  private loadNotifications(): void {
    this.loading = true;
    this.parametrageService.getAllNotificationConfigs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.notifications = response.data;
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement des notifications:', error);
          this.showError('Erreur lors du chargement des notifications');
        }
      });
  }

  private loadGeneralConfigurations(): void {
    this.loading = true;
    this.parametrageService.getConfigurationsByCategory(ConfigurationCategory.GENERAL)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.generalConfigurations = response.data;
          this.buildGeneralForm();
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors du chargement de la configuration générale:', error);
          this.showError('Erreur lors du chargement de la configuration générale');
        }
      });
  }

  // Form building methods
  private buildSeuilsForm(): void {
    const formControls: { [key: string]: any } = {};
    
    this.seuils.forEach(seuil => {
      formControls[`seuil_${seuil.id}`] = [seuil.valeur, [Validators.required, Validators.min(0)]];
    });
    
    this.seuilsForm = this.fb.group(formControls);
  }

  private buildGeneralForm(): void {
    const formControls: { [key: string]: any } = {};
    
    this.generalConfigurations.forEach(config => {
      const validators = [Validators.required];
      let defaultValue: any = config.value;
      
      if (config.type === 'BOOLEAN') {
        defaultValue = config.value === 'true';
      } else if (config.type === 'NUMBER') {
        validators.push(Validators.pattern(/^\d+$/));
        defaultValue = parseInt(config.value) || 0;
      }
      
      formControls[`config_${config.id}`] = [defaultValue, validators];
    });
    
    this.generalForm = this.fb.group(formControls);
  }

  private createDocumentTypeForm(): FormGroup {
    return this.fb.group({
      nom: ['', [Validators.required, Validators.maxLength(100)]],
      type: ['', [Validators.required, Validators.maxLength(50)]],
      category: ['', Validators.required],
      description: ['', Validators.maxLength(500)],
      tailleMaxMo: [10, [Validators.required, Validators.min(1), Validators.max(100)]],
      formatAutoriseStr: ['PDF,JPG,PNG', Validators.required],
      obligatoire: [false]
    });
  }

  // Save methods
  saveSeuils(): void {
    if (this.seuilsForm.invalid) {
      this.markFormGroupTouched(this.seuilsForm);
      return;
    }

    this.loading = true;
    const updatedSeuils = this.seuils.map(seuil => ({
      ...seuil,
      valeur: this.seuilsForm.get(`seuil_${seuil.id}`)?.value || seuil.valeur
    }));

    const request: SeuilRequest = { seuils: updatedSeuils };

    this.parametrageService.updateSeuils(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.seuils = response.data;
          this.buildSeuilsForm();
          this.showSuccess('Seuils mis à jour avec succès');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de la sauvegarde des seuils:', error);
          this.showError('Erreur lors de la sauvegarde des seuils');
        }
      });
  }

  saveNotifications(): void {
    this.loading = true;
    const request: NotificationConfigRequest = { notifications: this.notifications };

    this.parametrageService.updateNotificationConfigs(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.notifications = response.data;
          this.showSuccess('Configuration des notifications mise à jour');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de la sauvegarde des notifications:', error);
          this.showError('Erreur lors de la sauvegarde des notifications');
        }
      });
  }

  saveGeneralConfig(): void {
    if (this.generalForm.invalid) {
      this.markFormGroupTouched(this.generalForm);
      return;
    }

    this.loading = true;
    const updatedConfigs = this.generalConfigurations.map(config => ({
      ...config,
      value: this.getFormValue(config)
    }));

    const request = {
      category: ConfigurationCategory.GENERAL,
      configurations: updatedConfigs
    };

    this.parametrageService.updateConfigurations(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.generalConfigurations = response.updatedConfigurations;
          this.buildGeneralForm();
          this.showSuccess('Configuration générale mise à jour');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de la sauvegarde de la configuration:', error);
          this.showError('Erreur lors de la sauvegarde de la configuration');
        }
      });
  }

  private getFormValue(config: SystemConfiguration): string {
    const formValue = this.generalForm.get(`config_${config.id}`)?.value;
    
    if (config.type === 'BOOLEAN') {
      return formValue ? 'true' : 'false';
    }
    
    return formValue?.toString() || config.value;
  }

  // Document type management
  addDocumentType(): void {
    this.editingDocumentType = null;
    this.documentTypeForm.reset({
      tailleMaxMo: 10,
      formatAutoriseStr: 'PDF,JPG,PNG',
      obligatoire: false
    });
    this.showDocumentTypeModal = true;
  }

  editDocumentType(docType: DocumentTypeConfiguration): void {
    this.editingDocumentType = docType;
    this.documentTypeForm.patchValue({
      ...docType,
      formatAutoriseStr: docType.formatAutorise.join(',')
    });
    this.showDocumentTypeModal = true;
  }

  saveDocumentType(): void {
    if (this.documentTypeForm.invalid) {
      this.markFormGroupTouched(this.documentTypeForm);
      return;
    }

    this.loading = true;
    const formValue = this.documentTypeForm.value;
    const documentType: DocumentTypeConfiguration = {
      id: this.editingDocumentType?.id || 0,
      nom: formValue.nom,
      type: formValue.type,
      category: formValue.category,
      description: formValue.description,
      tailleMaxMo: formValue.tailleMaxMo,
      formatAutorise: formValue.formatAutoriseStr.split(',').map((f: string) => f.trim()),
      obligatoire: formValue.obligatoire
    };

    if (this.editingDocumentType) {
      // Update existing document type
      this.parametrageService.updateDocumentTypes({ documentTypes: [documentType] })
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loading = false;
            this.loadDocumentTypes();
            this.closeDocumentTypeModal();
            this.showSuccess('Type de document modifié avec succès');
          },
          error: (error: any) => {
            this.loading = false;
            console.error('Erreur lors de la sauvegarde du type de document:', error);
            this.showError('Erreur lors de la sauvegarde du type de document');
          }
        });
    } else {
      // Create new document type
      this.parametrageService.createDocumentType(documentType)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.loading = false;
            this.loadDocumentTypes();
            this.closeDocumentTypeModal();
            this.showSuccess('Type de document ajouté avec succès');
          },
          error: (error: any) => {
            this.loading = false;
            console.error('Erreur lors de la sauvegarde du type de document:', error);
            this.showError('Erreur lors de la sauvegarde du type de document');
          }
        });
    }
  }

  deleteDocumentType(id: number): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce type de document ?')) {
      return;
    }

    this.loading = true;
    this.parametrageService.deleteDocumentType(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.loadDocumentTypes();
          this.showSuccess('Type de document supprimé avec succès');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de la suppression du type de document:', error);
          this.showError('Erreur lors de la suppression du type de document');
        }
      });
  }

  closeDocumentTypeModal(): void {
    this.showDocumentTypeModal = false;
    this.editingDocumentType = null;
    this.documentTypeForm.reset();
  }

  // Notification management
  toggleNotification(id: number, event: any): void {
    const actif = event.target.checked;
    
    this.parametrageService.toggleNotificationConfig(id, actif)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          const notification = this.notifications.find(n => n.id === id);
          if (notification) {
            notification.actif = response.data.actif;
          }
          this.showSuccess(`Notification ${actif ? 'activée' : 'désactivée'}`);
        },
        error: (error: any) => {
          console.error('Erreur lors de la modification de la notification:', error);
          this.showError('Erreur lors de la modification de la notification');
          // Revert the checkbox state
          event.target.checked = !actif;
        }
      });
  }

  editNotification(notification: NotificationConfiguration): void {
    // TODO: Implement notification editing modal
    console.log('Edit notification:', notification);
  }

  // Reset methods
  resetSeuils(): void {
    if (!confirm('Êtes-vous sûr de vouloir réinitialiser tous les seuils aux valeurs par défaut ?')) {
      return;
    }

    this.loading = true;
    this.parametrageService.resetToDefaults(ConfigurationCategory.SEUILS)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.loadSeuils();
          this.showSuccess('Seuils réinitialisés aux valeurs par défaut');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de la réinitialisation des seuils:', error);
          this.showError('Erreur lors de la réinitialisation des seuils');
        }
      });
  }

  // Import/Export methods
  exportConfiguration(): void {
    this.loading = true;
    this.parametrageService.exportConfiguration()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (blob) => {
          this.loading = false;
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = `parametrage-${new Date().toISOString().split('T')[0]}.json`;
          link.click();
          window.URL.revokeObjectURL(url);
          this.showSuccess('Configuration exportée avec succès');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de l\'export de la configuration:', error);
          this.showError('Erreur lors de l\'export de la configuration');
        }
      });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (!file) return;

    if (file.type !== 'application/json') {
      this.showError('Veuillez sélectionner un fichier JSON valide');
      return;
    }

    this.loading = true;
    this.parametrageService.importConfiguration(file)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.loading = false;
          this.loadAllConfigurations();
          this.showSuccess('Configuration importée avec succès');
        },
        error: (error) => {
          this.loading = false;
          console.error('Erreur lors de l\'import de la configuration:', error);
          this.showError('Erreur lors de l\'import de la configuration');
        }
      });

    // Reset file input
    event.target.value = '';
  }

  // Utility methods
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  private showSuccess(message: string): void {
    // TODO: Implement toast notification service
    console.log('Success:', message);
    alert(message);
  }

  private showError(message: string): void {
    // TODO: Implement toast notification service
    console.error('Error:', message);
    alert(message);
  }
}