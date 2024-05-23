import { Component } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

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
    { name: '', startingChips: 1000, isBot: true }
  ];

  constructor(private http: HttpClient) { }

  addPlayer(): void {
    if (this.players.length < this.maxBotPlayers + this.maxHumanPlayers) {
      this.players.push({ name: '', startingChips: 1000, isBot: true });
    }
  }

  removePlayer(index: number): void {
    this.players.splice(index, 1);
  }

  private generateRandomName(): string {
    const commonNames = [
      'James', 'Mary', 'John', 'Patricia', 'Robert', 'Jennifer', 'Michael', 'Linda', 'William', 'Elizabeth',
      'David', 'Barbara', 'Richard', 'Susan', 'Joseph', 'Jessica', 'Thomas', 'Sarah', 'Charles', 'Karen',
      'Christopher', 'Nancy', 'Daniel', 'Margaret', 'Matthew', 'Lisa', 'Anthony', 'Betty', 'Donald', 'Dorothy',
      'Mark', 'Sandra', 'Paul', 'Ashley', 'Steven', 'Kimberly', 'Andrew', 'Donna', 'Kenneth', 'Emily',
      'Joshua', 'Michelle', 'George', 'Carol', 'Kevin', 'Amanda', 'Brian', 'Melissa', 'Edward', 'Deborah',
      'Ronald', 'Stephanie', 'Timothy', 'Rebecca', 'Jason', 'Laura', 'Jeffrey', 'Sharon', 'Ryan', 'Cynthia',
      'Jacob', 'Kathleen', 'Gary', 'Amy', 'Nicholas', 'Shirley', 'Eric', 'Angela', 'Stephen', 'Helen',
      'Jonathan', 'Anna', 'Larry', 'Brenda', 'Justin', 'Pamela', 'Scott', 'Nicole', 'Brandon', 'Emma',
      'Frank', 'Samantha', 'Benjamin', 'Katherine', 'Gregory', 'Christine', 'Raymond', 'Debra', 'Samuel', 'Rachel',
      'Patrick', 'Catherine', 'Alexander', 'Carolyn', 'Jack', 'Janet', 'Dennis', 'Ruth', 'Jerry', 'Maria'
    ];
    const randomIndex = Math.floor(Math.random() * commonNames.length);
    return commonNames[randomIndex];
  }

  private assignNamesToPlayers(): void {
    this.players.forEach(player => {
      if (!player.name.trim()) {
        player.name = this.generateRandomName();
      }
    });
  }

  onSubmit(): void {
    const botCount = this.players.filter(player => player.isBot).length;
    const humanCount = this.players.length - botCount;

    if (humanCount > this.maxHumanPlayers) {
      alert(`Maximum ${this.maxHumanPlayers} human player allowed.`);
      return;
    }

    if (botCount > this.maxBotPlayers) {
      alert(`Maximum ${this.maxBotPlayers} bot players allowed.`);
      return;
    }

    if (humanCount === 0) {
      alert('At least one human player is required to start the game.');
      return;
    }

    if (botCount === 0) {
      alert('At least one bot player is required to start the game.');
      return;
    }

    if (this.players.length < 2) {
      alert('At least two players are required to start the game.');
      return;
    }

    this.assignNamesToPlayers();

    this.http.post('http://localhost:8080/api/poker/start', this.players).subscribe({
      next: (response) => {
        console.log(response);
        window.location.href = '/start';
      },
      error: (error: HttpErrorResponse) => {
        console.error(error);
        alert('An error occurred during registration. Please try again later.');
      }
    });
  }
}
