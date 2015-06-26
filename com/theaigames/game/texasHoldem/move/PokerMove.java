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
