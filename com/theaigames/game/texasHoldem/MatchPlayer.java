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

package com.theaigames.game.texasHoldem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Vector;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.stevebrecher.HandEval;
import com.theaigames.game.texasHoldem.match.MatchInfo;
import com.theaigames.game.texasHoldem.match.MatchInfoType;
import com.theaigames.game.texasHoldem.move.PokerMove;
import com.theaigames.game.texasHoldem.move.PreMoveInfo;
import com.theaigames.game.texasHoldem.table.BetRound;
import com.theaigames.game.texasHoldem.table.Pot;
import com.theaigames.game.texasHoldem.table.cards.Card;
import com.theaigames.game.texasHoldem.table.cards.Deck;
import com.theaigames.game.texasHoldem.table.hand.Hand;
import com.theaigames.game.texasHoldem.table.hand.HandHoldem;
import com.theaigames.game.texasHoldem.table.hand.HandInfo;
import com.theaigames.game.texasHoldem.table.hand.HandInfoType;
import com.theaigames.game.texasHoldem.table.hand.HandOmaha;
import com.theaigames.game.texasHoldem.table.hand.HandResultInfo;

/**
 * Class that acts as engine for playing a game of poker at one table. It regulates all the actions and information
 * needed for playing a match, including the communication with the involved bots. The class can be used for both
 * holdem and omaha, for no limit and pot limit and for cash games and tournaments. For multi-table tournaments one
 * MatchPlayer should be made per table and when the players are re-devided over the tables, new MatchPlayers must be
 * made.
 */
public class MatchPlayer
{
	// current game and hand information
	private boolean finishedSetup;					// whether the setup has been finalized or not
	private int gameCode;							// short string representation of the game
	private boolean isTournament;					// whether the playing mode is tournament mode or cash game
	private int tournamentTableRound;				// number of times bots in a tournament are re-devided over tables
	private int tournamentTableNumber;				// integer id to identify this table in a tournament
	private int numberOfPlayersInTournament;		// total players participating/alive in case of a tournament
	private int numberOfPricesInTournament;			// top number of players that wins a prize in a tournament
	private int numberOfPlayersAtStartHand;			// number of players alive at the start of the current hand
	private int handNumber;							// current hand number of this match
	private int blindLevel;							// current blind level
	private ArrayList<Player> players;				// list of bots seated at the table
	private String[] botCodeNames;					// names of the bots as used in communication to the bots
	private int numberOfBots;						// length of the list 'bots', stored separaty because of high usage
	private Deck deck;								// card deck that is played with
	private Pot pot;								// object that keeps track of the pot sizes and involvement of bots
	private BetRound round;							// bet round within the hand: preflop, flop, turn or river
	private Vector<Card> tableCards;				// cards that are on the table
	private Hand[] botHands;						// hand cards of all the bots
	private String handHistory;						// used to store the match progress
	private int[] botStacksAtHandStart;				// amount of chips each bot had at the start of the current hand
	private int[] botStacks;						// amount of chips each bot has currently
	private int[] botBetsThisRound;					// amount of chips each bot has put in in the current bet round
	private boolean isAllowedToRaise;				// whether further raises are allowed in the current bet round
	private boolean noSmallBlindPayed;				// whether the small blind has to be payed this hand
	private int[] blindPriorities;					// number of hands that each bot played since being on the big blind
	
	// seat information, as index in list 'bots'
	private int bigBlindSeat;						// position of the big blind in the current hand
	private int smallBlindSeat;						// position of the small blind in the current hand
	private int buttonSeat;							// position of the dealer button in the current hand
	private int activeSeat;							// bot who's turn it is at the moment
	private int lastToActSeat;						// last bot that must make a move before going to the next bet round
	private int lastToMayRaiseSeat;					// last bot that is allowed to raise further
	
	// amounts information
	private int sizeBB, sizeSB;						// size of both the blinds in chips
	private int sizeMinimalRaise;					// minimal raise size that is allowed
	private int sizeMaximalRaise;					// maximal raise size that is allowed, if applied
	private int sizeCurrentRaise;					// size of the last normal raise in the current bet round
	
	// bot progress in the match
	private boolean[] isInvolvedInHand;				// which bots are still involved in the current hand
	private boolean[] isInvolvedInMatch;			// which bots are still alive in the match
    private int[] finishPosition;					// finishing positions of the bots, used for tournament mode
    private int finishedBots;						// number of bots that are finished now, used for tournament mode
    private int[] botGainLoss;						// amount of chips each bot won or lost in total, used for cash mode
    	
    // some match constants
	private final int SIZE_STARTSTACK;				// stack size that each bot starts the match with
	private final int HANDS_PER_BLINDLEVEL = 10;	// hands played before increasing the blinds, for tournament mode
	private final int ODDS_RUNS = 1000;				// number of simulations to determine the win chances of each bot	
	private final int[] BLINDLEVELHEIGHTS = {		// the size of the big blind in the consecutive blind levels
							20, 30, 40, 50, 60, 80,
							100, 120, 160, 200, 240, 300, 400, 500, 600, 800,
							1000, 1200, 1600, 2000, 2400, 3000, 4000, 5000, 6000, 8000,
							10000, 12000, 16000, 20000, 24000, 30000, 40000, 50000, 60000, 80000,
							100000, 120000, 160000, 200000, 240000, 300000, 400000, 500000, 600000, 800000,
							1000000};
	
	// some constructor input dependent constants, with easy human readable possible values
	private int gameType;							// whether holdem or omaha is played
	private final int HOLDEM = 0;
	private final int OMAHA = 1;	
	private int gameLimit;							// whether no limit or pot limit is played
	private final int NO_LIMIT = 0;
	private final int POT_LIMIT = 1;
	
	private Player winner;
	private String allHistory = "";
	
	
	/**
	* Constructor for a poker match engine. Setup a table with the given bots, so that a match can be played.
	* @param botList : a collection of PokerBots that play on this table.
	* @param game : the game that is being played, a code number for the game type.
	* @param startingStack : the stack size the bots start the game with.
	*/
	public MatchPlayer(Collection<Player> players, int game, int startingStack)
	{
		// current game and hand information
		finishedSetup = false;
		gameCode = game;
		handNumber = 0;
		blindLevel = 0;
		
		// game type and limit constants
        switch(gameCode)
        {
        case 11:
        case 12:
        case 13:
        	gameType = HOLDEM;
        	gameLimit = NO_LIMIT;
        	isTournament = true;
        	break;
        case 14:
        case 15:
        	gameType = HOLDEM;
        	gameLimit = NO_LIMIT;
        	isTournament = false;
        	break;
        case 16:
        case 17:
        case 18:
        	gameType = OMAHA;
        	gameLimit = POT_LIMIT;
        	isTournament = true;
        	break;
        case 19:
        case 20:
        	gameType = OMAHA;
        	gameLimit = POT_LIMIT;
        	isTournament = false;
        	break;
        default:
        	throw new RuntimeException("QUIT - MatchPlayer invoked with invalid game code: " + gameCode);
        }
        
        // initialize a lot of variables
        tournamentTableRound = 0;
		tournamentTableNumber = 0;				
		this.players = new ArrayList<Player>(players);			
		numberOfBots = players.size();
		numberOfPlayersInTournament = 0;
		numberOfPlayersAtStartHand = numberOfBots;
		deck = new Deck();
		pot = new Pot(players);
		round = BetRound.PREFLOP;
		tableCards = new Vector<Card>();
		if(gameType == HOLDEM)
			botHands = new HandHoldem[numberOfBots];
		else
			botHands = new HandOmaha[numberOfBots];
		
		handHistory = "";
		SIZE_STARTSTACK = startingStack;
		botStacks = new int[numberOfBots];
		botBetsThisRound = new int[numberOfBots];
//		botTimeBanks = new long[numberOfBots];
		blindPriorities = new int[numberOfBots];
		botCodeNames = new String[numberOfBots];
		for(int i = 0; i < numberOfBots; i++)
		{
			botCodeNames[i] = "player" + (i+1);
			botStacks[i] = SIZE_STARTSTACK;
			blindPriorities[i] = 1000;	// random, large enough
		}
		
		// seat information, the big blind position is specified, the other seat information will be updated
		// automatically at the start of the first hand
		bigBlindSeat = numberOfBots - 1;		
		smallBlindSeat = -1;
		buttonSeat = -1;
		activeSeat = -1;
		lastToActSeat = -1;
		lastToMayRaiseSeat = -1;
		isAllowedToRaise = true;
		noSmallBlindPayed = false;		
		
		// amounts information
		sizeBB = BLINDLEVELHEIGHTS[0];
		sizeSB = sizeBB / 2;
		sizeMinimalRaise = 0;
		sizeMaximalRaise = 0;
		sizeCurrentRaise = 0;
		
		// bot progress in the match
		isInvolvedInHand = new boolean[numberOfBots];
		isInvolvedInMatch = new boolean[numberOfBots];
        finishPosition = new int[numberOfBots];
        finishedBots = 0;
		for(int i = 0; i < numberOfBots; i++)
		{
			isInvolvedInHand[i] = true;
			isInvolvedInMatch[i] = true;
            finishPosition[i] = 0;
		}
		if(!isTournament)
			botGainLoss = new int[numberOfBots];
	}
	
	
	/**
	 * Sets the total number of players in case of a tournament. Can be used to update the number of players that is
	 * still alive in the tournament. This value is treated completely unconnected to the number of players at this
	 * table, it is only used to inform bots about the tournament status. Calling this method has no effect when the
	 * game is not a tournament. If called, it should be done before calling the method 'finishSetup'.
	 * @param playerCount : the current number of tournament players.
	 */
	public void setNumberOfPlayersInTournament(int playerCount)
	{
		numberOfPlayersInTournament = playerCount;
	}

	
	/**
	 * The number of finishing places in a tournament that get a prize. This value has no effect on the match, it is
	 * only used to inform bots about the tournament setup. Calling this method has no effect when the game is not a
	 * tournament. If called, it should be done before calling the method 'finishSetup'.
	 * @param prizesCount : the amount of players that get a prize.
	 */
	public void setNumberOfPlacedPaidInTournament(int prizesCount)
	{
		numberOfPricesInTournament = prizesCount;
	}
	
	
	/**
	 * Set the list of names for all the bots on this table that is used for communication towards the involved bots.
	 * Default code names are initialized, so calling this method can be skipped. But in case of multi-table
	 * tournaments this option can be used to keep bot code names consistent throughout the match, when bots are
	 * re-divided over tables now and then.
	 * @param names : a string list of bot communication names.
	 */
	public void setBotCommunicationNames(String[] names)
	{
		if(names.length != botCodeNames.length)
		{
			System.err.format("MatchPlayer: warning, ignoring the given set of bot code names since the list has" +
					" length %d instead of %d.", names.length, botCodeNames.length);
			return;
		}
		for(String name : names)
			if(name.contains(" "))
			{
				System.err.println("MatchPlayer: warning, ignoring the given set of bot code names since one of them" +
						" contains a space.");
				return;
			}
		botCodeNames = names;
	}
	
	
	/**
	 * Set the number of hands that each bot played since paying the big blind. This information is useful in multi-
	 * table tournaments for fair blind payments. Should be called if this table is not one of the tables at the start
	 * of a tournament.
	 * @param prios : the number of hands each bot played since paying a big blind.
	 */
	public void setBlindPriorities(int[] prios)
	{
		blindPriorities = prios;
	}
	
	
	/**
	 * Sets the starting stacks of the players according to the given input, the order in which the bot list was given
	 * is used to assign the stack sizes. Should be called before the first call to the run(...) method. Without a call
	 * to this method all bots with start with a default stack size.
	 * @param stacks : integer array of starting stacks.
	 */
	public void setStacksSizes(int[] stacks)
	{
		if(stacks.length == numberOfBots)
			for(int i = 0; i < numberOfBots; i++)
				botStacks[i] = stacks[i];
		else
			System.err.format("Failed setting the starting stacks because the list of stacks is not of length %d.",
					numberOfBots);
	}
	
	/**
	 * Sets the table round within a tournament, being the number of times that all the bots are re-devided over tables.
	 * Also sets a unique id for this table, to distinguish it from the other tables within a round. This extra
	 * information is useful in a multi-table tournament in which a single match is played on several tables. If called,
	 * it should be done before calling the method 'finishSetup'.
	 * @param tableRound : number of times the bot division over the tables changed.
	 * @param tableNumber : unique id of this table within a table round.
	 */
	public void setTournamentTableInfo(int tableRound, int tableNumber)
	{
		tournamentTableRound = tableRound;
		tournamentTableNumber = tableNumber;
	}
	
	
	/**
	 * Set the hand number with which the match will start (where the first hand would be 1, not 0). Setting the hand
	 * number determines the blind level that is used in this match. Without a call to this method it will start at the
	 * lowest blind level. Setting the hand number can be useful to regulate the blind levels in multi-table
	 * tournaments.
	 * @param hand : the hand number.
	 */
	public void setHandNumber(int number)
	{
		handNumber = number - 1;
		// in a tournament, increase the blinds after every fixed amount of hands
		if(isTournament)
		{
			blindLevel = handNumber / HANDS_PER_BLINDLEVEL;
			if(blindLevel >= BLINDLEVELHEIGHTS.length)
				blindLevel = BLINDLEVELHEIGHTS.length - 1;
			sizeBB = BLINDLEVELHEIGHTS[blindLevel];
			sizeSB = sizeBB / 2;
		}
	}
	
	
	/**
	 * Tells this MatchPlayer that all information for a correct setup is given. When calling this method, the involved
	 * bots will receive the match information from this MatchPlayer, provided that they did not already receive it.
	 * @param sendAllMatchInfo : whether the involved bots should be informed about all the game settings. Could be set
	 * to false for multi-table tournaments if this is not one of the first round tables.
	 */
	public void finishSetup(boolean sendAllMatchInfo)
	{
		if(finishedSetup)
			return;
		
		if(sendAllMatchInfo)
			sendMatchInfo(MatchInfoType.FIRST_TABLE);
		else
			sendMatchInfo(MatchInfoType.NEXT_TABLE);
		
		finishedSetup = true;
	}

	/**
	 * Plays a single round 
	 */
	public void playRound()
	{
		// in a tournament, increase the blinds after every fixed amount of hands
		if(isTournament && handNumber == (blindLevel + 1) * HANDS_PER_BLINDLEVEL &&
		   blindLevel < BLINDLEVELHEIGHTS.length - 1)
		{
			blindLevel++;
			sizeBB = BLINDLEVELHEIGHTS[blindLevel];
			sizeSB = sizeBB / 2;
		}
		
		// invoke the whole procedure for playing one hand
		playHand();
		handHistory += "\nMatch end hand";
		writeHistory();
		
		// a tiny sleep in a while loop seems to improve performance regarding the cpu
		try {Thread.sleep(5);}
		catch (InterruptedException e) {e.printStackTrace();}
	}
	
	
	/**
	 * Returns the stack size that the bot of the given index had at the start of the last hand.
	 * @param index : the index of the bot in the bot list of this MatchPlayer.
	 */
	public int getBotStartStack(int index)
	{
		if(index >= 0 && index < numberOfBots)
			return botStacksAtHandStart[index];
		else
			return 0;
	}
	
	
	/**
	 * Returns the current stack sizes of the bots, in the same order as the bot list that was given initially.
	 */
	public int[] getCurrentStacks()
	{
		return botStacks;
	}
	
	
	/**
	 * Returns a list with for each bot the number of hands it has played since being on the big blind. This
	 * information can be used in multi-table tournaments to provide a fair placing of bots regarding blinds payment
	 * when bots are distributed over new tables.
	 */
	public int[] getCurrentBlindPriorities()
	{
		return blindPriorities;
	}
		
	
	/**
	* This method acts as the controller for playing one hand in the match. It handles the setup for the new hand, the
	* blind payments, the dealing of cards, all the actions during the hand and the distribution of the pot at the end
	* of the hand.
	*/
	private void playHand()
	{
		// copy the stacks of all bots to look up later with what amount of chips they started the hand
        botStacksAtHandStart = Arrays.copyOf(botStacks, botStacks.length);

        // move the blind positions for the new hand, let the blinds be payed and deal the hand cards
		setupNextHand();
		payBlinds();
		dealHandCards();
		
		// the bot behind the big blind is first to act in the new hand
		activeSeat = bigBlindSeat;
		nextBotActive();
		
		// start the repeated sequence of setting up a bet round and giving bots the opportunity to make actions
		boolean handFinished = false;				
		while(!handFinished)
		{	
			// the betting round is finished when we are in a situation that no bot actions are needed
			boolean roundFinished = false;
			if(!setupBetRound())
				roundFinished = true;
			
			// while the round is not finished, give turns to the bots
			while(!roundFinished)
			{				
				askBotAction();
				
				// if the last bot that is allowed to make a move just made its move, or when all but one bots folded,
				// then the betting round is finished. Else, pass the action on to the next bot.
				if(lastToActSeat == activeSeat || numberOfRemainingBots(1) < 2)
					roundFinished = true;
				else
					nextBotActive();
			}
			
			// if there are less than two bots involved or when we are already on the river, then the hand is finished
			if(numberOfRemainingBots(1) <= 1 || !dealNextStreet())
				handFinished = true;
		}
		
		// now distribute the pot to the winners
		distributePot();
		
		// if the playing mode is cash, store gain/loss per bot and reset all stacks to starting stack size
		if(!isTournament)
		{
			for(int i = 0; i < numberOfBots; i++)
			{
				botGainLoss[i] += botStacks[i] - SIZE_STARTSTACK;
				botStacks[i] = SIZE_STARTSTACK;
				isInvolvedInMatch[i] = true;
				isInvolvedInHand[i] = true;
			}
		}
		// if this is a tournament, all bots that are still active in the match are set to active in the new hand. Also
		// store which bots are eliminated in the last hand
		else
		{
			List<Integer> eliminated = new ArrayList<Integer>();
			for(int i = 0; i < numberOfBots; i++)
			{
				if(isInvolvedInMatch[i] && botStacks[i] > 0)
					isInvolvedInHand[i] = true;
				else
				{
					isInvolvedInHand[i] = false;
					isInvolvedInMatch[i] = false;
	                if(finishPosition[i] == 0)
	                    eliminated.add(i);
				}
			}

	        // store the finishing positions of the eliminated bots. If more than one, then the bot with the higher
			// stack at the start of the hand gets the better finishing position. If two or more started the hand with
			// the same stack size then the bot further away from the dealer button ends with the better position.
	        Collections.sort(eliminated, new Comparator<Integer>()
	        {
	            @Override
	            public int compare(Integer p1, Integer p2)
	            {
	                if(botStacksAtHandStart[p1] != botStacksAtHandStart[p2])
	                    return botStacksAtHandStart[p1] - botStacksAtHandStart[p2];
	
	                int pos1 = (p1 - smallBlindSeat + numberOfBots) % numberOfBots;
	                int pos2 = (p2 - smallBlindSeat + numberOfBots) % numberOfBots;
	                return pos1 - pos2;
	            }
	        });
	        for(Integer p : eliminated)
	            finishPosition[p] = numberOfBots - finishedBots++;

	        //set last bot's finish position
	        if(numberOfBots - finishedBots == 1)
		        for(int i = 0; i < numberOfBots; i++)
					if(finishPosition[i] == 0)
						finishPosition[i] = 1;
		}
	}
	
	
	/**
	 * Returns the amount of bots that is still active in the match or in the current hand. The parameter defines how
	 * to interpret this query.
	 * @param type : 0 = still involved in the match, 1 = still involved in the current hand, 2 = still involved in the
	 * current hand and not all-in yet, so still able to make moves.
	 */
	public int numberOfRemainingBots(int type)
	{
		int numberOfBotsInvolved = 0;
		if(type == 1)
		{
			for(int i = 0; i < numberOfBots; i++)
				if(isInvolvedInHand[i])
					numberOfBotsInvolved++;
		}
		else if(type == 2)
		{
			for(int i = 0; i < numberOfBots; i++)
				if(isInvolvedInHand[i] && botStacks[i] > 0)
					numberOfBotsInvolved++;
		}
		else
		{
			for(int i = 0; i < numberOfBots; i++)
				if(isInvolvedInMatch[i])
					numberOfBotsInvolved++;
		}

		return numberOfBotsInvolved;
	}
	
	
	/**
	* Prepares for playing the next hand, the big blind is moved to the next bot and all bots are reset to being
	* involved in the next hand. If the game is not a tournament then the gain or loss per bot on the last hand is
	* stored and all stack sizes are reset to the standard starting stack and all bots are reset to being involved in
	* the match.
	*/
	private void setupNextHand()
	{			
		// increment the hand number and reset the table cards, the card deck and the pot
		handNumber++;
		tableCards = new Vector<Card>();
		deck.resetDeck();
		pot = new Pot(players);
		round = BetRound.PREFLOP;
		
		// record the number of bots at the start of this hand
		numberOfPlayersAtStartHand = numberOfRemainingBots(0);
		
		// update the position of the dealer button and the blind position and reset the bets of the bots
		setNextBlindsAndButtonPositions();
		botBetsThisRound = new int[numberOfBots];
		
		// send information about the new hand to the bots and write information to the history
		sendHandInfo(HandInfoType.HAND_START);
		handHistory += String.format("\nMatch hand %d", handNumber);
		handHistory += String.format("\nMatch dealerButton %s", players.get(buttonSeat).getName());
		for(int i = 0; i < numberOfBots; i++)
			if(botStacks[i] > 0)
				handHistory += String.format("\n%s stack %d", players.get(i).getName(), botStacks[i]);
	}
	
	
	/**
	 * Prepares for playing the next betting round of the current hand. This involves setting the bot that is next to
	 * act and that is last to act. It checks whether more actions are needed, which depends on bots being all-in by
	 * paying the blinds or bots going all-in later on. Returns a boolean telling whether more bot actions are needed.
	 */
	private boolean setupBetRound()
	{	
		lastToMayRaiseSeat = -1;
		
		// when a new bet round of the hand starts, raising is allowed, with a minimal size of one big blind
		isAllowedToRaise = true;
		sizeMinimalRaise = sizeBB;
		
		// no new bet round is needed if there are less than two bots still involved in the hand or when all involved
		// bots are already all-in
		int numberOfBotsInHand = numberOfRemainingBots(1);
		int numberOfBotsActiveInHand = numberOfRemainingBots(2);
		if(numberOfBotsInHand < 2 || numberOfBotsActiveInHand == 0)
			return false;
				
		// the setup for a preflop situation is different from later bet rounds and is handled here
		if(round.equals(BetRound.PREFLOP))
		{
			// the bot directly behind the big blind is to act first. The big blind is last to act
			activeSeat = bigBlindSeat;
			nextBotActive();
			lastToActSeat = bigBlindSeat;
			lastToMayRaiseSeat = lastToActSeat;

			// find the size of the largest blind that has been paid, usually but not necessarily equals the big blind
			sizeCurrentRaise = 0;
			for(int i = 0; i < numberOfBots; i++)
			{
				if(botBetsThisRound[i] > sizeCurrentRaise)
					sizeCurrentRaise = botBetsThisRound[i];
			}
			
			// handle some special cases with only two bots in which no further action might be needed
			// (numberOfBotsInHand == numberOfPlayersAtStartHand for the preflop case)
			if(numberOfBotsInHand == 2 && numberOfBotsActiveInHand == 1)
			{
				// if after paying the blinds the small blind is all-in or the big blind is all-in with an amount less
				// than a small blind, then there are no further bot actions needed
				if(botStacks[smallBlindSeat] == 0 || botBetsThisRound[bigBlindSeat] <= sizeSB)
					return false;
				
				// if the big blind is all-in by paying his blind and it is larger than the small blind, then the small
				// blind still has to make a decision, he is the last bot to act
				else
				{
					lastToActSeat = buttonSeat;
					lastToMayRaiseSeat = lastToActSeat;
				}
			}		
		}
		// the setup for the flop, turn or river
		else
		{
			// if only one of the involved bots is not all-in yet, then there are no further actions
			if(numberOfBotsActiveInHand == 1)
				return false;	
			
			// the small blind is first to act on postflop streets, except when the hand started with exactly two bots
			if(numberOfPlayersAtStartHand == 2)
				activeSeat = bigBlindSeat;
			else
				activeSeat = smallBlindSeat;

			// the button is the last seat to act, in case of only two players this equals the small blind position
			lastToActSeat = buttonSeat;
			lastToMayRaiseSeat = lastToActSeat;
			
			// reset the bet size per round
			sizeCurrentRaise = 0;
			botBetsThisRound = new int[numberOfBots];
		}
		
		return true;
	}
	
	
	/**
	 * Advances the blinds and the button in preparation for the next round. When only two players are left, the small
	 * blind position also gets the button. The standard "dead button" rule is used, meaning that the big blind will
	 * advance to the next still active player and the small blind and button are placed at the two active players in
	 * front of it. But if one or more players gets eliminated, then the small blind sometimes isn't payed and the
	 * button may stay at the same position.
	 */
	private void setNextBlindsAndButtonPositions()
	{		
		noSmallBlindPayed = false;
		
		// move the big blind position to the next bot that is still involved in the match
		while(true)
		{
			bigBlindSeat++;
			if(bigBlindSeat == numberOfBots)
				bigBlindSeat = 0;
			
			if(isInvolvedInMatch[bigBlindSeat])
				break;	
		}
		
		// if more than 2 players are left in the match, the small blind and button are the bots before the big blind
		if(numberOfPlayersAtStartHand > 2)
		{
			// loop backwards to the involved bot before the big blind
			int seatToCheck = bigBlindSeat;
			while(true)
			{
				seatToCheck--;
				if(seatToCheck < 0)
					seatToCheck = numberOfBots - 1;
				
				// the small blind should advance as well, so the seat to check may not pass the previous small blind
				// position. If it does, then no small blind is being payed in this new hand
				if(seatToCheck == smallBlindSeat)
				{
					noSmallBlindPayed = true;
					if(++seatToCheck == numberOfBots)
						seatToCheck = 0;
						
					break;
				}
				if(isInvolvedInMatch[seatToCheck])
					break;
			}
			smallBlindSeat = seatToCheck;
			
			// loop backwards to the involved bot before the small blind, this bot gets the button
			while(true)
			{
				seatToCheck--;
				if(seatToCheck < 0)
					seatToCheck = numberOfBots - 1;
				
				if(isInvolvedInMatch[seatToCheck])					
					break;
			}
			buttonSeat = seatToCheck;
		}
		// with only two players left in the match, the button seat equals the small blind seat
		else
		{
			for(int i = 0; i < numberOfBots; i++)
			{
				if(isInvolvedInMatch[i] && i != bigBlindSeat)
				{
					buttonSeat = i;
					smallBlindSeat = i;
					break;
				}
			}
		}
	}
		
	
	/**
	 * Asks the currently active bot to act, provided that it has a non-zero stack and is still involved in the current
	 * hand. Checks and possibly alters the received action according to the playing rules. Updates the state of the
	 * hand according to the chosen action. The involved bots are also informed about the action of the current bot.
	 */
	private void askBotAction()
	{
		if(botStacks[activeSeat] > 0 && isInvolvedInHand[activeSeat])
		{
			// the minimal call amount preflop with 3 or more players is the size of the BB, also when the bot on the
			// big blind is all-in with less than the size of a big blind. 
			int amountToCall; 
			if(round == BetRound.PREFLOP && numberOfRemainingBots(1) > 2 && sizeCurrentRaise < sizeBB)
				amountToCall = sizeBB - botBetsThisRound[activeSeat];
			else
				amountToCall = sizeCurrentRaise - botBetsThisRound[activeSeat];
						
			// if the current bot is the only bot active in the hand that is not all-in yet and he does not have to
			// call anything, then he shouldn't perform an action. Can occur with two players left when the small blind
			// goes all-in with an amount less than the big blind
			if(amountToCall <= 0 && numberOfRemainingBots(2) == 1)
				return;
			
			sendPreMoveInfo(amountToCall);
			
			// get the next move from the current bot
			PokerMove nextMove = players.get(activeSeat).requestMove();
			
			// if no move was received in time, then try to check as default action				
			String botAction = nextMove.getAction();
			String originalAction = nextMove.getAction();
			String error = "";
			int botActionAmount = nextMove.getAmount();
			int originalActionAmount = nextMove.getAmount();
			
			// handle several invalid / unlogical actions
			if(botAction.equals("raise"))
			{
				// if after this bot there was only an all-in smaller than a minimal raise, then raising is not allowed
				// see http://www.pagat.com/poker/rules/betting.html for more details about the rules
				if(!isAllowedToRaise && amountToCall < sizeMinimalRaise)
				{
					botAction = "call";
					error = "The action is not re-opened to your bot";
					outputErrorToBot(activeSeat, error + ", raise action changed to 'call'");
				}
				
				// if all other involved players are already all-in, then raising is useless
				boolean isRaisingUseful = false;
				for(int i = 0; i < numberOfBots; i++)
				{
					if(i != activeSeat && isInvolvedInHand[i] && botStacks[i] > 0)
					{
						isRaisingUseful = true;
						break;
					}
				}
				if(!isRaisingUseful)
				{
					botAction = "call";
					error = "Other involved players are already all-in";
					outputErrorToBot(activeSeat, error + ", raise action changed to 'call'");
				}
			}
			else if(botAction.equals("call") && amountToCall == 0)
			{
				botAction = "check";
				error = "There is no bet to call";
				outputErrorToBot(activeSeat, error + ", call action changed to 'check'");
			}
			else if(botAction.equals("check") && amountToCall > 0)
			{
				botAction = "fold";
				error = "Other player did make a bet";
				outputErrorToBot(activeSeat, error + ", check action changed to 'fold'");
			}
			
			// handle the actual action that is being made
			if(botAction.equals("raise"))
			{				
				// if the chosen raise size is too small, increase it to the minimum amount
				if(botActionAmount < sizeMinimalRaise)
				{
					botActionAmount = sizeMinimalRaise;
					error = "Raise is below minimum amount";
					if(botStacks[activeSeat] >= amountToCall + sizeMinimalRaise)
						outputErrorToBot(activeSeat, error + ", automatically changed to minimum");
					else {					
						outputErrorToBot(activeSeat, error + ", automatically put all-in because" +
								" remaining stack is lower than minimum amount");
					}
				}
				
				// if there is a limit to the raise size, then check whether the raise is not too large
				if(gameLimit == POT_LIMIT)
				{
					// if there is a maximum raise size, then calculate the current maximum
					sizeMaximalRaise = pot.getTotalPotSize() + amountToCall;
					
					if(botActionAmount > sizeMaximalRaise)
					{
						botActionAmount = sizeMaximalRaise;
						error = "Raise is above maximum amount";
						outputErrorToBot(activeSeat, error + ", automatically changed to maximum");
					}
				}
				
				// place the bet and store the amount that the bot put in
				int realBetAmount = placeBet(amountToCall + botActionAmount);
				
				// if the bot goes all-in with less than the amount to call, then it's effectively a call
				if(realBetAmount <= amountToCall)
				{
					botAction = "call";
					botActionAmount = realBetAmount;
					error = "Raise is below amount to call";
					outputErrorToBot(activeSeat, error + ", action automatically changed to 'call'");
				}
				else
				{
					sizeCurrentRaise = botBetsThisRound[activeSeat];
					botActionAmount = realBetAmount - amountToCall;
					// update the minimal allowed raise size and update the last seat to act
					if(botActionAmount >= sizeMinimalRaise)
					{
						sizeMinimalRaise = botActionAmount;
						updateLastSeatToAct(true);
					}
					else
						updateLastSeatToAct(false);
				}
			}
			else if(botAction.equals("call"))
			{
				botActionAmount = placeBet(amountToCall);
			}
			else if(botAction.equals("check"))
			{
				botActionAmount = 0;
			}
			// the default action is fold, when the bot sends an invalid action string
			else
			{
				botAction = "fold";
				botActionAmount = 0;
				isInvolvedInHand[activeSeat] = false;
			}

			// add the call or raise amount as the additional amount instead of the total bet
			int extraInfo = 0;
       		if(botAction.equals("call"))
        		extraInfo = amountToCall;
    		else if(botAction.equals("raise"))
        		extraInfo = botActionAmount;
			
			// send a message to all other bots about the action and store it in the history
			if(botAction.equals(originalAction) && botActionAmount == originalActionAmount) {
				handHistory += String.format("\n%s %s %d %d", players.get(activeSeat).getName(), botAction,
					botBetsThisRound[activeSeat], extraInfo);
			}
			else {
				handHistory += String.format("\n%s %s %d %d %s %d %s", players.get(activeSeat).getName(), botAction,
					botBetsThisRound[activeSeat], extraInfo, originalAction, originalActionAmount, error);
			}
			sendMoveInfo(botAction, activeSeat, botActionAmount);
			
			// when the current bot folds, the winning chances of the other bots change, thus they are updated
			if(botAction.equals("fold"))
				updateBotOdds();
		}
	}
	
	
	/**
	* Gives the turn to the next bot by incrementing 'activeSeat' with the modulus of the number of bots, thus it
	* becomes the position of the next bot. Also keeps track of whether the last position that is allowed to raise is
	* passed. If this is the case, then this is stored.
	*/
	private void nextBotActive()
	{
		// if the current bot is the last one that is allowed to raise, then the next bot(s) only have the
		// option to call in the current round
		if(isAllowedToRaise && numberOfBots > 2 && lastToMayRaiseSeat == activeSeat)
			isAllowedToRaise = false;
		
		activeSeat++;
		if(activeSeat == numberOfBots)
			activeSeat = 0;
	}
	
	
	/**
	* Updates which seat is the last seat that must act in the current round. The parameter gives the option to update
	* the last seat that is allowed to raise too. For normal raises this should be true. For all-ins that are a raise
	* but which are smaller than the minimal raise, the last previous raiser plus callers do not get the option of
	* raising again, unless someone else reopens the action. For such raises the parameter can be set to false, then
	* the last seat that may raise is not updated.
	* @param updateRaiseLast : whether the last seat that may raise should also be updated.
	*/
	private void updateLastSeatToAct(boolean updateRaiseLast)
	{
		lastToActSeat = activeSeat - 1;
		if(lastToActSeat < 0)
			lastToActSeat = numberOfBots - 1;
		
		if(updateRaiseLast)
			lastToMayRaiseSeat = lastToActSeat;
	}
	
	
	/**
	* Deals cards from the card deck to all the bots, and then passes the information to the bots. The number of
	* cards being dealt to each bot depends on the kind of game.
	*/
	private void dealHandCards()
	{
		for(int i = 0; i < numberOfBots; i++)
		{
			int index = (smallBlindSeat + i) % numberOfBots;
			if(isInvolvedInHand[index])
			{
				// take cards from the deck and deal them to the bots, the amount of cards depends on the game type
				if(gameType == HOLDEM)
				{
					Card card1 = deck.nextCard();
					Card card2 = deck.nextCard();
					botHands[index] = new HandHoldem(card1, card2);
				}
				else if(gameType == OMAHA)
				{
					Card card1 = deck.nextCard();
					Card card2 = deck.nextCard();
					Card card3 = deck.nextCard();
					Card card4 = deck.nextCard();
					botHands[index] = new HandOmaha(card1, card2, card3, card4);
				}
				handHistory += String.format("\n%s hand %s", players.get(index).getName(), botHands[index].toString());
			}
		}
		sendHandInfo(HandInfoType.HAND_CARDS);
		
		// output odds information
		updateBotOdds();
	}
	
	
	/**
	* Places the next card(s) on the table and informs the involved bots about the new table cards. Returns false if
	* the last street has already been dealt, returns true otherwise.
	*/
	private boolean dealNextStreet()
	{
		// if actions have been made after the previous street was dealt, then display the current pot size again
		if(!handHistory.endsWith("]"))
		{
			ArrayList<Integer> allPots = pot.getPots(botsInvolvedToArrayList());			
			handHistory += String.format("\nMatch pot %d", allPots.get(0));
			for(int i = 1; i < allPots.size(); i++)
				handHistory += String.format("\nMatch sidepot%d %d", i, allPots.get(i));
		}

		// return if no new street has to be dealt
		if(round == BetRound.RIVER)
			return false;
		
		// retrieve the name of the next bet round
		round = BetRound.values()[round.ordinal() + 1];
		
		// draw a card from the deck and put it on the table, draw 3 cards if we are on the flop
		Card newCard = deck.nextCard();
		tableCards.add(newCard);
		if(round == BetRound.FLOP)
		{
			newCard = deck.nextCard();
			tableCards.add(newCard);
			newCard = deck.nextCard();
			tableCards.add(newCard);
		}
		
		// store a string representation of the current table cards
		String table = "[" + tableCards.get(0).toString();
		for(int i = 1; i < tableCards.size(); i++)
			table += "," + tableCards.get(i).toString();
		table += "]";			
		sendHandInfo(HandInfoType.NEW_BETROUND);
		handHistory += "\nMatch table " + table;
		
		// output odds information
		updateBotOdds();		
		return true;
	}
	
	
	/**
	* Forces the bots on the new blind positions to pay the blinds. Informs all the bots about the blinds
	* that are payed.
	*/
	private void payBlinds()
	{	
		if(!noSmallBlindPayed)
		{
			botBetsThisRound[smallBlindSeat] = placeBet(sizeSB, smallBlindSeat);
			handHistory += String.format("\n%s post %s", players.get(smallBlindSeat).getName(),
					botBetsThisRound[smallBlindSeat]);
			sendMoveInfo("post", smallBlindSeat, botBetsThisRound[smallBlindSeat]);
		}
		
		botBetsThisRound[bigBlindSeat] = placeBet(sizeBB, bigBlindSeat);
		handHistory += String.format("\n%s post %s", players.get(bigBlindSeat).getName(), botBetsThisRound[bigBlindSeat]);
		sendMoveInfo("post", bigBlindSeat, botBetsThisRound[bigBlindSeat]);
		
		// update the number of hands each bot has played since paying the big blind
		for(int i = 0; i < numberOfBots; i++)
			blindPriorities[i]++;
		
		blindPriorities[bigBlindSeat] = 0;
	}
	
	
	/**
	* Places a bet for a specific bot. The stack of the bot is lowered with the bet amount and this amount is then
	* added to the pot. If the given bet size is larger than the remaining stack size of the bot, then the bet is
	* lowered to the remaining stack size of the bot, thereby putting him all-in. The bet size that is actually placed
	* is returned.
	* @param size : the desired size of the bet.
	* @param botIndex : the seat index of the bot that places the bet.
	*/
	private int placeBet(int size, int botIndex)
	{
		if(botStacks[botIndex] < size)
			size = botStacks[botIndex];
		
		botStacks[botIndex] -= size;
		botBetsThisRound[botIndex] += size;
		pot.addBet(players.get(botIndex), size, round);
		return size;
	}
	
	
	/**
	 * Places a bet for the currently active bot, see placeBet(int, int) for a detailed description.
	 * @param size : the desired size of the bet.
	 */
	private int placeBet(int size)
	{
		return placeBet(size, activeSeat);
	}
	
	
	/**
	 * Performs a number of random card drawings for the remaining board cards and uses this to compute the winning
	 * chance per bot. The chance per bot is computed as percentages multiplied with 10, for example 29.0% is stored as
	 * 290 so that simple integer representation is used with precision high enough. The odds information is added to
	 * the stored match history.
	 */
	private void updateBotOdds()
	{
		// set the current deck status as the starting point for each random draw
		deck.setSavePoint();
		int[] winsPerBot = new int[numberOfBots];
		
		// perform the given number of random draws for table cards and count the winners
		for(int n = 0; n < ODDS_RUNS; n++)
		{
			Vector<Card> tempTableCards = (Vector<Card>) tableCards.clone();
			while(tempTableCards.size() < 5)
				tempTableCards.add(deck.nextCard());
			
			int[] botHandStrengths = computeHandStrengths(botHands, tempTableCards);			
			int maxStrength = -1;
			ArrayList<Integer> winnerIndex = new ArrayList<Integer>();
			for(int i = 0; i < numberOfBots; i++)
		    {
				if(isInvolvedInHand[i] && botHandStrengths[i] >= maxStrength)
				{
					if(botHandStrengths[i] > maxStrength)
					{
						winnerIndex.clear();
						maxStrength = botHandStrengths[i];
					}
					winnerIndex.add(i);
				}
			}
			
			// keep track of the number of wins per bot in this simulation
			for(int i = 0; i < winnerIndex.size(); i++)
				winsPerBot[winnerIndex.get(i)]++;
				
			// restore the deck to the current state
			deck.restoreToSavePoint();
			tempTableCards.clear();
			
			// if all cards are on table, one run is sufficient, so make the stopping condition true
			if(tableCards.size() == 5)
				n = ODDS_RUNS;
		}
		
		// compute the total sum of winnings and use this to calculate the winning percentages
		int sum = 0;
		for(int i = 0; i < numberOfBots; i++)
			if(isInvolvedInHand[i])
				sum += winsPerBot[i];
				
		// output the odds of the involved players
		for(int i = 0; i < numberOfBots; i++)
		{
			String percentage;
			if(isInvolvedInHand[i])
			{
				int percTimesTen = (int) Math.round(1000*winsPerBot[i] / (sum + .0));
				percentage = Integer.toString(percTimesTen);
				// below 1%, add the zero that comes in font of the dot
				if(percTimesTen < 10)
					percentage = "0" + percentage;
				
				int stringLength = percentage.length();
				percentage = percentage.substring(0, stringLength - 1) + "." + percentage.substring(stringLength - 1);
			} else {
				percentage = "0.0";
			}
			handHistory += String.format("\n%s odds %s%%", players.get(i).getName(), percentage);
		}
	}
	
	
	/**
	 * Compute the strength of the given bot's hands for a given set of table cards. Returns an integer array of 
	 * strengths, with value -1 for the bots that are not involved in the hand anymore.
	 * @param cHands : the hands of the bots, array should have length equal to 'numberOfBots'.
	 * @param cBoard : vector of five Card objects representing the table cards.
	 */
	private int[] computeHandStrengths(Hand[] cHands, Vector<Card> cBoard)
	{
		int[] botHandStrengths = new int[numberOfBots];		
		for(int i = 0; i < numberOfBots; i++)
		{
			if(isInvolvedInHand[i])
			{				
				// calculate the combination strength of the hand, evaluation procedure depends on the game type
				if(gameType == HOLDEM)
				{
					// store the hand and table cards together
					long combinationCode = 0l;
					for(int j = 0; j < cHands[i].getNumberOfCards(); j++)
						combinationCode = combinationCode | cHands[i].getCard(j).getNumber();
					for(int j = 0; j < cBoard.size(); j++)
						combinationCode = combinationCode | cBoard.get(j).getNumber();
					
					botHandStrengths[i] = HandEval.hand7Eval(combinationCode);
				}
				/* TODO: a smarter way of computing the hand strength for Omaha might speed up this part of code.
				 * Currently, the strength of all 60 possible combinations of two hand cards and three table cards is
				 * computed to find the real hand strength. Maybe not interesting as long as Omaha is not played or
				 * as long as it is no computational bottleneck.
				 */
				else if(gameType == OMAHA)
				{
					int strength = 0;
					// loop over all 6 possible combinations of 2 cards out of 4 hand cards
					for(int j = 0; j < 3; j++)
					{
						for(int k = j + 1; k < 4; k++)
						{
							// loop over all 10 possible combinations of 3 cards out of 5 board cards
							for(int m = 0; m < 3; m++)
							{
								for(int n = m + 1; n < 4; n++)
								{
									for(int r = n + 1; r < 5; r++)
									{
										try
										{
											long combinationCode = cHands[i].getCard(j).getNumber() |
																   cHands[i].getCard(k).getNumber() |
																   cBoard.get(m).getNumber() |
																   cBoard.get(n).getNumber() |
																   cBoard.get(r).getNumber();
											strength = Math.max(strength, HandEval.hand5Eval(combinationCode));
										}
										catch(java.lang.ArrayIndexOutOfBoundsException e)
										{
											System.err.println("Error in Omaha hand strength calculation " + e);
										}
									}
								}
							}
						}
					}
					botHandStrengths[i] = strength;		
				}
			}
			else
				botHandStrengths[i] = -1;
		}
		
		return botHandStrengths;
	}
	
	
	/**
	 * Method for distributing the pot to the bots at the end of a hand. Checks whether there is a showdown or not. If
	 * there is a showdown, the winners are determined and they receive the part of the pot that they deserve. If there
	 * is no showdown, the only remaining bot receives the whole pot.
	 */
	private void distributePot()
	{	
		// check whether everyone but one player folded, if not we have to compute hand strengths
		int numberOfBotsOnShowdown = numberOfRemainingBots(1);
		
		// create a map of hand strength of all the involved bots
		HashMap<Player, Integer> botHandStrengths = new HashMap<Player, Integer>();	
		if(numberOfBotsOnShowdown >= 2)
		{			
			int[] handStrengths = computeHandStrengths(botHands, tableCards);
			for(int i = 0; i < numberOfBots; i++)
				if(handStrengths[i] >= 0) {
					botHandStrengths.put(players.get(i), handStrengths[i]);
					handHistory += String.format("\n%s strength %s", botCodeNames[i], rankToCategory(handStrengths[i]));
				}
		}
		else
		{
			for(int i = 0; i < numberOfBots; i++)
			{
				if(isInvolvedInHand[i])
					botHandStrengths.put(players.get(i), 1);
			}
		}
			
		// retrieve the information about the main pot and side pots and the winning bots per pot part
		Pot.PayoutWinnerInfo winnerInfo = pot.payoutWinners(botHandStrengths);
		ArrayList<Integer> potParts = winnerInfo.getPots();
		ArrayList<ArrayList<Player>> potPartWinners = winnerInfo.getWinnerPerPot();
		
		// divide each pot part among the bots that win them
		int[] winPerBot = new int[numberOfBots];
		for(int i = potParts.size() - 1; i >= 0; i--)
		{
			ArrayList<Player> currentPotWinners = potPartWinners.get(i);
			int currentPotSize = potParts.get(i);
			int numberOfWinners = currentPotWinners.size();
			int amountPerWinner = currentPotSize / numberOfWinners;
			int restChips = currentPotSize - (numberOfWinners*amountPerWinner);
			int currentSeat = (buttonSeat + 1) % numberOfBots;
			
			String potWinnersStr = "[";
			while(true)
			{
				Player currentBot = players.get(currentSeat);
				if(currentPotWinners.contains(currentBot))
				{
					int currentWinAmount = amountPerWinner;
					if(restChips-- > 0)
						currentWinAmount++;
					
					potWinnersStr += String.format("%s:%d,", currentBot.getName(), currentWinAmount);
					winPerBot[currentSeat] += currentWinAmount;
				}
				currentSeat = (currentSeat + 1) % numberOfBots;
				if(currentSeat == (buttonSeat + 1) % numberOfBots)
					break;
			}
			potWinnersStr = potWinnersStr.substring(0, potWinnersStr.length() - 1);
			potWinnersStr += "]";

			if(i > 0)
				handHistory += String.format("\nResult sidepot%d %s", i, potWinnersStr);
			else
				handHistory += String.format("\nResult pot %s", potWinnersStr);			
		}

		// return the winnings per bot to their stacks and send the information to all the bots
		for(int i = 0; i < numberOfBots; i++)
		{
			botStacks[i] += winPerBot[i];
		}
		sendResultInfo(winPerBot, numberOfBotsOnShowdown > 1);
	}
	
	
	/**
	 * Returns an ArrayList of all bots that are involved in the current hand.
	 */
	private ArrayList<Player> botsInvolvedToArrayList()
	{
		ArrayList<Player> botsInvolved = new ArrayList<Player>();
		for(int i = 0; i < isInvolvedInHand.length; i++)
			if(isInvolvedInHand[i])
				botsInvolved.add(players.get(i));		
		return botsInvolved;
	}
	
	
	/**
	 * Sends the match information to all the bots that are playing at this table. Gives the bots some time to prepare
	 * for playing a match, the method waits for all bots to return from setup for a maximum time of 'SETUP_TIME'. This
	 * method should be called at the start of a new match.
	 * @param type : the type of match information to send.
	 */
	private void sendMatchInfo(MatchInfoType type)
	{
		MatchInfo info = new MatchInfo(type, players, botCodeNames, isTournament,
									   HANDS_PER_BLINDLEVEL, SIZE_STARTSTACK, sizeBB, sizeSB,
									   numberOfPlayersInTournament, numberOfPricesInTournament);	
		
		for(int i = 0; i < numberOfBots; i++)
		{
			if(isInvolvedInMatch[i])
			{
				info.setCurrentBotInfo(i);
				players.get(i).sendInfo(info.toString());
			}
		}		
	}
	
	
	/**
	 * Sends the hand info to all the bots that are playing at this table. This method should be called at the start
	 * of each new round and at each new bet round.
	 * @param type : the type of hand information to send.
	 */
	private void sendHandInfo(HandInfoType type)
	{
		HandInfo info = new HandInfo(type, isTournament, handNumber, numberOfPlayersInTournament,
									 players, botCodeNames, botStacks, sizeBB, sizeSB, buttonSeat,
									 tableCards.toString().replaceAll("\\s", ""));
		
		for(int i = 0; i < numberOfBots; i++)
		{
			if(isInvolvedInMatch[i])
			{
				info.setCurrentBotInfo(i, botHands[i]);
				players.get(i).sendInfo(info.toString());
			}
		}
	}
	
	
	/**
	 * Sends extra relevant information to the bot that has to make a move now.
	 * @param amountToCall : the amount the current player has to put in to make a call.
	 */
	private void sendPreMoveInfo(int amountToCall)
	{
		// adapt the amount to call and the maximum pot that can be won to the players remaining stack size
		int maxPotSizeToWin;
		if(botStacks[activeSeat] < amountToCall)
		{
			amountToCall = botStacks[activeSeat];
			maxPotSizeToWin = pot.getMaxPotToWin(players.get(activeSeat), amountToCall);
		}
		else
			maxPotSizeToWin = pot.getTotalPotSize();
		
		PreMoveInfo info = new PreMoveInfo(maxPotSizeToWin, amountToCall);
		
		// The pre-move info only goes to the active bot.
		players.get(activeSeat).sendInfo(info.toString());
	}
	
	
	/**
	 * Sends a message to all bots about the move that a bot made.
	 * @param action : the action that the bot made.
	 * @param playerIndex : the array index of the bot.
	 * @param amount : the amount belonging to the action.
	 */
	private void sendMoveInfo(String action, int playerIndex, int amount)
	{
		PokerMove move = new PokerMove(action, amount);
		move.setPlayer(botCodeNames[playerIndex]);
		for(int i = 0; i < numberOfBots; i++)
			if(isInvolvedInMatch[i])
				players.get(i).sendInfo(move.toString());
	}
	
	
	/**
	 * Sends a message to all bots about the result of the hand, thus how the pot is distributed, and in case of a
	 * showdown it also provides the hands of the winners.
	 * @param potDivision : array with the amount of the pot that each bot gets.
	 * @param showdown : whether there is a showdown or not.
	 */
	private void sendResultInfo(int[] potDivision, boolean showdown)
	{
		HandResultInfo resultInfo = new HandResultInfo(players, botCodeNames, potDivision);
		if(showdown)
		{
			for(int i = 0; i < numberOfBots; i++)
				if(isInvolvedInHand[i])
					resultInfo.setBotHand(i, botHands[i]);
		}
		for(int i = 0; i < numberOfBots; i++)
			if(isInvolvedInMatch[i])
				players.get(i).sendInfo(resultInfo.toString());
	}
	
	private void outputErrorToBot(int seat, String output) 
	{
		players.get(seat).getBot().addToDump("Engine says: \"" + output + "\"\n");
	}
	
	/**
	 * small method to convert the int 'rank' to a readable enum called HandCategory
	 */
	public HandEval.HandCategory rankToCategory(int rank) {
		return HandEval.HandCategory.values()[rank >> HandEval.VALUE_SHIFT];
	}

	/**
	 * Writes the history that is currently stored in 'handHistory' to the standard out channel and empties the string
	 * afterwards. Should be called after each finished hand. This print information is kept locally and is not meant
	 * to be communicated to the bots.
	 */
	private void writeHistory()
	{
		allHistory += handHistory;
//		System.out.println(handHistory);
		handHistory = "";
	}
	
	public Player getWinner() 
	{
		if(isTournament) 
		{
			for(int i = 0; i < numberOfBots; i++)
			{
				if(finishPosition[i] == 1) 
					return players.get(i);		
			}
		}
		return null;
	}
	
	public int getHandNumber()
	{
		return this.handNumber;
	}
	
	public String getHistory()
	{
		return this.allHistory;
	}
	
	public boolean isTournament()
	{
		return this.isTournament;
	}
}
