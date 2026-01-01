import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';

export interface MenuItem {
    label: string;
    icon: string;
    route?: string;
    children?: MenuItem[];
    roles?: string[];  // Allowed roles
    badge?: number;    // Badge count
    external?: boolean; // External link
}

/**
 * Sidebar Component
 * Hierarchical navigation menu with role-based filtering
 * 
 * @example
 * <app-sidebar 
 *   [menuItems]="menuItems"
 *   [collapsed]="isSidebarCollapsed"
 *   (collapsedChange)="isSidebarCollapsed = $event">
 * </app-sidebar>
 */
@Component({
    selector: 'app-sidebar',
    standalone: true,
    imports: [CommonModule, RouterModule],
    template: `
    <aside class="sidebar" [class.collapsed]="collapsed">
      <div class="sidebar-content">
        <!-- Menu Items -->
        <nav class="sidebar-nav">
          <ng-container *ngFor="let item of visibleMenuItems">
            <!-- Parent Item with Children -->
            <div *ngIf="item.children && item.children.length > 0" class="menu-group">
              <button 
                class="menu-item menu-parent"
                (click)="toggleGroup(item)"
                [class.active]="isGroupActive(item)"
                [class.expanded]="isGroupExpanded(item)"
                type="button">
                <i class="menu-icon icon-{{item.icon}}"></i>
                <span class="menu-label">{{ item.label }}</span>
                <i class="menu-arrow icon-chevron-down"></i>
              </button>
              
              <div class="menu-children" *ngIf="isGroupExpanded(item)">
                <a 
                  *ngFor="let child of getVisibleChildren(item)"
                  [routerLink]="child.route"
                  routerLinkActive="active"
                  class="menu-item menu-child">
                  <i class="menu-icon icon-{{child.icon}}"></i>
                  <span class="menu-label">{{ child.label }}</span>
                  <span *ngIf="child.badge" class="menu-badge">{{ child.badge }}</span>
                </a>
              </div>
            </div>

            <!-- Single Item without Children -->
            <a 
              *ngIf="!item.children || item.children.length === 0"
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{exact: item.route === '/'}"
              class="menu-item"
              [attr.target]="item.external ? '_blank' : null">
              <i class="menu-icon icon-{{item.icon}}"></i>
              <span class="menu-label">{{ item.label }}</span>
              <span *ngIf="item.badge" class="menu-badge">{{ item.badge }}</span>
            </a>
          </ng-container>
        </nav>
      </div>

      <!-- Collapse Toggle Button -->
      <button 
        class="collapse-toggle"
        (click)="toggleCollapse()"
        type="button"
        [attr.aria-label]="collapsed ? 'Expand sidebar' : 'Collapse sidebar'">
        <i class="icon-chevron-left" [class.rotate]="collapsed"></i>
      </button>
    </aside>
  `,
    styles: [`
    .sidebar {
      width: 260px;
      height: 100vh;
      background-color: #1f2937;
      color: white;
      display: flex;
      flex-direction: column;
      position: fixed;
      left: 0;
      top: 64px;
      transition: width 0.3s ease;
      z-index: 999;
      overflow: hidden;
    }

    .sidebar.collapsed {
      width: 64px;
    }

    .sidebar-content {
      flex: 1;
      overflow-y: auto;
      overflow-x: hidden;
      padding: 1rem 0;
    }

    .sidebar-nav {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .menu-group {
      margin-bottom: 0.25rem;
    }

    .menu-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 1rem;
      color: #d1d5db;
      text-decoration: none;
      background: none;
      border: none;
      width: 100%;
      cursor: pointer;
      transition: all 0.2s;
      font-size: 0.875rem;
      position: relative;
    }

    .menu-item:hover {
      background-color: #374151;
      color: white;
    }

    .menu-item.active {
      background-color: #3b82f6;
      color: white;
    }

    .menu-parent {
      justify-content: space-between;
      font-weight: 500;
    }

    .menu-parent.expanded .menu-arrow {
      transform: rotate(180deg);
    }

    .menu-child {
      padding-left: 3rem;
      font-size: 0.8125rem;
    }

    .menu-icon {
      font-size: 1.25rem;
      flex-shrink: 0;
    }

    .menu-label {
      flex: 1;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: left;
    }

    .sidebar.collapsed .menu-label,
    .sidebar.collapsed .menu-arrow,
    .sidebar.collapsed .menu-badge {
      display: none;
    }

    .menu-arrow {
      font-size: 1rem;
      transition: transform 0.2s;
    }

    .menu-badge {
      background-color: #ef4444;
      color: white;
      font-size: 0.75rem;
      font-weight: 600;
      padding: 0.125rem 0.5rem;
      border-radius: 9999px;
      min-width: 1.25rem;
      text-align: center;
    }

    .menu-children {
      overflow: hidden;
      animation: slideDown 0.2s ease-out;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        max-height: 0;
      }
      to {
        opacity: 1;
        max-height: 500px;
      }
    }

    .collapse-toggle {
      background-color: #374151;
      border: none;
      color: white;
      padding: 0.75rem;
      cursor: pointer;
      transition: background-color 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .collapse-toggle:hover {
      background-color: #4b5563;
    }

    .collapse-toggle .icon-chevron-left {
      transition: transform 0.3s;
    }

    .collapse-toggle .icon-chevron-left.rotate {
      transform: rotate(180deg);
    }

    @media (max-width: 768px) {
      .sidebar {
        transform: translateX(-100%);
        transition: transform 0.3s ease;
      }

      .sidebar.open {
        transform: translateX(0);
      }
    }
  `]
})
export class SidebarComponent implements OnInit {
    @Input() menuItems: MenuItem[] = [];
    @Input() collapsed: boolean = false;

    @Output() collapsedChange = new EventEmitter<boolean>();

    visibleMenuItems: MenuItem[] = [];
    expandedGroups: Set<string> = new Set();
    currentRoute: string = '';

    constructor(
        private authService: AuthService,
        private router: Router
    ) {
        // Track current route
        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe((event: NavigationEnd) => {
                this.currentRoute = event.url;
            });
    }

    ngOnInit(): void {
        this.filterMenuItemsByRole();
        this.currentRoute = this.router.url;
    }

    filterMenuItemsByRole(): void {
        this.visibleMenuItems = this.menuItems.filter(item => this.hasAccess(item));
    }

    hasAccess(item: MenuItem): boolean {
        if (!item.roles || item.roles.length === 0) return true;
        return item.roles.some(role => this.authService.hasRole(role));
    }

    getVisibleChildren(item: MenuItem): MenuItem[] {
        if (!item.children) return [];
        return item.children.filter(child => this.hasAccess(child));
    }

    toggleGroup(item: MenuItem): void {
        const key = item.label;
        if (this.expandedGroups.has(key)) {
            this.expandedGroups.delete(key);
        } else {
            this.expandedGroups.add(key);
        }
    }

    isGroupExpanded(item: MenuItem): boolean {
        return this.expandedGroups.has(item.label);
    }

    isGroupActive(item: MenuItem): boolean {
        if (!item.children) return false;
        return item.children.some(child =>
            child.route && this.currentRoute.startsWith(child.route)
        );
    }

    toggleCollapse(): void {
        this.collapsed = !this.collapsed;
        this.collapsedChange.emit(this.collapsed);
    }
}
