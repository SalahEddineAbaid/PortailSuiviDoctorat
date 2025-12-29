import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-soutenance-container',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="soutenance-container">
      <nav class="soutenance-nav">
        <h2>Gestion des Soutenances</h2>
        <ul class="nav-links">
          <li>
            <a routerLink="dashboard" 
               routerLinkActive="active"
               class="nav-link">
              <i class="icon-dashboard"></i>
              Tableau de bord
            </a>
          </li>
          <li>
            <a routerLink="nouvelle" 
               routerLinkActive="active"
               class="nav-link">
              <i class="icon-plus"></i>
              Nouvelle demande
            </a>
          </li>
          <li>
            <a routerLink="liste" 
               routerLinkActive="active"
               class="nav-link">
              <i class="icon-list"></i>
              Mes demandes
            </a>
          </li>
        </ul>
      </nav>
      
      <main class="soutenance-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styleUrls: ['./soutenance-container.scss']
})
export class SoutenanceContainer {
  constructor() {}
}