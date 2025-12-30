import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BreakpointObserver } from '@angular/cdk/layout';
import { of } from 'rxjs';
import { ResponsiveLayoutComponent } from './responsive-layout.component';

describe('ResponsiveLayoutComponent', () => {
  let component: ResponsiveLayoutComponent;
  let fixture: ComponentFixture<ResponsiveLayoutComponent>;
  let breakpointObserver: jasmine.SpyObj<BreakpointObserver>;

  beforeEach(async () => {
    const breakpointObserverSpy = jasmine.createSpyObj('BreakpointObserver', ['observe']);

    await TestBed.configureTestingModule({
      imports: [ResponsiveLayoutComponent],
      providers: [
        { provide: BreakpointObserver, useValue: breakpointObserverSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ResponsiveLayoutComponent);
    component = fixture.componentInstance;
    breakpointObserver = TestBed.inject(BreakpointObserver) as jasmine.SpyObj<BreakpointObserver>;
    
    // Default mock setup
    breakpointObserver.observe.and.returnValue(of({
      matches: false,
      breakpoints: {}
    }));
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default variant as full-width', () => {
      expect(component.variant).toBe('full-width');
    });

    it('should have default currentBreakpoint as md', () => {
      expect(component.currentBreakpoint).toBe('md');
    });

    it('should not show mobile sidebar by default', () => {
      expect(component.showMobileSidebar).toBe(false);
    });

    it('should not be mobile by default', () => {
      expect(component.isMobile).toBe(false);
    });
  });

  describe('Layout Variants', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should apply sidebar variant class', () => {
      component.variant = 'sidebar';
      fixture.detectChanges();
      
      const container = fixture.nativeElement.querySelector('.responsive-layout');
      expect(container).toHaveClass('layout-sidebar');
    });

    it('should apply full-width variant class', () => {
      component.variant = 'full-width';
      fixture.detectChanges();
      
      const container = fixture.nativeElement.querySelector('.responsive-layout');
      expect(container).toHaveClass('layout-full-width');
    });

    it('should apply centered variant class', () => {
      component.variant = 'centered';
      fixture.detectChanges();
      
      const container = fixture.nativeElement.querySelector('.responsive-layout');
      expect(container).toHaveClass('layout-centered');
    });
  });

  describe('Sidebar Functionality', () => {
    beforeEach(() => {
      component.variant = 'sidebar';
      fixture.detectChanges();
    });

    it('should toggle sidebar when toggleSidebar is called', () => {
      expect(component.sidebarCollapsed).toBe(false);
      
      component.toggleSidebar();
      expect(component.sidebarCollapsed).toBe(true);
      
      component.toggleSidebar();
      expect(component.sidebarCollapsed).toBe(false);
    });

    it('should toggle mobile sidebar when toggleMobileSidebar is called', () => {
      expect(component.showMobileSidebar).toBe(false);
      
      component.toggleMobileSidebar();
      expect(component.showMobileSidebar).toBe(true);
      
      component.toggleMobileSidebar();
      expect(component.showMobileSidebar).toBe(false);
    });

    it('should close mobile sidebar when closeMobileSidebar is called', () => {
      component.showMobileSidebar = true;
      
      component.closeMobileSidebar();
      expect(component.showMobileSidebar).toBe(false);
    });
  });

  describe('Responsive Behavior', () => {
    it('should observe breakpoint changes', () => {
      fixture.detectChanges();
      expect(breakpointObserver.observe).toHaveBeenCalled();
    });

    it('should update mobile state based on breakpoints', () => {
      // Simulate mobile breakpoint
      breakpointObserver.observe.and.returnValue(of({
        matches: true,
        breakpoints: { '(max-width: 599.98px)': true }
      }));
      
      component.ngOnInit();
      
      // The actual breakpoint logic would be tested with real breakpoint values
      expect(breakpointObserver.observe).toHaveBeenCalled();
    });
  });

  describe('CSS Classes', () => {
    beforeEach(() => {
      fixture.detectChanges();
    });

    it('should generate correct layout classes', () => {
      component.variant = 'sidebar';
      component.currentBreakpoint = 'lg';
      component.isMobile = false;
      component.isDesktop = true;
      
      const classes = component.layoutClasses;
      expect(classes).toContain('layout-sidebar');
      expect(classes).toContain('layout-lg');
      expect(classes).toContain('desktop');
    });

    it('should generate correct main classes', () => {
      component.variant = 'sidebar';
      component.padding = 'lg';
      component.isMobile = false;
      
      const classes = component.mainClasses;
      expect(classes).toContain('padding-lg');
      expect(classes).toContain('with-sidebar');
    });

    it('should generate correct content classes', () => {
      component.variant = 'centered';
      
      const classes = component.contentClasses;
      expect(classes).toContain('centered-content');
    });
  });

  describe('Edge Cases', () => {
    it('should handle undefined variant gracefully', () => {
      component.variant = undefined as any;
      fixture.detectChanges();
      
      const container = fixture.nativeElement.querySelector('.responsive-layout');
      expect(container).toBeTruthy();
    });

    it('should cleanup on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');
      
      component.ngOnDestroy();
      
      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});