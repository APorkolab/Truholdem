import { Card } from "./card";

export interface Player {
	id: string;
	name: string | null;
	hand: Card[];
	chips: number;
	currentBet: number;
	folded: boolean;
}