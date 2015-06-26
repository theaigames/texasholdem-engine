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

package com.theaigames.game.texasHoldem.match;

import java.util.ArrayList;

import com.theaigames.game.texasHoldem.Player;

public class MatchInfo
{
	private MatchInfoType infoType;
	private ArrayList<Player> bots;
	private String[] botCodeNames;
	private int mySeat;
	private boolean isTournament;
	private int totalBots;
	private int prizepoolSize;
	private int handsPerLevel;
	private int startingStack;
	private int sizeBB, sizeSB;
	
	public MatchInfo(MatchInfoType type, ArrayList<Player> botList, String[] botNames, boolean tournament,
					 int handsPerBlindLevel, int stackSize, int BBsize, int SBsize,
					 int totalPlayers, int numberOfPrizes)
	{
		infoType = type;
		bots = botList;
		botCodeNames = botNames;
		isTournament = tournament;
		totalBots = totalPlayers;
		prizepoolSize = numberOfPrizes;
//		timeBank = (int) bankTime;
//		timePerMove = (int) moveTime;
		handsPerLevel = handsPerBlindLevel;
		startingStack = stackSize;
		sizeBB = BBsize;
		sizeSB = SBsize;
	}
	
	
	/**
	 * Sets on which seat the bot is that receives this MatchInfo
	 * @param botName : the name of the bot
	 */
	public void setCurrentBotInfo(int seat)
	{
		if(seat >= bots.size() || seat < 0)
			System.err.println("The given bot is not part of this match!");
		mySeat = seat;
	}
	
	
	/**
	 * Returns a String representation of the match information.
	 */
	public String toString()
	{
		String str = "";
		if(infoType.equals(MatchInfoType.FIRST_TABLE))
		{
//			str += String.format("Settings timeBank %d\n", timeBank);
//			str += String.format("Settings timePerMove %d\n", timePerMove);
			if(isTournament)
			{
				str += String.format("Settings hands_per_level %d\n", handsPerLevel);
				str += String.format("Settings starting_stack %d\n", startingStack);
			}
			else
			{
				str += String.format("Settings small_blind %d\n", sizeSB);
				str += String.format("Settings big_blind %d\n", sizeBB);
			}
//			str += String.format("Settings your_bot %s\n", botCodeNames[mySeat]);
		}
		//not needed in headsup
		// else if(isTournament)
		// 	str += String.format("Settings table new\n");
		
		// str += String.format("Settings players %d\n", bots.size());		
		
//		if(infoType.equals(MatchInfoType.FIRST_TABLE)) 
//		{
			//not needed in headsup
//			if(isTournament) 
//			{
//				str += String.format("Settings totalPlayers %d\n", totalBots);
//			 	str += String.format("Settings numberOfPrizes %d\n", prizepoolSize);
//			}
//			str += String.format("Settings yourBot %s\n", botCodeNames[mySeat]);
//		}
		
		//not needed in headsup
//		for(int i = 0; i < bots.size(); i++)
//			str += String.format("%s seat %d\n", botCodeNames[i], i);
		
		str = str.trim();
		return str;
	}
}
