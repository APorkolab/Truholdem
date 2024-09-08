import { RaiseInputComponent } from './../raise-input/raise-input.component';
import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';
import { PlayerService } from '../services/player.service';
import { PlayerInfo } from '../register-players/register-players.component';

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
  currentPot = 0;
  playerActions: Map<string, boolean> = new Map();
  players: PlayerInfo[] = [];
  @ViewChild(RaiseInputComponent) raiseInputComponent!: RaiseInputComponent;
  @ViewChild('raiseModal') raiseModal!: ElementRef;

  constructor(private http: HttpClient, private playerService: PlayerService) { }

  ngOnInit(): void {
    this.players = this.playerService.getPlayers();
    this.getGameStatus();
  }

  setCurrentNonBotPlayerId(): void {
    if (this.nonBotPlayer) {
      this.currentNonBotPlayerId = this.nonBotPlayer.id;
    }
  }

  getGameStatus(): void {
    this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
      next: (data: Game) => {
        if (data && data.players) {
          this.game = data;
          this.setCurrentNonBotPlayerId();
          this.sortPlayers();
          this.updateCurrentPot();
          this.checkAllPlayersActionTaken();
        } else {
          console.error('Received game status does not contain players');
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('Hiba a játék állapotának lekérésekor:', error.message);
        if (error.status === 404) {
          this.startNewGame();
        } else {
          alert('Hiba történt a játék állapotának lekérésekor. Kérjük, próbálja újra később.');
        }
      }
    });
  }

  sortPlayers(): void {
    if (this.game && this.game.players) {
      const nonBotPlayers = this.game.players.filter(player => !player.name?.startsWith('Bot'));
      const botPlayers = this.game.players.filter(player => player.name?.startsWith('Bot'));
      this.game.players = [...botPlayers, ...nonBotPlayers];
      this.nonBotPlayer = nonBotPlayers.length ? nonBotPlayers[0] : undefined;
    } else {
      console.error('Players list is undefined or null');
    }
  }

  dealFlop() {
    this.http.get<Game>('http://localhost:8080/api/poker/flop').subscribe(
      (data: Game) => {
        this.updateGameStatus(data);
        console.log('Community cards after flop:', this.game.communityCards);
      },
      (error: HttpErrorResponse) => {
        console.error('Error during dealing the flop:', error);
        if (error.status === 400) {
          alert('Invalid request to deal the flop. Please check the game state and try again.');
        } else {
          alert('An unexpected error occurred. Please try again later.');
        }
      }
    );
  }

  dealTurn() {
    this.http.get('http://localhost:8080/api/poker/turn').subscribe(
      (data: any) => {
        this.updateGameStatus(data);
        this.calculateCurrentPot();
      },
      (error: HttpErrorResponse) => {
        console.error('Error during dealing the turn:', error);
        if (error.status === 400) {
          alert('Invalid request to deal the turn. Please check the game state and try again.');
        } else {
          alert('An unexpected error occurred. Please try again later.');
        }
      }
    );
  }

  dealRiver() {
    this.http.get<Game>('http://localhost:8080/api/poker/river').subscribe({
      next: (data: Game) => {
        this.updateGameStatus(data);
        this.calculateCurrentPot();
        if (this.checkAllPlayersActionTaken()) {
          this.endGame();
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error during dealing the river:', error.message);
        if (error.status === 400) {
          alert('Cannot deal river at this phase or some players have not acted.');
        } else {
          alert('An unexpected error occurred. Please try again later.');
        }
      }
    });
  }

  endGame(): void {
    this.http.get('http://localhost:8080/api/poker/end', { responseType: 'text' }).subscribe(
      (response: string) => {
        this.gameResultMessage = `The winner is: ${response}`;
        this.showModal = true;
        this.updateCurrentPot();
      },
      (error) => {
        this.gameResultMessage = 'Game end failed or no winner.';
        this.showModal = true;
        this.updateCurrentPot();
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
          this.progressToNextPhase();
        },
        error: (error) => console.error('Error during fold:', error)
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  progressToNextPhase(): void {
    switch (this.game.phase) {
      case 'PRE_FLOP':
        this.dealFlop();
        break;
      case 'FLOP':
        this.dealTurn();
        break;
      case 'TURN':
        this.dealRiver();
        break;
      case 'RIVER':
        if (this.checkAllPlayersActionTaken()) {
          this.endGame();
        } else {
          console.log('Waiting for players to finish their actions in the River phase.');
          alert('Final betting round after the River.');
        }
        break;
      case 'SHOWDOWN':
        this.endGame();
        break;
    }
  }

  checkAllPlayersActionTaken(): boolean {
    const allActionsTaken = this.game.players.every(player => this.playerActions.get(player.id) === true);
    console.log('All players actions taken: ', allActionsTaken);
    return allActionsTaken;
  }

  updatePlayerAction(playerId: string): void {
    this.playerActions.set(playerId, true);
    if (this.checkAllPlayersActionTaken()) {
      this.proceedToNextPhase();
    }
  }

  proceedToNextPhase(): void {
    switch (this.game.phase) {
      case 'PRE_FLOP':
        this.dealFlop();
        break;
      case 'FLOP':
        this.dealTurn();
        break;
      case 'TURN':
        this.dealRiver();
        break;
      case 'RIVER':
        this.endGame();
        break;
    }
  }

  playerRaise(playerId: string, amount: number): void {
    const payload = { playerId, amount };
    this.http.post('http://localhost:8080/api/poker/raise', payload).subscribe({
      next: () => {
        this.updatePlayerAction(playerId);
        this.getGameStatus();
        this.updateCurrentPot();
      },
      error: (error) => {
        console.error('Error during player raise:', error);
      }
    });
  }

  playerBet(playerId: string, amount: number): void {
    const payload = { playerId, amount };
    this.http.post('http://localhost:8080/api/poker/bet', payload).subscribe({
      next: () => {
        this.updatePlayerAction(playerId);
        this.getGameStatus();
        this.updateCurrentPot();
      },
      error: (error) => {
        console.error('Error during player bet:', error);
      }
    });
  }

  calculateCurrentPot(): number {
    return this.game.players.reduce((total, player) => total + (player.betAmount || 0), 0);
  }

  updateCurrentPot(): void {
    this.currentPot = this.calculateCurrentPot();
  }

  check(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const currentBet = Math.max(...this.game.players.map(player => player.betAmount || 0));

      if (this.nonBotPlayer.betAmount === currentBet) {
        const params = new HttpParams().set('playerId', this.nonBotPlayer.id);
        this.http.post('http://localhost:8080/api/poker/check', null, { params: params, responseType: 'text' }).subscribe({
          next: (response) => {
            console.log("Check successful", response);
            this.playerActionTaken = true;
            this.getGameStatus();
            this.progressToNextPhase();
          },
          error: (error: HttpErrorResponse) => {
            console.error('Error during check:', error.message);
          }
        });
      } else {
        alert('Nem checkelhet, ha a tét magasabb a jelenlegi betétnél.');
      }
    } else {
      console.error('No non-bot player found');
    }
  }

  allIn(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);
      this.http.post('http://localhost:8080/api/poker/bet', { playerId: this.nonBotPlayer.id, amount: this.nonBotPlayer.chips }, { responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("All-in successful", response);
          this.playerActionTaken = true;
          this.getGameStatus();
          this.updateCurrentPot();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error during all-in:', error.message);
        }
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  resetGame(): void {
    this.http.post('http://localhost:8080/api/poker/reset', {}).subscribe({
      next: () => {
        this.getGameStatus();
        this.closeModal();
        this.updateCurrentPot();
        this.nonBotPlayer!.folded = false;
      },
      error: (error) => console.error('Error during game reset:', error)
    });
  }

  startNewMatch(): void {
    // Ellenőrizzük, hogy a nem bot játékos dobott-e
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer && this.nonBotPlayer.folded) {
      // Ha a nem bot játékos dobott, kezdünk egy teljes új játékot
      this.startNewGame();
    } else {
      // Ellenkező esetben új meccset kezdünk
      this.http.post<Game>('http://localhost:8080/api/poker/new-match', {}).subscribe({
        next: (response) => {
          if (response && response.players && response.players.length > 0) {
            this.game = response;
            this.playerActionTaken = false; // Reset player action flag
            this.closeModal();
            this.updateCurrentPot(); // Update pot on new match
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

  startNewGame(): void {
    this.http.post<Game>('http://localhost:8080/api/poker/start', []).subscribe({
      next: (response) => {
        if (response && response.players && response.players.length > 0) {
          this.game = response;
          this.playerActionTaken = false;
          this.closeModal();
          this.currentPot = 0;
        } else {
          alert('Failed to start new game.');
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error during new game start:', error);
        alert('An error occurred while starting a new game. Please try again later.');
      }
    });
  }

  openModal(): void {
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
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

  updateGameStatus(data: Game): void {
    console.log('Received game status:', data);

    if (!data || !data.players || !data.phase) {
      console.error('Invalid game status data received.');
      return;
    }

    this.game = data;

    if (data.phase === 'PRE_FLOP') {
      console.log('Game is in PRE_FLOP phase, no community cards expected');
      this.game.communityCards = [];
    } else if (data.communityCards && data.communityCards.length > 0) {
      console.log('Community cards received:', data.communityCards);
      this.game.communityCards = data.communityCards;
    }

    this.setCurrentNonBotPlayerId();
    this.sortPlayers();
    this.updateCurrentPot();

    if (data.phase === 'SHOWDOWN') {
      console.log('Showdown phase reached, ending the game.');
      this.endGame();
    } else if (data.phase === 'RIVER') {
      if (this.checkAllPlayersActionTaken()) {
        console.log('All players acted in River phase. Ending game...');
        this.endGame();
      }
    }
  }

  isFolded(): boolean {
    return this.nonBotPlayer ? this.nonBotPlayer.folded : false;
  }

  handleRaiseAction(): void {
    this.playerActionTaken = true;
    this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
      next: (data) => {
        this.game = data;
        this.setCurrentNonBotPlayerId();
        this.sortPlayers();
        this.updateCurrentPot(); // Update pot on raise action
      },
      error: (error) => {
        console.error('Error fetching game status:', error);
      }
    });
  }

}