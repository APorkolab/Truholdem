import { Component, OnInit } from '@angular/core';
import { Game } from '../model/game';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';


@Component({
  selector: 'app-raise-input',
  templateUrl: './raise-input.component.html',
  styleUrls: ['./raise-input.component.scss']
})
export class RaiseInputComponent implements OnInit {
  game!: Game;
  isRaiseInputVisible = false;
  raiseAmount = 0;
  maxRaiseAmount = 0;

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.setMaxRaiseAmount();
  }

  setMaxRaiseAmount(): void {
    this.maxRaiseAmount = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.chips || 0;
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
      // Assume currentPlayer.chips is accessible and previously set
      if (raiseAmount <= currentPlayer!.chips) {
        const headers = new HttpHeaders().set('Content-Type', 'application/json'); // Adjusted for JSON content type
        const body = {
          playerId: currentPlayerId,
          amount: raiseAmount
        };

        this.http.post('http://localhost:8080/api/poker/bet', body, { headers }).subscribe({
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
    }
  }

  allIn(): void {
    const currentPlayer = this.game?.players.find(player => !player.name?.startsWith('Bot'));
    if (currentPlayer) {
      this.raise(currentPlayer.chips);
    }
  }

  getGameStatus(): void {
    this.http.get<Game>('http://localhost:8080/api/poker/status').subscribe({
      next: (data) => {
        this.game = data;
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error fetching game status:', error.message);
        alert('An error occurred while fetching game status. Please try again later.');
      }
    });
  }
}


