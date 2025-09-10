import { Card } from "./card";
import { Player } from "./player";

export class Game {
	currentPot: number;
	players: Player[];
	communityCards: Card[];
	phase: string;
	currentBet: number;
	playerActions: Record<string, boolean>;
	playersWhoHaveNotActed?: Player[];  // Opcionális mező, mivel néha hiányozhat

	constructor() {
		this.currentPot = 0;
		this.players = [];
		this.communityCards = [];
		this.phase = '';
		this.currentBet = 0;
		this.playerActions = {};
		this.playersWhoHaveNotActed = [];
	}
}

