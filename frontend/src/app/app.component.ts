import { Component, OnInit } from '@angular/core';
import { PlayerInfo } from './register-players/register-players.component';
import { PlayerService } from './services/player.service';
import { Router } from '@angular/router';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: false
})
export class AppComponent implements OnInit {
  title = 'texas-holdem-frontend';
  registeredPlayers: PlayerInfo[] = [];
  gameStarted = false;

  constructor(private playerService: PlayerService, private router: Router) { }

  ngOnInit(): void {
    // Feliratkozunk a PlayerService változásaira
    this.playerService.players$.subscribe(players => {
      this.registeredPlayers = players;
      this.gameStarted = players.length > 0;
    });
  }

  onPlayersRegistered(playersInfo: PlayerInfo[]): void {
    if (this.isValidPlayersArray(playersInfo)) {
      console.log('Registered players:', playersInfo);
      this.playerService.setPlayers(playersInfo); // Játékosok tárolása
      this.router.navigate(['/start']); // Navigálás a játéktérre
    } else {
      console.error('Unexpected players data:', playersInfo);
    }
  }

  // Ellenőrzi, hogy a játékosok adatai érvényesek-e
  private isValidPlayersArray(playersInfo: PlayerInfo[]): boolean {
    return Array.isArray(playersInfo) && playersInfo.length > 0;
  }
}
