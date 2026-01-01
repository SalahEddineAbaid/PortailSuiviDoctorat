import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertComponent } from '../alert/alert.component';
import { ToastService, ToastMessage } from '../../../../core/services/toast.service';

/**
 * Toast Container Component
 * Displays toast notifications in a fixed position on screen
 * 
 * Add this component once in your app.component.html:
 * <app-toast-container></app-toast-container>
 */
@Component({
    selector: 'app-toast-container',
    standalone: true,
    imports: [CommonModule, AlertComponent],
    template: `
    <div class="toast-container toast-{{position}}">
      <app-alert
        *ngFor="let toast of toasts$ | async"
        [type]="toast.type"
        [message]="toast.message"
        [title]="toast.title"
        [closeable]="true"
        (closed)="toastService.remove(toast.id)">
      </app-alert>
    </div>
  `,
    styles: [`
    .toast-container {
      position: fixed;
      z-index: 9999;
      max-width: 420px;
      width: 100%;
      pointer-events: none;
    }
    
    .toast-container > * {
      pointer-events: auto;
    }
    
    .toast-top-right {
      top: 1rem;
      right: 1rem;
    }
    
    .toast-top-left {
      top: 1rem;
      left: 1rem;
    }
    
    .toast-bottom-right {
      bottom: 1rem;
      right: 1rem;
    }
    
    .toast-bottom-left {
      bottom: 1rem;
      left: 1rem;
    }
    
    @media (max-width: 640px) {
      .toast-container {
        max-width: 100%;
        left: 0.5rem;
        right: 0.5rem;
      }
      
      .toast-top-right,
      .toast-top-left {
        top: 0.5rem;
      }
      
      .toast-bottom-right,
      .toast-bottom-left {
        bottom: 0.5rem;
      }
    }
  `]
})
export class ToastContainerComponent {
    @Input() position: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' = 'top-right';

    toasts$ = this.toastService.toasts$;

    constructor(public toastService: ToastService) { }
}
