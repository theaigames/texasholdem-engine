// Copyright 2015 theaigames.com (developers@theaigames.com)

//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at

//        http://www.apache.org/licenses/LICENSE-2.0

//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//	
//    For the full copyright and license information, please view the LICENSE
//    file that was distributed with this source code.

package com.theaigames.game.texasHoldem.table.cards;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Class representing a single deck of cards, which is shuffled in random order.
 * Cards can be drawn from the deck.
 */
public class Deck
{
	private ArrayList<Integer> cardOrder;
	private ArrayList<Integer> cardOrderSavePoint;
	
	/**
	 * Creates a new deck of 52 cards, represented by integers 0 to 51, which are
	 * then shuffled.
	 */
	public Deck()
	{
		cardOrder = new ArrayList<Integer>();
		for(int i = 0; i < 52; i++)
			cardOrder.add(i);
		
		Collections.shuffle(cardOrder);
	}
	
	
	/**
	 * Refreshes the deck such that it is a shuffled deck of 52 cards again.
	 */
	public void resetDeck()
	{
		cardOrder = new ArrayList<Integer>();
		for(int i = 0; i < 52; i++)
			cardOrder.add(i);
		
		Collections.shuffle(cardOrder);
	}
	
	
	/**
	 * Set a save point for the deck status, can be used for trying multiple random draws from a non-complete deck.
	 */
	public void setSavePoint()
	{
		cardOrderSavePoint = (ArrayList<Integer>) cardOrder.clone();
	}
	
	
	/**
	 * Set the deck back to the status of the last restore point, reshuffling the remaining cards.
	 */
	public void restoreToSavePoint()
	{
		cardOrder = (ArrayList<Integer>) cardOrderSavePoint.clone();
		Collections.shuffle(cardOrder);
	}
	
	
	/**
	 * Pushes and returns the next card from the deck.
	 */
	public Card nextCard()
	{
		if(cardOrder.size() <= 0)
		{
			System.err.println("The deck is empty");
			return null;
		}
			
		int nextCardNumber = cardOrder.remove(cardOrder.size() - 1);
		Card card = new Card(nextCardNumber);
		return card;
	}
}
