import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TabsComponent, TabItem } from './tabs.component';

describe('TabsComponent', () => {
    let component: TabsComponent;
    let fixture: ComponentFixture<TabsComponent>;

    const mockTabs: TabItem[] = [
        { id: 'tab1', label: 'Informations générales' },
        { id: 'tab2', label: 'Documents' },
        { id: 'tab3', label: 'Validation' }
    ];

    beforeEach(async () => {
        await TestBed.configureTestingModule({
            imports: [TabsComponent]
        }).compileComponents();

        fixture = TestBed.createComponent(TabsComponent);
        component = fixture.componentInstance;
    });

    describe('Component Initialization', () => {
        it('should create', () => {
            expect(component).toBeTruthy();
        });

        it('should have empty tabs array by default', () => {
            expect(component.tabs).toEqual([]);
        });

        it('should have default activeTabId as empty string', () => {
            expect(component.activeTabId).toBe('');
        });

        it('should have default variant as default', () => {
            expect(component.variant).toBe('default');
        });

        it('should have default size as md', () => {
            expect(component.size).toBe('md');
        });
    });

    describe('Tab Display', () => {
        beforeEach(() => {
            component.tabs = mockTabs;
            component.activeTabId = 'tab1';
            fixture.detectChanges();
        });

        it('should display all tabs', () => {
            const tabButtons = fixture.nativeElement.querySelectorAll('.tab-button');
            expect(tabButtons.length).toBe(mockTabs.length);
        });

        it('should display tab labels', () => {
            const tabButtons = fixture.nativeElement.querySelectorAll('.tab-button');
            expect(tabButtons[0].textContent.trim()).toContain('Informations générales');
            expect(tabButtons[1].textContent.trim()).toContain('Documents');
            expect(tabButtons[2].textContent.trim()).toContain('Validation');
        });

        it('should mark active tab with correct class', () => {
            const activeTab = fixture.nativeElement.querySelector('.tab-button.active');
            expect(activeTab).toBeTruthy();
            expect(activeTab.textContent.trim()).toContain('Informations générales');
        });

        it('should not mark inactive tabs as active', () => {
            const inactiveTabs = fixture.nativeElement.querySelectorAll('.tab-button:not(.active)');
            expect(inactiveTabs.length).toBe(2);
        });
    });

    describe('Tab Navigation', () => {
        beforeEach(() => {
            component.tabs = mockTabs;
            component.activeTabId = 'tab1';
            fixture.detectChanges();
        });

        it('should emit tabChange event when tab is clicked', () => {
            spyOn(component.tabChange, 'emit');

            const secondTab = fixture.nativeElement.querySelectorAll('.tab-button')[1];
            secondTab.click();

            expect(component.tabChange.emit).toHaveBeenCalledWith('tab2');
        });

        it('should call selectTab method when tab is clicked', () => {
            spyOn(component, 'selectTab');

            const secondTab = fixture.nativeElement.querySelectorAll('.tab-button')[1];
            secondTab.click();

            expect(component.selectTab).toHaveBeenCalledWith('tab2');
        });

        it('should update activeTabId when selectTab is called', () => {
            component.selectTab('tab2');
            expect(component.activeTabId).toBe('tab2');
        });

        it('should not select disabled tabs', () => {
            const disabledTabs = [...mockTabs];
            disabledTabs[1].disabled = true;
            component.tabs = disabledTabs;
            fixture.detectChanges();

            spyOn(component.tabChange, 'emit');

            const disabledTab = fixture.nativeElement.querySelectorAll('.tab-button')[1];
            disabledTab.click();

            expect(component.tabChange.emit).not.toHaveBeenCalled();
        });
    });

    describe('Keyboard Navigation', () => {
        beforeEach(() => {
            component.tabs = mockTabs;
            component.activeTabId = 'tab1';
            fixture.detectChanges();
        });

        it('should handle arrow key navigation', () => {
            spyOn(component, 'navigateTab');

            const firstTab = fixture.nativeElement.querySelector('.tab-button');
            const event = new KeyboardEvent('keydown', { key: 'ArrowRight' });
            firstTab.dispatchEvent(event);

            expect(component.navigateTab).toHaveBeenCalledWith(1, event);
        });

        it('should handle Home key navigation', () => {
            spyOn(component, 'navigateToFirstTab');

            const firstTab = fixture.nativeElement.querySelector('.tab-button');
            const event = new KeyboardEvent('keydown', { key: 'Home' });
            firstTab.dispatchEvent(event);

            expect(component.navigateToFirstTab).toHaveBeenCalledWith(event);
        });

        it('should handle End key navigation', () => {
            spyOn(component, 'navigateToLastTab');

            const firstTab = fixture.nativeElement.querySelector('.tab-button');
            const event = new KeyboardEvent('keydown', { key: 'End' });
            firstTab.dispatchEvent(event);

            expect(component.navigateToLastTab).toHaveBeenCalledWith(event);
        });
    });

    describe('Accessibility', () => {
        beforeEach(() => {
            component.tabs = mockTabs;
            component.activeTabId = 'tab1';
            fixture.detectChanges();
        });

        it('should have proper ARIA attributes on tab list', () => {
            const tabList = fixture.nativeElement.querySelector('.tabs-header');
            expect(tabList.getAttribute('role')).toBe('tablist');
        });

        it('should have proper ARIA attributes on tab buttons', () => {
            const tabButtons = fixture.nativeElement.querySelectorAll('.tab-button');

            tabButtons.forEach((button, index) => {
                expect(button.getAttribute('role')).toBe('tab');
                expect(button.getAttribute('aria-controls')).toBe(`tab-panel-${mockTabs[index].id}`);
                expect(button.getAttribute('id')).toBe(`tab-${mockTabs[index].id}`);
            });
        });

        it('should mark active tab with aria-selected', () => {
            const activeTab = fixture.nativeElement.querySelector('.tab-button.active');
            expect(activeTab.getAttribute('aria-selected')).toBe('true');
        });

        it('should mark inactive tabs with aria-selected false', () => {
            const inactiveTabs = fixture.nativeElement.querySelectorAll('.tab-button:not(.active)');
            inactiveTabs.forEach(tab => {
                expect(tab.getAttribute('aria-selected')).toBe('false');
            });
        });

        it('should have proper role for tab panels', () => {
            const tabPanels = fixture.nativeElement.querySelectorAll('.tab-panel');
            tabPanels.forEach(panel => {
                expect(panel.getAttribute('role')).toBe('tabpanel');
            });
        });
    });

    describe('Tab Variants and Styling', () => {
        beforeEach(() => {
            component.tabs = mockTabs;
            fixture.detectChanges();
        });

        it('should apply default variant class', () => {
            component.variant = 'default';
            fixture.detectChanges();

            const container = fixture.nativeElement.querySelector('.tabs-container');
            expect(container).toHaveClass('tabs-default');
        });

        it('should apply pills variant class', () => {
            component.variant = 'pills';
            fixture.detectChanges();

            const container = fixture.nativeElement.querySelector('.tabs-container');
            expect(container).toHaveClass('tabs-pills');
        });

        it('should apply underline variant class', () => {
            component.variant = 'underline';
            fixture.detectChanges();

            const container = fixture.nativeElement.querySelector('.tabs-container');
            expect(container).toHaveClass('tabs-underline');
        });

        it('should apply size classes', () => {
            component.size = 'lg';
            fixture.detectChanges();

            const container = fixture.nativeElement.querySelector('.tabs-container');
            expect(container).toHaveClass('tabs-lg');
        });
    });

    describe('Tab Features', () => {
        beforeEach(() => {
            fixture.detectChanges();
        });

        it('should display tab icons when provided', () => {
            const tabsWithIcons: TabItem[] = [
                { id: 'tab1', label: 'Home', icon: 'home' }
            ];
            component.tabs = tabsWithIcons;
            fixture.detectChanges();

            const icon = fixture.nativeElement.querySelector('.tab-icon');
            expect(icon).toBeTruthy();
            expect(icon.textContent.trim()).toBe('home');
        });

        it('should display tab badges when provided', () => {
            const tabsWithBadges: TabItem[] = [
                { id: 'tab1', label: 'Messages', badge: '5' }
            ];
            component.tabs = tabsWithBadges;
            fixture.detectChanges();

            const badge = fixture.nativeElement.querySelector('.tab-badge');
            expect(badge).toBeTruthy();
            expect(badge.textContent.trim()).toBe('5');
        });

        it('should show close button for closable tabs', () => {
            const closableTabs: TabItem[] = [
                { id: 'tab1', label: 'Closable Tab', closable: true }
            ];
            component.tabs = closableTabs;
            fixture.detectChanges();

            const closeButton = fixture.nativeElement.querySelector('.tab-close');
            expect(closeButton).toBeTruthy();
        });

        it('should emit tabClose event when close button is clicked', () => {
            const closableTabs: TabItem[] = [
                { id: 'tab1', label: 'Closable Tab', closable: true }
            ];
            component.tabs = closableTabs;
            fixture.detectChanges();

            spyOn(component.tabClose, 'emit');

            const closeButton = fixture.nativeElement.querySelector('.tab-close');
            closeButton.click();

            expect(component.tabClose.emit).toHaveBeenCalledWith('tab1');
        });

        it('should show add button when showAddButton is true', () => {
            component.showAddButton = true;
            fixture.detectChanges();

            const addButton = fixture.nativeElement.querySelector('.add-tab-button');
            expect(addButton).toBeTruthy();
        });

        it('should emit addTab event when add button is clicked', () => {
            component.showAddButton = true;
            fixture.detectChanges();

            spyOn(component.addTab, 'emit');

            const addButton = fixture.nativeElement.querySelector('.add-tab-button');
            addButton.click();

            expect(component.addTab.emit).toHaveBeenCalled();
        });
    });

    describe('Edge Cases', () => {
        it('should handle empty tabs array', () => {
            component.tabs = [];
            fixture.detectChanges();

            const tabButtons = fixture.nativeElement.querySelectorAll('.tab-button');
            expect(tabButtons.length).toBe(0);
        });

        it('should handle single tab', () => {
            component.tabs = [{ id: 'single', label: 'Single Tab' }];
            component.activeTabId = 'single';
            fixture.detectChanges();

            const tabButtons = fixture.nativeElement.querySelectorAll('.tab-button');
            expect(tabButtons.length).toBe(1);
            expect(tabButtons[0]).toHaveClass('active');
        });

        it('should handle tabs without active tab', () => {
            component.tabs = mockTabs;
            component.activeTabId = '';
            fixture.detectChanges();

            const activeTabs = fixture.nativeElement.querySelectorAll('.tab-button.active');
            expect(activeTabs.length).toBe(0);
        });

        it('should handle invalid activeTabId', () => {
            component.tabs = mockTabs;
            component.activeTabId = 'invalid-id';
            fixture.detectChanges();

            const activeTabs = fixture.nativeElement.querySelectorAll('.tab-button.active');
            expect(activeTabs.length).toBe(0);
        });
    });

    describe('Track By Function', () => {
        it('should track tabs by id', () => {
            const tab = mockTabs[0];
            const result = component.trackByTabId(0, tab);
            expect(result).toBe(tab.id);
        });
    });
});
});