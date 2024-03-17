import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';
import * as bootstrap from 'bootstrap';


@Component({
  selector: 'app-game-table',
  templateUrl: './game-table.component.html',
  styleUrls: ['./game-table.component.scss']
})
export class GameTableComponent implements OnInit {
  game!: Game;
  raiseAmount: number = 0;
  playerChips: number = 0;

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.getGameStatus();
  }
  @ViewChild('raiseModal') raiseModal!: ElementRef;

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
    // A játékosok regisztrációs információit ide kellene beilleszteni, ha szükséges
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
    this.http.get('http://localhost:8080/api/poker/end').subscribe(data => {
      alert(`Game ended. Winner is: ${data}`);
      this.getGameStatus();
    });
  }

  fold(): void {
    this.http.post('http://localhost:8080/api/poker/fold', { playerId: 'yourUserId' })
      .subscribe({
        next: (data) => {
          this.getGameStatus();
        },
        error: (error) => {
          console.error('Error during fold:', error);
        }
      });
  }

  raise(raiseAmount: number): void {
    const currentPlayer = this.game?.players.find(p => p.id === 'yourUserId');
    if (currentPlayer) {
      if (raiseAmount > 0 && raiseAmount <= currentPlayer.chips) {
        this.http.post('http://localhost:8080/api/poker/bet', { playerId: 'yourUserId', amount: raiseAmount })
          .subscribe({
            next: (data) => {
              // A válasz kezelése
              this.getGameStatus();
              this.closeRaiseModal(); // Itt használjuk a modális bezárására szolgáló metódust
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
    const currentPlayer = this.game?.players.find(p => p.id === 'yourUserId');
    if (currentPlayer) {
      this.raise(currentPlayer.chips);
    }
  }

  closeRaiseModal(): void {
    const modalElement = this.raiseModal.nativeElement;
    const modalInstance = bootstrap.Modal.getInstance(modalElement);
    if (modalInstance) {
      modalInstance.hide();
    }
  }

}
