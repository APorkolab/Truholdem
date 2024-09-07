import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Game } from '../model/game';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Player } from '../model/player';

interface PlayerInfo {
  name: string;
  startingChips: number;
  isBot: boolean;
}

@Component({
  selector: 'app-raise-input',
  templateUrl: './raise-input.component.html',
  styleUrls: ['./raise-input.component.scss']
})
export class RaiseInputComponent implements OnInit {
  @Input() game!: Game;
  @Output() actionTaken = new EventEmitter<void>();

  isRaiseInputVisible = false;
  raiseAmount = 0;
  maxRaiseAmount = 0;
  minRaiseAmount = 0;
  suggestedRaiseAmount = 0;
  currentPlayer: Player | undefined;
  currentBet: number = 0;

  players: PlayerInfo[] = [];

  constructor(private http: HttpClient) { }

  async ngOnInit(): Promise<void> {
    await this.getGameStatus();
    this.initializePlayers();
  }

  async setMaxRaiseAmount(): Promise<void> {
    this.currentPlayer = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'));
    if (this.currentPlayer) {
      this.maxRaiseAmount = this.currentPlayer.chips || 10;
      this.currentBet = this.game?.players.reduce((max, player) => player.betAmount > max ? player.betAmount : max, 0) || 0;
      this.minRaiseAmount = Math.ceil(this.currentBet * 1.5);

      const suggestedRaisePercentage = 1 + (Math.random() * 0.05 + 0.05);
      this.suggestedRaiseAmount = Math.ceil(this.minRaiseAmount * suggestedRaisePercentage);

      if (this.suggestedRaiseAmount > this.maxRaiseAmount) {
        this.suggestedRaiseAmount = this.maxRaiseAmount;
      }

      if (this.suggestedRaiseAmount < this.minRaiseAmount) {
        this.suggestedRaiseAmount = this.minRaiseAmount;
      }

      console.log(this.game?.players);
    }
  }

  showRaiseInput(): void {
    this.isRaiseInputVisible = true;
  }

  cancelRaise(): void {
    this.isRaiseInputVisible = false;
  }

  isFolded(): boolean {
    return this.currentPlayer ? this.currentPlayer.folded : false;
  }

  async raise(raiseAmount: number): Promise<void> {
    this.isRaiseInputVisible = false;
    if (this.currentPlayer && raiseAmount > 0 && raiseAmount <= this.currentPlayer.chips && raiseAmount >= this.minRaiseAmount) {
      const headers = new HttpHeaders({
        'Content-Type': 'application/json',
      });

      const body = {
        playerId: this.currentPlayer.id,
        amount: raiseAmount
      };

      try {
        const response = await this.http.post('http://localhost:8080/api/poker/bet', body, { headers }).toPromise();
        this.getGameStatus();
        this.actionTaken.emit();
      } catch (error) {
        console.error('Error during raise:', error);
        this.getGameStatus();
      }
    } else {
      alert(`The raise amount must be greater than ${this.minRaiseAmount}, and cannot exceed your chip count or be zero.`);
    }
  }

  async allIn(): Promise<void> {
    if (this.currentPlayer) {
      await this.raise(this.currentPlayer.chips);
      this.actionTaken.emit();
    }
    await this.getGameStatus();
  }

  async check(): Promise<void> {
    if (this.currentPlayer) {
      if (this.currentPlayer.betAmount === this.currentBet || this.currentPlayer.chips === 0) {
        this.isRaiseInputVisible = false;
        this.actionTaken.emit();
        await this.getGameStatus();
        return;
      } else {
        alert('You cannot check unless your current bet matches the highest bet or you are all-in.');
      }
    }
  }

  async fold(): Promise<void> {
    if (this.currentPlayer) {
      const params = new HttpParams().set('playerId', this.currentPlayer.id);

      this.http.post('http://localhost:8080/api/poker/fold', null, { params: params, responseType: 'text' }).subscribe({
        next: (response) => {
          console.log("Fold successful", response);
          this.actionTaken.emit();
          this.getGameStatus();
        },
        error: (error) => console.error('Error during fold:', error)
      });
    } else {
      console.error('No current player found');
    }
  }

  getGameStatus(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
        next: (data: Game) => {
          this.game = data;
          this.initializePlayers();
          this.setMaxRaiseAmountAfterGameStatusUpdate();
          resolve();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error fetching game status:', error.message);
          alert('An error occurred while fetching game status. Please try again later.');
          reject(error);
        }
      });
    });
  }

  async setMaxRaiseAmountAfterGameStatusUpdate(): Promise<void> {
    await this.setMaxRaiseAmount();
  }

  initializePlayers(): void {
    this.players = this.game.players.map((player: Player) => ({
      name: player.name || '',
      startingChips: player.chips,
      isBot: player.name.startsWith('Bot')
    }));
  }

  startNewGame(): void {
    this.players.forEach((player: PlayerInfo) => {
      if (player.startingChips === 0) {
        player.startingChips = 1000;
      }
    });

    this.http.post('http://localhost:8080/api/poker/start', this.players).subscribe({
      next: (response) => {
        console.log(response);
        this.getGameStatus();
      },
      error: (error) => {
        console.error('Error starting new game:', error);
        alert('An error occurred while starting a new game. Please try again later.');
      }
    });
  }

  resetGame(): void {
    if (confirm('Are you sure you want to reset the game? All players will start with default chips.')) {
      this.players.forEach((player: PlayerInfo) => {
        player.startingChips = 1000;
      });

      this.http.post('http://localhost:8080/api/poker/reset', { keepPlayers: true }).subscribe({
        next: (response) => {
          console.log(response);
          this.getGameStatus();
        },
        error: (error: HttpErrorResponse) => {
          console.error('Error resetting game:', error.message);
          alert('An error occurred while resetting the game. Please try again later.');
        }
      });
    }
  }


}