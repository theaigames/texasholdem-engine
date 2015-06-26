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

import java.io.*;
import java.util.ArrayList;

import com.theaigames.engine.Engine;
import com.theaigames.engine.Logic;
import com.theaigames.engine.io.IOPlayer;

import java.lang.Thread;

public class TexasHoldem implements Logic
{
	private String playerName1, playerName2;
	
	private MatchPlayer matchPlayer;
	private ArrayList<Player> players;
    
    private final long TIME_PER_MOVE = 500l; 		// time in milliseconds that bots get per move
	private final long TIMEBANK_MAX = 10000l;		// time bank each bot receives
	private final int GAME_TYPE = 13;				// no limit Texas Hold 'em, tournament form
    private final int STARTING_STACK = 500;
    private final int MAX_HANDS = Integer.MAX_VALUE;
    private final int MAX_PLAYERS_LEFT = 1;
 
	
	public TexasHoldem()
	{
		this.playerName1 = "player1";
		this.playerName2 = "player2";

        players = new ArrayList<Player>();
	}
	
	@Override
	public void setupGame(ArrayList<IOPlayer> ioPlayers) throws IncorrectPlayerCountException, IOException
	{
		System.out.println("setting up game...");
		
		// Determine array size is two players
        if (ioPlayers.size() != 2) {
            throw new IncorrectPlayerCountException("Should be two players");
        }
        
        players.add(new Player(playerName1, ioPlayers.get(0), TIMEBANK_MAX, TIME_PER_MOVE));
        players.add(new Player(playerName2, ioPlayers.get(1), TIMEBANK_MAX, TIME_PER_MOVE));
		
        // start the match player and send setup info to bots
        System.out.println("starting game ...");
		matchPlayer = new MatchPlayer(players, GAME_TYPE, STARTING_STACK);
		matchPlayer.finishSetup(true);
		
		// set the timebank to maximum amount to start with and send timebank info
		for(Player player : players) {
			player.setTimeBank(TIMEBANK_MAX);
			sendSettings(player);
		}
	}
	
	@Override
	public void playRound(int roundNumber)
	{
		// round number is handled in the MatchPlayer
		
		this.matchPlayer.playRound();
	}
	
	@Override
	public boolean isGameWon()
	{
		if(this.matchPlayer.getHandNumber() >= MAX_HANDS)
			return true;
		
		if(this.matchPlayer.isTournament() && this.matchPlayer.numberOfRemainingBots(0) <= MAX_PLAYERS_LEFT)
			return true;
		
		return false;
	}
	
	@Override
	// close the bot processes, save, exit program
	public void finish() throws Exception
	{
		for(Player player : players) {
			player.getBot().finish();
		}
		Thread.sleep(100);

		// write everything
		try { 
			this.saveGame(); 
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Done.");
		
        System.exit(0);
	}

	private void sendSettings(Player player)
	{
		player.sendInfo("Settings your_bot " + player.getName());
		player.sendInfo("Settings timebank " + TIMEBANK_MAX);
		player.sendInfo("Settings time_per_move " + TIME_PER_MOVE); 
	}
	
	public void saveGame() throws Exception {
		
		Player winner = this.matchPlayer.getWinner();
		int score = this.matchPlayer.getHandNumber();
		IOPlayer bot1 = players.get(0).getBot();
		IOPlayer bot2 = players.get(1).getBot();
		
		if(winner != null) {
			System.out.println("winner: " + winner.getName());
		} else {
			System.out.println("winner: draw");
		}
		
		System.out.println("Saving the game...");
		
		// print stuff here... (like the bot dumps)
	}
    
    public static void main(String args[]) throws Exception
	{	
		String bot1 = args[0];
		String bot2 = args[1];
		
		Engine engine = new Engine();
		
		engine.setLogic(new TexasHoldem());
		
		// Add players
        engine.addPlayer(bot1);
        engine.addPlayer(bot2);
		
        engine.start();
	}


}