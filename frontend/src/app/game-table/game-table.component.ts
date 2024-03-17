import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';

declare var bootstrap: any;
@Component({
  selector: 'app-game-table',
  templateUrl: './game-table.component.html',
  styleUrls: ['./game-table.component.scss']
})
export class GameTableComponent implements OnInit {
  game!: Game;
  raiseAmount: number = 0;
  playerChips: number = 0;
  currentNonBotPlayerId: string = '';

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.getGameStatus();
    this.setCurrentNonBotPlayerId();
  }
  @ViewChild('raiseModal') raiseModal!: ElementRef;

  setCurrentNonBotPlayerId(): void {
    const nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));
    if (nonBotPlayer) {
      this.currentNonBotPlayerId = nonBotPlayer.id;
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

  getCardImage(card: Card): string {
    if (!card) return '../../assets/cards/back.png';

    let rank = card['value'].toLowerCase();
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

    const suit = card['suit'].toLowerCase();

    return `../../assets/cards/${rank}_of_${suit}.png`;
  }

  startGame(): void {
    this.http.post('http://localhost:8080/api/poker/start', []).subscribe(data => {
      this.getGameStatus();
    });
  }

  dealFlop(): void {
    this.http.get('http://localhost:8080/api/poker/flop').subscribe({
      next: (data) => {
        this.getGameStatus();
      },
      error: (error) => {
        console.error('Error during dealing the flop:', error);
      }
    });
  }

  dealTurn(): void {
    this.http.get('http://localhost:8080/api/poker/turn').subscribe(data => {
      this.getGameStatus();
    });
  }

  dealRiver(): void {
    this.http.get('http://localhost:8080/api/poker/river').subscribe(data => {
      this.getGameStatus();
    });
  }

  endGame(): void {
    this.http.get<string>('http://localhost:8080/api/poker/end', { responseType: 'text' as 'json' }).subscribe(data => {
      alert(data);
      this.getGameStatus();
    });
  }

  fold(): void {
    // Megkeressük az első nem-bot játékost
    const nonBotPlayer = this.game.players.find(player => !player.name?.startsWith('Bot'));

    if (nonBotPlayer) {
      const params = new HttpParams().set('playerId', nonBotPlayer.id);

      this.http.post('http://localhost:8080/api/poker/fold', null, { params: params, responseType: 'text' })
        .subscribe({
          next: (response) => {
            console.log("Fold successful", response);
            this.getGameStatus();
            this.endGame();

          },
          error: (error) => {
            console.error('Error during fold:', error);
          }
        });
    } else {
      console.error('No non-bot player found');
    }
  }

  raise(raiseAmount: number): void {
    const currentPlayerId = this.game?.players.find(player => !player.name?.startsWith('Bot'))?.id;
    const currentPlayer = this.game?.players.find(player => !player.name?.startsWith('Bot'));
    if (currentPlayerId && raiseAmount > 0) {
      // Itt feltételezzük, hogy currentPlayer.chips elérhető és korábban be van állítva
      if (raiseAmount <= currentPlayer!.chips) {
        const body = new HttpParams()
          .set('playerId', currentPlayerId)
          .set('amount', raiseAmount.toString());

        this.http.post('http://localhost:8080/api/poker/bet', body.toString(), {
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        }).subscribe({
          next: (data) => {
            // A válasz kezelése
            this.getGameStatus(); // Frissíti a játék állapotát
          },
          error: (error) => {
            console.error('Error during raise:', error);
          }
        });
      } else {
        // Informáld a játékost, hogy a megadott összeg túl magas
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

}