import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, BehaviorSubject, combineLatest } from 'rxjs';
import { map, startWith, debounceTime, distinctUntilChanged } from 'rxjs/operators';

import { UserService, CreateUserRequest, UpdateUserRequest } from '../../../core/services/user.service';
import { UserResponse } from '../../../core/models/user.model';
import { RoleName } from '../../../core/models/role.model';
import { CustomValidators } from '../../../core/validators/custom-validators';
import { UserFormComponent } from './user-form/user-form.component';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatSnackBarModule,
    MatPaginatorModule,
    MatSortModule,
    MatCardModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  displayedColumns: string[] = ['id', 'name', 'email', 'roles', 'enabled', 'actions'];
  
  users$ = new BehaviorSubject<UserResponse[]>([]);
  filteredUsers$: Observable<UserResponse[]>;
  
  searchForm: FormGroup;
  loading = false;
  
  // Pagination
  pageSize = 10;
  pageIndex = 0;
  totalUsers = 0;
  
  // Sorting
  sortField = 'id';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Filters
  roleFilter: RoleName | '' = '';
  statusFilter: 'all' | 'enabled' | 'disabled' = 'all';
  
  readonly roleNames = Object.values(RoleName);
  readonly roleLabels = {
    [RoleName.ADMIN]: 'Administrateur',
    [RoleName.DIRECTEUR]: 'Directeur de thèse',
    [RoleName.DOCTORANT]: 'Doctorant'
  };

  constructor(
    private userService: UserService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {
    this.searchForm = this.fb.group({
      searchTerm: ['']
    });
    
    this.filteredUsers$ = combineLatest([
      this.users$,
      this.searchForm.get('searchTerm')!.valueChanges.pipe(
        startWith(''),
        debounceTime(300),
        distinctUntilChanged()
      )
    ]).pipe(
      map(([users, searchTerm]) => this.filterUsers(users, searchTerm))
    );
  }

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => {
        this.users$.next(users);
        this.totalUsers = users.length;
        this.loading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des utilisateurs:', error);
        this.snackBar.open('Erreur lors du chargement des utilisateurs', 'Fermer', {
          duration: 3000
        });
        this.loading = false;
      }
    });
  }

  private filterUsers(users: UserResponse[], searchTerm: string): UserResponse[] {
    let filtered = users;

    // Filtre par terme de recherche
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(user =>
        user.FirstName.toLowerCase().includes(term) ||
        user.LastName.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term)
      );
    }

    // Filtre par rôle
    if (this.roleFilter) {
      filtered = filtered.filter(user =>
        user.roles.some(role => role.name === this.roleFilter)
      );
    }

    // Filtre par statut
    if (this.statusFilter !== 'all') {
      const enabled = this.statusFilter === 'enabled';
      filtered = filtered.filter(user => user.enabled === enabled);
    }

    // Tri
    filtered.sort((a, b) => {
      let aValue: any, bValue: any;
      
      switch (this.sortField) {
        case 'name':
          aValue = `${a.FirstName} ${a.LastName}`;
          bValue = `${b.FirstName} ${b.LastName}`;
          break;
        case 'email':
          aValue = a.email;
          bValue = b.email;
          break;
        default:
          aValue = a.id;
          bValue = b.id;
      }

      if (aValue < bValue) return this.sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    return filtered;
  }

  onSortChange(sort: Sort): void {
    this.sortField = sort.active;
    this.sortDirection = sort.direction as 'asc' | 'desc';
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
  }

  onRoleFilterChange(role: RoleName | ''): void {
    this.roleFilter = role;
  }

  onStatusFilterChange(status: 'all' | 'enabled' | 'disabled'): void {
    this.statusFilter = status;
  }

  openCreateUserDialog(): void {
    const dialogRef = this.dialog.open(UserFormComponent, {
      width: '600px',
      data: { mode: 'create' }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.createUser(result);
      }
    });
  }

  openEditUserDialog(user: UserResponse): void {
    const dialogRef = this.dialog.open(UserFormComponent, {
      width: '600px',
      data: { mode: 'edit', user }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.updateUser(user.id, result);
      }
    });
  }

  createUser(userData: CreateUserRequest): void {
    this.userService.createUser(userData).subscribe({
      next: (user) => {
        this.snackBar.open('Utilisateur créé avec succès', 'Fermer', {
          duration: 3000
        });
        this.loadUsers();
      },
      error: (error) => {
        console.error('Erreur lors de la création de l\'utilisateur:', error);
        this.snackBar.open('Erreur lors de la création de l\'utilisateur', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  updateUser(userId: number, userData: UpdateUserRequest): void {
    this.userService.updateUser(userId, userData).subscribe({
      next: (user) => {
        this.snackBar.open('Utilisateur mis à jour avec succès', 'Fermer', {
          duration: 3000
        });
        this.loadUsers();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour de l\'utilisateur:', error);
        this.snackBar.open('Erreur lors de la mise à jour de l\'utilisateur', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  toggleUserStatus(user: UserResponse): void {
    const newStatus = !user.enabled;
    const action = newStatus ? 'activé' : 'désactivé';
    
    this.userService.toggleUserStatus(user.id, newStatus).subscribe({
      next: () => {
        this.snackBar.open(`Utilisateur ${action} avec succès`, 'Fermer', {
          duration: 3000
        });
        this.loadUsers();
      },
      error: (error) => {
        console.error('Erreur lors du changement de statut:', error);
        this.snackBar.open('Erreur lors du changement de statut', 'Fermer', {
          duration: 3000
        });
      }
    });
  }

  deleteUser(user: UserResponse): void {
    if (confirm(`Êtes-vous sûr de vouloir supprimer l'utilisateur ${user.FirstName} ${user.LastName} ?`)) {
      this.userService.deleteUser(user.id).subscribe({
        next: () => {
          this.snackBar.open('Utilisateur supprimé avec succès', 'Fermer', {
            duration: 3000
          });
          this.loadUsers();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression de l\'utilisateur:', error);
          this.snackBar.open('Erreur lors de la suppression de l\'utilisateur', 'Fermer', {
            duration: 3000
          });
        }
      });
    }
  }

  getRoleLabel(roleName: RoleName): string {
    return this.roleLabels[roleName] || roleName;
  }

  getUserRoles(user: UserResponse): string {
    return user.roles.map(role => this.getRoleLabel(role.name)).join(', ');
  }
}