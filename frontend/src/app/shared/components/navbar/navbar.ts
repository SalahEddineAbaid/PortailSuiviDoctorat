import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService, UserInfo } from '../../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class Navbar implements OnInit {
  currentUser: UserInfo | null = null;
  showUserMenu = false;
  isLoading = true;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.authService.currentUser$.subscribe(user => {
      this.currentUser = user;
      this.isLoading = false;
      console.log('👤 Utilisateur actuel:', user);
    });
  }

  toggleUserMenu(): void {
    this.showUserMenu = !this.showUserMenu;
  }

  getUserRoleLabel(): string {
    if (!this.currentUser || !this.currentUser.roles || this.currentUser.roles.length === 0) {
      return 'Utilisateur';
    }

    const role = this.currentUser.roles[0].name;

    switch (role) {
      case 'ROLE_DOCTORANT':
        return 'Doctorant';
      case 'ROLE_DIRECTEUR':
        return 'Directeur de thèse';
      case 'ROLE_ADMIN':
        return 'Administrateur';
      default:
        return 'Utilisateur';
    }
  }

  goToProfile(): void {
    this.showUserMenu = false;
    this.router.navigate(['/profile']);
  }

  goToSettings(): void {
    this.showUserMenu = false;
    this.router.navigate(['/settings']);
  }

  logout(): void {
    this.showUserMenu = false;
    this.authService.logout();
  }
}