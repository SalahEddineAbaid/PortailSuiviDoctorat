import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { BreadcrumbComponent } from './breadcrumb.component';
import { BreadcrumbItem } from '../../../core/models/dashboard.model';

describe('BreadcrumbComponent', () => {
  let component: BreadcrumbComponent;
  let fixture: ComponentFixture<BreadcrumbComponent>;
  let router: Router;

  const mockBreadcrumbs: BreadcrumbItem[] = [
    { label: 'Accueil', route: '/' },
    { label: 'Dashboard', route: '/dashboard' },
    { label: 'Inscription', route: '/inscription' },
    { label: 'Nouvelle inscription' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BreadcrumbComponent, RouterTestingModule]
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have empty breadcrumbs by default', () => {
      expect(component.breadcrumbs).toEqual([]);
    });
  });

  describe('Breadcrumb Display', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
      fixture.detectChanges();
    });

    it('should display all breadcrumb items', () => {
      const breadcrumbItems = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      expect(breadcrumbItems.length).toBe(mockBreadcrumbs.length);
    });

    it('should display breadcrumb labels', () => {
      const breadcrumbItems = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      expect(breadcrumbItems[0].textContent.trim()).toContain('Accueil');
      expect(breadcrumbItems[1].textContent.trim()).toContain('Dashboard');
      expect(breadcrumbItems[2].textContent.trim()).toContain('Inscription');
      expect(breadcrumbItems[3].textContent.trim()).toContain('Nouvelle inscription');
    });

    it('should show links for items with routes', () => {
      const links = fixture.nativeElement.querySelectorAll('.breadcrumb-link');
      expect(links.length).toBe(3); // First 3 items have routes
    });

    it('should show current item without link', () => {
      const currentItem = fixture.nativeElement.querySelector('.breadcrumb-current');
      expect(currentItem).toBeTruthy();
      expect(currentItem.textContent.trim()).toBe('Nouvelle inscription');
    });

    it('should show separators between items', () => {
      const separators = fixture.nativeElement.querySelectorAll('.breadcrumb-separator');
      expect(separators.length).toBe(3); // n-1 separators for n items
    });
  });

  describe('Navigation', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
      fixture.detectChanges();
    });

    it('should navigate when clicking on breadcrumb link', () => {
      spyOn(router, 'navigate');
      
      const firstLink = fixture.nativeElement.querySelector('.breadcrumb-link');
      firstLink.click();
      
      expect(router.navigate).toHaveBeenCalledWith(['/']);
    });

    it('should call onBreadcrumbClick method', () => {
      spyOn(component, 'onBreadcrumbClick');
      
      const firstLink = fixture.nativeElement.querySelector('.breadcrumb-link');
      firstLink.click();
      
      expect(component.onBreadcrumbClick).toHaveBeenCalledWith(mockBreadcrumbs[0]);
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
      fixture.detectChanges();
    });

    it('should have proper ARIA attributes', () => {
      const nav = fixture.nativeElement.querySelector('nav');
      expect(nav.getAttribute('aria-label')).toBe('Fil d\'ariane');
    });

    it('should have proper role for breadcrumb list', () => {
      const breadcrumbList = fixture.nativeElement.querySelector('.breadcrumb');
      expect(breadcrumbList.getAttribute('role')).toBe('list');
    });

    it('should have proper role for breadcrumb items', () => {
      const breadcrumbItems = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      breadcrumbItems.forEach(item => {
        expect(item.getAttribute('role')).toBe('listitem');
      });
    });

    it('should mark current page with aria-current', () => {
      const currentItem = fixture.nativeElement.querySelector('.breadcrumb-current');
      expect(currentItem.getAttribute('aria-current')).toBe('page');
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty breadcrumbs array', () => {
      component.breadcrumbs = [];
      fixture.detectChanges();
      
      const breadcrumbItems = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      expect(breadcrumbItems.length).toBe(0);
    });

    it('should handle single breadcrumb item', () => {
      component.breadcrumbs = [{ label: 'Home' }];
      fixture.detectChanges();
      
      const breadcrumbItems = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      const separators = fixture.nativeElement.querySelectorAll('.breadcrumb-separator');
      
      expect(breadcrumbItems.length).toBe(1);
      expect(separators.length).toBe(0);
    });

    it('should handle breadcrumb without route', () => {
      const breadcrumbsWithoutRoute: BreadcrumbItem[] = [
        { label: 'Home', route: '/' },
        { label: 'Current Page' }
      ];
      
      component.breadcrumbs = breadcrumbsWithoutRoute;
      fixture.detectChanges();
      
      const links = fixture.nativeElement.querySelectorAll('.breadcrumb-link');
      const currentItem = fixture.nativeElement.querySelector('.breadcrumb-current');
      
      expect(links.length).toBe(1);
      expect(currentItem).toBeTruthy();
    });
  });

  describe('Responsive Behavior', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
      fixture.detectChanges();
    });

    it('should have responsive classes', () => {
      const breadcrumbContainer = fixture.nativeElement.querySelector('.breadcrumb-container');
      expect(breadcrumbContainer).toHaveClass('responsive');
    });

    it('should handle long breadcrumb labels', () => {
      const longBreadcrumbs: BreadcrumbItem[] = [
        { label: 'Very Long Breadcrumb Label That Might Overflow', route: '/long' },
        { label: 'Another Very Long Label That Should Be Handled Properly' }
      ];
      
      component.breadcrumbs = longBreadcrumbs;
      fixture.detectChanges();
      
      const breadcrumbItems = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      expect(breadcrumbItems.length).toBe(2);
    });
  });
});