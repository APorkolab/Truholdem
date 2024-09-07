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
        this.game = data;
        this.setCurrentNonBotPlayerId();
        this.sortPlayers();
        this.playerActionTaken = this.isFolded() || this.playerActionTaken;
        this.updateCurrentPot();
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

  dealFlop() {
    this.http.get('http://localhost:8080/api/poker/flop').subscribe(
      (data: any) => {
        this.updateGameStatus(data);
        this.calculateCurrentPot();
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

  // Ez a metódus frissíti a játék állapotát
  updateGameStatus(data: any): void {
    this.game = data;
    // Esetleg más állapotfrissítések
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
      (response) => {
        this.gameResultMessage = response;
        this.showModal = true;
        this.updateCurrentPot(); // Update pot on end game
      },
      (error) => {
        this.gameResultMessage = 'Game end failed or no winner.';
        this.showModal = true;
        this.updateCurrentPot(); // Update pot on end game error
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
          this.getGameStatus(); // Update game status after fold
          this.progressToNextPhase(); // Manually progress to next phase
        },
        error: (error) => console.error('Error during fold:', error)
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  private progressToNextPhase(): void {
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

  isFolded(): boolean {
    return this.nonBotPlayer ? this.nonBotPlayer.folded : false;
  }

  showRaiseInput(): void {
    this.raiseInputComponent.showRaiseInput();
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

  handleCheckAction(): void {
    this.playerActionTaken = true;
    this.updateCurrentPot(); // Update pot on check action
  }

  allIn(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);
      this.http.post('http://localhost:8080/api/poker/bet', null, { params: params, responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("All-in successful", response);
          this.playerActionTaken = true;
          this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
            next: (data) => {
              this.game = data;
              this.setCurrentNonBotPlayerId();
              this.sortPlayers();
              this.updateCurrentPot(); // Update pot on all-in action
            },
            error: (error) => {
              console.error('Error fetching game status:', error);
            }
          });
        },
        error: (error) => console.error('Error during all-in:', error)
      });
    } else {
      console.error('No non-bot player found');
    }
  }

  calculateCurrentPot(): number {
    // Ellenőrizd, hogy minden játékos betAmount értéke helyesen van-e inicializálva és nem undefined
    return this.game.players.reduce((total, player) => total + (player.betAmount || 0), 0);
  }

  updateCurrentPot(): void {
    this.currentPot = this.calculateCurrentPot();
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
          this.playerActionTaken = false; // Reset player action flag
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

  automateBotActions(): void {
    this.game.players.forEach(player => {
      if (player.name?.startsWith('Bot') && !this.playerActions.get(player.id)) {
        const currentBet = this.game.players.reduce((max, player) => Math.max(max, player.betAmount || 0), 0);
        const raiseAmount = currentBet + Math.floor(Math.random() * 100);
        if (player.chips >= raiseAmount) {
          this.playerRaise(player.id, raiseAmount);
        } else if (player.chips >= currentBet) {
          this.playerBet(player.id, currentBet);
        } else {
          this.playerFold(player.id);
        }
      }
    });
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

  checkAllPlayersActionTaken(): boolean {
    return Array.from(this.playerActions.values()).every(actionTaken => actionTaken);
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

  playerFold(playerId: string): void {
    const payload = { playerId };
    this.http.post('http://localhost:8080/api/poker/fold', payload).subscribe({
      next: () => {
        this.updatePlayerAction(playerId);
        this.getGameStatus();
        this.updateCurrentPot();
      },
      error: (error) => {
        console.error('Error during player fold:', error);
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

  check(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);

      this.http.post('http://localhost:8080/api/poker/check', null, { params: params, responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("Check successful", response);
          this.playerActionTaken = true;
          this.getGameStatus(); // Update the game status after the check action
          this.progressToNextPhase(); // Only proceed to next phase if the check was successful
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error during check:', error.message);
          if (error.status === 400) {
            alert('A check csak akkor lehetséges, ha az aktuális tét megegyezik az Ön által már betett összeggel. Ha más játékos emelt, akkor Önnek is emelnie vagy dobnia kell.');
            // DO NOT call progressToNextPhase on error
          } else {
            alert('Váratlan hiba történt a check művelet során.');
          }
        }
      });
    } else {
      console.error('No non-bot player found');
    }
  }
}