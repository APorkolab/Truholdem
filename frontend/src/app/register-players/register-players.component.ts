// register-players.component.ts
import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface PlayerInfo {
  name: string;
  startingChips: number;
  isBot: boolean;
}

@Component({
  selector: 'app-register-players',
  templateUrl: './register-players.component.html',
  styleUrls: ['./register-players.component.scss']
})
export class RegisterPlayersComponent {
  maxBotPlayers = 3;
  maxHumanPlayers = 1;
  players: PlayerInfo[] = [
    { name: '', startingChips: 1000, isBot: false }
  ];

  constructor(private http: HttpClient) { }

  addPlayer(): void {
    if (this.players.length < this.maxBotPlayers + this.maxHumanPlayers) {
      this.players.push({ name: '', startingChips: 1000, isBot: false });
    }
  }

  removePlayer(index: number): void {
    this.players.splice(index, 1);
  }

  onSubmit(): void {
    const botCount = this.players.filter(player => player.isBot).length;
    const humanCount = this.players.length - botCount;

    if (botCount > this.maxBotPlayers || humanCount > this.maxHumanPlayers) {
      alert(`Maximum ${this.maxBotPlayers} bot és maximum ${this.maxHumanPlayers} emberi játékos regisztrálhat.`);
      return;
    }

    this.http.post('http://localhost:8080/api/poker/start', this.players).subscribe({
      next: (response) => {
        console.log(response);
        window.location.href = '/start';
      },
      error: (error) => {
        console.error(error);
        alert('Hiba történt a regisztráció során. Kérlek, próbáld újra később.');
      }
    });
  }
}
