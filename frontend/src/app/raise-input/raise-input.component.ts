import { Component, Input, OnInit } from '@angular/core';
import { Game } from '../model/game';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Player } from '../model/player';


@Component({
  selector: 'app-raise-input',
  templateUrl: './raise-input.component.html',
  styleUrls: ['./raise-input.component.scss']
})
export class RaiseInputComponent implements OnInit {
  @Input()
  game!: Game;

  isRaiseInputVisible = false;
  raiseAmount = 0;
  maxRaiseAmount = 0;
  currentPlayer: Player | undefined;

  constructor(private http: HttpClient) { }

  async ngOnInit(): Promise<void> {
    await this.getGameStatus();
  }

  async setMaxRaiseAmount(): Promise<void> {
    this.maxRaiseAmount = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.chips || 10;
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
    const currentPlayerId = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.id;
    const currentPlayer = this.game?.players.find(player => !player.name?.startsWith('Bot'));
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
    const currentPlayer = this.game?.players.find(player => !player.name?.startsWith('Bot'));
    if (currentPlayer) {
      await this.raise(currentPlayer.chips);
    }
    await this.getGameStatus();
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

}


