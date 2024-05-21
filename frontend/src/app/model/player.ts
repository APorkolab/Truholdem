import { Card } from "./card";

export interface Player {
	id: string;
	name: string;
	hand: Card[];
	chips: number;
	currentBet: number;
	folded: boolean;
	isBot: boolean;
}