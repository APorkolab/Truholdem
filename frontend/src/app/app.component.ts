import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerInfo } from './register-players/register-players.component';
import { PlayerService } from './services/player.service';
import { Router } from '@angular/router';
import { RegisterPlayersComponent } from './register-players/register-players.component';
import { GameTableComponent } from './game-table/game-table.component';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrls: ['./app.component.scss'],
    standalone: true,
    imports: [CommonModule, RegisterPlayersComponent, GameTableComponent]
})
export class AppComponent implements OnInit {
  private playerService = inject(PlayerService);
  private router = inject(Router);

  title = 'texas-holdem-frontend';
  registeredPlayers: PlayerInfo[] = [];
  gameStarted = false;


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
