import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Empty State Component
 * Displays when no data is available
 * 
 * @example
 * <app-empty-state 
 *   icon="users" 
 *   message="Aucun utilisateur trouvé"
 *   actionText="Créer un utilisateur"
 *   (action)="createUser()">
 * </app-empty-state>
 */
@Component({
    selector: 'app-empty-state',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="empty-state">
      <div class="empty-state-icon">
        <i class="icon-{{icon}}"></i>
      </div>
      <h3 class="empty-state-title">{{title}}</h3>
      <p class="empty-state-message">{{message}}</p>
      <button 
        *ngIf="actionText" 
        class="btn btn-primary"
        (click)="action.emit()"
        type="button">
        {{actionText}}
      </button>
    </div>
  `,
    styles: [`
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 3rem 1.5rem;
      text-align: center;
    }
    
    .empty-state-icon {
      width: 4rem;
      height: 4rem;
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: #f3f4f6;
      border-radius: 50%;
      margin-bottom: 1.5rem;
      font-size: 2rem;
      color: #9ca3af;
    }
    
    .empty-state-title {
      font-size: 1.25rem;
      font-weight: 600;
      color: #111827;
      margin: 0 0 0.5rem 0;
    }
    
    .empty-state-message {
      color: #6b7280;
      margin: 0 0 1.5rem 0;
      max-width: 28rem;
    }
    
    .btn {
      padding: 0.625rem 1.25rem;
      border-radius: 0.375rem;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s;
    }
    
    .btn-primary {
      background-color: #3b82f6;
      color: white;
    }
    
    .btn-primary:hover {
      background-color: #2563eb;
    }
  `]
})
export class EmptyStateComponent {
    @Input() icon: string = 'inbox';
    @Input() title: string = 'Aucune donnée';
    @Input() message: string = '';
    @Input() actionText?: string;

    @Output() action = new EventEmitter<void>();
}
