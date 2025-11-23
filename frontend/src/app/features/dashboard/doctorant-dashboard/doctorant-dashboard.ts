import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../core/models/user.model';
import { Navbar } from '../../../shared/components/navbar/navbar';
import { Sidebar, MenuItem } from '../../../shared/components/sidebar/sidebar';

@Component({
  selector: 'app-doctorant-dashboard',
  standalone: true,
  imports: [CommonModule, Navbar, Sidebar],
  templateUrl: './doctorant-dashboard.html',
  styleUrl: './doctorant-dashboard.scss'
})
export class DoctorantDashboard implements OnInit {
  user: UserResponse | null = null;
  isLoading = true;

  menuItems: MenuItem[] = [
    { icon: 'fas fa-home', label: 'Accueil', route: '/dashboard/doctorant' },
    { icon: 'fas fa-book', label: 'Ma thèse', route: '/dashboard/doctorant/these', badge: '1' },
    { icon: 'fas fa-file-alt', label: 'Publications', route: '/dashboard/doctorant/publications' },
    { icon: 'fas fa-calendar', label: 'Soutenances', route: '/dashboard/doctorant/soutenances' },
    { icon: 'fas fa-user', label: 'Mon profil', route: '/dashboard/doctorant/profil' }
  ];

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => {
        this.user = user;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('❌ Erreur:', error);
        this.isLoading = false;
      }
    });
  }

  get userInfo(): UserResponse | null {
    return this.user;
  }

  getWelcomeMessage(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  getUserName(): string {
    return this.user?.FirstName || 'Doctorant';
  }
}