import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatisticsComponent } from './statistics.component';
import { DashboardStats } from '../../../core/models/dashboard.model';

describe('StatisticsComponent', () => {
  let component: StatisticsComponent;
  let fixture: ComponentFixture<StatisticsComponent>;

  const mockStats: DashboardStats = {
    totalInscriptions: 150,
    inscriptionsEnAttente: 12,
    inscriptionsValidees: 120,
    inscriptionsRejetees: 18,
    totalSoutenances: 45,
    soutenancesEnAttente: 8,
    soutenancesAutorisees: 37
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatisticsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(StatisticsComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default title', () => {
      expect(component.title).toBe('Statistiques');
    });

    it('should have showTrends false by default', () => {
      expect(component.showTrends).toBe(false);
    });
  });

  describe('Statistics Cards Generation', () => {
    beforeEach(() => {
      component.stats = mockStats;
      fixture.detectChanges();
    });

    it('should generate correct number of statistic cards', () => {
      const cards = component.statisticCards;
      expect(cards.length).toBe(6);
    });

    it('should generate cards with correct values', () => {
      const cards = component.statisticCards;
      
      expect(cards[0].title).toBe('Total Inscriptions');
      expect(cards[0].value).toBe(150);
      expect(cards[0].color).toBe('blue');

      expect(cards[1].title).toBe('En Attente');
      expect(cards[1].value).toBe(12);
      expect(cards[1].color).toBe('orange');

      expect(cards[2].title).toBe('Validées');
      expect(cards[2].value).toBe(120);
      expect(cards[2].color).toBe('green');
    });

    it('should include trends when showTrends is true', () => {
      component.showTrends = true;
      const cards = component.statisticCards;
      
      expect(cards[0].trend).toBeDefined();
      expect(cards[0].trend?.value).toBe(12);
      expect(cards[0].trend?.direction).toBe('up');
    });

    it('should not include trends when showTrends is false', () => {
      component.showTrends = false;
      const cards = component.statisticCards;
      
      expect(cards[3].trend).toBeUndefined(); // Rejetées card doesn't have trend
    });
  });

  describe('Helper Methods', () => {
    it('should return correct color classes', () => {
      expect(component.getColorClasses('blue')).toContain('bg-blue-50');
      expect(component.getColorClasses('green')).toContain('bg-green-50');
      expect(component.getColorClasses('orange')).toContain('bg-orange-50');
    });

    it('should return correct icon color classes', () => {
      expect(component.getIconColorClasses('blue')).toBe('text-blue-600');
      expect(component.getIconColorClasses('green')).toBe('text-green-600');
      expect(component.getIconColorClasses('red')).toBe('text-red-600');
    });

    it('should return correct trend color classes', () => {
      expect(component.getTrendColorClasses('up')).toBe('text-green-600');
      expect(component.getTrendColorClasses('down')).toBe('text-red-600');
    });

    it('should return correct trend icons', () => {
      expect(component.getTrendIcon('up')).toBe('fas fa-arrow-up');
      expect(component.getTrendIcon('down')).toBe('fas fa-arrow-down');
    });
  });

  describe('Edge Cases', () => {
    it('should handle undefined stats gracefully', () => {
      component.stats = undefined as any;
      const cards = component.statisticCards;
      expect(cards).toEqual([]);
    });

    it('should handle unknown color gracefully', () => {
      const result = component.getColorClasses('unknown' as any);
      expect(result).toContain('bg-gray-50');
    });
  });
});