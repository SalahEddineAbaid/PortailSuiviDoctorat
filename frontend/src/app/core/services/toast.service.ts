import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ToastType = 'info' | 'success' | 'warning' | 'error';

export interface ToastMessage {
    id: string;
    type: ToastType;
    message: string;
    title?: string;
    duration?: number;
}

/**
 * Global Toast Notification Service
 * Use this service to display temporary notifications to users
 */
@Injectable({
    providedIn: 'root'
})
export class ToastService {
    private toastsSubject = new BehaviorSubject<ToastMessage[]>([]);
    public toasts$: Observable<ToastMessage[]> = this.toastsSubject.asObservable();

    private readonly DEFAULT_DURATION = 5000; // 5 seconds

    /**
     * Show a toast notification
     */
    show(type: ToastType, message: string, title?: string, duration?: number): void {
        const toast: ToastMessage = {
            id: this.generateId(),
            type,
            message,
            title,
            duration: duration ?? this.DEFAULT_DURATION
        };

        console.log(`🔔 [TOAST] ${type.toUpperCase()}: ${message}`);

        const current = this.toastsSubject.value;
        this.toastsSubject.next([...current, toast]);

        // Auto dismiss
        if (toast.duration && toast.duration > 0) {
            setTimeout(() => this.remove(toast.id), toast.duration);
        }
    }

    /**
     * Show success toast
     */
    success(message: string, title: string = 'Succès'): void {
        this.show('success', message, title);
    }

    /**
     * Show error toast
     */
    error(message: string, title: string = 'Erreur'): void {
        this.show('error', message, title);
    }

    /**
     * Show info toast
     */
    info(message: string, title: string = 'Information'): void {
        this.show('info', message, title);
    }

    /**
     * Show warning toast
     */
    warning(message: string, title: string = 'Attention'): void {
        this.show('warning', message, title);
    }

    /**
     * Remove a specific toast
     */
    remove(id: string): void {
        const current = this.toastsSubject.value;
        this.toastsSubject.next(current.filter(t => t.id !== id));
    }

    /**
     * Clear all toasts
     */
    clearAll(): void {
        this.toastsSubject.next([]);
    }

    /**
     * Generate unique ID for toast
     */
    private generateId(): string {
        return `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    }
}
