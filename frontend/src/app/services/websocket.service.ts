import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { Game } from '../model/game';

declare var SockJS: any;
declare var Stomp: any;

export interface GameUpdateMessage {
  type: string;
  gameState: Game | null;
  message: string;
  timestamp: number;
}

export interface PlayerActionMessage {
  playerId: string;
  action: string;
  amount: number;
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private readonly WEBSOCKET_URL = 'http://localhost:8080/ws';
  
  private stompClient: any;
  private connected = false;
  private gameId: string | null = null;

  private connectionStatusSubject = new BehaviorSubject<boolean>(false);
  public connectionStatus$ = this.connectionStatusSubject.asObservable();

  private gameUpdatesSubject = new Subject<GameUpdateMessage>();
  public gameUpdates$ = this.gameUpdatesSubject.asObservable();

  private errorSubject = new Subject<string>();
  public errors$ = this.errorSubject.asObservable();

  constructor(private authService: AuthService) {
    // Auto-connect when authenticated
    this.authService.isAuthenticated$.subscribe(isAuth => {
      if (isAuth && !this.connected) {
        this.connect();
      } else if (!isAuth && this.connected) {
        this.disconnect();
      }
    });
  }

  connect(): void {
    if (this.connected) {
      return;
    }

    const token = this.authService.getToken();
    if (!token) {
      console.warn('Cannot connect WebSocket: No authentication token');
      return;
    }

    try {
      const socket = new SockJS(this.WEBSOCKET_URL);
      this.stompClient = Stomp.over(socket);

      // Configure headers with auth token
      const headers = {
        'Authorization': `Bearer ${token}`
      };

      this.stompClient.connect(headers, 
        (frame: any) => {
          console.log('WebSocket connected:', frame);
          this.connected = true;
          this.connectionStatusSubject.next(true);
        },
        (error: any) => {
          console.error('WebSocket connection error:', error);
          this.handleConnectionError(error);
        }
      );

      // Handle disconnection
      this.stompClient.ws.onclose = () => {
        console.log('WebSocket disconnected');
        this.connected = false;
        this.connectionStatusSubject.next(false);
        
        // Auto-reconnect after 5 seconds if still authenticated
        if (this.authService.isAuthenticated()) {
          setTimeout(() => {
            if (!this.connected) {
              this.connect();
            }
          }, 5000);
        }
      };

    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      this.handleConnectionError(error);
    }
  }

  disconnect(): void {
    if (this.stompClient && this.connected) {
      this.stompClient.disconnect(() => {
        console.log('WebSocket disconnected');
      });
      
      this.connected = false;
      this.gameId = null;
      this.connectionStatusSubject.next(false);
    }
  }

  subscribeToGame(gameId: string): void {
    if (!this.connected || !this.stompClient) {
      console.warn('Cannot subscribe to game: WebSocket not connected');
      return;
    }

    // Unsubscribe from previous game if any
    if (this.gameId && this.gameId !== gameId) {
      this.unsubscribeFromGame();
    }

    this.gameId = gameId;

    // Subscribe to game updates
    this.stompClient.subscribe(`/topic/game/${gameId}`, (message: any) => {
      try {
        const gameUpdate: GameUpdateMessage = JSON.parse(message.body);
        this.gameUpdatesSubject.next(gameUpdate);
      } catch (error) {
        console.error('Error parsing game update message:', error);
        this.errorSubject.next('Failed to parse game update');
      }
    });

    // Subscribe to user-specific messages
    this.stompClient.subscribe('/user/queue/messages', (message: any) => {
      try {
        const userMessage = JSON.parse(message.body);
        console.log('Received personal message:', userMessage);
        // Handle personal messages (notifications, errors, etc.)
      } catch (error) {
        console.error('Error parsing personal message:', error);
      }
    });

    console.log(`Subscribed to game: ${gameId}`);
  }

  unsubscribeFromGame(): void {
    if (this.gameId && this.stompClient && this.connected) {
      // Note: In a real implementation, you'd track subscriptions to properly unsubscribe
      this.gameId = null;
      console.log('Unsubscribed from game');
    }
  }

  sendPlayerAction(action: PlayerActionMessage): void {
    if (!this.connected || !this.stompClient || !this.gameId) {
      console.warn('Cannot send player action: WebSocket not connected or no game selected');
      return;
    }

    try {
      this.stompClient.send(
        `/app/game/${this.gameId}/action`,
        {},
        JSON.stringify(action)
      );
      console.log('Player action sent:', action);
    } catch (error) {
      console.error('Failed to send player action:', error);
      this.errorSubject.next('Failed to send player action');
    }
  }

  joinGame(playerName: string): void {
    if (!this.connected || !this.stompClient || !this.gameId) {
      console.warn('Cannot join game: WebSocket not connected or no game selected');
      return;
    }

    try {
      this.stompClient.send(
        `/app/game/${this.gameId}/join`,
        {},
        JSON.stringify(playerName)
      );
      console.log('Joined game:', this.gameId);
    } catch (error) {
      console.error('Failed to join game:', error);
      this.errorSubject.next('Failed to join game');
    }
  }

  leaveGame(playerName: string): void {
    if (!this.connected || !this.stompClient || !this.gameId) {
      return;
    }

    try {
      this.stompClient.send(
        `/app/game/${this.gameId}/leave`,
        {},
        JSON.stringify(playerName)
      );
      console.log('Left game:', this.gameId);
    } catch (error) {
      console.error('Failed to leave game:', error);
    } finally {
      this.unsubscribeFromGame();
    }
  }

  private handleConnectionError(error: any): void {
    this.connected = false;
    this.connectionStatusSubject.next(false);
    
    let errorMessage = 'WebSocket connection failed';
    if (error && error.message) {
      errorMessage += ': ' + error.message;
    }
    
    this.errorSubject.next(errorMessage);
  }

  // Getters
  isConnected(): boolean {
    return this.connected;
  }

  getCurrentGameId(): string | null {
    return this.gameId;
  }
}
