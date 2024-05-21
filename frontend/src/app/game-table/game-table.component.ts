import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from './../model/player';
import { Card } from '../model/card';
import { RaiseInputComponent } from '../raise-input/raise-input.component';

@Component({
  selector: 'app-game-table',
  templateUrl: './game-table.component.html',
  styleUrls: ['./game-table.component.scss']
})
export class GameTableComponent implements OnInit {
  game: Game = { currentPot: 0, players: [], communityCards: [], phase: 'PRE_FLOP' }; // Initialize with default values
  raiseAmount: number = 0;
  playerChips: number = 0;
  nonBotPlayer: Player | undefined;
  currentNonBotPlayerId: string = '';
  showModal: boolean = false;

  @ViewChild(RaiseInputComponent) raiseInputComponent!: RaiseInputComponent;
  @ViewChild('raiseModal') raiseModal!: ElementRef;

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.getGameStatus();
  }

  setCurrentNonBotPlayerId(): void {
    if (this.nonBotPlayer) {
      this.currentNonBotPlayerId = this.nonBotPlayer.id;
    }
  }

  getGameStatus(): void {
    this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
      next: (data) => {
        this.game = data;
        this.setCurrentNonBotPlayerId();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error fetching game status:', error.message);
        alert('An error occurred while fetching game status. Please try again later.');
      }
    });
  }

  getCardImage(card: Card): string {
    if (!card) return '../../assets/cards/back.png';

    let rank = card.value.toLowerCase();
    switch (rank) {
      case 'two': rank = '2'; break;
      case 'three': rank = '3'; break;
      case 'four': rank = '4'; break;
      case 'five': rank = '5'; break;
      case 'six': rank = '6'; break;
      case 'seven': rank = '7'; break;
      case 'eight': rank = '8'; break;
      case 'nine': rank = '9'; break;
      case 'ten': rank = '10'; break;
      case 'jack': rank = 'jack'; break;
      case 'queen': rank = 'queen'; break;
      case 'king': rank = 'king'; break;
      case 'ace': rank = 'ace'; break;
      default: break;
    }

    const suit = card.suit.toLowerCase();

    return `../../assets/cards/${rank}_of_${suit}.png`;
  }

  startGame(): void {
    this.http.post('http://localhost:8080/api/poker/start', []).subscribe({
      next: () => this.getGameStatus(),
      error: (error) => console.error('Error starting game:', error)
    });
  }

  dealFlop(): void {
    this.http.get('http://localhost:8080/api/poker/flop').subscribe({
      next: () => this.getGameStatus(),
      error: (error) => console.error('Error during dealing the flop:', error)
    });
  }

  dealTurn(): void {
    this.http.get('http://localhost:8080/api/poker/turn').subscribe({
      next: () => this.getGameStatus(),
      error: (error) => console.error('Error during dealing the turn:', error)
    });
  }

  dealRiver(): void {
    this.http.get('http://localhost:8080/api/poker/river').subscribe({
      next: () => this.getGameStatus(),
      error: (error) => console.error('Error during dealing the river:', error)
    });
  }

  endGame(): void {
    this.http.get<string>('http://localhost:8080/api/poker/end', { responseType: 'text' as 'json' }).subscribe({
      next: (data) => {
        alert(data);
        this.getGameStatus();
      },
      error: (error) => console.error('Error ending game:', error)
    });
  }

  fold(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);

      this.http.post('http://localhost:8080/api/poker/fold', null, { params: params, responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("Fold successful", response);
          this.getGameStatus();
          this.endGame();
        },
        error: (error) => console.error('Error during fold:', error)
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  showRaiseInput(): void {
    this.raiseInputComponent.showRaiseInput();
  }

  allIn(): void {
    this.raiseInputComponent.allIn();
  }

  closeModal(): void {
    this.showModal = false;
  }

  openModal(): void {
    this.showModal = true;
  }
}
