import { FormControl, FormGroup } from '@angular/forms';
import { CustomValidators } from './custom-validators';

describe('CustomValidators', () => {

  describe('Email Validators', () => {
    describe('email', () => {
      it('should return null for valid email addresses', () => {
        const validEmails = [
          'test@example.com',
          'user.name@domain.co.uk',
          'firstname+lastname@example.org',
          'email@123.123.123.123', // IP address
          'user_name@example-domain.com'
        ];

        validEmails.forEach(email => {
          const control = new FormControl(email);
          expect(CustomValidators.email(control)).toBeNull();
        });
      });

      it('should return error for invalid email addresses', () => {
        const invalidEmails = [
          'invalid-email',
          '@example.com',
          'user@',
          'user..name@example.com',
          'user@.com',
          'user@com',
          ''
        ];

        invalidEmails.forEach(email => {
          const control = new FormControl(email);
          const result = CustomValidators.email(control);
          if (email === '') {
            expect(result).toBeNull(); // Empty values should be handled by required validator
          } else {
            expect(result).toEqual({ email: { value: email } });
          }
        });
      });

      it('should return null for empty values', () => {
        const control = new FormControl('');
        expect(CustomValidators.email(control)).toBeNull();
        
        const nullControl = new FormControl(null);
        expect(CustomValidators.email(nullControl)).toBeNull();
      });
    });

    describe('academicEmail', () => {
      it('should return null for valid academic email addresses', () => {
        const validAcademicEmails = [
          'professor@university.edu',
          'student@ac.uk',
          'researcher@univ-paris.fr',
          'admin@ens.fr',
          'scientist@cnrs.fr',
          'teacher@college.edu'
        ];

        validAcademicEmails.forEach(email => {
          const control = new FormControl(email);
          expect(CustomValidators.academicEmail(control)).toBeNull();
        });
      });

      it('should return error for non-academic email addresses', () => {
        const nonAcademicEmails = [
          'user@gmail.com',
          'contact@company.com',
          'info@business.org'
        ];

        nonAcademicEmails.forEach(email => {
          const control = new FormControl(email);
          expect(CustomValidators.academicEmail(control)).toEqual({ 
            academicEmail: { value: email } 
          });
        });
      });
    });
  });

  describe('Phone Validators', () => {
    describe('phoneNumber', () => {
      it('should return null for valid French phone numbers', () => {
        const validPhones = [
          '0123456789',
          '01 23 45 67 89',
          '01.23.45.67.89',
          '01-23-45-67-89',
          '+33123456789',
          '+33 1 23 45 67 89'
        ];

        validPhones.forEach(phone => {
          const control = new FormControl(phone);
          expect(CustomValidators.phoneNumber(control)).toBeNull();
        });
      });

      it('should return error for invalid phone numbers', () => {
        const invalidPhones = [
          '123456789', // Too short
          '01234567890', // Too long
          '0023456789', // Invalid area code
          'abcdefghij', // Non-numeric
          '+44123456789' // Wrong country code
        ];

        invalidPhones.forEach(phone => {
          const control = new FormControl(phone);
          expect(CustomValidators.phoneNumber(control)).toEqual({ 
            phoneNumber: { value: phone } 
          });
        });
      });
    });

    describe('internationalPhone', () => {
      it('should return null for valid international phone numbers', () => {
        const validIntlPhones = [
          '+33123456789',
          '+44123456789',
          '+1234567890123'
        ];

        validIntlPhones.forEach(phone => {
          const control = new FormControl(phone);
          expect(CustomValidators.internationalPhone(control)).toBeNull();
        });
      });

      it('should return error for invalid international phone numbers', () => {
        const invalidIntlPhones = [
          '123456789', // No country code
          '+1234', // Too short
          '+123456789012345678' // Too long
        ];

        invalidIntlPhones.forEach(phone => {
          const control = new FormControl(phone);
          expect(CustomValidators.internationalPhone(control)).toEqual({ 
            internationalPhone: { value: phone } 
          });
        });
      });
    });
  });

  describe('File Validators', () => {
    describe('fileSize', () => {
      it('should return null for files within size limit', () => {
        const mockFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
        Object.defineProperty(mockFile, 'size', { value: 1024 * 1024 }); // 1MB

        const control = new FormControl(mockFile);
        const validator = CustomValidators.fileSize(2); // 2MB limit
        expect(validator(control)).toBeNull();
      });

      it('should return error for files exceeding size limit', () => {
        const mockFile = new File(['content'], 'large.pdf', { type: 'application/pdf' });
        Object.defineProperty(mockFile, 'size', { value: 3 * 1024 * 1024 }); // 3MB

        const control = new FormControl(mockFile);
        const validator = CustomValidators.fileSize(2); // 2MB limit
        const result = validator(control);
        
        expect(result).toEqual({
          fileSize: {
            actualSize: 3 * 1024 * 1024,
            maxSize: 2 * 1024 * 1024,
            maxSizeMB: 2,
            actualSizeMB: 3
          }
        });
      });

      it('should return null for non-file values', () => {
        const control = new FormControl('not a file');
        const validator = CustomValidators.fileSize(2);
        expect(validator(control)).toBeNull();
      });
    });

    describe('fileType', () => {
      it('should return null for allowed file types', () => {
        const mockPdfFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
        const control = new FormControl(mockPdfFile);
        const validator = CustomValidators.fileType(['application/pdf']);
        expect(validator(control)).toBeNull();
      });

      it('should return error for disallowed file types', () => {
        const mockTxtFile = new File(['content'], 'test.txt', { type: 'text/plain' });
        const control = new FormControl(mockTxtFile);
        const validator = CustomValidators.fileType(['application/pdf']);
        
        expect(validator(control)).toEqual({
          fileType: {
            actualType: 'text/plain',
            actualExtension: '.txt',
            allowedTypes: ['application/pdf']
          }
        });
      });

      it('should handle wildcard types', () => {
        const mockImageFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
        const control = new FormControl(mockImageFile);
        const validator = CustomValidators.fileType(['image/*']);
        expect(validator(control)).toBeNull();
      });
    });

    describe('pdfOnly', () => {
      it('should return null for PDF files', () => {
        const mockPdfFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
        const control = new FormControl(mockPdfFile);
        expect(CustomValidators.pdfOnly(control)).toBeNull();
      });

      it('should return error for non-PDF files', () => {
        const mockImageFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
        const control = new FormControl(mockImageFile);
        expect(CustomValidators.pdfOnly(control)).toBeTruthy();
      });
    });

    describe('imageOnly', () => {
      it('should return null for image files', () => {
        const mockImageFile = new File(['content'], 'test.jpg', { type: 'image/jpeg' });
        const control = new FormControl(mockImageFile);
        expect(CustomValidators.imageOnly(control)).toBeNull();
      });

      it('should return error for non-image files', () => {
        const mockPdfFile = new File(['content'], 'test.pdf', { type: 'application/pdf' });
        const control = new FormControl(mockPdfFile);
        expect(CustomValidators.imageOnly(control)).toBeTruthy();
      });
    });
  });

  describe('Date Validators', () => {
    describe('minDate', () => {
      it('should return null for dates after minimum date', () => {
        const minDate = new Date('2023-01-01');
        const control = new FormControl('2023-06-01');
        const validator = CustomValidators.minDate(minDate);
        expect(validator(control)).toBeNull();
      });

      it('should return error for dates before minimum date', () => {
        const minDate = new Date('2023-01-01');
        const control = new FormControl('2022-12-31');
        const validator = CustomValidators.minDate(minDate);
        const result = validator(control);
        
        expect(result).toEqual({
          minDate: {
            actualDate: new Date('2022-12-31'),
            minDate: minDate,
            actualDateString: new Date('2022-12-31').toLocaleDateString('fr-FR'),
            minDateString: minDate.toLocaleDateString('fr-FR')
          }
        });
      });

      it('should return error for invalid dates', () => {
        const minDate = new Date('2023-01-01');
        const control = new FormControl('invalid-date');
        const validator = CustomValidators.minDate(minDate);
        expect(validator(control)).toEqual({ invalidDate: { value: 'invalid-date' } });
      });
    });

    describe('futureDate', () => {
      it('should return null for future dates', () => {
        const futureDate = new Date();
        futureDate.setDate(futureDate.getDate() + 1);
        const control = new FormControl(futureDate.toISOString().split('T')[0]);
        expect(CustomValidators.futureDate(control)).toBeNull();
      });

      it('should return error for past dates', () => {
        const pastDate = new Date();
        pastDate.setDate(pastDate.getDate() - 1);
        const control = new FormControl(pastDate.toISOString().split('T')[0]);
        const result = CustomValidators.futureDate(control);
        expect(result).toEqual({
          futureDate: {
            actualDate: jasmine.any(Date),
            actualDateString: jasmine.any(String)
          }
        });
      });
    });

    describe('pastDate', () => {
      it('should return null for past dates', () => {
        const pastDate = new Date();
        pastDate.setDate(pastDate.getDate() - 1);
        const control = new FormControl(pastDate.toISOString().split('T')[0]);
        expect(CustomValidators.pastDate(control)).toBeNull();
      });

      it('should return error for future dates', () => {
        const futureDate = new Date();
        futureDate.setDate(futureDate.getDate() + 1);
        const control = new FormControl(futureDate.toISOString().split('T')[0]);
        const result = CustomValidators.pastDate(control);
        expect(result).toEqual({
          pastDate: {
            actualDate: jasmine.any(Date),
            actualDateString: jasmine.any(String)
          }
        });
      });
    });
  });

  describe('Text Validators', () => {
    describe('name', () => {
      it('should return null for valid names', () => {
        const validNames = [
          'Jean',
          'Marie-Claire',
          'Jean-Baptiste',
          'O\'Connor',
          'José María',
          'François'
        ];

        validNames.forEach(name => {
          const control = new FormControl(name);
          expect(CustomValidators.name(control)).toBeNull();
        });
      });

      it('should return error for invalid names', () => {
        const invalidNames = [
          'Jean123',
          'Marie@Claire',
          'Jean_Baptiste',
          'Name with numbers 123'
        ];

        invalidNames.forEach(name => {
          const control = new FormControl(name);
          expect(CustomValidators.name(control)).toEqual({ name: { value: name } });
        });
      });
    });

    describe('frenchPostalCode', () => {
      it('should return null for valid French postal codes', () => {
        const validCodes = ['75001', '13000', '69000', '33000'];

        validCodes.forEach(code => {
          const control = new FormControl(code);
          expect(CustomValidators.frenchPostalCode(control)).toBeNull();
        });
      });

      it('should return error for invalid postal codes', () => {
        const invalidCodes = ['7500', '750001', 'ABCDE', ''];

        invalidCodes.forEach(code => {
          const control = new FormControl(code);
          if (code === '') {
            expect(CustomValidators.frenchPostalCode(control)).toBeNull();
          } else {
            expect(CustomValidators.frenchPostalCode(control)).toEqual({ 
              frenchPostalCode: { value: code } 
            });
          }
        });
      });
    });
  });

  describe('Password Validators', () => {
    describe('strongPassword', () => {
      it('should return null for strong passwords', () => {
        const strongPasswords = [
          'Password123!',
          'MyStr0ng@Pass',
          'Secure#Pass1'
        ];

        strongPasswords.forEach(password => {
          const control = new FormControl(password);
          expect(CustomValidators.strongPassword(control)).toBeNull();
        });
      });

      it('should return error for weak passwords', () => {
        const weakPasswords = [
          { password: 'weak', expectedErrors: ['minLength', 'uppercase', 'number', 'specialChar'] },
          { password: 'WeakPassword', expectedErrors: ['number', 'specialChar'] },
          { password: 'weakpassword123!', expectedErrors: ['uppercase'] },
          { password: 'WEAKPASSWORD123!', expectedErrors: ['lowercase'] }
        ];

        weakPasswords.forEach(({ password, expectedErrors }) => {
          const control = new FormControl(password);
          const result = CustomValidators.strongPassword(control);
          expect(result).toBeTruthy();
          expect(result!['strongPassword']).toBeTruthy();
          
          expectedErrors.forEach(error => {
            expect(result!['strongPassword'][error]).toBe(true);
          });
        });
      });
    });
  });

  describe('Form Group Validators', () => {
    describe('matchFields', () => {
      it('should return null when fields match', () => {
        const formGroup = new FormGroup({
          password: new FormControl('password123'),
          confirmPassword: new FormControl('password123')
        });

        const validator = CustomValidators.matchFields('password', 'confirmPassword');
        expect(validator(formGroup)).toBeNull();
      });

      it('should return error when fields do not match', () => {
        const formGroup = new FormGroup({
          password: new FormControl('password123'),
          confirmPassword: new FormControl('different')
        });

        const validator = CustomValidators.matchFields('password', 'confirmPassword');
        const result = validator(formGroup);
        
        expect(result).toEqual({
          fieldMismatch: { field1: 'password', field2: 'confirmPassword' }
        });
        expect(formGroup.get('confirmPassword')?.errors?.['fieldMismatch']).toBe(true);
      });
    });

    describe('dateRange', () => {
      it('should return null when end date is after start date', () => {
        const formGroup = new FormGroup({
          startDate: new FormControl('2023-01-01'),
          endDate: new FormControl('2023-12-31')
        });

        const validator = CustomValidators.dateRange('startDate', 'endDate');
        expect(validator(formGroup)).toBeNull();
      });

      it('should return error when end date is before start date', () => {
        const formGroup = new FormGroup({
          startDate: new FormControl('2023-12-31'),
          endDate: new FormControl('2023-01-01')
        });

        const validator = CustomValidators.dateRange('startDate', 'endDate');
        const result = validator(formGroup);
        
        expect(result).toEqual({
          dateRange: { startDateField: 'startDate', endDateField: 'endDate' }
        });
        expect(formGroup.get('endDate')?.errors?.['dateRange']).toBe(true);
      });
    });
  });

  describe('Utility Methods', () => {
    describe('getErrorMessage', () => {
      it('should return appropriate error messages', () => {
        expect(CustomValidators.getErrorMessage({ required: true }))
          .toBe('Ce champ est obligatoire.');
        
        expect(CustomValidators.getErrorMessage({ email: true }))
          .toBe('Veuillez saisir une adresse email valide.');
        
        expect(CustomValidators.getErrorMessage({ phoneNumber: true }))
          .toBe('Veuillez saisir un numéro de téléphone français valide.');
        
        expect(CustomValidators.getErrorMessage({ 
          fileSize: { actualSizeMB: 5, maxSizeMB: 2 } 
        })).toBe('Le fichier est trop volumineux (5MB). Taille maximale: 2MB.');
        
        expect(CustomValidators.getErrorMessage({ fieldMismatch: true }))
          .toBe('Les champs ne correspondent pas.');
        
        expect(CustomValidators.getErrorMessage({ 
          strongPassword: { minLength: true, uppercase: true } 
        })).toContain('Le mot de passe doit contenir:');
      });

      it('should use custom field name in error messages', () => {
        expect(CustomValidators.getErrorMessage({ required: true }, 'Email'))
          .toBe('Email est obligatoire.');
      });

      it('should return default message for unknown errors', () => {
        expect(CustomValidators.getErrorMessage({ unknownError: true }))
          .toBe('Valeur invalide.');
      });
    });
  });
});