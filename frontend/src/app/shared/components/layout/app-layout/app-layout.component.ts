import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NavbarComponent } from '../../navigation/navbar/navbar.component';
import { SidebarComponent, MenuItem } from '../../navigation/sidebar/sidebar.component';
import { BreadcrumbComponent } from '../../navigation/breadcrumb/breadcrumb.component';

/**
 * App Layout Component
 * Main responsive layout with navbar, sidebar, and content area
 * 
 * @example
 * <app-layout [menuItems]="menuItems">
 *   <router-outlet></router-outlet>
 * </app-layout>
 */
@Component({
    selector: 'app-layout',
    standalone: true,
    imports: [CommonModule, RouterModule, NavbarComponent, SidebarComponent, BreadcrumbComponent],
    template: `
    <!-- Skip Links for Accessibility -->
    <a href="#main-content" class="skip-link">Aller au contenu principal</a>
    <a href="#navigation" class="skip-link">Aller à la navigation</a>

    <div class="app-layout" [class.sidebar-collapsed]="sidebarCollapsed" [class.mobile-sidebar-open]="mobileSidebarOpen">
      <!-- Navbar -->
      <app-navbar
        [appName]="appName"
        [logoSrc]="logoSrc"
        [showNotifications]="showNotifications"
        [unreadCount]="unreadCount"
        (menuToggle)="toggleMobileSidebar()">
      </app-navbar>

      <!-- Sidebar -->
      <aside id="navigation" class="sidebar-wrapper" [class.mobile-open]="mobileSidebarOpen">
        <app-sidebar
          [menuItems]="menuItems"
          [collapsed]="sidebarCollapsed"
          (collapsedChange)="onSidebarCollapsedChange($event)">
        </app-sidebar>
        
        <!-- Mobile Overlay -->
        <div 
          *ngIf="mobileSidebarOpen"
          class="mobile-overlay"
          (click)="closeMobileSidebar()"
          aria-hidden="true">
        </div>
      </aside>

      <!-- Main Content -->
      <main id="main-content" class="main-content" role="main">
        <!-- Breadcrumb -->
        <app-breadcrumb *ngIf="showBreadcrumb"></app-breadcrumb>
        
        <!-- Content Area -->
        <div class="content-container" [class.no-padding]="noPadding">
          <ng-content></ng-content>
        </div>
      </main>
    </div>
  `,
    styles: [`
    /* Skip Links */
    .skip-link {
      position: absolute;
      top: -40px;
      left: 0;
      background: #3b82f6;
      color: white;
      padding: 0.5rem 1rem;
      text-decoration: none;
      z-index: 10000;
      border-radius: 0 0 0.25rem 0;
    }

    .skip-link:focus {
      top: 0;
    }

    /* Layout Structure */
    .app-layout {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
      background-color: #f3f4f6;
    }

    /* Navbar is at top (sticky) */
    app-navbar {
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    /* Sidebar */
    .sidebar-wrapper {
      position: fixed;
      left: 0;
      top: 64px; /* Navbar height */
      bottom: 0;
      z-index: 999;
      transition: transform 0.3s ease;
    }

    /* Main Content */
    .main-content {
      margin-left: 260px; /* Sidebar width */
      margin-top: 0;
      min-height: calc(100vh - 64px);
      transition: margin-left 0.3s ease;
      display: flex;
      flex-direction: column;
    }

    .app-layout.sidebar-collapsed .main-content {
      margin-left: 64px;
    }

    /* Breadcrumb */
    app-breadcrumb {
      flex-shrink: 0;
    }

    /* Content Container */
    .content-container {
      flex: 1;
      padding: 1.5rem;
    }

    .content-container.no-padding {
      padding: 0;
    }

    /* Mobile Overlay */
    .mobile-overlay {
      display: none;
    }

    /* Responsive - Tablet */
    @media (max-width: 1024px) {
      .main-content {
        margin-left: 64px;
      }

      .app-layout.sidebar-collapsed .main-content {
        margin-left: 64px;
      }
    }

    /* Responsive - Mobile */
    @media (max-width: 768px) {
      .sidebar-wrapper {
        transform: translateX(-100%);
      }

      .sidebar-wrapper.mobile-open {
        transform: translateX(0);
      }

      .mobile-overlay {
        display: block;
        position: fixed;
        top: 64px;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: rgba(0, 0, 0, 0.5);
        z-index: 998;
      }

      .main-content {
        margin-left: 0;
      }

      .app-layout.sidebar-collapsed .main-content {
        margin-left: 0;
      }

      .content-container {
        padding: 1rem;
      }
    }

    /* Responsive - Small Mobile */
    @media (max-width: 480px) {
      .content-container {
        padding: 0.75rem;
      }
    }

    /* Print Styles */
    @media print {
      .skip-link,
      app-navbar,
      .sidebar-wrapper,
      app-breadcrumb {
        display: none !important;
      }

      .main-content {
        margin-left: 0 !important;
      }
    }
  `]
})
export class AppLayoutComponent implements OnInit {
    @Input() appName: string = 'Portail Suivi Doctorat';
    @Input() logoSrc?: string;
    @Input() menuItems: MenuItem[] = [];
    @Input() showNotifications: boolean = true;
    @Input() showBreadcrumb: boolean = true;
    @Input() unreadCount: number = 0;
    @Input() noPadding: boolean = false;

    sidebarCollapsed: boolean = false;
    mobileSidebarOpen: boolean = false;
    isMobile: boolean = false;

    ngOnInit(): void {
        this.checkMobile();
        window.addEventListener('resize', () => this.checkMobile());
    }

    checkMobile(): void {
        this.isMobile = window.innerWidth < 768;
        if (!this.isMobile) {
            this.mobileSidebarOpen = false;
        }
    }

    toggleMobileSidebar(): void {
        if (this.isMobile) {
            this.mobileSidebarOpen = !this.mobileSidebarOpen;
        } else {
            this.sidebarCollapsed = !this.sidebarCollapsed;
        }
    }

    closeMobileSidebar(): void {
        this.mobileSidebarOpen = false;
    }

    onSidebarCollapsedChange(collapsed: boolean): void {
        this.sidebarCollapsed = collapsed;
    }
}
