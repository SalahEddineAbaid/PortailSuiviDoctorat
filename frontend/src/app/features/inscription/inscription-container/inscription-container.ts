import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-inscription-container',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  template: `
    <div class="inscription-container">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .inscription-container {
      display: block;
      min-height: 100vh;
      padding: 20px;
    }
  `]
})
export class InscriptionContainer {}