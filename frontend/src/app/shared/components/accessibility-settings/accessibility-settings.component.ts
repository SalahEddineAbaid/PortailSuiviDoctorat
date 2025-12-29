import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccessibilityService, AccessibilityPreferences } from '../../../core/services/accessibility.service';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-accessibility-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="accessibility-settings" role="region" aria-labelledby="a11y-settings-title">
      <h2 id="a11y-settings-title" class="settings-title">
        <span class="material-icons" aria-hidden="true">accessibility</span>
        Paramètres d'accessibilité
      </h2>
      
      <div class="settings-content">
        <div class="setting-group">
          <h3 class="group-title">Préférences visuelles</h3>
          
          <div class="setting-item">
            <label class="setting-label">
              <input
                type="checkbox"
                [(ngModel)]="preferences.highContrast"
                (change)="updatePreference('highContrast', $event)"
                class="setting-checkbox"
                id="high-contrast"
                [attr.aria-describedby]="'high-contrast-desc'"
              />
              <span class="checkbox-custom" aria-hidden="true"></span>
              <span class="setting-text">
                <strong>Contraste élevé</strong>
                <small id="high-contrast-desc">Améliore la lisibilité avec des couleurs plus contrastées</small>
              </span>
            </label>
          </div>
          
          <div class="setting-item">
            <label class="setting-label">
              <input
                type="checkbox"
                [(ngModel)]="preferences.largeText"
                (change)="updatePreference('largeText', $event)"
                class="setting-checkbox"
                id="large-text"
                [attr.aria-describedby]="'large-text-desc'"
              />
              <span class="checkbox-custom" aria-hidden="true"></span>
              <span class="setting-text">
                <strong>Texte agrandi</strong>
                <small id="large-text-desc">Augmente la taille du texte pour une meilleure lisibilité</small>
              </span>
            </label>
          </div>
          
          <div class="setting-item">
            <label class="setting-label">
              <input
                type="checkbox"
                [(ngModel)]="preferences.reduceMotion"
                (change)="updatePreference('reduceMotion', $event)"
                class="setting-checkbox"
                id="reduce-motion"
                [attr.aria-describedby]="'reduce-motion-desc'"
              />
              <span class="checkbox-custom" aria-hidden="true"></span>
              <span class="setting-text">
                <strong>Réduire les animations</strong>
                <small id="reduce-motion-desc">Limite les animations et transitions pour réduire les distractions</small>
              </span>
            </label>
          </div>
        </div>
        
        <div class="setting-group">
          <h3 class="group-title">Préférences de navigation</h3>
          
          <div class="setting-item">
            <label class="setting-label">
              <input
                type="checkbox"
                [(ngModel)]="preferences.keyboardNavigation"
                (change)="updatePreference('keyboardNavigation', $event)"
                class="setting-checkbox"
                id="keyboard-nav"
                [attr.aria-describedby]="'keyboard-nav-desc'"
              />
              <span class="checkbox-custom" aria-hidden="true"></span>
              <span class="setting-text">
                <strong>Navigation au clavier améliorée</strong>
                <small id="keyboard-nav-desc">Améliore la visibilité du focus pour la navigation au clavier</small>
              </span>
            </label>
          </div>
          
          <div class="setting-item">
            <label class="setting-label">
              <input
                type="checkbox"
                [(ngModel)]="preferences.screenReaderMode"
                (change)="updatePreference('screenReaderMode', $event)"
                class="setting-checkbox"
                id="screen-reader"
                [attr.aria-describedby]="'screen-reader-desc'"
              />
              <span class="checkbox-custom" aria-hidden="true"></span>
              <span class="setting-text">
                <strong>Mode lecteur d'écran</strong>
                <small id="screen-reader-desc">Optimise l'interface pour les lecteurs d'écran</small>
              </span>
            </label>
          </div>
        </div>
        
        <div class="setting-actions">
          <button
            type="button"
            class="btn btn-secondary"
            (click)="resetToDefaults()"
            aria-label="Réinitialiser tous les paramètres d'accessibilité"
          >
            <span class="material-icons" aria-hidden="true">refresh</span>
            Réinitialiser
          </button>
          
          <button
            type="button"
            class="btn btn-primary"
            (click)="testAccessibility()"
            aria-label="Tester l'accessibilité de la page actuelle"
          >
            <span class="material-icons" aria-hidden="true">check_circle</span>
            Tester l'accessibilité
          </button>
        </div>
        
        <div *ngIf="testResults" class="test-results" role="region" aria-labelledby="test-results-title">
          <h4 id="test-results-title">Résultats du test d'accessibilité</h4>
          
          <div *ngIf="testResults.errors.length > 0" class="test-errors">
            <h5>
              <span class="material-icons" aria-hidden="true">error</span>
              Erreurs ({{ testResults.errors.length }})
            </h5>
            <ul>
              <li *ngFor="let error of testResults.errors">{{ error }}</li>
            </ul>
          </div>
          
          <div *ngIf="testResults.warnings.length > 0" class="test-warnings">
            <h5>
              <span class="material-icons" aria-hidden="true">warning</span>
              Avertissements ({{ testResults.warnings.length }})
            </h5>
            <ul>
              <li *ngFor="let warning of testResults.warnings">{{ warning }}</li>
            </ul>
          </div>
          
          <div *ngIf="testResults.errors.length === 0 && testResults.warnings.length === 0" class="test-success">
            <span class="material-icons" aria-hidden="true">check_circle</span>
            Aucun problème d'accessibilité détecté !
          </div>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./accessibility-settings.component.scss']
})
export class AccessibilitySettingsComponent implements OnInit, OnDestroy {
  preferences: AccessibilityPreferences = {
    reduceMotion: false,
    highContrast: false,
    largeText: false,
    screenReaderMode: false,
    keyboardNavigation: false
  };
  
  testResults?: { errors: string[]; warnings: string[] };
  
  private destroy$ = new Subject<void>();

  constructor(private accessibilityService: AccessibilityService) {}

  ngOnInit(): void {
    this.accessibilityService.preferences$
      .pipe(takeUntil(this.destroy$))
      .subscribe(preferences => {
        this.preferences = { ...preferences };
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  updatePreference(key: keyof AccessibilityPreferences, event: Event): void {
    const target = event.target as HTMLInputElement;
    const value = target.checked;
    
    this.accessibilityService.updatePreference(key, value);
    this.accessibilityService.announce(
      `${key === 'highContrast' ? 'Contraste élevé' :
        key === 'largeText' ? 'Texte agrandi' :
        key === 'reduceMotion' ? 'Réduction des animations' :
        key === 'keyboardNavigation' ? 'Navigation au clavier' :
        'Mode lecteur d\'écran'} ${value ? 'activé' : 'désactivé'}`
    );
  }

  resetToDefaults(): void {
    const defaults: AccessibilityPreferences = {
      reduceMotion: false,
      highContrast: false,
      largeText: false,
      screenReaderMode: false,
      keyboardNavigation: false
    };
    
    Object.keys(defaults).forEach(key => {
      this.accessibilityService.updatePreference(
        key as keyof AccessibilityPreferences,
        defaults[key as keyof AccessibilityPreferences]
      );
    });
    
    this.accessibilityService.announce('Paramètres d\'accessibilité réinitialisés');
  }

  testAccessibility(): void {
    this.testResults = this.accessibilityService.validateAccessibility();
    
    const totalIssues = this.testResults.errors.length + this.testResults.warnings.length;
    if (totalIssues === 0) {
      this.accessibilityService.announce('Test d\'accessibilité terminé : aucun problème détecté');
    } else {
      this.accessibilityService.announce(
        `Test d\'accessibilité terminé : ${this.testResults.errors.length} erreur(s) et ${this.testResults.warnings.length} avertissement(s) trouvé(s)`,
        'assertive'
      );
    }
  }
}