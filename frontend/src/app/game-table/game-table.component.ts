import { Component, OnInit } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Game } from '../model/game';
import { Player } from '../model/player';
import { Card } from '../model/card';


@Component({
  selector: 'app-game-table',
  templateUrl: './game-table.component.html',
  styleUrls: ['./game-table.component.scss']
})
export class GameTableComponent implements OnInit {
  game!: Game; // A Game típusú változó tartalmazza a játék állapotát

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.getGameStatus(); // Kezdő állapot lekérése
  }

  getGameStatus(): void {
    // Feltételezve, hogy az API a játék teljes állapotát adja vissza egy Game objektumként
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

    // Átalakítja a rangot a fájlnév formátumához megfelelően
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
      // További esetleges átalakítások
      default: break;
    }

    // Átalakítja a színt a fájlnév formátumához megfelelően
    const suit = card['suit'].toLowerCase();

    return `../../assets/cards/${rank}_of_${suit}.png`;
  }

  startGame(): void {
    // A játékosok regisztrációs információit ide kellene beilleszteni, ha szükséges
    this.http.post('http://localhost:8080/api/poker/start', []).subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  dealFlop(): void {
    this.http.get('http://localhost:8080/api/poker/flop').subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  dealTurn(): void {
    this.http.get('http://localhost:8080/api/poker/turn').subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  dealRiver(): void {
    this.http.get('http://localhost:8080/api/poker/river').subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  endGame(): void {
    this.http.get('http://localhost:8080/api/poker/end').subscribe(data => {
      alert(`Game ended. Winner is: ${data}`);
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }
}