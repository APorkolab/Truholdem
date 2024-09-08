import { Component } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { PlayerService } from '../services/player.service';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

export interface PlayerInfo {
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
    { name: this.generateRandomName(), startingChips: 1000, isBot: false },  // Human player by default
    { name: this.generateRandomName(), startingChips: 1000, isBot: true },   // Bot players
    { name: this.generateRandomName(), startingChips: 1000, isBot: true },
    { name: this.generateRandomName(), startingChips: 1000, isBot: true }
  ];

  constructor(private http: HttpClient, private playerService: PlayerService, private router: Router) { }

  // Új játékos hozzáadása (bot vagy nem bot)
  addPlayer(): void {
    if (this.players.length >= 4) {
      alert('You can\'t have more than 4 players.');
      return;
    }
    // Hozzáadáskor generálunk egy nevet
    this.players.push({ name: this.generateRandomName(), startingChips: 1000, isBot: true });
  }

  // Játékos eltávolítása
  removePlayer(index: number): void {
    this.players.splice(index, 1);
  }

  // A játékosok nevének véglegesítése (amelyek meg lettek adva, vagy generált nevek)
  private finalizeNames(): void {
    this.players.forEach(player => {
      if (!player.name.trim()) {
        player.name = this.generateRandomName();
      }
      if (player.isBot && !player.name.startsWith('Bot')) {
        player.name = 'Bot ' + player.name;
      }
    });
  }

  onSubmit(): void {
    // Nevek véglegesítése
    this.finalizeNames();

    // Játék visszaállítása
    this.http.post('http://localhost:8080/api/poker/reset', {}).subscribe({
      next: () => {
        // A játékosok regisztrálása a visszaállítás után
        this.registerPlayers();
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error resetting game:', error.message);
        alert('An error occurred while resetting the game. Please try again later.');
      }
    });
  }

  registerPlayers(): void {
    this.http.post<any>('http://localhost:8080/api/poker/start', this.players).subscribe({
      next: (response: any) => {
        if (response && response.players && Array.isArray(response.players)) {
          this.changePlayerNames(response.players);
        } else {
          console.error('Unexpected response format. Expected players array but got:', response);
          alert('Unexpected response from server.');
        }
      },
      error: (error: HttpErrorResponse) => {
        console.error('Error registering players:', error.message);
        alert('An error occurred during registration. Please try again later.');
      }
    });
  }

  changePlayerNames(serverPlayers: any[]): void {
    const changePlayerNameRecursively = (index: number) => {
      if (index >= serverPlayers.length) {
        // Minden névváltoztatás sikeres volt, most mentjük a játékosokat és navigálunk
        this.playerService.setPlayers(this.players);
        this.router.navigate(['/start']);
        return;
      }

      const serverPlayer = serverPlayers[index];
      const clientPlayer = this.players[index];

      // Csak akkor küldjük el a változtatást, ha a nevek eltérnek
      if (serverPlayer.name !== clientPlayer.name) {
        this.http.post('http://localhost:8080/api/poker/change-name', {
          playerId: serverPlayer.id,
          newName: clientPlayer.name
        }).subscribe({
          next: () => {
            // Következő játékos nevének módosítása
            changePlayerNameRecursively(index + 1);
          },
          error: (error: HttpErrorResponse) => {
            console.error('Error changing player name for', clientPlayer.name, ':', error.message);
            alert('An error occurred while changing player names. Please try again later.');
          }
        });
      } else {
        // Ha a név nem változott, folytatjuk a következő játékossal
        changePlayerNameRecursively(index + 1);
      }
    };

    // Kezdjük az első játékossal
    changePlayerNameRecursively(0);
  }

  // Név generálása (alapértelmezett név játékosnak vagy botnak)
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
    return commonNames[Math.floor(Math.random() * commonNames.length)];
  }
}