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

public class HandInfo
{
	private HandInfoType infoType;
	private boolean isTournament;
	private int round;
	private int playersLeft;
	private ArrayList<Player> bots;
	private String[] botCodeNames;
	private int[] botStacks;
	private int sizeBB, sizeSB;
	private int mySeat;
	private Hand myHand;
	private int buttonSeat;
	private String table;
	
	public HandInfo(HandInfoType type, boolean isTourney, int roundNumber, int playersLeftInTournament,
			ArrayList<Player> botList, String[] botNames, int[] stacks, int bigBlindSize, int smallBlindSize,
			int button, String tableCards)
	{
		infoType = type;
		isTournament = isTourney;
		round = roundNumber;
		playersLeft = playersLeftInTournament;
		bots = botList;
		botCodeNames = botNames;
		botStacks = stacks;
		sizeBB = bigBlindSize;
		sizeSB = smallBlindSize;
		buttonSeat = button;
		table = tableCards;
	}
	
	
	/**
	 * Sets on which seat the bot is that receives this HandInfo
	 * @param botName : the name of the bot
	 */
	public void setCurrentBotInfo(int seat, Hand hand)
	{
		if(seat >= bots.size() || seat < 0)
			System.err.println("The given bot is not part of this match!");
		mySeat = seat;
		myHand = hand;
	}
	
	/**
	 * Returns a String representation of the current table situation.
	 */
	public String toString()
	{
		String str = "";
		
		if(infoType.equals(HandInfoType.HAND_CARDS)) {
			str += String.format("%s hand %s\n", botCodeNames[mySeat], myHand.toString());
		}
		
		else if(infoType.equals(HandInfoType.HAND_START))
		{
			str += String.format("Match round %d\n", round);
			
			// only give the blind sizes at the start of each hand for tournaments
			if(isTournament)	
			{
				//not needed in headsup
				// str += String.format("Match totalPlayersLeft %d\n", playersLeft);
				str += String.format("Match smallBlind %d\n", sizeSB);
				str += String.format("Match bigBlind %d\n", sizeBB);
			}
			str += String.format("Match onButton %s\n", botCodeNames[buttonSeat]);
			for(int i = 0; i < bots.size(); i++)
				if(botStacks[i] > 0)
					str += String.format("%s stack %d\n", botCodeNames[i], botStacks[i]);
		}
		
		else if( infoType.equals(HandInfoType.NEW_BETROUND) ) {
			str += String.format("Match table %s\n", table);
		}
		
		str = str.trim();
		return str;
	}
}
