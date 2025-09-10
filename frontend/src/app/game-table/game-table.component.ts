import { RaiseInputComponent } from './../raise-input/raise-input.component';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';
import { PlayerService } from '../services/player.service';
import { PlayerInfo } from '../register-players/register-players.component';
import { Observable, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { NgFor, NgIf } from '@angular/common';
import { RaiseInputComponent as RaiseInputComponent_1 } from '../raise-input/raise-input.component';

@Component({
    selector: 'app-game-table',
    templateUrl: './game-table.component.html',
    styleUrls: ['./game-table.component.scss'],
    imports: [NgFor, NgIf, RaiseInputComponent_1]
})
export class GameTableComponent implements OnInit {
  private http = inject(HttpClient);
  private playerService = inject(PlayerService);
  private router = inject(Router);

  game: Game = {
    currentPot: 0, players: [], communityCards: [], phase: 'PRE_FLOP',
    currentBet: 0,
    playerActions: {}
  };
  raiseAmount = 0;
  playerChips = 0;
  nonBotPlayer: Player | undefined;
  currentNonBotPlayerId = '';
  showModal = false;
  gameResultMessage = '';
  playerActionTaken = false;
  currentPot = 0;
  playerActions = new Map<string, boolean>();
  players: PlayerInfo[] = [];

  @ViewChild(RaiseInputComponent) raiseInputComponent!: RaiseInputComponent;
  @ViewChild('raiseModal') raiseModal!: ElementRef;

  constructor() { 
    // Component initialization handled in ngOnInit
  }

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
        if (Array.isArray(data)) {
          this.updateGameStatus({ players: data });
        } else if (data && data.players) {
          this.updateGameStatus(data);
        } else {
          console.error('Érvénytelen játékállapot adatok érkeztek:', data);
        }
        console.log('Játékosok száma:', this.game.players.length);
      },
      error: (error: HttpErrorResponse) => {
        console.error('Hiba a játékállapot lekérdezésekor:', error.message);
        if (error.status === 404) {
          this.startNewGame();
        } else {
          alert('Hiba a játékállapot lekérdezésekor. Kérjük, próbálja újra később.');
        }
      }
    });
  }


  updateGameStatus(gameStatus: Partial<Game>): void {
    if (gameStatus.players) {
      this.game.players = gameStatus.players;
      console.log('Játékosok:', this.game.players);

      this.setCurrentNonBotPlayerId();
      this.sortPlayers();
      this.updateCurrentPot();

      if (gameStatus.phase) {
        this.game.phase = gameStatus.phase;
      }

      if (gameStatus.playerActions) {
        this.game.playerActions = gameStatus.playerActions;
      }

      if (gameStatus.communityCards) {
        this.game.communityCards = gameStatus.communityCards;
        console.log('Közösségi lapok:', this.game.communityCards);
      }

      if (this.game.phase === 'SHOWDOWN') {
        this.endGame();
      } else {
        this.checkBotActions();
      }
    } else {
      console.error('Érvénytelen játékállapot adatok érkeztek:', gameStatus);
    }
  }




  sortPlayers(): void {
    if (this.game && Array.isArray(this.game.players)) {
      const nonBotPlayers = this.game.players.filter(player => player?.name && !player.name.startsWith('Bot'));
      const botPlayers = this.game.players.filter(player => player?.name && player.name.startsWith('Bot'));
      this.game.players = [...botPlayers, ...nonBotPlayers];
      this.nonBotPlayer = nonBotPlayers.length ? nonBotPlayers[0] : undefined;
    } else {
      console.error('Players list is undefined or not an array');
    }
  }

  dealFlop(): void {
    this.sendPhaseRequest('flop').subscribe({
      next: (response) => {
        console.log('Flop dealt successfully:', response);
        this.getGameStatus();
      },
      error: (error) => {
        console.error('Error dealing flop:', error);
        this.getGameStatus();
      }
    });
  }

  dealTurn(): void {
    this.sendPhaseRequest('turn').subscribe({
      next: (response) => {
        console.log('Turn dealt successfully:', response);
        this.getGameStatus();
      },
      error: (error) => {
        console.error('Error dealing turn:', error);
        this.getGameStatus();
      }
    });
  }

  dealRiver(): void {
    this.sendPhaseRequest('river').subscribe({
      next: (response) => {
        console.log('River dealt successfully:', response);
        this.getGameStatus();
      },
      error: (error) => {
        console.error('Error dealing river:', error);
        this.getGameStatus();
      }
    });
  }

  sendPhaseRequest(phase: string): Observable<string> {
    return this.http.get(`http://localhost:8080/api/poker/${phase}`, { responseType: 'text' }).pipe(
      catchError((error) => {
        console.error(`Error during ${phase} request:`, error);
        return throwError(() => error);
      })
    );
  }

  endGame(): void {
    this.http.get('http://localhost:8080/api/poker/end', { responseType: 'json' }).subscribe({
      next: (response: {message?: string}) => {
        if (response && response.message) {
          const winnerMessage = response.message;
          const winnerName = winnerMessage.split(': ')[1];
          this.gameResultMessage = `A győztes: ${winnerName}`;
        } else {
          this.gameResultMessage = 'Nem sikerült meghatározni a győztest.';
        }
        this.showModal = true;
        this.updateCurrentPot();
      },
      error: () => {
        this.gameResultMessage = 'A játék vége sikertelen vagy nincs győztes.';
        this.showModal = true;
        this.updateCurrentPot();
      }
    });
  }

  fold(): void {
    if (this.nonBotPlayer) {
      const params = new HttpParams().set('playerId', this.nonBotPlayer.id);
      this.http.post('http://localhost:8080/api/poker/fold', null, { params, responseType: 'text' }).subscribe({
        next: () => {
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
    if (!this.checkAllPlayersActionTaken()) {
      console.log('Nem minden játékos cselekedett még, várakozás...');
      return;
    }

    this.resetPlayerActions();

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
      case 'SHOWDOWN':
        this.endGame();
        break;
      default:
        console.error('Ismeretlen fázis:', this.game.phase);
        break;
    }
  }

  resetPlayerActions(): void {
    this.game.players.forEach(player => {
      this.game.playerActions[player.id] = false;
    });
  }

  checkAllPlayersActionTaken(): boolean {
    const allActionsTaken = this.game.players.every(player =>
      this.game.playerActions[player.id] === true || player.folded || player.chips === 0
    );
    console.log('All players actions taken:', allActionsTaken);
    return allActionsTaken;
  }

  updatePlayerAction(playerId: string): void {
    this.playerActions.set(playerId, true);
    if (this.checkAllPlayersActionTaken()) {
      this.progressToNextPhase();
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

  allIn(): void {
    if (this.nonBotPlayer) {
      this.http.post('http://localhost:8080/api/poker/bet', { playerId: this.nonBotPlayer.id, amount: this.nonBotPlayer.chips }, { responseType: 'text' }).subscribe({
        next: () => {
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
        this.nonBotPlayer!.folded = false;
      },
      error: (error) => console.error('Error during game reset:', error)
    });
  }

  startNewMatch(): void {
    this.nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (this.nonBotPlayer && this.nonBotPlayer.folded) {
      this.startNewGame();
    } else {
      this.http.post<Game>('http://localhost:8080/api/poker/new-match', {}).subscribe({
        next: (response) => {
          if (response?.players?.length) {
            this.game = response;
            this.playerActionTaken = false;
            this.closeModal();
            this.updateCurrentPot();
          } else {
            alert('No players with chips left or failed to start new match.');
          }
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error during new match start:', error);
        }
      });
    }
  }

  startNewGame(): void {
    this.http.post<Game>('http://localhost:8080/api/poker/start', []).subscribe({
      next: (response) => {
        if (response?.players?.length) {
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
      default: break;
    }

    const suit = card.suit.toLowerCase();
    return `../../assets/cards/${rank}_of_${suit}.png`;
  }

  isFolded(): boolean {
    return this.nonBotPlayer ? this.nonBotPlayer.folded : false;
  }

  handleRaiseAction(): void {
    this.playerActionTaken = true;
    this.getGameStatus();
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

  checkBotActions(): void {
    const botPlayers = this.game.players.filter(player =>
      player.name.startsWith('Bot') &&
      !this.game.playerActions[player.id] &&
      !player.folded
    );

    if (botPlayers.length === 0) {
      console.log('Nincs több bot cselekvés');
      this.progressToNextPhase();
      return;
    }

    const botId = botPlayers[0].id;
    this.performBotAction(botId);
  }

  performBotAction(botId: string): void {
    this.http.post(`http://localhost:8080/api/poker/bot-action/${botId}`, {}).subscribe({
      next: (response: {message?: string}) => {
        console.log('Bot cselekvés sikeres:', response.message);
        this.getGameStatus();
        this.checkBotActions();
      },
      error: (error) => {
        console.error('Hiba történt a bot cselekvés során:', error);
        this.getGameStatus();
      }
    });
  }

  reloadPageOnStart() {
    window.location.assign('/');
  }
}
