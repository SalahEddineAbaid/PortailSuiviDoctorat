import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { UserFormComponent, UserFormData } from './user-form.component';
import { UserResponse } from '../../../../core/models/user.model';
import { RoleName } from '../../../../core/models/role.model';

describe('UserFormComponent', () => {
  let component: UserFormComponent;
  let fixture: ComponentFixture<UserFormComponent>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<UserFormComponent>>;

  const mockUser: UserResponse = {
    id: 1,
    FirstName: 'John',
    LastName: 'Doe',
    email: 'john.doe@example.com',
    phoneNumber: '0123456789',
    adresse: '123 rue de la Paix',
    ville: 'Paris',
    pays: 'France',
    enabled: true,
    roles: [{ id: 1, name: RoleName.DOCTORANT }]
  };

  beforeEach(async () => {
    const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      imports: [
        UserFormComponent,
        ReactiveFormsModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: { mode: 'create' } as UserFormData }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserFormComponent);
    component = fixture.componentInstance;
    mockDialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<UserFormComponent>>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form in create mode', () => {
    component.ngOnInit();
    
    expect(component.isEditMode).toBe(false);
    expect(component.userForm.get('password')).toBeTruthy();
    expect(component.userForm.get('confirmPassword')).toBeTruthy();
    expect(component.userForm.get('enabled')).toBeFalsy();
  });

  it('should initialize form in edit mode', () => {
    component.data = { mode: 'edit', user: mockUser };
    component.isEditMode = true;
    component.userForm = component['createForm']();
    component.ngOnInit();
    
    expect(component.isEditMode).toBe(true);
    expect(component.userForm.get('password')).toBeFalsy();
    expect(component.userForm.get('confirmPassword')).toBeFalsy();
    expect(component.userForm.get('enabled')).toBeTruthy();
  });

  it('should populate form with user data in edit mode', () => {
    component.data = { mode: 'edit', user: mockUser };
    component.isEditMode = true;
    component.userForm = component['createForm']();
    component.ngOnInit();
    
    expect(component.userForm.get('FirstName')?.value).toBe('John');
    expect(component.userForm.get('LastName')?.value).toBe('Doe');
    expect(component.userForm.get('email')?.value).toBe('john.doe@example.com');
    expect(component.userForm.get('enabled')?.value).toBe(true);
  });

  it('should validate required fields', () => {
    component.ngOnInit();
    
    expect(component.userForm.get('FirstName')?.hasError('required')).toBe(true);
    expect(component.userForm.get('LastName')?.hasError('required')).toBe(true);
    expect(component.userForm.get('email')?.hasError('required')).toBe(true);
    expect(component.userForm.get('roles')?.hasError('required')).toBe(true);
  });

  it('should validate password match in create mode', () => {
    component.ngOnInit();
    
    component.userForm.patchValue({
      password: 'Password123!',
      confirmPassword: 'DifferentPassword123!'
    });
    
    expect(component.userForm.hasError('passwordMismatch')).toBe(true);
  });

  it('should get correct field errors', () => {
    component.ngOnInit();
    
    const firstNameControl = component.userForm.get('FirstName');
    firstNameControl?.markAsTouched();
    
    expect(component.getFieldError('FirstName')).toBe('Ce champ est obligatoire');
  });

  it('should get role labels correctly', () => {
    expect(component.getRoleLabel(RoleName.ADMIN)).toBe('Administrateur');
    expect(component.getRoleLabel(RoleName.DIRECTEUR)).toBe('Directeur de thèse');
    expect(component.getRoleLabel(RoleName.DOCTORANT)).toBe('Doctorant');
  });

  it('should return correct title for create mode', () => {
    component.isEditMode = false;
    expect(component.title).toBe('Créer un utilisateur');
  });

  it('should return correct title for edit mode', () => {
    component.isEditMode = true;
    expect(component.title).toBe('Modifier l\'utilisateur');
  });

  it('should return correct submit button text', () => {
    component.isEditMode = false;
    expect(component.submitButtonText).toBe('Créer');
    
    component.isEditMode = true;
    expect(component.submitButtonText).toBe('Mettre à jour');
  });

  it('should close dialog on cancel', () => {
    component.onCancel();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it('should submit valid form in create mode', () => {
    component.ngOnInit();
    
    component.userForm.patchValue({
      FirstName: 'John',
      LastName: 'Doe',
      email: 'john.doe@example.com',
      phoneNumber: '0123456789',
      adresse: '123 rue de la Paix',
      ville: 'Paris',
      pays: 'France',
      password: 'Password123!',
      confirmPassword: 'Password123!',
      roles: [RoleName.DOCTORANT]
    });
    
    component.onSubmit();
    
    expect(mockDialogRef.close).toHaveBeenCalledWith(jasmine.objectContaining({
      FirstName: 'John',
      LastName: 'Doe',
      email: 'john.doe@example.com',
      password: 'Password123!',
      roles: [RoleName.DOCTORANT]
    }));
  });
});