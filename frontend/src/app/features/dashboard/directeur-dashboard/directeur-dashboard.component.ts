import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Observable, combineLatest } from 'rxjs';
import { map } from 'rxjs/operators';

import { DashboardService } from '../../../core/services/dashboard.service';
import { AuthService } from '../../../core/services/auth.service';
import { InscriptionService } from '../../../core/services/inscription.service';
import { SoutenanceService } from '../../../core/services/soutenance.service';
import { DoctorantListComponent } from '../../../shared/components/doctorant-list/doctorant-list.component';
import { StatusWidgetComponent } from '../../../shared/components/status-widget/status-widget.component';
import { AlertComponent } from '../../../shared/components/alert/alert.component';
import { 
  DashboardStats,
  DashboardAlert 
} from '../../../core/models/dashboard.model';

interface DirecteurDashboardData {
  stats: DashboardStats;
  doctorants: UserInfo[];
  soutenances: SoutenanceResponse[];
  alerts: DashboardAlert[];
  recentActivity: any[];
}
import { UserInfo } from '../../../core/services/auth.service';
import { Inscription } from '../../../core/models/inscription.model';
import { SoutenanceResponse } from '../../../core/models/soutenance.model';

@Component({
  selector: 'app-directeur-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    DoctorantListComponent,
    StatusWidgetComponent,
    AlertComponent
  ],
  templateUrl: './directeur-dashboard.component.html',
  styleUrls: ['./directeur-dashboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DirecteurDashboardComponent implements OnInit {
  dashboardData$!: Observable<DirecteurDashboardData>;
  currentUser: UserInfo | null = null;

  constructor(
    private dashboardService: DashboardService,
    private authService: AuthService,
    private inscriptionService: InscriptionService,
    private soutenanceService: SoutenanceService
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    this.loadDashboardData();
  }

  private loadDashboardData(): void {
    // Combine multiple API calls to build the dashboard data
    this.dashboardData$ = combineLatest([
      this.dashboardService.getDirecteurStats(),
      this.inscriptionService.getDoctorantsByDirecteur(),
      this.soutenanceService.getSoutenancesByDirecteur(),
      this.dashboardService.getDirecteurAlerts()
    ]).pipe(
      map(([stats, doctorants, soutenances, alerts]) => ({
        stats,
        doctorants,
        soutenances,
        alerts,
        recentActivity: this.buildRecentActivity(doctorants, soutenances)
      }))
    );
  }

  private buildRecentActivity(doctorants: UserInfo[], soutenances: SoutenanceResponse[]): any[] {
    const activities: any[] = [];

    // Add recent inscriptions
    doctorants.forEach(doctorant => {
      activities.push({
        type: 'inscription',
        doctorant: doctorant,
        date: new Date(), // This would come from the actual inscription date
        message: `Nouvelle inscription de ${doctorant.FirstName} ${doctorant.LastName}`
      });
    });

    // Add recent soutenance requests
    soutenances.forEach(soutenance => {
      activities.push({
        type: 'soutenance',
        doctorant: soutenance.doctorant,
        date: new Date(), // This would come from the actual submission date
        message: `Demande de soutenance de ${soutenance.doctorant.FirstName} ${soutenance.doctorant.LastName}`
      });
    });

    // Sort by date (most recent first) and take only the last 5
    return activities
      .sort((a, b) => b.date.getTime() - a.date.getTime())
      .slice(0, 5);
  }

  onDoctorantSelected(doctorant: UserInfo): void {
    // Navigate to doctorant details or open consultation modal
    console.log('Doctorant selected:', doctorant);
  }

  onRefreshData(): void {
    this.loadDashboardData();
  }
}