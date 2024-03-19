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

  ngOnInit(): void {
    this.setMaxRaiseAmount();
  }

  setMaxRaiseAmount(): void {

    this.maxRaiseAmount = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.chips || 10;
    console.log(this.currentPlayer);
    console.log(this.game?.players);
  }

  showRaiseInput(): void {
    this.isRaiseInputVisible = true;
  }

  cancelRaise(): void {
    this.isRaiseInputVisible = false;
  }

  raise(raiseAmount: number): void {
    this.isRaiseInputVisible = false;
    const currentPlayerId = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.id;
    const currentPlayer = this.game?.players.find(player => !player.name?.startsWith('Bot'));
    if (currentPlayerId && raiseAmount > 0) {
      if (raiseAmount <= currentPlayer!.chips) {
        const headers = new HttpHeaders({
          'Content-Type': 'application/x-www-form-urlencoded', // Módosítva az adattípust
        });

        // Az adatokat URL kódolt formában kell elküldeni
        const body = new HttpParams()
          .set('playerId', currentPlayerId)
          .set('amount', raiseAmount.toString());

        this.http.post('http://localhost:8080/api/poker/bet', body.toString(), { headers }).subscribe({
          next: (data) => {
            // Response handling
            this.getGameStatus();
          },
          error: (error) => {
            console.error('Error during raise:', error);
          }
        });
      } else {
        alert('The raise amount cannot exceed your chip count.');
      }
      this.getGameStatus();
    }

  }

  allIn(): void {
    if (this.currentPlayer) {
      this.raise(this.currentPlayer.chips);
    }
  }

  getGameStatus(): void {
    this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
      next: (data) => {
        this.game = data;
        this.maxRaiseAmount = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.chips || 10;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error fetching game status:', error.message);
        alert('An error occurred while fetching game status. Please try again later.');
      }
    });
  }
}


