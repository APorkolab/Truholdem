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
  currentPlayer: Player | undefined;
  currentBet: number = 0;

  players: PlayerInfo[] = []; // Biztosítjuk, hogy a players tulajdonság definiálva van

  constructor(private http: HttpClient) { }

  async ngOnInit(): Promise<void> {
    await this.getGameStatus();
    this.initializePlayers(); // Initialize players from the game object
  }

  async setMaxRaiseAmount(): Promise<void> {
    this.maxRaiseAmount = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'))?.chips || 10;
    this.currentBet = this.game?.players.reduce((max, player) => player.betAmount > max ? player.betAmount : max, 0) || 0;
    console.log(this.game?.players);
  }

  showRaiseInput(): void {
    this.isRaiseInputVisible = true;
  }

  cancelRaise(): void {
    this.isRaiseInputVisible = false;
  }

  async raise(raiseAmount: number): Promise<void> {
    this.isRaiseInputVisible = false;
    const currentPlayerId = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'))?.id;
    const currentPlayer = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'));
    if (currentPlayerId && raiseAmount > 0) {
      if (raiseAmount <= currentPlayer!.chips) {
        const headers = new HttpHeaders({
          'Content-Type': 'application/x-www-form-urlencoded',
        });

        const body = new HttpParams()
          .set('playerId', currentPlayerId)
          .set('amount', raiseAmount.toString());

        try {
          const response = await this.http.post('http://localhost:8080/api/poker/bet', body.toString(), { headers }).toPromise();
          this.getGameStatus();
          this.actionTaken.emit(); // Esemény kibocsátása
        } catch (error) {
          console.error('Error during raise:', error);
          this.getGameStatus();
        }
      } else {
        alert('The raise amount cannot exceed your chip count.');
      }
    }
  }

  async allIn(): Promise<void> {
    const currentPlayer = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'));
    if (currentPlayer) {
      await this.raise(currentPlayer.chips);
      this.actionTaken.emit(); // Esemény kibocsátása
    }
    await this.getGameStatus();
  }

  async check(): Promise<void> {
    const currentPlayer = this.game?.players.find((player: Player) => !player.name?.startsWith('Bot'));
    if (currentPlayer) {
      // Check if the current player's bet equals the highest bet or if the player is all-in
      if (currentPlayer.betAmount === this.currentBet || currentPlayer.chips === 0) {
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

      this.http.post('http://localhost:8080/api/poker/reset-game', this.players).subscribe({
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
