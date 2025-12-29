import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { UserManagementComponent } from './user-management.component';
import { UserService } from '../../../core/services/user.service';
import { UserResponse } from '../../../core/models/user.model';
import { RoleName } from '../../../core/models/role.model';

describe('UserManagementComponent', () => {
  let component: UserManagementComponent;
  let fixture: ComponentFixture<UserManagementComponent>;
  let mockUserService: jasmine.SpyObj<UserService>;

  const mockUsers: UserResponse[] = [
    {
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
    },
    {
      id: 2,
      FirstName: 'Jane',
      LastName: 'Smith',
      email: 'jane.smith@example.com',
      phoneNumber: '0987654321',
      adresse: '456 avenue des Champs',
      ville: 'Lyon',
      pays: 'France',
      enabled: false,
      roles: [{ id: 2, name: RoleName.DIRECTEUR }]
    }
  ];

  beforeEach(async () => {
    const userServiceSpy = jasmine.createSpyObj('UserService', [
      'getAllUsers',
      'createUser',
      'updateUser',
      'deleteUser',
      'toggleUserStatus'
    ]);

    await TestBed.configureTestingModule({
      imports: [
        UserManagementComponent,
        ReactiveFormsModule,
        MatDialogModule,
        MatSnackBarModule,
        NoopAnimationsModule
      ],
      providers: [
        { provide: UserService, useValue: userServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UserManagementComponent);
    component = fixture.componentInstance;
    mockUserService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    
    // Setup default mock responses
    mockUserService.getAllUsers.and.returnValue(of(mockUsers));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load users on init', () => {
    component.ngOnInit();
    
    expect(mockUserService.getAllUsers).toHaveBeenCalled();
    expect(component.users$.value).toEqual(mockUsers);
    expect(component.totalUsers).toBe(2);
  });

  it('should filter users by search term', (done) => {
    component.users$.next(mockUsers);
    component.searchForm.patchValue({ searchTerm: 'john' });
    
    component.filteredUsers$.subscribe(filteredUsers => {
      expect(filteredUsers.length).toBe(1);
      expect(filteredUsers[0].FirstName).toBe('John');
      done();
    });
  });

  it('should filter users by role', () => {
    component.users$.next(mockUsers);
    component.onRoleFilterChange(RoleName.DOCTORANT);
    
    component.filteredUsers$.subscribe(filteredUsers => {
      expect(filteredUsers.length).toBe(1);
      expect(filteredUsers[0].roles[0].name).toBe(RoleName.DOCTORANT);
    });
  });

  it('should filter users by status', () => {
    component.users$.next(mockUsers);
    component.onStatusFilterChange('enabled');
    
    component.filteredUsers$.subscribe(filteredUsers => {
      expect(filteredUsers.length).toBe(1);
      expect(filteredUsers[0].enabled).toBe(true);
    });
  });

  it('should get role label correctly', () => {
    expect(component.getRoleLabel(RoleName.ADMIN)).toBe('Administrateur');
    expect(component.getRoleLabel(RoleName.DIRECTEUR)).toBe('Directeur de thèse');
    expect(component.getRoleLabel(RoleName.DOCTORANT)).toBe('Doctorant');
  });

  it('should get user roles as string', () => {
    const user = mockUsers[0];
    const rolesString = component.getUserRoles(user);
    expect(rolesString).toBe('Doctorant');
  });

  it('should toggle user status', () => {
    const user = mockUsers[0];
    mockUserService.toggleUserStatus.and.returnValue(of(user));
    
    component.toggleUserStatus(user);
    
    expect(mockUserService.toggleUserStatus).toHaveBeenCalledWith(user.id, false);
  });

  it('should handle sort change', () => {
    const sortEvent = { active: 'name', direction: 'desc' as const };
    component.onSortChange(sortEvent);
    
    expect(component.sortField).toBe('name');
    expect(component.sortDirection).toBe('desc');
  });

  it('should handle page change', () => {
    const pageEvent = { pageIndex: 1, pageSize: 20, length: 100 };
    component.onPageChange(pageEvent);
    
    expect(component.pageIndex).toBe(1);
    expect(component.pageSize).toBe(20);
  });
});