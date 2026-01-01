import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { Observable } from 'rxjs';
import { filter, map, distinctUntilChanged } from 'rxjs/operators';

export interface Breadcrumb {
    label: string;
    url: string;
}

/**
 * Breadcrumb Component
 * Automatic breadcrumb navigation based on Angular routing
 * 
 * Add 'breadcrumb' data to your routes:
 * @example
 * {
 *   path: 'admin',
 *   data: { breadcrumb: 'Administration' },
 *   children: [
 *     { path: 'users', data: { breadcrumb: 'Utilisateurs' }, component: UserList }
 *   ]
 * }
 * 
 * Then add component to template:
 * <app-breadcrumb></app-breadcrumb>
 */
@Component({
    selector: 'app-breadcrumb',
    standalone: true,
    imports: [CommonModule, RouterModule],
    template: `
    <nav class="breadcrumb" aria-label="Breadcrumb" *ngIf="breadcrumbs$ | async as breadcrumbs">
      <ol class="breadcrumb-list">
        <li class="breadcrumb-item">
          <a routerLink="/" class="breadcrumb-link">
            <i class="icon-home"></i>
            <span>Accueil</span>
          </a>
        </li>
        <li 
          *ngFor="let breadcrumb of breadcrumbs; let last = last" 
          class="breadcrumb-item"
          [class.active]="last">
          <i class="breadcrumb-separator icon-chevron-right"></i>
          <a 
            *ngIf="!last" 
            [routerLink]="breadcrumb.url" 
            class="breadcrumb-link">
            {{ breadcrumb.label }}
          </a>
          <span *ngIf="last" class="breadcrumb-current">
            {{ breadcrumb.label }}
          </span>
        </li>
      </ol>
    </nav>
  `,
    styles: [`
    .breadcrumb {
      padding: 1rem 1.5rem;
      background-color: #f9fafb;
      border-bottom: 1px solid #e5e7eb;
    }

    .breadcrumb-list {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 0.5rem;
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .breadcrumb-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.875rem;
    }

    .breadcrumb-separator {
      color: #9ca3af;
      font-size: 0.75rem;
    }

    .breadcrumb-link {
      display: flex;
      align-items: center;
      gap: 0.375rem;
      color: #3b82f6;
      text-decoration: none;
      transition: color 0.2s;
    }

    .breadcrumb-link:hover {
      color: #2563eb;
      text-decoration: underline;
    }

    .breadcrumb-link i {
      font-size: 1rem;
    }

    .breadcrumb-current {
      color: #6b7280;
      font-weight: 500;
    }

    .breadcrumb-item.active {
      color: #111827;
    }

    @media (max-width: 640px) {
      .breadcrumb {
        padding: 0.75rem 1rem;
      }

      .breadcrumb-list {
        font-size: 0.8125rem;
      }
    }
  `]
})
export class BreadcrumbComponent implements OnInit {
    breadcrumbs$!: Observable<Breadcrumb[]>;

    constructor(
        private router: Router,
        private activatedRoute: ActivatedRoute
    ) { }

    ngOnInit(): void {
        this.breadcrumbs$ = this.router.events.pipe(
            filter(event => event instanceof NavigationEnd),
            distinctUntilChanged(),
            map(() => this.buildBreadcrumbs(this.activatedRoute.root))
        );
    }

    private buildBreadcrumbs(
        route: ActivatedRoute,
        url: string = '',
        breadcrumbs: Breadcrumb[] = []
    ): Breadcrumb[] {
        // Get children routes
        const children: ActivatedRoute[] = route.children;

        // If no children, return breadcrumbs
        if (children.length === 0) {
            return breadcrumbs;
        }

        // Iterate over children
        for (const child of children) {
            // Get route URL segments
            const routeURL: string = child.snapshot.url
                .map(segment => segment.path)
                .join('/');

            // Append route URL to current URL
            if (routeURL !== '') {
                url += `/${routeURL}`;
            }

            // Get breadcrumb label from route data
            const label = child.snapshot.data['breadcrumb'];

            // Add breadcrumb if label exists
            if (label) {
                breadcrumbs.push({ label, url });
            }

            // Recursive call
            return this.buildBreadcrumbs(child, url, breadcrumbs);
        }

        return breadcrumbs;
    }
}
