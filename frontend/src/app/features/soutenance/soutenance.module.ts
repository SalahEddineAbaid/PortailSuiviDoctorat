import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

import { soutenanceRoutes } from './soutenance.routes';
import { SoutenanceContainer } from './soutenance-container/soutenance-container';
import { SoutenanceDashboard } from './soutenance-dashboard/soutenance-dashboard';
import { SoutenanceForm } from './soutenance-form';
import { SoutenanceDetail } from './soutenance-detail';
import { SoutenanceList } from './soutenance-list';
import { JuryProposalComponent } from './jury-proposal/jury-proposal.component';
import { StatusTrackingComponent } from '../../shared/components/status-tracking/status-tracking.component';

@NgModule({
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild(soutenanceRoutes),
    // Standalone components are imported automatically
    SoutenanceContainer,
    SoutenanceDashboard,
    SoutenanceForm,
    SoutenanceDetail,
    SoutenanceList,
    JuryProposalComponent,
    StatusTrackingComponent
  ]
})
export class SoutenanceModule { }