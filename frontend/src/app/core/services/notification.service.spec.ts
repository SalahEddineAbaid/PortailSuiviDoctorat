import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { NotificationService } from './notification.service';
import { environment } from '../../environments/environment';
import {
  NotificationRequest,
  NotificationResponse,
  NotificationSettings,
  NotificationType,
  WebSocketMessage
} from '../models/notification.model';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;
  const API_URL = `${environment.apiUrl}/notifications`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        NotificationService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    service.disconnectWebSocket();
  });

  describe('HTTP Endpoints', () => {
    it('should get my notifications', () => {
      const mockResponse: NotificationResponse[] = [
        {
          id: 1,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Test Notification',
          message: 'This is a test notification',
          type: NotificationType.INFO,
          dateCreation: new Date(),
          lue: false
        }
      ];

      service.getMyNotifications().subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.length).toBe(1);
        expect(response[0].lue).toBe(false);
      });

      const req = httpMock.expectOne(`${API_URL}/me`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should mark notification as read', () => {
      const notificationId = 1;

      service.markAsRead(notificationId).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${API_URL}/${notificationId}/read`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush({ success: true });
    });

    it('should mark all notifications as read', () => {
      service.markAllAsRead().subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${API_URL}/read-all`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({});
      req.flush({ success: true });
    });

    it('should delete notification', () => {
      const notificationId = 1;

      service.deleteNotification(notificationId).subscribe(response => {
        expect(response).toBeTruthy();
      });

      const req = httpMock.expectOne(`${API_URL}/${notificationId}`);
      expect(req.request.method).toBe('DELETE');
      req.flush({ success: true });
    });

    it('should get notification settings', () => {
      const mockResponse: NotificationSettings = {
        emailEnabled: true,
        pushEnabled: true,
        inscriptionNotifications: true,
        soutenanceNotifications: true,
        adminNotifications: false
      };

      service.getNotificationSettings().subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.emailEnabled).toBe(true);
      });

      const req = httpMock.expectOne(`${API_URL}/settings`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should update notification settings', () => {
      const mockSettings: NotificationSettings = {
        emailEnabled: false,
        pushEnabled: true,
        inscriptionNotifications: true,
        soutenanceNotifications: false,
        adminNotifications: true
      };

      service.updateNotificationSettings(mockSettings).subscribe(response => {
        expect(response).toEqual(mockSettings);
        expect(response.emailEnabled).toBe(false);
      });

      const req = httpMock.expectOne(`${API_URL}/settings`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(mockSettings);
      req.flush(mockSettings);
    });

    it('should send notification', () => {
      const mockRequest: NotificationRequest = {
        destinataireId: 1,
        titre: 'Admin Notification',
        message: 'This is an admin notification',
        type: NotificationType.WARNING
      };

      const mockResponse: NotificationResponse = {
        id: 1,
        destinataire: {
          id: 1,
          FirstName: 'John',
          LastName: 'Doe',
          email: 'john@test.com',
          phoneNumber: '0123456789',
          adresse: 'Test Address',
          ville: 'Test City',
          pays: 'France',
          enabled: true,
          roles: []
        },
        titre: 'Admin Notification',
        message: 'This is an admin notification',
        type: NotificationType.WARNING,
        dateCreation: new Date(),
        lue: false
      };

      service.sendNotification(mockRequest).subscribe(response => {
        expect(response).toEqual(mockResponse);
        expect(response.type).toBe(NotificationType.WARNING);
      });

      const req = httpMock.expectOne(API_URL);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(mockRequest);
      req.flush(mockResponse);
    });
  });

  describe('State Management', () => {
    it('should update notifications state when marking as read', () => {
      const notificationId = 1;
      const initialNotifications: NotificationResponse[] = [
        {
          id: 1,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Test Notification',
          message: 'This is a test notification',
          type: NotificationType.INFO,
          dateCreation: new Date(),
          lue: false
        }
      ];

      // Mock the initial load
      service.getMyNotifications().subscribe();
      const initialReq = httpMock.expectOne(`${API_URL}/me`);
      initialReq.flush(initialNotifications);

      // Test marking as read
      service.markNotificationAsRead(notificationId);
      const markReadReq = httpMock.expectOne(`${API_URL}/${notificationId}/read`);
      markReadReq.flush({ success: true });

      // Verify state update
      service.notifications$.subscribe(notifications => {
        const notification = notifications.find(n => n.id === notificationId);
        expect(notification?.lue).toBe(true);
        expect(notification?.dateLecture).toBeDefined();
      });
    });

    it('should update unread count when notifications change', () => {
      const notifications: NotificationResponse[] = [
        {
          id: 1,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Unread Notification 1',
          message: 'This is unread',
          type: NotificationType.INFO,
          dateCreation: new Date(),
          lue: false
        },
        {
          id: 2,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Read Notification',
          message: 'This is read',
          type: NotificationType.SUCCESS,
          dateCreation: new Date(),
          lue: true,
          dateLecture: new Date()
        }
      ];

      service.getMyNotifications().subscribe();
      const req = httpMock.expectOne(`${API_URL}/me`);
      req.flush(notifications);

      service.unreadCount$.subscribe(count => {
        expect(count).toBe(1);
      });
    });

    it('should remove notification from state when deleted', () => {
      const notificationId = 1;
      const initialNotifications: NotificationResponse[] = [
        {
          id: 1,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Test Notification',
          message: 'This will be deleted',
          type: NotificationType.INFO,
          dateCreation: new Date(),
          lue: false
        }
      ];

      // Mock initial load
      service.getMyNotifications().subscribe();
      const initialReq = httpMock.expectOne(`${API_URL}/me`);
      initialReq.flush(initialNotifications);

      // Test deletion
      service.removeNotification(notificationId);
      const deleteReq = httpMock.expectOne(`${API_URL}/${notificationId}`);
      deleteReq.flush({ success: true });

      // Verify state update
      service.notifications$.subscribe(notifications => {
        expect(notifications.length).toBe(0);
        expect(notifications.find(n => n.id === notificationId)).toBeUndefined();
      });
    });
  });

  describe('Utility Methods', () => {
    it('should get notification icon by type', () => {
      expect(service.getNotificationIcon(NotificationType.INFO)).toBe('info');
      expect(service.getNotificationIcon(NotificationType.SUCCESS)).toBe('check_circle');
      expect(service.getNotificationIcon(NotificationType.WARNING)).toBe('warning');
      expect(service.getNotificationIcon(NotificationType.ERROR)).toBe('error');
      expect(service.getNotificationIcon(NotificationType.REMINDER)).toBe('schedule');
    });

    it('should get notification color by type', () => {
      expect(service.getNotificationColor(NotificationType.INFO)).toBe('blue');
      expect(service.getNotificationColor(NotificationType.SUCCESS)).toBe('green');
      expect(service.getNotificationColor(NotificationType.WARNING)).toBe('orange');
      expect(service.getNotificationColor(NotificationType.ERROR)).toBe('red');
      expect(service.getNotificationColor(NotificationType.REMINDER)).toBe('purple');
    });

    it('should get current notifications', () => {
      const mockNotifications: NotificationResponse[] = [
        {
          id: 1,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Test Notification',
          message: 'Test message',
          type: NotificationType.INFO,
          dateCreation: new Date(),
          lue: false
        }
      ];

      service.getMyNotifications().subscribe();
      const req = httpMock.expectOne(`${API_URL}/me`);
      req.flush(mockNotifications);

      const currentNotifications = service.getCurrentNotifications();
      expect(currentNotifications).toEqual(mockNotifications);
    });

    it('should get unread count', () => {
      const mockNotifications: NotificationResponse[] = [
        {
          id: 1,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Unread 1',
          message: 'Test message',
          type: NotificationType.INFO,
          dateCreation: new Date(),
          lue: false
        },
        {
          id: 2,
          destinataire: {
            id: 1,
            FirstName: 'John',
            LastName: 'Doe',
            email: 'john@test.com',
            phoneNumber: '0123456789',
            adresse: 'Test Address',
            ville: 'Test City',
            pays: 'France',
            enabled: true,
            roles: []
          },
          titre: 'Read',
          message: 'Test message',
          type: NotificationType.SUCCESS,
          dateCreation: new Date(),
          lue: true,
          dateLecture: new Date()
        }
      ];

      service.getMyNotifications().subscribe();
      const req = httpMock.expectOne(`${API_URL}/me`);
      req.flush(mockNotifications);

      expect(service.getUnreadCount()).toBe(1);
    });
  });

  describe('WebSocket Management', () => {
    beforeEach(() => {
      // Mock localStorage for token
      spyOn(localStorage, 'getItem').and.returnValue('mock-token');
    });

    it('should not connect websocket without token', () => {
      (localStorage.getItem as jasmine.Spy).and.returnValue(null);
      
      spyOn(console, 'warn');
      service.connectWebSocket();
      
      expect(console.warn).toHaveBeenCalledWith('⚠️ [NOTIFICATION SERVICE] Pas de token pour WebSocket');
    });

    it('should handle websocket messages', (done) => {
      const mockMessage: WebSocketMessage = {
        type: 'NOTIFICATION',
        data: {
          id: 1,
          titre: 'New Notification',
          message: 'WebSocket notification',
          type: NotificationType.INFO
        },
        timestamp: new Date()
      };

      service.websocketMessages$.subscribe(message => {
        expect(message.type).toBe('NOTIFICATION');
        expect(message.data.titre).toBe('New Notification');
        done();
      });

      // Simulate WebSocket message reception
      service['websocketSubject'].next(mockMessage);
    });
  });

  describe('Error Handling', () => {
    it('should handle HTTP 400 error', () => {
      const invalidRequest: NotificationRequest = {
        destinataireId: 0,
        titre: '',
        message: '',
        type: NotificationType.INFO
      };

      service.sendNotification(invalidRequest).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(400);
          expect(error.error.message).toBe('Invalid notification data');
        }
      });

      const req = httpMock.expectOne(API_URL);
      req.flush({ message: 'Invalid notification data' }, { status: 400, statusText: 'Bad Request' });
    });

    it('should handle HTTP 404 error', () => {
      const notificationId = 999;

      service.markAsRead(notificationId).subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
          expect(error.error.message).toBe('Notification not found');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/${notificationId}/read`);
      req.flush({ message: 'Notification not found' }, { status: 404, statusText: 'Not Found' });
    });

    it('should handle HTTP 403 error', () => {
      service.getMyNotifications().subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(403);
          expect(error.error.message).toBe('Access denied');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/me`);
      req.flush({ message: 'Access denied' }, { status: 403, statusText: 'Forbidden' });
    });

    it('should handle HTTP 500 error', () => {
      service.getNotificationSettings().subscribe({
        next: () => fail('Should have failed'),
        error: (error) => {
          expect(error.status).toBe(500);
          expect(error.error.message).toBe('Internal server error');
        }
      });

      const req = httpMock.expectOne(`${API_URL}/settings`);
      req.flush({ message: 'Internal server error' }, { status: 500, statusText: 'Internal Server Error' });
    });
  });
});