import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { InscriptionService } from '../../../core/services/inscription.service';
import { CampagneService } from '../../../core/services/campagne.service';
import { AuthService, UserInfo } from '../../../core/services/auth.service';

import { 
  InscriptionResponse, 
  StatutInscription, 
  TypeInscription,
  getStatutLabel,
  getStatutColor,
  getTypeInscriptionLabel
} from '../../../core/models/inscription.model';
import { CampagneResponse } from '../../../core/models/campagne.model';

@Component({
  selector: 'app-inscription-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatMenuModule
  ],
  templateUrl: './inscription-list.html',
  styleUrls: ['./inscription-list.scss']
})
export class InscriptionList implements OnInit {
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  dataSource = new MatTableDataSource<InscriptionResponse>();
  displayedColumns: string[] = [
    'id',
    'anneeInscription',
    'type',
    'sujetThese',
    'statut',
    'dateCreation',
    'actions'
  ];

  filterForm!: FormGroup;
  loading = false;
  currentUser: UserInfo | null = null;
  campagnes: CampagneResponse[] = [];

  // Enums for template
  statutOptions = Object.values(StatutInscription);
  typeOptions = Object.values(TypeInscription);

  // Statistics
  totalInscriptions = 0;
  inscriptionsValidees = 0;
  inscriptionsEnAttente = 0;
  inscriptionsRejetees = 0;

  constructor(
    private fb: FormBuilder,
    private inscriptionService: InscriptionService,
    private campagneService: CampagneService,
    private authService: AuthService,
    private router: Router
  ) {
    this.initializeFilterForm();
  }

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      if (user) {
        this.loadData();
      }
    });
    this.setupFilters();
  }

  private initializeFilterForm(): void {
    this.filterForm = this.fb.group({
      searchTerm: [''],
      statut: [''],
      type: [''],
      annee: [''],
      campagneId: ['']
    });
  }

  private loadData(): void {
    if (!this.currentUser) return;
    
    this.loading = true;

    // Load campagnes for filter
    this.campagneService.getAllCampagnes().subscribe({
      next: (campagnes) => {
        this.campagnes = campagnes;
      }
    });

    // Load inscriptions based on user role
    const userId = this.currentUser.id;
    const role = this.currentUser.roles?.[0] || '';

    if (role === 'ROLE_DOCTORANT') {
      this.loadDoctorantInscriptions(userId);
    } else if (role === 'ROLE_DIRECTEUR') {
      this.loadDirecteurInscriptions(userId);
    } else if (role === 'ROLE_ADMIN') {
      this.loadAllInscriptions();
    }
  }

  private loadDoctorantInscriptions(doctorantId: number): void {
    this.inscriptionService.getInscriptionsDoctorant(doctorantId).subscribe({
      next: (inscriptions) => {
        this.processInscriptions(inscriptions);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading inscriptions:', error);
        this.loading = false;
      }
    });
  }

  private loadDirecteurInscriptions(directeurId: number): void {
    this.inscriptionService.getInscriptionsEnAttenteDirecteur(directeurId).subscribe({
      next: (inscriptions) => {
        this.processInscriptions(inscriptions);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading inscriptions:', error);
        this.loading = false;
      }
    });
  }

  private loadAllInscriptions(): void {
    // For admin, we would need an endpoint to get all inscriptions
    // For now, we'll use a placeholder
    this.loading = false;
  }

  private processInscriptions(inscriptions: InscriptionResponse[]): void {
    this.dataSource.data = inscriptions;
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
    
    // Calculate statistics
    this.totalInscriptions = inscriptions.length;
    this.inscriptionsValidees = inscriptions.filter(i => i.statut === StatutInscription.VALIDE).length;
    this.inscriptionsEnAttente = inscriptions.filter(i => 
      [StatutInscription.SOUMIS, StatutInscription.EN_ATTENTE_DIRECTEUR, StatutInscription.EN_ATTENTE_ADMIN].includes(i.statut)
    ).length;
    this.inscriptionsRejetees = inscriptions.filter(i => i.statut === StatutInscription.REJETE).length;

    // Setup custom filter
    this.dataSource.filterPredicate = this.createFilter();
  }

  private setupFilters(): void {
    this.filterForm.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged()
      )
      .subscribe(() => {
        this.applyFilters();
      });
  }

  private createFilter(): (data: InscriptionResponse, filter: string) => boolean {
    return (data: InscriptionResponse, filter: string): boolean => {
      const filters = JSON.parse(filter);
      
      // Search term
      if (filters.searchTerm) {
        const searchTerm = filters.searchTerm.toLowerCase();
        const matchesSearch = 
          data.sujetThese.toLowerCase().includes(searchTerm) ||
          data.infosDoctorant?.cin?.toLowerCase().includes(searchTerm) ||
          data.infosThese?.titreThese?.toLowerCase().includes(searchTerm);
        
        if (!matchesSearch) return false;
      }

      // Statut filter
      if (filters.statut && data.statut !== filters.statut) {
        return false;
      }

      // Type filter
      if (filters.type && data.type !== filters.type) {
        return false;
      }

      // Année filter
      if (filters.annee && data.anneeInscription !== parseInt(filters.annee)) {
        return false;
      }

      // Campagne filter - use campagneId directly
      if (filters.campagneId) {
        // Skip campagne filter if we don't have campagne info
        return true;
      }

      return true;
    };
  }

  private applyFilters(): void {
    const filterValue = JSON.stringify(this.filterForm.value);
    this.dataSource.filter = filterValue;

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  clearFilters(): void {
    this.filterForm.reset();
  }

  onView(inscription: InscriptionResponse): void {
    this.router.navigate(['/inscription', inscription.id]);
  }

  onEdit(inscription: InscriptionResponse): void {
    this.router.navigate(['/inscription', inscription.id, 'edit']);
  }

  onDelete(inscription: InscriptionResponse): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette inscription ?')) {
      // Delete logic here
      console.log('Delete inscription:', inscription.id);
    }
  }

  canEdit(inscription: InscriptionResponse): boolean {
    const role = this.currentUser?.roles?.[0] || '';
    return inscription.statut === StatutInscription.BROUILLON && 
           role === 'ROLE_DOCTORANT';
  }

  canDelete(inscription: InscriptionResponse): boolean {
    const role = this.currentUser?.roles?.[0] || '';
    return inscription.statut === StatutInscription.BROUILLON && 
           role === 'ROLE_DOCTORANT';
  }

  getStatutLabel(statut: StatutInscription): string {
    return getStatutLabel(statut);
  }

  getStatutColor(statut: StatutInscription): string {
    return getStatutColor(statut);
  }

  getTypeLabel(type: TypeInscription): string {
    return getTypeInscriptionLabel(type);
  }

  getAvailableYears(): number[] {
    const currentYear = new Date().getFullYear();
    const years = [];
    for (let i = 0; i < 5; i++) {
      years.push(currentYear - i);
    }
    return years;
  }

  exportToExcel(): void {
    // Export logic here
    console.log('Export to Excel');
  }

  exportToPDF(): void {
    // Export logic here
    console.log('Export to PDF');
  }
}
