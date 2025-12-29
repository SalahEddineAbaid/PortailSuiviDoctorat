import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface AccessibilityPreferences {
  reduceMotion: boolean;
  highContrast: boolean;
  largeText: boolean;
  screenReaderMode: boolean;
  keyboardNavigation: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AccessibilityService {
  private preferencesSubject = new BehaviorSubject<AccessibilityPreferences>({
    reduceMotion: false,
    highContrast: false,
    largeText: false,
    screenReaderMode: false,
    keyboardNavigation: false
  });

  public preferences$ = this.preferencesSubject.asObservable();
  private liveRegion?: HTMLElement;

  constructor() {
    this.initializePreferences();
    this.createLiveRegion();
    this.setupKeyboardDetection();
  }

  private initializePreferences(): void {
    // Check for system preferences
    const preferences: AccessibilityPreferences = {
      reduceMotion: this.prefersReducedMotion(),
      highContrast: this.prefersHighContrast(),
      largeText: false,
      screenReaderMode: this.detectScreenReader(),
      keyboardNavigation: false
    };

    // Load saved preferences from localStorage
    const saved = localStorage.getItem('accessibility-preferences');
    if (saved) {
      try {
        const savedPrefs = JSON.parse(saved);
        Object.assign(preferences, savedPrefs);
      } catch (error) {
        console.warn('Failed to parse saved accessibility preferences:', error);
      }
    }

    this.preferencesSubject.next(preferences);
    this.applyPreferences(preferences);
  }

  private prefersReducedMotion(): boolean {
    return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  }

  private prefersHighContrast(): boolean {
    return window.matchMedia('(prefers-contrast: high)').matches;
  }

  private detectScreenReader(): boolean {
    // Basic screen reader detection
    return !!(
      navigator.userAgent.match(/NVDA|JAWS|VoiceOver|ORCA|Dragon/i) ||
      window.speechSynthesis ||
      document.querySelector('[aria-hidden]')
    );
  }

  private createLiveRegion(): void {
    this.liveRegion = document.createElement('div');
    this.liveRegion.setAttribute('aria-live', 'polite');
    this.liveRegion.setAttribute('aria-atomic', 'true');
    this.liveRegion.setAttribute('class', 'sr-only');
    this.liveRegion.style.cssText = `
      position: absolute !important;
      width: 1px !important;
      height: 1px !important;
      padding: 0 !important;
      margin: -1px !important;
      overflow: hidden !important;
      clip: rect(0, 0, 0, 0) !important;
      white-space: nowrap !important;
      border: 0 !important;
    `;
    
    document.body.appendChild(this.liveRegion);
  }

  private setupKeyboardDetection(): void {
    let keyboardUsed = false;
    
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Tab') {
        keyboardUsed = true;
        document.body.classList.add('keyboard-navigation');
        this.updatePreference('keyboardNavigation', true);
      }
    });

    document.addEventListener('mousedown', () => {
      if (keyboardUsed) {
        keyboardUsed = false;
        document.body.classList.remove('keyboard-navigation');
        this.updatePreference('keyboardNavigation', false);
      }
    });
  }

  private applyPreferences(preferences: AccessibilityPreferences): void {
    const body = document.body;
    
    // Apply CSS classes based on preferences
    body.classList.toggle('reduce-motion', preferences.reduceMotion);
    body.classList.toggle('high-contrast', preferences.highContrast);
    body.classList.toggle('large-text', preferences.largeText);
    body.classList.toggle('screen-reader-mode', preferences.screenReaderMode);
    body.classList.toggle('keyboard-navigation', preferences.keyboardNavigation);
  }

  updatePreference<K extends keyof AccessibilityPreferences>(
    key: K,
    value: AccessibilityPreferences[K]
  ): void {
    const current = this.preferencesSubject.value;
    const updated = { ...current, [key]: value };
    
    this.preferencesSubject.next(updated);
    this.applyPreferences(updated);
    
    // Save to localStorage
    localStorage.setItem('accessibility-preferences', JSON.stringify(updated));
  }

  getPreferences(): AccessibilityPreferences {
    return this.preferencesSubject.value;
  }

  announce(message: string, priority: 'polite' | 'assertive' = 'polite'): void {
    if (!this.liveRegion || !message.trim()) {
      return;
    }

    // Update the aria-live attribute if needed
    if (this.liveRegion.getAttribute('aria-live') !== priority) {
      this.liveRegion.setAttribute('aria-live', priority);
    }

    // Clear and then set the message
    this.liveRegion.textContent = '';
    setTimeout(() => {
      if (this.liveRegion) {
        this.liveRegion.textContent = message;
      }
    }, 100);
  }

  focusElement(selector: string | HTMLElement, options?: FocusOptions): boolean {
    let element: HTMLElement | null = null;
    
    if (typeof selector === 'string') {
      element = document.querySelector(selector);
    } else {
      element = selector;
    }
    
    if (element) {
      // Make element focusable if it's not already
      if (!element.hasAttribute('tabindex') && !this.isFocusable(element)) {
        element.setAttribute('tabindex', '-1');
      }
      
      element.focus(options);
      return true;
    }
    
    return false;
  }

  private isFocusable(element: HTMLElement): boolean {
    const focusableElements = [
      'a[href]',
      'button:not([disabled])',
      'textarea:not([disabled])',
      'input:not([disabled])',
      'select:not([disabled])',
      '[tabindex]:not([tabindex="-1"])',
      '[contenteditable="true"]'
    ];
    
    return focusableElements.some(selector => element.matches(selector));
  }

  skipToContent(targetId: string): void {
    const target = document.getElementById(targetId);
    if (target) {
      this.focusElement(target);
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      this.announce(`Navigation vers ${target.getAttribute('aria-label') || 'le contenu principal'}`);
    }
  }

  checkColorContrast(foreground: string, background: string): number {
    // Simple contrast ratio calculation
    const getLuminance = (color: string): number => {
      const rgb = this.hexToRgb(color);
      if (!rgb) return 0;
      
      const [r, g, b] = [rgb.r, rgb.g, rgb.b].map(c => {
        c = c / 255;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
      });
      
      return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    };
    
    const l1 = getLuminance(foreground);
    const l2 = getLuminance(background);
    const lighter = Math.max(l1, l2);
    const darker = Math.min(l1, l2);
    
    return (lighter + 0.05) / (darker + 0.05);
  }

  private hexToRgb(hex: string): { r: number; g: number; b: number } | null {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null;
  }

  validateAccessibility(): { errors: string[]; warnings: string[] } {
    const errors: string[] = [];
    const warnings: string[] = [];
    
    // Check for missing alt text on images
    const images = document.querySelectorAll('img:not([alt])');
    if (images.length > 0) {
      errors.push(`${images.length} image(s) sans attribut alt trouvée(s)`);
    }
    
    // Check for missing form labels
    const inputs = document.querySelectorAll('input:not([aria-label]):not([aria-labelledby])');
    inputs.forEach(input => {
      const id = input.getAttribute('id');
      if (!id || !document.querySelector(`label[for="${id}"]`)) {
        warnings.push('Champ de formulaire sans label trouvé');
      }
    });
    
    // Check for missing heading structure
    const headings = document.querySelectorAll('h1, h2, h3, h4, h5, h6');
    if (headings.length === 0) {
      warnings.push('Aucun titre trouvé dans la page');
    }
    
    // Check for missing main landmark
    const main = document.querySelector('main, [role="main"]');
    if (!main) {
      warnings.push('Aucun élément main trouvé');
    }
    
    return { errors, warnings };
  }
}