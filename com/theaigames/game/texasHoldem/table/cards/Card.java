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

import java.util.HashMap;
import java.util.Map;

/**
 * A Card class object represents one card
 */
public class Card
{
	private CardHeight height;
	private CardSuit suit;
	private int number;
	private static Map<String,Card> stringToCard;

	
	/**
	 * Creates a card object based on a number between 0 and 51
	 */
	public Card(int num)
	{
		number = num;
		int findSuit = number / 13;
		switch(findSuit)
		{
			case 0 : suit = CardSuit.SPADES; break;
			case 1 : suit = CardSuit.HEARTS; break;
			case 2 : suit = CardSuit.CLUBS; break;
			default : suit = CardSuit.DIAMONDS;
		}
		
		int findHeight = number % 13;
		switch(findHeight)
		{
			case 0 : height = CardHeight.DEUCE; break;
			case 1 : height = CardHeight.THREE; break;
			case 2 : height = CardHeight.FOUR; break;
			case 3 : height = CardHeight.FIVE; break;
			case 4 : height = CardHeight.SIX; break;
			case 5 : height = CardHeight.SEVEN; break;
			case 6 : height = CardHeight.EIGHT; break;
			case 7 : height = CardHeight.NINE; break;
			case 8 : height = CardHeight.TEN; break;
			case 9 : height = CardHeight.JACK; break;
			case 10 : height = CardHeight.QUEEN; break;
			case 11 : height = CardHeight.KING; break;
			default : height = CardHeight.ACE;
		}
	}
	
	
	
	/**
	 * Returns the Card object that corresponds with the given card string. The first time this method is called, a
	 * map of all Cards corresponding with correct input strings is created.
	 * @param string : the string to be converted to a Card
	 */
	public static Card getCard(String string)
	{
		if(stringToCard == null)
		{
			stringToCard = new HashMap<String,Card>();
			for(int i = 0; i < 52; ++i)
			{
				Card card = new Card(i);
				stringToCard.put(card.toString(), card);
			}
		}
		return stringToCard.get(string);
	}


	/**
	 * Returns the number of the card as a long.
	 */
	public long getNumber()
	{
		int suitShift = number / 13;
		int heightShift = number % 13;
		return (1l << (16*suitShift + heightShift));
	}
	
	
	/**
	 * Returns the height of this card.
	 */
	public CardHeight getHeight()
	{
		return height;
	}
	
	
	/**
	 * Returns the suit of this card.
	 */
	public CardSuit getSuit()
	{
		return suit;
	}
	
	
	/**
	 * Returns a String representation of this card.
	 */
	public String toString()
	{
		String str = null;
		int findHeight = number % 13;
		switch(findHeight)
		{
			case 0 : str = "2"; break;
			case 1 : str = "3"; break;
			case 2 : str = "4"; break;
			case 3 : str = "5"; break;
			case 4 : str = "6"; break;
			case 5 : str = "7"; break;
			case 6 : str = "8"; break;
			case 7 : str = "9"; break;
			case 8 : str = "T"; break;
			case 9 : str = "J"; break;
			case 10 : str = "Q"; break;
			case 11 : str = "K"; break;
			case 12 : str = "A";
		}
		int findSuit = number / 13;
		switch(findSuit)
		{
			case 0 : str += "s"; break;
			case 1 : str += "h"; break;
			case 2 : str += "c"; break;
			default : str += "d";
		}		
		
		return str;
	}
}
