import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';

export interface ConfirmDialogData {
    title: string;
    message: string;
    confirmText?: string;
    cancelText?: string;
    type?: 'info' | 'warning' | 'danger';
}

/**
 * Dialog Service
 * Provides confirmation dialogs and modal management
 * 
 * @example
 * this.dialogService.confirm({
 *   title: 'Supprimer l\'utilisateur',
 *   message: 'Cette action est irréversible',
 *   type: 'danger'
 * }).subscribe(confirmed => {
 *   if (confirmed) {
 *     // Perform action
 *   }
 * });
 */
@Injectable({
    providedIn: 'root'
})
export class DialogService {
    private dialogSubject = new BehaviorSubject<ConfirmDialogData | null>(null);
    public currentDialog$ = this.dialogSubject.asObservable();

    private resultSubject = new Subject<boolean>();

    /**
     * Show confirmation dialog
     */
    confirm(data: ConfirmDialogData): Observable<boolean> {
        const dialogData: ConfirmDialogData = {
            ...data,
            confirmText: data.confirmText || 'Confirmer',
            cancelText: data.cancelText || 'Annuler',
            type: data.type || 'info'
        };

        console.log(`🔔 [DIALOG] ${data.type?.toUpperCase() || 'CONFIRM'}: ${data.title}`);

        this.dialogSubject.next(dialogData);

        return this.resultSubject.asObservable().pipe(take(1));
    }

    /**
     * Resolve the current dialog with result
     */
    resolve(result: boolean): void {
        this.resultSubject.next(result);
        this.dialogSubject.next(null);
    }

    /**
     * Close current dialog without result
     */
    close(): void {
        this.resolve(false);
    }
}
