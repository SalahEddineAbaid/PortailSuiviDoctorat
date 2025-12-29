import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

export interface AvisData {
  type: 'inscription' | 'soutenance';
  dossierId: number;
  doctorantName: string;
  currentAvis?: {
    statut: string;
    commentaire: string;
    dateAvis: Date;
  };
}

export interface AvisSubmission {
  statut: 'FAVORABLE' | 'DEFAVORABLE' | 'RESERVE';
  commentaire: string;
}

@Component({
  selector: 'app-avis-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './avis-form.component.html',
  styleUrl: './avis-form.component.scss'
})
export class AvisFormComponent implements OnInit {
  @Input() avisData!: AvisData;
  @Input() isLoading: boolean = false;
  @Output() submitAvis = new EventEmitter<AvisSubmission>();
  @Output() cancel = new EventEmitter<void>();

  avisForm!: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnInit(): void {
    this.initForm();
  }

  private initForm(): void {
    this.avisForm = this.fb.group({
      statut: [this.avisData.currentAvis?.statut || '', Validators.required],
      commentaire: [
        this.avisData.currentAvis?.commentaire || '', 
        [Validators.required, Validators.minLength(10)]
      ]
    });
  }

  onSubmit(): void {
    if (this.avisForm.valid) {
      const formValue = this.avisForm.value;
      this.submitAvis.emit({
        statut: formValue.statut,
        commentaire: formValue.commentaire
      });
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }

  getTypeLabel(): string {
    return this.avisData.type === 'inscription' ? 'inscription' : 'soutenance';
  }

  getTypeIcon(): string {
    return this.avisData.type === 'inscription' ? 'fas fa-user-plus' : 'fas fa-graduation-cap';
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'FAVORABLE':
        return 'green';
      case 'DEFAVORABLE':
        return 'red';
      case 'RESERVE':
        return 'orange';
      default:
        return 'gray';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'FAVORABLE':
        return 'Favorable';
      case 'DEFAVORABLE':
        return 'Défavorable';
      case 'RESERVE':
        return 'Avec réserves';
      default:
        return status;
    }
  }

  isFormValid(): boolean {
    return this.avisForm.valid;
  }

  hasCurrentAvis(): boolean {
    return !!this.avisData.currentAvis;
  }
}