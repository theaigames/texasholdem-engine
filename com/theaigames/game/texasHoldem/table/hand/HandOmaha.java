package com.theaigames.game.texasHoldem.table.hand;

import com.theaigames.game.texasHoldem.table.cards.Card;

public class HandOmaha extends Hand
{	
	/**
	 * A hand containing four cards
	 * @param firstCard : the first card
	 * @param secondCard : the second card
	 * @param thirdCard : the third card
	 * @param fourthCard : the fourth card
	 */
	public HandOmaha(Card firstCard, Card secondCard, Card thirdCard, Card fourthCard)
	{
		cards = new Card[4];
		cards[0] = firstCard;
		cards[1] = secondCard;
		cards[2] = thirdCard;
		cards[3] = fourthCard;
	}
}

