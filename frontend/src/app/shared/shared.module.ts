import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Navigation Components
import { NavbarComponent } from './components/navigation/navbar/navbar.component';
import { SidebarComponent } from './components/navigation/sidebar/sidebar.component';
import { BreadcrumbComponent } from './components/navigation/breadcrumb/breadcrumb.component';

// Feedback Components
import { AlertComponent } from './components/feedback/alert/alert.component';
import { ToastContainerComponent } from './components/feedback/toast-container/toast-container.component';
import { LoadingSpinnerComponent } from './components/feedback/loading-spinner/loading-spinner.component';
import { ProgressBarComponent } from './components/feedback/progress-bar/progress-bar.component';

// Utility Components
import { EmptyStateComponent } from './components/utility/empty-state/empty-state.component';
import { ErrorStateComponent } from './components/utility/error-state/error-state.component';
import { CardComponent } from './components/utility/card/card.component';

@NgModule({
  declarations: [
    // Note: All components are standalone, so we don't declare them here
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    // Navigation
    NavbarComponent,
    SidebarComponent,
    BreadcrumbComponent,
    // Feedback
    AlertComponent,
    ToastContainerComponent,
    LoadingSpinnerComponent,
    ProgressBarComponent,
    // Utility
    EmptyStateComponent,
    ErrorStateComponent,
    CardComponent
  ],
  exports: [
    // Export commonly used modules for convenience
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule,
    // Navigation
    NavbarComponent,
    SidebarComponent,
    BreadcrumbComponent,
    // Feedback
    AlertComponent,
    ToastContainerComponent,
    LoadingSpinnerComponent,
    ProgressBarComponent,
    // Utility
    EmptyStateComponent,
    ErrorStateComponent,
    CardComponent
  ]
})
export class SharedModule { }