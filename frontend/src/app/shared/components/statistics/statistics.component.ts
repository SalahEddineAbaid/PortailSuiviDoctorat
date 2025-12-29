import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DashboardStats } from '../../../core/models/dashboard.model';

export interface StatisticCard {
  title: string;
  value: number;
  subtitle?: string;
  icon: string;
  color: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'gray';
  trend?: {
    value: number;
    direction: 'up' | 'down';
    label: string;
  };
}

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss'
})
export class StatisticsComponent {
  @Input() stats!: DashboardStats;
  @Input() title = 'Statistiques';
  @Input() showTrends = false;

  get statisticCards(): StatisticCard[] {
    if (!this.stats) return [];

    return [
      {
        title: 'Total Inscriptions',
        value: this.stats.totalInscriptions,
        subtitle: 'Cette année',
        icon: 'fas fa-user-graduate',
        color: 'blue',
        trend: this.showTrends ? {
          value: 12,
          direction: 'up',
          label: 'vs mois dernier'
        } : undefined
      },
      {
        title: 'En Attente',
        value: this.stats.inscriptionsEnAttente,
        subtitle: 'À valider',
        icon: 'fas fa-clock',
        color: 'orange',
        trend: this.showTrends ? {
          value: 5,
          direction: 'down',
          label: 'vs semaine dernière'
        } : undefined
      },
      {
        title: 'Validées',
        value: this.stats.inscriptionsValidees,
        subtitle: 'Approuvées',
        icon: 'fas fa-check-circle',
        color: 'green',
        trend: this.showTrends ? {
          value: 8,
          direction: 'up',
          label: 'vs mois dernier'
        } : undefined
      },
      {
        title: 'Rejetées',
        value: this.stats.inscriptionsRejetees,
        subtitle: 'Non conformes',
        icon: 'fas fa-times-circle',
        color: 'red'
      },
      {
        title: 'Soutenances',
        value: this.stats.totalSoutenances,
        subtitle: 'Total',
        icon: 'fas fa-graduation-cap',
        color: 'purple',
        trend: this.showTrends ? {
          value: 3,
          direction: 'up',
          label: 'vs trimestre dernier'
        } : undefined
      },
      {
        title: 'Autorisées',
        value: this.stats.soutenancesAutorisees,
        subtitle: 'Prêtes',
        icon: 'fas fa-certificate',
        color: 'green'
      }
    ];
  }

  getColorClasses(color: string): string {
    const colorMap = {
      blue: 'bg-blue-50 border-blue-200 text-blue-800',
      green: 'bg-green-50 border-green-200 text-green-800',
      orange: 'bg-orange-50 border-orange-200 text-orange-800',
      red: 'bg-red-50 border-red-200 text-red-800',
      purple: 'bg-purple-50 border-purple-200 text-purple-800',
      gray: 'bg-gray-50 border-gray-200 text-gray-800'
    };
    return colorMap[color as keyof typeof colorMap] || colorMap.gray;
  }

  getIconColorClasses(color: string): string {
    const colorMap = {
      blue: 'text-blue-600',
      green: 'text-green-600',
      orange: 'text-orange-600',
      red: 'text-red-600',
      purple: 'text-purple-600',
      gray: 'text-gray-600'
    };
    return colorMap[color as keyof typeof colorMap] || colorMap.gray;
  }

  getTrendColorClasses(direction: 'up' | 'down'): string {
    return direction === 'up' ? 'text-green-600' : 'text-red-600';
  }

  getTrendIcon(direction: 'up' | 'down'): string {
    return direction === 'up' ? 'fas fa-arrow-up' : 'fas fa-arrow-down';
  }
}