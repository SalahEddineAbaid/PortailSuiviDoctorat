import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * 🔍 Validators personnalisés pour le Portail de Suivi du Doctorat
 */
export class CustomValidators {

  // ===== EMAIL VALIDATORS =====

  /**
   * 📧 Validator pour email avec format français/international
   */
  static email(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null; // Laisser le validator 'required' gérer les champs vides
    }

    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
    
    if (!emailRegex.test(control.value)) {
      return { email: { value: control.value } };
    }

    return null;
  }

  /**
   * 📧 Validator pour email académique (.edu, .ac, .univ, etc.)
   */
  static academicEmail(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const academicDomains = [
      '.edu', '.ac.', '.univ-', '.university', '.college',
      '.edu.', '.ac.fr', '.univ.fr', '.ens.fr', '.cnrs.fr'
    ];

    const hasAcademicDomain = academicDomains.some(domain => 
      control.value.toLowerCase().includes(domain)
    );

    if (!hasAcademicDomain) {
      return { academicEmail: { value: control.value } };
    }

    return null;
  }

  // ===== PHONE VALIDATORS =====

  /**
   * 📞 Validator pour numéro de téléphone français
   */
  static phoneNumber(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    // Formats acceptés:
    // - 0123456789
    // - 01 23 45 67 89
    // - 01.23.45.67.89
    // - 01-23-45-67-89
    // - +33123456789
    // - +33 1 23 45 67 89
    const phoneRegex = /^(\+33|0)[1-9](\d{8}|\s?\d{2}\s?\d{2}\s?\d{2}\s?\d{2}|[\.\-\s]?\d{2}[\.\-\s]?\d{2}[\.\-\s]?\d{2}[\.\-\s]?\d{2})$/;
    
    // Nettoyer le numéro (enlever espaces, points, tirets)
    const cleanedPhone = control.value.replace(/[\s\.\-]/g, '');
    
    if (!phoneRegex.test(cleanedPhone)) {
      return { phoneNumber: { value: control.value } };
    }

    return null;
  }

  /**
   * 📞 Validator pour numéro de téléphone international
   */
  static internationalPhone(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    // Format international: +[1-4 chiffres][4-15 chiffres]
    const intlPhoneRegex = /^\+[1-9]\d{1,3}\d{4,15}$/;
    const cleanedPhone = control.value.replace(/[\s\.\-]/g, '');
    
    if (!intlPhoneRegex.test(cleanedPhone)) {
      return { internationalPhone: { value: control.value } };
    }

    return null;
  }

  // ===== FILE VALIDATORS =====

  /**
   * 📁 Validator pour la taille de fichier
   */
  static fileSize(maxSizeInMB: number): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const file = control.value as File;
      if (!(file instanceof File)) {
        return null; // Pas un fichier, laisser d'autres validators gérer
      }

      const maxSizeInBytes = maxSizeInMB * 1024 * 1024;
      
      if (file.size > maxSizeInBytes) {
        return { 
          fileSize: { 
            actualSize: file.size,
            maxSize: maxSizeInBytes,
            maxSizeMB: maxSizeInMB,
            actualSizeMB: Math.round(file.size / (1024 * 1024) * 100) / 100
          } 
        };
      }

      return null;
    };
  }

  /**
   * 📁 Validator pour les types de fichiers autorisés
   */
  static fileType(allowedTypes: string[]): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const file = control.value as File;
      if (!(file instanceof File)) {
        return null;
      }

      // Vérifier le type MIME
      const isValidMimeType = allowedTypes.some(type => {
        if (type.includes('*')) {
          // Support pour les wildcards comme 'image/*'
          const baseType = type.split('/')[0];
          return file.type.startsWith(baseType + '/');
        }
        return file.type === type;
      });

      // Vérifier l'extension
      const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();
      const extensionMap: { [key: string]: string[] } = {
        'application/pdf': ['.pdf'],
        'image/jpeg': ['.jpg', '.jpeg'],
        'image/png': ['.png'],
        'image/gif': ['.gif'],
        'application/msword': ['.doc'],
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
        'application/vnd.ms-excel': ['.xls'],
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx']
      };

      const isValidExtension = allowedTypes.some(type => {
        const validExtensions = extensionMap[type] || [];
        return validExtensions.includes(fileExtension);
      });

      if (!isValidMimeType && !isValidExtension) {
        return { 
          fileType: { 
            actualType: file.type,
            actualExtension: fileExtension,
            allowedTypes: allowedTypes
          } 
        };
      }

      return null;
    };
  }

  /**
   * 📁 Validator pour les fichiers PDF uniquement
   */
  static pdfOnly(control: AbstractControl): ValidationErrors | null {
    return CustomValidators.fileType(['application/pdf'])(control);
  }

  /**
   * 📁 Validator pour les images uniquement
   */
  static imageOnly(control: AbstractControl): ValidationErrors | null {
    return CustomValidators.fileType(['image/jpeg', 'image/png', 'image/gif'])(control);
  }

  // ===== DATE VALIDATORS =====

  /**
   * 📅 Validator pour date minimale
   */
  static minDate(minDate: Date): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const inputDate = new Date(control.value);
      
      if (isNaN(inputDate.getTime())) {
        return { invalidDate: { value: control.value } };
      }

      if (inputDate < minDate) {
        return { 
          minDate: { 
            actualDate: inputDate,
            minDate: minDate,
            actualDateString: inputDate.toLocaleDateString('fr-FR'),
            minDateString: minDate.toLocaleDateString('fr-FR')
          } 
        };
      }

      return null;
    };
  }

  /**
   * 📅 Validator pour date maximale
   */
  static maxDate(maxDate: Date): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) {
        return null;
      }

      const inputDate = new Date(control.value);
      
      if (isNaN(inputDate.getTime())) {
        return { invalidDate: { value: control.value } };
      }

      if (inputDate > maxDate) {
        return { 
          maxDate: { 
            actualDate: inputDate,
            maxDate: maxDate,
            actualDateString: inputDate.toLocaleDateString('fr-FR'),
            maxDateString: maxDate.toLocaleDateString('fr-FR')
          } 
        };
      }

      return null;
    };
  }

  /**
   * 📅 Validator pour date dans le futur
   */
  static futureDate(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const inputDate = new Date(control.value);
    const today = new Date();
    today.setHours(0, 0, 0, 0); // Début de journée

    if (isNaN(inputDate.getTime())) {
      return { invalidDate: { value: control.value } };
    }

    if (inputDate <= today) {
      return { 
        futureDate: { 
          actualDate: inputDate,
          actualDateString: inputDate.toLocaleDateString('fr-FR')
        } 
      };
    }

    return null;
  }

  /**
   * 📅 Validator pour date dans le passé
   */
  static pastDate(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const inputDate = new Date(control.value);
    const today = new Date();
    today.setHours(23, 59, 59, 999); // Fin de journée

    if (isNaN(inputDate.getTime())) {
      return { invalidDate: { value: control.value } };
    }

    if (inputDate >= today) {
      return { 
        pastDate: { 
          actualDate: inputDate,
          actualDateString: inputDate.toLocaleDateString('fr-FR')
        } 
      };
    }

    return null;
  }

  // ===== TEXT VALIDATORS =====

  /**
   * 📝 Validator pour nom/prénom (lettres, espaces, tirets, apostrophes)
   */
  static name(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    // Accepte: lettres (avec accents), espaces, tirets, apostrophes
    const nameRegex = /^[a-zA-ZÀ-ÿ\s\-']+$/;
    
    if (!nameRegex.test(control.value)) {
      return { name: { value: control.value } };
    }

    return null;
  }

  /**
   * 📝 Validator pour code postal français
   */
  static frenchPostalCode(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const postalCodeRegex = /^[0-9]{5}$/;
    
    if (!postalCodeRegex.test(control.value)) {
      return { frenchPostalCode: { value: control.value } };
    }

    return null;
  }

  /**
   * 📝 Validator pour SIRET
   */
  static siret(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const siretRegex = /^[0-9]{14}$/;
    
    if (!siretRegex.test(control.value)) {
      return { siret: { value: control.value } };
    }

    // Vérification de la clé de contrôle (algorithme de Luhn modifié)
    const digits = control.value.split('').map(Number);
    let sum = 0;
    
    for (let i = 0; i < 14; i++) {
      let digit = digits[i];
      if (i % 2 === 1) {
        digit *= 2;
        if (digit > 9) {
          digit = Math.floor(digit / 10) + (digit % 10);
        }
      }
      sum += digit;
    }

    if (sum % 10 !== 0) {
      return { siret: { value: control.value, invalidChecksum: true } };
    }

    return null;
  }

  // ===== PASSWORD VALIDATORS =====

  /**
   * 🔐 Validator pour mot de passe fort
   */
  static strongPassword(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }

    const password = control.value;
    const errors: any = {};

    // Au moins 8 caractères
    if (password.length < 8) {
      errors.minLength = true;
    }

    // Au moins une minuscule
    if (!/[a-z]/.test(password)) {
      errors.lowercase = true;
    }

    // Au moins une majuscule
    if (!/[A-Z]/.test(password)) {
      errors.uppercase = true;
    }

    // Au moins un chiffre
    if (!/[0-9]/.test(password)) {
      errors.number = true;
    }

    // Au moins un caractère spécial
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
      errors.specialChar = true;
    }

    return Object.keys(errors).length > 0 ? { strongPassword: errors } : null;
  }

  // ===== FORM GROUP VALIDATORS =====

  /**
   * 🔐 Validator pour confirmer que deux champs sont identiques
   */
  static matchFields(field1: string, field2: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value1 = control.get(field1)?.value;
      const value2 = control.get(field2)?.value;

      if (value1 && value2 && value1 !== value2) {
        // Ajouter l'erreur au deuxième champ
        const field2Control = control.get(field2);
        if (field2Control) {
          field2Control.setErrors({ ...field2Control.errors, fieldMismatch: true });
        }
        
        return { fieldMismatch: { field1, field2 } };
      }

      // Supprimer l'erreur du deuxième champ si les valeurs correspondent
      const field2Control = control.get(field2);
      if (field2Control?.errors?.['fieldMismatch']) {
        delete field2Control.errors['fieldMismatch'];
        if (Object.keys(field2Control.errors).length === 0) {
          field2Control.setErrors(null);
        }
      }

      return null;
    };
  }

  /**
   * 📅 Validator pour vérifier qu'une date de fin est après une date de début
   */
  static dateRange(startDateField: string, endDateField: string): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startDate = control.get(startDateField)?.value;
      const endDate = control.get(endDateField)?.value;

      if (startDate && endDate) {
        const start = new Date(startDate);
        const end = new Date(endDate);

        if (start >= end) {
          const endDateControl = control.get(endDateField);
          if (endDateControl) {
            endDateControl.setErrors({ ...endDateControl.errors, dateRange: true });
          }
          
          return { dateRange: { startDateField, endDateField } };
        }
      }

      // Supprimer l'erreur si les dates sont valides
      const endDateControl = control.get(endDateField);
      if (endDateControl?.errors?.['dateRange']) {
        delete endDateControl.errors['dateRange'];
        if (Object.keys(endDateControl.errors).length === 0) {
          endDateControl.setErrors(null);
        }
      }

      return null;
    };
  }

  // ===== UTILITY METHODS =====

  /**
   * 🔧 Obtenir le message d'erreur approprié pour un validator
   */
  static getErrorMessage(errors: ValidationErrors, fieldName: string = 'Ce champ'): string {
    if (errors['required']) {
      return `${fieldName} est obligatoire.`;
    }
    
    if (errors['email']) {
      return 'Veuillez saisir une adresse email valide.';
    }
    
    if (errors['academicEmail']) {
      return 'Veuillez utiliser une adresse email académique.';
    }
    
    if (errors['phoneNumber']) {
      return 'Veuillez saisir un numéro de téléphone français valide.';
    }
    
    if (errors['internationalPhone']) {
      return 'Veuillez saisir un numéro de téléphone international valide.';
    }
    
    if (errors['fileSize']) {
      const error = errors['fileSize'];
      return `Le fichier est trop volumineux (${error.actualSizeMB}MB). Taille maximale: ${error.maxSizeMB}MB.`;
    }
    
    if (errors['fileType']) {
      return 'Type de fichier non autorisé.';
    }
    
    if (errors['minDate']) {
      const error = errors['minDate'];
      return `La date doit être postérieure au ${error.minDateString}.`;
    }
    
    if (errors['maxDate']) {
      const error = errors['maxDate'];
      return `La date doit être antérieure au ${error.maxDateString}.`;
    }
    
    if (errors['futureDate']) {
      return 'La date doit être dans le futur.';
    }
    
    if (errors['pastDate']) {
      return 'La date doit être dans le passé.';
    }
    
    if (errors['name']) {
      return 'Seules les lettres, espaces, tirets et apostrophes sont autorisés.';
    }
    
    if (errors['frenchPostalCode']) {
      return 'Veuillez saisir un code postal français valide (5 chiffres).';
    }
    
    if (errors['siret']) {
      return 'Veuillez saisir un numéro SIRET valide (14 chiffres).';
    }
    
    if (errors['strongPassword']) {
      const requirements = [];
      if (errors['strongPassword'].minLength) requirements.push('8 caractères minimum');
      if (errors['strongPassword'].lowercase) requirements.push('une minuscule');
      if (errors['strongPassword'].uppercase) requirements.push('une majuscule');
      if (errors['strongPassword'].number) requirements.push('un chiffre');
      if (errors['strongPassword'].specialChar) requirements.push('un caractère spécial');
      
      return `Le mot de passe doit contenir: ${requirements.join(', ')}.`;
    }
    
    if (errors['fieldMismatch']) {
      return 'Les champs ne correspondent pas.';
    }
    
    if (errors['dateRange']) {
      return 'La date de fin doit être postérieure à la date de début.';
    }
    
    if (errors['minlength']) {
      return `Minimum ${errors['minlength'].requiredLength} caractères.`;
    }
    
    if (errors['maxlength']) {
      return `Maximum ${errors['maxlength'].requiredLength} caractères.`;
    }
    
    if (errors['pattern']) {
      return 'Format invalide.';
    }

    return 'Valeur invalide.';
  }
}