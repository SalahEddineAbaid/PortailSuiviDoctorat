import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-validation-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="validation-form">
      <h3>Formulaire de Validation</h3>
      <form [formGroup]="validationForm" (ngSubmit)="onSubmit()">
        <div class="form-group">
          <label>Décision</label>
          <select formControlName="approuve">
            <option [value]="true">Approuver</option>
            <option [value]="false">Rejeter</option>
          </select>
        </div>
        <div class="form-group">
          <label>Commentaire</label>
          <textarea formControlName="commentaire" rows="4"></textarea>
        </div>
        <button type="submit" [disabled]="!validationForm.valid">Valider</button>
      </form>
    </div>
  `,
  styles: [`
    .validation-form { padding: 20px; }
    .form-group { margin-bottom: 15px; }
    .form-group label { display: block; margin-bottom: 5px; }
    .form-group select, .form-group textarea { width: 100%; padding: 8px; }
    button { padding: 10px 20px; background: #007bff; color: white; border: none; cursor: pointer; }
  `]
})
export class ValidationFormComponent implements OnInit {
  @Input() dossierId?: number;
  @Output() validated = new EventEmitter<any>();
  
  validationForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.validationForm = this.fb.group({
      approuve: [true, Validators.required],
      commentaire: ['']
    });
  }

  ngOnInit(): void {}

  onSubmit(): void {
    if (this.validationForm.valid) {
      this.validated.emit({
        approuve: this.validationForm.value.approuve,
        commentaire: this.validationForm.value.commentaire
      });
    }
  }
}
