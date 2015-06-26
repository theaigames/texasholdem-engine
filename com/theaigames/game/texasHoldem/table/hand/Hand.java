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

package com.theaigames.game.texasHoldem.table.hand;

import com.theaigames.game.texasHoldem.table.cards.Card;

public abstract class Hand
{
	protected Card[] cards;
	
	
	/**
	* Returns a specific card of this hand
	*/
	public Card getCard(int index)
	{
		if(index >= 0 && index < cards.length)
			return cards[index];
		else
			return null;
	}
	
	
	/**
	 * Returns the number of cards in the hand/
	 */
	public int getNumberOfCards()
	{
		return cards.length;
	}
	
	
	/**
	* Returns an array of the two hand cards
	*/
	public Card[] getCards()
	{
		return cards;
	}
	
	
	/**
	 * Returns a string representation of the hand
	 */
	public String toString()
	{
		String str = "[";
		for(int i = 0; i < cards.length - 1; i++)
			str += cards[i].toString() + ",";
		
		str += cards[cards.length - 1].toString() + "]";
		return str;
	}
}
