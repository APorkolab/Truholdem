import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface Card {
  rank: string;
  suit: string;
}

interface Player {
  name: string;
  cards: Card[];
  isUser: boolean;
}

@Component({
  selector: 'app-game-table',
  templateUrl: './game-table.component.html',
  styleUrls: ['./game-table.component.css']
})
export class GameTableComponent implements OnInit {
  gamePhase: string = 'preFlop'; // Kezdeti fázis
  players: Player[] = []; // Tömb a játékosok tárolására
  communityCards: Card[] = []; // Közösségi lapok

  constructor(private http: HttpClient) { }

  ngOnInit(): void {
    this.getGameStatus(); // Kezdő állapot lekérése
  }

  getGameStatus(): void {
    // Backend hívás a játék állapotának lekéréséhez
    this.http.get('/api/poker/status').subscribe((data: any) => {
      // Frissítsd az állapotot a válasz alapján
      this.players = data.players;
      this.communityCards = data.communityCards;
      this.gamePhase = data.gamePhase;
    });
  }

  getCardImage(card: Card): string {
    if (!card) return '/assets/cards/back.png'; // Alapértelmezett hátlap, ha nincs kártya
    return `/assets/cards/${card.rank.toLowerCase()}_of_${card.suit.toLowerCase()}.png`;
  }

  startGame(): void {
    // A játékosok regisztrációs információit ide kellene beilleszteni, ha szükséges
    this.http.post('/api/poker/start', []).subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  dealFlop(): void {
    this.http.get('/api/poker/flop').subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  dealTurn(): void {
    this.http.get('/api/poker/turn').subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  dealRiver(): void {
    this.http.get('/api/poker/river').subscribe(data => {
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }

  endGame(): void {
    this.http.get('/api/poker/end').subscribe(data => {
      alert(`Game ended. Winner is: ${data}`);
      this.getGameStatus(); // Frissítsd az állapotot
    });
  }
}