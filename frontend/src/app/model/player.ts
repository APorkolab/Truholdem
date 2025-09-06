import { Card } from "./card";

export class Player {
	id: string;
	name: string;
	hand: Card[];
	chips: number;
	betAmount: number;
	folded: boolean;
	isBot: boolean;

	constructor() {
		this.id = '';
		this.name = '';
		this.hand = [];
		this.chips = 0;
		this.betAmount = 0;
		this.folded = false;
		this.isBot = false;
	}
}