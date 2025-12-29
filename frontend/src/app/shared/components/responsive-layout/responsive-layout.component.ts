import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Subject, takeUntil } from 'rxjs';

export type LayoutVariant = 'sidebar' | 'full-width' | 'centered';
export type BreakpointSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'app-responsive-layout',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="responsive-layout" [class]="layoutClasses">
      <!-- Sidebar for larger screens -->
      <aside 
        *ngIf="variant === 'sidebar' && !isMobile"
        class="layout-sidebar"
        [class.collapsed]="sidebarCollapsed"
      >
        <ng-content select="[slot=sidebar]"></ng-content>
      </aside>
      
      <!-- Mobile sidebar overlay -->
      <aside 
        *ngIf="variant === 'sidebar' && isMobile && showMobileSidebar"
        class="layout-sidebar mobile-sidebar"
        (click)="closeMobileSidebar()"
      >
        <div class="sidebar-content" (click)="$event.stopPropagation()">
          <ng-content select="[slot=sidebar]"></ng-content>
        </div>
      </aside>
      
      <!-- Main content area -->
      <main class="layout-main" [class]="mainClasses">
        <!-- Header/Toolbar -->
        <header *ngIf="showHeader" class="layout-header">
          <div class="header-content">
            <!-- Mobile menu button -->
            <button
              *ngIf="variant === 'sidebar' && isMobile"
              class="mobile-menu-btn"
              (click)="toggleMobileSidebar()"
              [attr.aria-expanded]="showMobileSidebar"
              aria-label="Toggle navigation menu"
            >
              <span class="material-icons">
                {{ showMobileSidebar ? 'close' : 'menu' }}
              </span>
            </button>
            
            <!-- Desktop sidebar toggle -->
            <button
              *ngIf="variant === 'sidebar' && !isMobile && showSidebarToggle"
              class="sidebar-toggle-btn"
              (click)="toggleSidebar()"
              [attr.aria-expanded]="!sidebarCollapsed"
              aria-label="Toggle sidebar"
            >
              <span class="material-icons">
                {{ sidebarCollapsed ? 'menu_open' : 'menu' }}
              </span>
            </button>
            
            <ng-content select="[slot=header]"></ng-content>
          </div>
        </header>
        
        <!-- Content area -->
        <div class="layout-content" [class]="contentClasses">
          <ng-content></ng-content>
        </div>
        
        <!-- Footer -->
        <footer *ngIf="showFooter" class="layout-footer">
          <ng-content select="[slot=footer]"></ng-content>
        </footer>
      </main>
      
      <!-- Mobile sidebar backdrop -->
      <div 
        *ngIf="variant === 'sidebar' && isMobile && showMobileSidebar"
        class="mobile-sidebar-backdrop"
        (click)="closeMobileSidebar()"
        aria-hidden="true"
      ></div>
    </div>
  `,
  styleUrls: ['./responsive-layout.component.scss']
})
export class ResponsiveLayoutComponent implements OnInit, OnDestroy {
  @Input() variant: LayoutVariant = 'full-width';
  @Input() showHeader = true;
  @Input() showFooter = false;
  @Input() showSidebarToggle = true;
  @Input() sidebarWidth = '280px';
  @Input() sidebarCollapsedWidth = '64px';
  @Input() maxWidth = '1200px';
  @Input() padding = 'md';
  
  isMobile = false;
  isTablet = false;
  isDesktop = false;
  currentBreakpoint: BreakpointSize = 'md';
  sidebarCollapsed = false;
  showMobileSidebar = false;
  
  private destroy$ = new Subject<void>();

  constructor(
    private breakpointObserver: BreakpointObserver,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Observe breakpoint changes
    this.breakpointObserver
      .observe([
        Breakpoints.XSmall,
        Breakpoints.Small,
        Breakpoints.Medium,
        Breakpoints.Large,
        Breakpoints.XLarge
      ])
      .pipe(takeUntil(this.destroy$))
      .subscribe(result => {
        this.updateBreakpoints(result.breakpoints);
        this.cdr.markForCheck();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateBreakpoints(breakpoints: { [key: string]: boolean }): void {
    if (breakpoints[Breakpoints.XSmall]) {
      this.currentBreakpoint = 'xs';
      this.isMobile = true;
      this.isTablet = false;
      this.isDesktop = false;
    } else if (breakpoints[Breakpoints.Small]) {
      this.currentBreakpoint = 'sm';
      this.isMobile = true;
      this.isTablet = false;
      this.isDesktop = false;
    } else if (breakpoints[Breakpoints.Medium]) {
      this.currentBreakpoint = 'md';
      this.isMobile = false;
      this.isTablet = true;
      this.isDesktop = false;
    } else if (breakpoints[Breakpoints.Large]) {
      this.currentBreakpoint = 'lg';
      this.isMobile = false;
      this.isTablet = false;
      this.isDesktop = true;
    } else if (breakpoints[Breakpoints.XLarge]) {
      this.currentBreakpoint = 'xl';
      this.isMobile = false;
      this.isTablet = false;
      this.isDesktop = true;
    }

    // Auto-close mobile sidebar when switching to desktop
    if (!this.isMobile && this.showMobileSidebar) {
      this.showMobileSidebar = false;
    }
  }

  toggleSidebar(): void {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  toggleMobileSidebar(): void {
    this.showMobileSidebar = !this.showMobileSidebar;
  }

  closeMobileSidebar(): void {
    this.showMobileSidebar = false;
  }

  get layoutClasses(): string {
    return [
      'layout',
      `layout-${this.variant}`,
      `layout-${this.currentBreakpoint}`,
      this.isMobile ? 'mobile' : '',
      this.isTablet ? 'tablet' : '',
      this.isDesktop ? 'desktop' : '',
      this.sidebarCollapsed ? 'sidebar-collapsed' : '',
      this.showMobileSidebar ? 'mobile-sidebar-open' : ''
    ].filter(Boolean).join(' ');
  }

  get mainClasses(): string {
    return [
      'main',
      `padding-${this.padding}`,
      this.variant === 'sidebar' && !this.isMobile ? 'with-sidebar' : '',
      this.variant === 'centered' ? 'centered' : ''
    ].filter(Boolean).join(' ');
  }

  get contentClasses(): string {
    return [
      'content',
      this.variant === 'centered' ? 'centered-content' : ''
    ].filter(Boolean).join(' ');
  }
}