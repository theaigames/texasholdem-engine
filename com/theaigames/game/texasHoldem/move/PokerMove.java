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

package com.theaigames.game.texasHoldem.move;

/**
 * Class that represents the action of a bot.
 */
public class PokerMove {
	
	String player = null;
	String action = null;
	int amount;

	public PokerMove(String act, int amt)
	{
		action = act;
		amount = amt;
	}

	/**
	 * Sets the player to which the action belongs.
	 * @param plr : the name of the bot
	 */
	public void setPlayer(String plr)
	{
		player = plr;
	}

	public String getPlayer()
	{
		return player;
	}

	public String getAction()
	{
		return action;
	}
	
	public int getAmount()
	{
		return amount;
	}

	
	@Override
	/**
	 * Returns a string representation of the move as a sentence of three words, being the player name, the action
	 * string and the action amount.
	 */
	public String toString() {
		return (player != null) ? String.format("%s %s %d", player, action, amount) :
			String.format("_unknown_ %s %d", action, amount);
	}
	
}
