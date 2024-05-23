import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from '../model/player';
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
  gameResultMessage: string = '';
  playerActionTaken: boolean = false;

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
        this.sortPlayers();
        this.playerActionTaken = this.isFolded() || this.playerActionTaken;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error fetching game status:', error.message);
        alert('An error occurred while fetching game status. Please try again later.');
      }
    });
  }

  sortPlayers(): void {
    const nonBotPlayers = this.game.players.filter(player => !player.name?.startsWith('Bot'));
    const botPlayers = this.game.players.filter(player => player.name?.startsWith('Bot'));
    this.game.players = [...botPlayers, ...nonBotPlayers];
    this.nonBotPlayer = nonBotPlayers.length ? nonBotPlayers[0] : undefined;
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
    if (this.playerActionTaken) {
      this.http.get('http://localhost:8080/api/poker/flop').subscribe({
        next: () => this.getGameStatus(),
        error: (error) => console.error('Error during dealing the flop:', error)
      });
    } else {
      alert('You must take an action before proceeding to the next phase.');
    }
  }

  dealTurn(): void {
    if (this.playerActionTaken) {
      this.http.get('http://localhost:8080/api/poker/turn').subscribe({
        next: () => this.getGameStatus(),
        error: (error) => console.error('Error during dealing the turn:', error)
      });
    } else {
      alert('You must take an action before proceeding to the next phase.');
    }
  }

  dealRiver(): void {
    if (this.playerActionTaken) {
      this.http.get('http://localhost:8080/api/poker/river').subscribe({
        next: () => this.getGameStatus(),
        error: (error) => console.error('Error during dealing the river:', error)
      });
    } else {
      alert('You must take an action before proceeding to the next phase.');
    }
  }

  endGame(): void {
    this.http.get('http://localhost:8080/api/poker/end', { responseType: 'text' }).subscribe(
      (response) => {
        this.gameResultMessage = response;
        this.showModal = true;
      },
      (error) => {
        this.gameResultMessage = 'Game end failed or no winner.';
        this.showModal = true;
      }
    );
  }

  fold(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);

      this.http.post('http://localhost:8080/api/poker/fold', null, { params: params, responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("Fold successful", response);
          this.playerActionTaken = true;
          this.getGameStatus();
        },
        error: (error) => console.error('Error during fold:', error)
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  isFolded(): boolean {
    return this.nonBotPlayer ? this.nonBotPlayer.folded : false;
  }

  showRaiseInput(): void {
    this.raiseInputComponent.showRaiseInput();
  }

  handleRaiseAction(): void {
    this.playerActionTaken = true;
    this.getGameStatus(); // Ensure we fetch the latest game status after a raise
  }

  handleCheckAction(): void {
    this.playerActionTaken = true;
  }

  allIn(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);
      this.http.post('http://localhost:8080/api/poker/bet', null, { params: params, responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("All-in successful", response);
          this.playerActionTaken = true;
          this.getGameStatus();
        },
        error: (error) => console.error('Error during all-in:', error)
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  closeModal(): void {
    this.showModal = false;
  }

  openModal(): void {
    this.showModal = true;
  }

  resetGame(): void {
    this.http.post('http://localhost:8080/api/poker/reset', {}).subscribe({
      next: () => {
        this.getGameStatus();
        this.closeModal();
      },
      error: (error) => console.error('Error during game reset:', error)
    });
  }

  startNewMatch(): void {
    this.http.post<Game>('http://localhost:8080/api/poker/new-match', {}).subscribe({
      next: (response) => {
        if (response && response.players && response.players.length > 0) { // Check if response contains players
          this.game = response;
          this.playerActionTaken = false; // Reset player action flag
          this.closeModal();
        } else {
          alert('No players with chips left or failed to start new match.');
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error during new match start:', error);
        alert('An error occurred while starting a new match. Please try again later.');
      }
    });
  }


}
