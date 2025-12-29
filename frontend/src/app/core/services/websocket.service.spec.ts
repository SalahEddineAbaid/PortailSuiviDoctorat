import { TestBed } from '@angular/core/testing';
import { WebSocketService, WebSocketState, WebSocketMessage } from './websocket.service';

describe('WebSocketService', () => {
  let service: WebSocketService;
  let mockWebSocket: jasmine.SpyObj<WebSocket>;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WebSocketService);

    // Mock WebSocket
    mockWebSocket = jasmine.createSpyObj('WebSocket', ['send', 'close'], {
      readyState: WebSocket.CONNECTING,
      onopen: null,
      onmessage: null,
      onclose: null,
      onerror: null
    });

    // Mock WebSocket constructor
    spyOn(window, 'WebSocket').and.returnValue(mockWebSocket);
  });

  afterEach(() => {
    service.disconnect();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Connection Management', () => {
    it('should connect to WebSocket with URL', (done) => {
      const config = { url: 'ws://localhost:8080/ws' };
      
      service.connect(config).subscribe(state => {
        if (state === WebSocketState.CONNECTING) {
          expect(window.WebSocket).toHaveBeenCalledWith('ws://localhost:8080/ws', undefined);
          done();
        }
      });
    });

    it('should connect with authentication token', (done) => {
      const baseUrl = 'ws://localhost:8080/ws';
      const token = 'test-token';
      
      service.connectWithAuth(baseUrl, token).subscribe(state => {
        if (state === WebSocketState.CONNECTING) {
          expect(window.WebSocket).toHaveBeenCalledWith('ws://localhost:8080/ws?token=test-token', undefined);
          done();
        }
      });
    });

    it('should handle connection open', (done) => {
      const config = { url: 'ws://localhost:8080/ws' };
      
      service.state$.subscribe(state => {
        if (state === WebSocketState.CONNECTED) {
          expect(state).toBe(WebSocketState.CONNECTED);
          done();
        }
      });

      service.connect(config);
      
      // Simulate connection open
      mockWebSocket.readyState = WebSocket.OPEN;
      if (mockWebSocket.onopen) {
        mockWebSocket.onopen(new Event('open'));
      }
    });

    it('should handle connection close', (done) => {
      const config = { url: 'ws://localhost:8080/ws', maxReconnectAttempts: 0 };
      
      service.state$.subscribe(state => {
        if (state === WebSocketState.DISCONNECTED) {
          expect(state).toBe(WebSocketState.DISCONNECTED);
          done();
        }
      });

      service.connect(config);
      
      // Simulate connection close
      if (mockWebSocket.onclose) {
        mockWebSocket.onclose(new CloseEvent('close', { code: 1001 }));
      }
    });

    it('should disconnect properly', () => {
      const config = { url: 'ws://localhost:8080/ws' };
      
      service.connect(config);
      service.disconnect();
      
      expect(mockWebSocket.close).toHaveBeenCalledWith(1000, 'Déconnexion volontaire');
      expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
    });
  });

  describe('Message Handling', () => {
    beforeEach(() => {
      const config = { url: 'ws://localhost:8080/ws' };
      service.connect(config);
      mockWebSocket.readyState = WebSocket.OPEN;
    });

    it('should send messages when connected', () => {
      const message: WebSocketMessage = {
        type: 'TEST',
        data: { test: 'data' }
      };
      
      const result = service.send(message);
      
      expect(result).toBe(true);
      expect(mockWebSocket.send).toHaveBeenCalled();
    });

    it('should not send messages when disconnected', () => {
      mockWebSocket.readyState = WebSocket.CLOSED;
      
      const message: WebSocketMessage = {
        type: 'TEST',
        data: { test: 'data' }
      };
      
      const result = service.send(message);
      
      expect(result).toBe(false);
      expect(mockWebSocket.send).not.toHaveBeenCalled();
    });

    it('should receive and parse messages', (done) => {
      const testMessage = {
        type: 'NOTIFICATION',
        data: { id: 1, title: 'Test' },
        timestamp: new Date()
      };

      service.messages$.subscribe(message => {
        expect(message.type).toBe('NOTIFICATION');
        expect(message.data.id).toBe(1);
        done();
      });

      // Simulate message reception
      if (mockWebSocket.onmessage) {
        const messageEvent = new MessageEvent('message', {
          data: JSON.stringify(testMessage)
        });
        mockWebSocket.onmessage(messageEvent);
      }
    });

    it('should handle ping/pong', () => {
      const result = service.ping();
      expect(result).toBe(true);
      expect(mockWebSocket.send).toHaveBeenCalled();
    });
  });

  describe('State Management', () => {
    it('should return current state', () => {
      expect(service.getState()).toBe(WebSocketState.DISCONNECTED);
    });

    it('should check if connected', () => {
      expect(service.isConnected()).toBe(false);
      
      // Simulate connection
      mockWebSocket.readyState = WebSocket.OPEN;
      service.connect({ url: 'ws://test' });
      
      // Need to trigger state change manually in test
      service['stateSubject'].next(WebSocketState.CONNECTED);
      
      expect(service.isConnected()).toBe(true);
    });

    it('should provide connection statistics', () => {
      const stats = service.getConnectionStats();
      
      expect(stats.state).toBe(WebSocketState.DISCONNECTED);
      expect(stats.reconnectAttempts).toBe(0);
      expect(stats.url).toBeNull();
    });
  });

  describe('Error Handling', () => {
    it('should handle WebSocket errors', (done) => {
      service.errors$.subscribe(error => {
        expect(error).toBeDefined();
        done();
      });

      service.connect({ url: 'ws://localhost:8080/ws' });
      
      // Simulate error
      if (mockWebSocket.onerror) {
        mockWebSocket.onerror(new Event('error'));
      }
    });

    it('should handle malformed messages', (done) => {
      service.messages$.subscribe(message => {
        if (message.type === 'PARSE_ERROR') {
          expect(message.data.rawData).toBe('invalid json');
          done();
        }
      });

      service.connect({ url: 'ws://localhost:8080/ws' });
      
      // Simulate malformed message
      if (mockWebSocket.onmessage) {
        const messageEvent = new MessageEvent('message', {
          data: 'invalid json'
        });
        mockWebSocket.onmessage(messageEvent);
      }
    });
  });

  describe('Reconnection Logic', () => {
    it('should attempt reconnection on unexpected close', (done) => {
      const config = { 
        url: 'ws://localhost:8080/ws',
        maxReconnectAttempts: 1,
        reconnectInterval: 100
      };
      
      let connectCount = 0;
      const originalConnect = service.connect.bind(service);
      spyOn(service, 'connect').and.callFake((cfg) => {
        connectCount++;
        if (connectCount === 2) {
          expect(connectCount).toBe(2); // Initial + 1 reconnect
          done();
        }
        return originalConnect(cfg);
      });

      service.connect(config);
      
      // Simulate unexpected close (not code 1000)
      if (mockWebSocket.onclose) {
        mockWebSocket.onclose(new CloseEvent('close', { code: 1001 }));
      }
    });

    it('should not reconnect after max attempts', (done) => {
      const config = { 
        url: 'ws://localhost:8080/ws',
        maxReconnectAttempts: 1,
        reconnectInterval: 50
      };
      
      service.state$.subscribe(state => {
        if (state === WebSocketState.ERROR) {
          // Should reach error state after max attempts
          done();
        }
      });

      service.connect(config);
      
      // Simulate multiple failures
      setTimeout(() => {
        if (mockWebSocket.onclose) {
          mockWebSocket.onclose(new CloseEvent('close', { code: 1001 }));
        }
      }, 10);
      
      setTimeout(() => {
        if (mockWebSocket.onclose) {
          mockWebSocket.onclose(new CloseEvent('close', { code: 1001 }));
        }
      }, 100);
    });
  });
});