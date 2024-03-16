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
  players: PlayerInfo[] = [
    { name: '', startingChips: 1000, isBot: false }
  ];

  constructor(private http: HttpClient) { }

  addPlayer(): void {
    this.players.push({ name: '', startingChips: 1000, isBot: false });
  }

  onSubmit(): void {
    this.http.post('/api/start', this.players).subscribe({
      next: (response) => console.log(response),
      error: (error) => console.error(error)
    });
  }
}