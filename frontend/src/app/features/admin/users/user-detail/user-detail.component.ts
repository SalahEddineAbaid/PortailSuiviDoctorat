import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UserManagementService, UserResponse } from '../../../../core/services/user-management.service';

@Component({
    selector: 'app-user-detail',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './user-detail.component.html',
    styleUrls: ['./user-detail.component.css']
})
export class UserDetailComponent implements OnInit {
    user: UserResponse | null = null;
    isLoading = true;
    errorMessage = '';
    successMessage = '';

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private userService: UserManagementService
    ) { }

    ngOnInit(): void {
        const userId = this.route.snapshot.paramMap.get('id');
        if (userId) {
            this.loadUser(parseInt(userId, 10));
        } else {
            this.errorMessage = 'ID utilisateur invalide';
            this.isLoading = false;
        }
    }

    loadUser(id: number): void {
        this.isLoading = true;
        this.userService.getUserById(id).subscribe({
            next: (user) => {
                this.user = user;
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading user:', error);
                this.errorMessage = 'Impossible de charger les informations de l\'utilisateur';
                this.isLoading = false;
            }
        });
    }

    getFullName(): string {
        if (!this.user) return '';
        return this.userService.getFullName(this.user);
    }

    getUserStatus(): string {
        if (!this.user) return '';
        return this.userService.getUserStatus(this.user);
    }

    getStatusColor(): string {
        if (!this.user) return '';
        return this.userService.getStatusColor(this.user);
    }

    isUserEnabled(): boolean {
        return this.user ? this.userService.isUserEnabled(this.user) : false;
    }

    editUser(): void {
        if (this.user) {
            this.router.navigate(['/admin/users', this.user.id, 'edit']);
        }
    }

    toggleUserStatus(): void {
        if (!this.user) return;

        if (this.isUserEnabled()) {
            const reason = prompt('Raison de la désactivation :');
            if (!reason) return;

            this.userService.disableUser(this.user.id, reason).subscribe({
                next: () => {
                    this.successMessage = 'Utilisateur désactivé avec succès';
                    this.loadUser(this.user!.id);
                    setTimeout(() => this.successMessage = '', 3000);
                },
                error: () => {
                    this.errorMessage = 'Erreur lors de la désactivation';
                    setTimeout(() => this.errorMessage = '', 3000);
                }
            });
        } else {
            this.userService.enableUser(this.user.id).subscribe({
                next: () => {
                    this.successMessage = 'Utilisateur activé avec succès';
                    this.loadUser(this.user!.id);
                    setTimeout(() => this.successMessage = '', 3000);
                },
                error: () => {
                    this.errorMessage = 'Erreur lors de l\'activation';
                    setTimeout(() => this.errorMessage = '', 3000);
                }
            });
        }
    }

    deleteUser(): void {
        if (!this.user) return;

        if (confirm(`⚠️ Êtes-vous sûr de vouloir supprimer définitivement ${this.getFullName()} ?\n\nCette action est irréversible !`)) {
            this.userService.deleteUser(this.user.id).subscribe({
                next: () => {
                    this.router.navigate(['/admin/users']);
                },
                error: () => {
                    this.errorMessage = 'Erreur lors de la suppression';
                    setTimeout(() => this.errorMessage = '', 3000);
                }
            });
        }
    }

    goBack(): void {
        this.router.navigate(['/admin/users']);
    }
}
