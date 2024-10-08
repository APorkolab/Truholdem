import { Card } from "./card";

export interface Player {
	id: string;
	name: string;
	hand: Card[];
	chips: number;
	betAmount: number;
	folded: boolean;
	isBot: boolean;
}