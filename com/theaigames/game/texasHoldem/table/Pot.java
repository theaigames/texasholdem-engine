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

package com.theaigames.game.texasHoldem.table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.theaigames.game.texasHoldem.Player;

/**
 * Class Pot is used for keeping track of the pot size, both from the main pot and
 * the possible side pots, and the players that are involved in the side pots.
 */
public class Pot
{
	private Map<Player, Integer> botBetSizes;
	private int totalPot;
	private int roundPot;
	private BetRound round;
	
	/**
	 * Creates a Pot object, used for keeping track of the pot for a specific hand.
	 * @param bots : integer array of bot IDs that play in the current hand
	 */
	public Pot(Collection<Player> bots)
	{
		botBetSizes = new HashMap<Player, Integer>();
		Iterator<Player> botIterator = bots.iterator();
		while(botIterator.hasNext())
			botBetSizes.put(botIterator.next(), 0);
		
		totalPot = 0;
		roundPot = 0;
		round = BetRound.PREFLOP;
	}
	
	
	/**
	 * Stores the bet of a bot.
	 */
	public void addBet(Player bot, int size, BetRound round)
	{
		botBetSizes.put(bot, botBetSizes.get(bot) + size);
		totalPot += size;
		
		// if a new round has started, then reset the round pot, else update it
		if(round.equals(this.round))
			roundPot += size;
		else
		{
			roundPot = size;
			this.round = round;
		}
	}
	
	
	/**
	 * Return value of function payoutWinners.
	 */
	public class PayoutWinnerInfo
	{
		private ArrayList<Integer> pots;
		private ArrayList<ArrayList<Player>> winnerPerPot;
		public PayoutWinnerInfo(ArrayList<Integer> pots, ArrayList<ArrayList<Player>> winnerPerPot)
		{
			super();
			this.pots = pots;
			this.winnerPerPot = winnerPerPot;
		}
		public ArrayList<Integer> getPots() {
			return pots;
		}
		public ArrayList<ArrayList<Player>> getWinnerPerPot() {
			return winnerPerPot;
		}
	}
	
	/**
	 * Calculates for all the bots which pots they win. It first calculates which main pot and side pots there are.
	 * Then it computes which bot(s) win which pot. The returned ArrayList contains three objects. The first object is
	 * an ArrayList of the pot sizes represented as integers. The second object is an ArrayList of winners per pot,
	 * where each element is itself an ArrayList of winners of the corresponding pot.
	 * @param botHandStrengths : A map of PokerBots paired with their corresponding hand strengths
	 */
	public PayoutWinnerInfo payoutWinners(HashMap<Player, Integer> botHandStrengths)
	{	
		// Calculate with the involved bots how much each bot put in the main pot and how much per side pot
		ArrayList<Integer> involvedBotBets = new ArrayList<Integer>();
		for(Entry<Player, Integer> entry : botHandStrengths.entrySet())
			involvedBotBets.add(botBetSizes.get(entry.getKey()));
		Collections.sort(involvedBotBets);
		ArrayList<Integer> potsAmountPerBot = new ArrayList<Integer>();
		int previousAmount = 0;
		for(int i = 0; i < involvedBotBets.size(); i++)
		{
			potsAmountPerBot.add(involvedBotBets.get(i) - previousAmount);
			previousAmount = involvedBotBets.get(i);
		}
		
		// Get the sizes of the main pot and the side pots
		ArrayList<Player> bots = new ArrayList<Player>(botHandStrengths.keySet());
		ArrayList<Integer> pots = getPots(bots);
		
		// Calculate per pot part which players are winning it
		ArrayList<ArrayList<Player>> winnerPerPot = new ArrayList<ArrayList<Player>>();
		int potIndex = 0;
		int sumHandledPots = 0;
		while(botHandStrengths.size() > 0)
		{		
			// Get out of the remaining bots the bot(s) that has/have the best hand
			int bestHandValue = 0;
			ArrayList<Player> currentBestBots = new ArrayList<Player>();
			for(Entry<Player, Integer> entry : botHandStrengths.entrySet())
			{
				int value = entry.getValue();
				if(value > bestHandValue)
					currentBestBots.clear();
				if(value >= bestHandValue)
				{
					bestHandValue = value;
					currentBestBots.add(entry.getKey());
				}					
			}
				
			// Calculate for each bot with currently the best hand in which remaining pots he is involved
			int maxPotIndex = 0;
			int maxSumHandledPots = 0;
			for(int i = 0; i < currentBestBots.size(); i++)
			{
				int currentPotIndex = potIndex;
				int currentSumHandledPots = sumHandledPots;
				Player currentBot = currentBestBots.get(i);
				botHandStrengths.remove(currentBot);
				while(botBetSizes.get(currentBot) > currentSumHandledPots)
				{
					ArrayList<Player> currentPotWinners = new ArrayList<Player>();
					if(currentPotIndex <= winnerPerPot.size() - 1)
					{
						currentPotWinners = winnerPerPot.get(currentPotIndex);
						currentPotWinners.add(currentBot);
						winnerPerPot.set(currentPotIndex, currentPotWinners);
					}
					else
					{
						currentPotWinners.add(currentBot);
						winnerPerPot.add(currentPotWinners);
					}
					//currentSumHandledPots += pots.get(currentPotIndex++);
					currentSumHandledPots += potsAmountPerBot.get(currentPotIndex++);
				}
				maxPotIndex = Math.max(maxPotIndex, currentPotIndex);
				maxSumHandledPots = Math.max(maxSumHandledPots, currentSumHandledPots);
			}
			potIndex = maxPotIndex;
			sumHandledPots = maxSumHandledPots;
		}
		
		return new PayoutWinnerInfo(pots, winnerPerPot);
	}
	
	
	/**
	 * Calculates the part of the current pot that a given player can win in total. A remaining stack size of the player
	 * must be given. This is useful when a player has less chips than the current raise, because the player can then
	 * only win a part of the total pot. If you know that a player has enough chips to call, then it is faster to just
	 * request the total pot size.
	 * @param bot : the bot to do the request for.
	 * @param maxSize : the amount of chips the given player can add before being all-in.
	 */
	public int getMaxPotToWin(Player bot, int chipsToAllIn)
	{
		int maxBet = botBetSizes.get(bot) + chipsToAllIn;		
		int maxPotPart = 0;
		for(Entry<Player, Integer> entry : botBetSizes.entrySet())
		{
		    int botBet = entry.getValue();
		    if(botBet < maxBet)
		    	maxPotPart += botBet;
		    else
		    	maxPotPart += maxBet;
		}		
		return maxPotPart;
	}
	
	
	/**
	 * Returns whether the pot is empty or not.
	 */
	public boolean isEmpty()
	{
		return totalPot == 0;
	}
	
	
	/**
	 * Returns the total size of the pot.
	 */
	public int getTotalPotSize()
	{
		return totalPot;
	}
	
	
	/**
	 * Returns the sum of bets in the current round.
	 */
	public int getRoundPotSize()
	{
		return roundPot;
	}
	
	
	/**
	 * Returns the size of the main pot and possible side pots, given the bots that are still involved in the hand.
	 * @param involvedBots : the bots that are still in the hand
	 */
	public ArrayList<Integer> getPots(ArrayList<Player> involvedBots)
	{
		ArrayList<Integer> pots = new ArrayList<Integer>();
		Map<Player, Integer> tempBotBetSizes = new HashMap<Player, Integer>(botBetSizes);
		while(involvedBots.size() > 0)
		{
			int lowestBet = Integer.MAX_VALUE;
			int currentPotSize = 0;
			for(int i = 0; i < involvedBots.size(); i++)
				lowestBet = Math.min(tempBotBetSizes.get(involvedBots.get(i)), lowestBet);
			
			for(Entry<Player, Integer> entry : tempBotBetSizes.entrySet())
			{
			    Player key = entry.getKey();
			    int value = entry.getValue();
			    int adjustment = Math.min(value, lowestBet);
			    currentPotSize += adjustment;
				tempBotBetSizes.put(key, value - adjustment);
				if(value == adjustment)
					involvedBots.remove(key);				
			}
			
			pots.add(currentPotSize);
		}
		return pots;
	}
}
