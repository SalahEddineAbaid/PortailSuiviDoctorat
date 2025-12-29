import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Import existing components
import { AlertComponent } from './components/alert/alert.component';
import { StatisticsComponent } from './components/statistics/statistics.component';
import { TimelineComponent } from './components/timeline/timeline.component';
import { ProgressWidgetComponent } from './components/progress-widget/progress-widget.component';
import { StatusWidgetComponent } from './components/status-widget/status-widget.component';
import { DoctorantListComponent } from './components/doctorant-list/doctorant-list.component';
import { DossierConsultationComponent } from './components/dossier-consultation/dossier-consultation.component';
import { DossierValidationListComponent } from './components/dossier-validation-list/dossier-validation-list.component';
import { AvisFormComponent } from './components/avis-form/avis-form.component';
import { StatusTrackingComponent } from './components/status-tracking/status-tracking.component';
import { PrerequisCheckComponent } from './components/prerequis-check/prerequis-check.component';
import { DocumentViewerComponent } from './components/document-viewer/document-viewer.component';
import { DocumentDownloadComponent } from './components/document-download/document-download.component';
import { AttestationGeneratorComponent } from './components/attestation-generator/attestation-generator.component';
import { AutorisationSoutenanceComponent } from './components/autorisation-soutenance/autorisation-soutenance.component';
import { ProcesVerbalComponent } from './components/proces-verbal/proces-verbal.component';
import { DocumentValidatorComponent } from './components/document-validator/document-validator.component';

// Import new UI components
import { StepperComponent } from './components/stepper/stepper.component';
import { ProgressBarComponent } from './components/progress-bar/progress-bar.component';
import { FileUploadComponent } from './components/file-upload/file-upload.component';
import { LoadingSpinnerComponent } from './components/loading-spinner/loading-spinner.component';
import { ConfirmationDialogComponent } from './components/confirmation-dialog/confirmation-dialog.component';

// Import navigation components
import { BreadcrumbComponent } from './components/breadcrumb/breadcrumb.component';
import { TabsComponent, TabComponent } from './components/tabs/tabs.component';
import { ResponsiveLayoutComponent } from './components/responsive-layout/responsive-layout.component';
import { AccessibilitySettingsComponent } from './components/accessibility-settings/accessibility-settings.component';

// Import accessibility directives
import { FocusTrapDirective } from './directives/focus-trap.directive';
import { SkipLinkDirective } from './directives/skip-link.directive';
import { AnnounceDirective } from './directives/announce.directive';

@NgModule({
  declarations: [
    // Note: Most components are already standalone, so we don't declare them here
    // This module serves as a central place to export commonly used modules
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule
  ],
  exports: [
    // Export commonly used modules
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    
    // Export existing standalone components
    AlertComponent,
    StatisticsComponent,
    TimelineComponent,
    ProgressWidgetComponent,
    StatusWidgetComponent,
    DoctorantListComponent,
    DossierConsultationComponent,
    DossierValidationListComponent,
    AvisFormComponent,
    StatusTrackingComponent,
    PrerequisCheckComponent,
    DocumentViewerComponent,
    DocumentDownloadComponent,
    AttestationGeneratorComponent,
    AutorisationSoutenanceComponent,
    ProcesVerbalComponent,
    DocumentValidatorComponent,
    
    // Export new UI components
    StepperComponent,
    ProgressBarComponent,
    FileUploadComponent,
    LoadingSpinnerComponent,
    ConfirmationDialogComponent,
    
    // Export navigation components
    BreadcrumbComponent,
    TabsComponent,
    TabComponent,
    ResponsiveLayoutComponent,
    AccessibilitySettingsComponent,
    
    // Export accessibility directives
    FocusTrapDirective,
    SkipLinkDirective,
    AnnounceDirective
  ]
})
export class SharedModule { }