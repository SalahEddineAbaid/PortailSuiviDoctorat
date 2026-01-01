import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Data Widgets
import { ProgressWidgetComponent } from './data-widgets/progress-widget/progress-widget.component';
import { StatusWidgetComponent } from './data-widgets/status-widget/status-widget.component';
import { StatisticsWidgetComponent } from './data-widgets/statistics-widget/statistics-widget.component';

// Document Widgets
import { FileUploadComponent } from './document-widgets/file-upload/file-upload.component';
import { DocumentViewerComponent } from './document-widgets/document-viewer/document-viewer.component';
import { DocumentValidatorComponent } from './document-widgets/document-validator/document-validator.component';

// List Widgets
import { StatusTrackingComponent } from './list-widgets/status-tracking/status-tracking.component';
import { DossierConsultationComponent } from './list-widgets/dossier-consultation/dossier-consultation.component';

/**
 * Widgets Module
 * Exports all reusable widget components
 * 
 * Import this module in feature modules to use widgets:
 * @example
 * import { WidgetsModule } from '@app/shared/widgets/widgets.module';
 * 
 * @NgModule({
 *   imports: [WidgetsModule]
 * })
 * export class FeatureModule { }
 */
@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        RouterModule,
        // Import standalone components
        ProgressWidgetComponent,
        StatusWidgetComponent,
        StatisticsWidgetComponent,
        FileUploadComponent,
        DocumentViewerComponent,
        DocumentValidatorComponent,
        StatusTrackingComponent,
        DossierConsultationComponent
    ],
    exports: [
        // Export all widgets for use in other modules
        ProgressWidgetComponent,
        StatusWidgetComponent,
        StatisticsWidgetComponent,
        FileUploadComponent,
        DocumentViewerComponent,
        DocumentValidatorComponent,
        StatusTrackingComponent,
        DossierConsultationComponent
    ]
})
export class WidgetsModule { }
