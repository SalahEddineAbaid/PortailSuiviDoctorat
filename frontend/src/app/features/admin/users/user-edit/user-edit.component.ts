import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { UserManagementService, UserResponse } from '../../../../core/services/user-management.service';

@Component({
    selector: 'app-user-edit',
    standalone: true,
    imports: [CommonModule, RouterModule, ReactiveFormsModule],
    templateUrl: './user-edit.component.html',
    styleUrls: ['./user-edit.component.css']
})
export class UserEditComponent implements OnInit {
    user: UserResponse | null = null;
    userForm: FormGroup;
    isLoading = true;
    isSaving = false;
    errorMessage = '';
    successMessage = '';

    availableRoles = ['ADMIN', 'DIRECTEUR', 'DOCTORANT', 'PED'];

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private fb: FormBuilder,
        private userService: UserManagementService
    ) {
        this.userForm = this.fb.group({
            firstName: ['', [Validators.required, Validators.minLength(2)]],
            lastName: ['', [Validators.required, Validators.minLength(2)]],
            email: ['', [Validators.required, Validators.email]],
            tel: [''],
            roles: [[], Validators.required]
        });
    }

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
                this.userForm.patchValue({
                    firstName: user.FirstName || user.firstName || '',
                    lastName: user.LastName || user.lastName || '',
                    email: user.email,
                    tel: user.tel || user.phoneNumber || '',
                    roles: user.roles || []
                });
                this.isLoading = false;
            },
            error: (error) => {
                console.error('Error loading user:', error);
                this.errorMessage = 'Impossible de charger les informations de l\'utilisateur';
                this.isLoading = false;
            }
        });
    }

    toggleRole(role: string): void {
        const currentRoles = this.userForm.get('roles')?.value || [];
        const index = currentRoles.indexOf(role);

        if (index > -1) {
            currentRoles.splice(index, 1);
        } else {
            currentRoles.push(role);
        }

        this.userForm.patchValue({ roles: [...currentRoles] });
    }

    hasRole(role: string): boolean {
        const roles = this.userForm.get('roles')?.value || [];
        return roles.includes(role);
    }

    saveUser(): void {
        if (this.userForm.invalid || !this.user) {
            this.userForm.markAllAsTouched();
            return;
        }

        this.isSaving = true;
        const formData = this.userForm.value;

        this.userService.updateUser(this.user.id, {
            FirstName: formData.firstName,
            LastName: formData.lastName,
            email: formData.email,
            tel: formData.tel,
            roles: formData.roles
        } as any).subscribe({
            next: () => {
                this.successMessage = 'Utilisateur mis à jour avec succès';
                this.isSaving = false;
                setTimeout(() => {
                    this.router.navigate(['/admin/users', this.user!.id]);
                }, 1500);
            },
            error: (error) => {
                console.error('Error updating user:', error);
                this.errorMessage = error.error?.message || 'Erreur lors de la mise à jour';
                this.isSaving = false;
                setTimeout(() => this.errorMessage = '', 5000);
            }
        });
    }

    cancel(): void {
        if (this.user) {
            this.router.navigate(['/admin/users', this.user.id]);
        } else {
            this.router.navigate(['/admin/users']);
        }
    }

    goBack(): void {
        this.router.navigate(['/admin/users']);
    }
}
