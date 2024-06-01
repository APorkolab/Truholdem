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
  @Output() actionTaken = new EventEmitter<void>(); // Esemény kibocsátása akció végrehajtásakor

  isRaiseInputVisible = false;
  raiseAmount = 0;
  maxRaiseAmount = 0;
  minRaiseAmount = 0; // Új mező a minimális tétnek
  suggestedRaiseAmount = 0; // Új mező az ajánlott tétnek
  currentPlayer: Player | undefined;
  currentBet: number = 0;

  players: PlayerInfo[] = []; // Biztosítjuk, hogy a players tulajdonság definiálva van

  constructor(private http: HttpClient) { }

  async ngOnInit(): Promise<void> {
    await this.getGameStatus();
    this.initializePlayers(); // Initialize players from the game object
  }

  async setMaxRaiseAmount(): Promise<void> {
    this.currentPlayer = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'));
    if (this.currentPlayer) {
      this.maxRaiseAmount = this.currentPlayer.chips || 10;
      this.currentBet = this.game?.players.reduce((max, player) => player.betAmount > max ? player.betAmount : max, 0) || 0;
      this.minRaiseAmount = Math.ceil(this.currentBet * 1.5); // Minimális tét 1.5x az aktuális tét

      // Az ajánlott tét a minimum tét + 5-10%
      const suggestedRaisePercentage = 1 + (Math.random() * 0.05 + 0.05); // 5-10% között
      this.suggestedRaiseAmount = Math.ceil(this.minRaiseAmount * suggestedRaisePercentage);

      // Ügyeljünk arra, hogy az ajánlott tét érvényes legyen
      if (this.suggestedRaiseAmount > this.maxRaiseAmount) {
        this.suggestedRaiseAmount = this.maxRaiseAmount;
      }

      // Az ajánlott tét nem lehet kisebb a minimális tét összegénél
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
        this.actionTaken.emit(); // Esemény kibocsátása
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
      this.actionTaken.emit(); // Esemény kibocsátása
    }
    await this.getGameStatus();
  }

  async check(): Promise<void> {
    if (this.currentPlayer) {
      // Check if the current player's bet equals the highest bet or if the player is all-in
      if (this.currentPlayer.betAmount === this.currentBet || this.currentPlayer.chips === 0) {
        this.isRaiseInputVisible = false;
        this.actionTaken.emit(); // Esemény kibocsátása a check gombbal
      } else {
        alert('You cannot check unless your current bet matches the highest bet or you are all-in.');
      }
    }
  }

  getGameStatus(): void {
    this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
      next: (data) => {
        this.game = data;
        this.setMaxRaiseAmountAfterGameStatusUpdate();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error fetching game status:', error.message);
        alert('An error occurred while fetching game status. Please try again later.');
      }
    });
  }

  async setMaxRaiseAmountAfterGameStatusUpdate(): Promise<void> {
    await this.setMaxRaiseAmount();
  }

  initializePlayers(): void {
    this.players = this.game.players.map((player: Player) => ({
      name: player.name || '', // Biztosítjuk, hogy a name soha ne legyen null
      startingChips: player.chips,
      isBot: player.name.startsWith('Bot')
    }));
  }

  startNewGame(): void {
    // Ensure all players with 0 chips are removed or reset their chips
    this.players.forEach((player: PlayerInfo) => {
      if (player.startingChips === 0) {
        player.startingChips = 1000; // Reset to default starting chips
      }
    });

    // Start new game with current players and their remaining chips
    this.http.post('http://localhost:8080/api/poker/new-game', this.players).subscribe({
      next: (response) => {
        console.log(response);
        window.location.href = '/start';
      },
      error: (error) => {
        console.error(error);
        alert('An error occurred while starting a new game. Please try again later.');
      }
    });
  }

  resetGame(): void {
    if (confirm('Are you sure you want to reset the game? All players will start with default chips.')) {
      this.players.forEach((player: PlayerInfo) => {
        player.startingChips = 1000; // Reset all players' chips to default
      });

      this.http.post('http://localhost:8080/api/poker/reset', this.players).subscribe({
        next: (response) => {
          console.log(response);
          window.location.href = '/start';
        },
        error: (error) => {
          console.error(error);
          alert('An error occurred while resetting the game. Please try again later.');
        }
      });
    }
  }
}
