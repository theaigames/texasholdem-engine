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

import java.util.ArrayList;

import com.theaigames.game.texasHoldem.Player;

public class HandResultInfo
{
	private ArrayList<Player> bots;
	private String[] botCodeNames;
	private int[] potParts;
	private Hand[] hands;
	
	public HandResultInfo(ArrayList<Player> botList, String[] botNames, int[] potDistribution)
	{
		bots = botList;
		botCodeNames = botNames;
		potParts = potDistribution;
		hands = new Hand[bots.size()];
	}
	
	
	/**
	 * Sets the hand of a bot that is involved in the showdown.
	 * @param botName : the name of the bot
	 * @param hand : the hand of the bot
	 */
	public void setBotHand(int seat, Hand hand)
	{
		if(seat >= bots.size() || seat < 0)
			System.err.println("The given bot is not part of this match!");
		else
			hands[seat] = hand;
	}
	
	
	/**
	 * Returns a String representation of the match result information.
	 */
	public String toString()
	{
		String str = "";
		for(int i = 0; i < bots.size(); i++)
			if(hands[i] != null)
				str += String.format("%s hand %s\n", botCodeNames[i], hands[i].toString());
		for(int i = 0; i < bots.size(); i++)
			if(potParts[i] > 0)
				str += String.format("%s wins %d\n", botCodeNames[i], potParts[i]);
				
		str = str.trim();
		return str;
	}
}
