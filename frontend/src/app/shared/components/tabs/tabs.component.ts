import { Component, Input, Output, EventEmitter, ChangeDetectionStrategy, ContentChildren, QueryList, AfterContentInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

export interface TabItem {
  id: string;
  label: string;
  icon?: string;
  disabled?: boolean;
  badge?: string | number;
  closable?: boolean;
}

@Component({
  selector: 'app-tab',
  standalone: true,
  imports: [CommonModule],
  template: `<ng-content></ng-content>`,
  styles: [`
    :host {
      display: block;
      width: 100%;
    }
    
    :host:not(.active) {
      display: none;
    }
  `]
})
export class TabComponent {
  @Input() id!: string;
  @Input() label!: string;
  @Input() icon?: string;
  @Input() disabled = false;
  @Input() badge?: string | number;
  @Input() closable = false;
  @Input() active = false;
}

@Component({
  selector: 'app-tabs',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="tabs-container" [class]="containerClasses">
      <div class="tabs-header" role="tablist" [attr.aria-label]="ariaLabel">
        <div class="tabs-nav">
          <button
            *ngFor="let tab of tabs; trackBy: trackByTabId"
            class="tab-button"
            [class.active]="tab.id === activeTabId"
            [class.disabled]="tab.disabled"
            [disabled]="tab.disabled"
            [attr.aria-selected]="tab.id === activeTabId"
            [attr.aria-controls]="'tab-panel-' + tab.id"
            [attr.id]="'tab-' + tab.id"
            role="tab"
            (click)="selectTab(tab.id)"
            (keydown.arrowleft)="navigateTab(-1, $any($event))"
            (keydown.arrowright)="navigateTab(1, $any($event))"
            (keydown.home)="navigateToFirstTab($any($event))"
            (keydown.end)="navigateToLastTab($any($event))"
          >
            <span *ngIf="tab.icon" class="material-icons tab-icon" aria-hidden="true">{{ tab.icon }}</span>
            <span class="tab-label">{{ tab.label }}</span>
            <span *ngIf="tab.badge" class="tab-badge" [attr.aria-label]="tab.badge + ' éléments'">{{ tab.badge }}</span>
            <button
              *ngIf="tab.closable"
              class="tab-close"
              (click)="closeTab(tab.id, $event)"
              [attr.aria-label]="'Fermer ' + tab.label"
              type="button"
            >
              <span class="material-icons" aria-hidden="true">close</span>
            </button>
          </button>
        </div>
        
        <div *ngIf="showAddButton" class="tabs-actions">
          <button
            class="add-tab-button"
            (click)="addTab.emit()"
            aria-label="Ajouter un onglet"
            type="button"
          >
            <span class="material-icons" aria-hidden="true">add</span>
          </button>
        </div>
      </div>
      
      <div class="tabs-content">
        <div
          *ngFor="let tab of tabs; trackBy: trackByTabId"
          class="tab-panel"
          [class.active]="tab.id === activeTabId"
          [attr.id]="'tab-panel-' + tab.id"
          [attr.aria-labelledby]="'tab-' + tab.id"
          [attr.hidden]="tab.id !== activeTabId"
          role="tabpanel"
        >
          <ng-content></ng-content>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./tabs.component.scss']
})
export class TabsComponent implements AfterContentInit, OnDestroy {
  @Input() tabs: TabItem[] = [];
  @Input() activeTabId = '';
  @Input() variant: 'default' | 'pills' | 'underline' = 'default';
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
  @Input() showAddButton = false;
  @Input() ariaLabel = 'Onglets';
  
  @Output() tabChange = new EventEmitter<string>();
  @Output() tabClose = new EventEmitter<string>();
  @Output() addTab = new EventEmitter<void>();

  @ContentChildren(TabComponent) tabComponents!: QueryList<TabComponent>;
  
  private destroy$ = new Subject<void>();

  get containerClasses(): string {
    return [
      'tabs',
      `tabs-${this.variant}`,
      `tabs-${this.size}`
    ].join(' ');
  }

  ngAfterContentInit(): void {
    // Initialize tabs from content children if not provided via input
    if (this.tabs.length === 0 && this.tabComponents) {
      this.tabs = this.tabComponents.map(tab => ({
        id: tab.id,
        label: tab.label,
        icon: tab.icon,
        disabled: tab.disabled,
        badge: tab.badge,
        closable: tab.closable
      }));
    }

    // Set first tab as active if none specified
    if (!this.activeTabId && this.tabs.length > 0) {
      const firstEnabledTab = this.tabs.find(tab => !tab.disabled);
      if (firstEnabledTab) {
        this.activeTabId = firstEnabledTab.id;
      }
    }

    // Update tab components active state
    this.updateTabComponents();

    // Listen for changes in tab components
    this.tabComponents.changes
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.updateTabComponents();
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private updateTabComponents(): void {
    if (this.tabComponents) {
      this.tabComponents.forEach(tab => {
        tab.active = tab.id === this.activeTabId;
      });
    }
  }

  selectTab(tabId: string): void {
    const tab = this.tabs.find(t => t.id === tabId);
    if (!tab || tab.disabled) {
      return;
    }

    this.activeTabId = tabId;
    this.updateTabComponents();
    this.tabChange.emit(tabId);
  }

  closeTab(tabId: string, event: Event): void {
    event.stopPropagation();
    
    const tabIndex = this.tabs.findIndex(t => t.id === tabId);
    if (tabIndex === -1) return;

    // If closing active tab, select another tab
    if (tabId === this.activeTabId) {
      const nextTab = this.tabs[tabIndex + 1] || this.tabs[tabIndex - 1];
      if (nextTab && !nextTab.disabled) {
        this.selectTab(nextTab.id);
      }
    }

    this.tabClose.emit(tabId);
  }

  navigateTab(direction: number, event: KeyboardEvent): void {
    event.preventDefault();
    
    const currentIndex = this.tabs.findIndex(t => t.id === this.activeTabId);
    if (currentIndex === -1) return;

    const enabledTabs = this.tabs.filter(t => !t.disabled);
    const currentEnabledIndex = enabledTabs.findIndex(t => t.id === this.activeTabId);
    
    let nextIndex = currentEnabledIndex + direction;
    
    // Wrap around
    if (nextIndex < 0) {
      nextIndex = enabledTabs.length - 1;
    } else if (nextIndex >= enabledTabs.length) {
      nextIndex = 0;
    }

    const nextTab = enabledTabs[nextIndex];
    if (nextTab) {
      this.selectTab(nextTab.id);
      this.focusTab(nextTab.id);
    }
  }

  navigateToFirstTab(event: KeyboardEvent): void {
    event.preventDefault();
    const firstEnabledTab = this.tabs.find(t => !t.disabled);
    if (firstEnabledTab) {
      this.selectTab(firstEnabledTab.id);
      this.focusTab(firstEnabledTab.id);
    }
  }

  navigateToLastTab(event: KeyboardEvent): void {
    event.preventDefault();
    const enabledTabs = this.tabs.filter(t => !t.disabled);
    const lastEnabledTab = enabledTabs[enabledTabs.length - 1];
    if (lastEnabledTab) {
      this.selectTab(lastEnabledTab.id);
      this.focusTab(lastEnabledTab.id);
    }
  }

  private focusTab(tabId: string): void {
    const tabButton = document.getElementById(`tab-${tabId}`);
    if (tabButton) {
      tabButton.focus();
    }
  }

  trackByTabId(index: number, tab: TabItem): string {
    return tab.id;
  }
}