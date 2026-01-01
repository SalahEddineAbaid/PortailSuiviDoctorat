import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Card Component
 * Reusable card container with header, body, and footer
 * 
 * @example
 * <app-card title="User Details" [shadow]="true">
 *   <p>Card content here</p>
 * </app-card>
 */
@Component({
    selector: 'app-card',
    standalone: true,
    imports: [CommonModule],
    template: `
    <div class="card" [class.shadow]="shadow" [class.hoverable]="hoverable">
      <div class="card-header" *ngIf="title || headerContent">
        <ng-content select="[header]"></ng-content>
        <h3 *ngIf="title && !headerContent" class="card-title">{{title}}</h3>
      </div>
      <div class="card-body" [class.no-padding]="noPadding">
        <ng-content></ng-content>
      </div>
      <div class="card-footer" *ngIf="footerContent">
        <ng-content select="[footer]"></ng-content>
      </div>
    </div>
  `,
    styles: [`
    .card {
      background-color: white;
      border: 1px solid #e5e7eb;
      border-radius: 0.5rem;
      overflow: hidden;
      transition: all 0.2s;
    }
    
    .card.shadow {
      box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06);
    }
    
    .card.hoverable:hover {
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
      transform: translateY(-2px);
    }
    
    .card-header {
      padding: 1.25rem 1.5rem;
      border-bottom: 1px solid #e5e7eb;
      background-color: #f9fafb;
    }
    
    .card-title {
      margin: 0;
      font-size: 1.125rem;
      font-weight: 600;
      color: #111827;
    }
    
    .card-body {
      padding: 1.5rem;
    }
    
    .card-body.no-padding {
      padding: 0;
    }
    
    .card-footer {
      padding: 1rem 1.5rem;
      border-top: 1px solid #e5e7eb;
      background-color: #f9fafb;
    }
  `]
})
export class CardComponent {
    @Input() title?: string;
    @Input() shadow: boolean = false;
    @Input() hoverable: boolean = false;
    @Input() noPadding: boolean = false;

    headerContent: boolean = false;
    footerContent: boolean = false;

    ngAfterContentInit(): void {
        // Check if header/footer content is projected
        // This is a simplified check
    }
}
