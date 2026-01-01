import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { UserManagementService, UserResponse, Page } from '../../../core/services/user-management.service';

interface FilterCriteria {
    searchTerm: string;
    role: string;
    status: 'ALL' | 'ACTIVE' | 'DISABLED';
}

@Component({
    selector: 'app-user-list',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './user-list.html',
    styleUrls: ['./user-list.scss']
})
export class UserList implements OnInit {
    users: UserResponse[] = [];
    filteredUsers: UserResponse[] = [];
    isLoading = true;
    errorMessage = '';
    successMessage = '';

    // Pagination
    currentPage = 0;
    pageSize = 20;
    totalPages = 0;
    totalElements = 0;

    // Filters
    filters: FilterCriteria = {
        searchTerm: '',
        role: 'ALL',
        status: 'ALL'
    };

    private searchSubject = new Subject<string>();

    // Available roles for filter
    availableRoles = ['ALL', 'ADMIN', 'DIRECTEUR', 'DOCTORANT', 'PED'];

    constructor(
        private userManagementService: UserManagementService,
        private router: Router
    ) {
        // Setup search debouncing
        this.searchSubject.pipe(
            debounceTime(300),
            distinctUntilChanged()
        ).subscribe(searchTerm => {
            this.filters.searchTerm = searchTerm;
            this.applyFilters();
        });
    }

    ngOnInit(): void {
        this.loadUsers();
    }

    loadUsers(): void {
        this.isLoading = true;
        this.errorMessage = '';

        this.userManagementService.getAllUsers().subscribe({
            next: (data) => {
                this.users = data;
                this.applyFilters();
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading users:', error);
                this.errorMessage = 'Impossible de charger les utilisateurs';
                this.isLoading = false;
            }
        });
    }

    onSearchChange(searchTerm: string): void {
        this.searchSubject.next(searchTerm);
    }

    onFilterChange(): void {
        this.currentPage = 0;
        this.applyFilters();
    }

    applyFilters(): void {
        let filtered = [...this.users];

        // Search filter
        if (this.filters.searchTerm) {
            const term = this.filters.searchTerm.toLowerCase();
            filtered = filtered.filter(u =>
                u.email.toLowerCase().includes(term) ||
                u.firstName.toLowerCase().includes(term) ||
                u.lastName.toLowerCase().includes(term) ||
                (u.tel && u.tel.includes(term))
            );
        }

        // Role filter
        if (this.filters.role && this.filters.role !== 'ALL') {
            filtered = filtered.filter(u =>
                this.userManagementService.hasRole(u, this.filters.role)
            );
        }

        // Status filter
        if (this.filters.status && this.filters.status !== 'ALL') {
            if (this.filters.status === 'ACTIVE') {
                filtered = filtered.filter(u => this.userManagementService.isUserEnabled(u));
            } else if (this.filters.status === 'DISABLED') {
                filtered = filtered.filter(u => !this.userManagementService.isUserEnabled(u));
            }
        }

        this.filteredUsers = filtered;
        this.calculatePagination();
    }

    calculatePagination(): void {
        this.totalElements = this.filteredUsers.length;
        this.totalPages = Math.ceil(this.totalElements / this.pageSize);
        if (this.currentPage >= this.totalPages && this.totalPages > 0) {
            this.currentPage = 0;
        }
    }

    getPaginatedUsers(): UserResponse[] {
        const startIndex = this.currentPage * this.pageSize;
        const endIndex = startIndex + this.pageSize;
        return this.filteredUsers.slice(startIndex, endIndex);
    }

    onPageChange(page: number): void {
        if (page >= 0 && page < this.totalPages) {
            this.currentPage = page;
        }
    }

    clearFilters(): void {
        this.filters = {
            searchTerm: '',
            role: 'ALL',
            status: 'ALL'
        };
        this.applyFilters();
    }

    // Navigation
    viewUser(userId: number): void {
        this.router.navigate(['/admin/users', userId]);
    }

    editUser(userId: number): void {
        this.router.navigate(['/admin/users', userId, 'edit']);
    }

    // Actions
    disableUser(user: UserResponse): void {
        const reason = prompt('Raison de la désactivation :');
        if (!reason) return;

        this.userManagementService.disableUser(user.id, reason).subscribe({
            next: () => {
                this.successMessage = `Utilisateur ${this.userManagementService.getFullName(user)} désactivé avec succès`;
                this.loadUsers();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (error) => {
                console.error('Error disabling user:', error);
                this.errorMessage = 'Erreur lors de la désactivation de l\'utilisateur';
                setTimeout(() => this.errorMessage = '', 3000);
            }
        });
    }

    enableUser(user: UserResponse): void {
        if (!confirm(`Êtes-vous sûr de vouloir réactiver ${this.userManagementService.getFullName(user)} ?`)) {
            return;
        }

        this.userManagementService.enableUser(user.id).subscribe({
            next: () => {
                this.successMessage = `Utilisateur ${this.userManagementService.getFullName(user)} réactivé avec succès`;
                this.loadUsers();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (error) => {
                console.error('Error enabling user:', error);
                this.errorMessage = 'Erreur lors de la réactivation de l\'utilisateur';
                setTimeout(() => this.errorMessage = '', 3000);
            }
        });
    }

    deleteUser(user: UserResponse): void {
        if (!confirm(`⚠️ ATTENTION : Êtes-vous sûr de vouloir supprimer définitivement ${this.userManagementService.getFullName(user)} ?\n\nCette action est irréversible !`)) {
            return;
        }

        this.userManagementService.deleteUser(user.id).subscribe({
            next: () => {
                this.successMessage = `Utilisateur supprimé avec succès`;
                this.loadUsers();
                setTimeout(() => this.successMessage = '', 3000);
            },
            error: (error) => {
                console.error('Error deleting user:', error);
                this.errorMessage = 'Erreur lors de la suppression de l\'utilisateur';
                setTimeout(() => this.errorMessage = '', 3000);
            }
        });
    }

    // Helper methods
    getFullName(user: UserResponse): string {
        return this.userManagementService.getFullName(user);
    }

    getUserStatus(user: UserResponse): string {
        return this.userManagementService.getUserStatus(user);
    }

    getStatusColor(user: UserResponse): string {
        return this.userManagementService.getStatusColor(user);
    }

    getUserRoles(user: UserResponse): string {
        return this.userManagementService.getUserRoles(user).join(', ');
    }

    formatDate(date: Date): string {
        return new Date(date).toLocaleDateString('fr-FR', {
            year: 'numeric',
            month: 'short',
            day: 'numeric'
        });
    }
}
