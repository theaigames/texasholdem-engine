package com.theaigames.game.texasHoldem.move;

public class PreMoveInfo
{
	private int maxWinPot;
	private int amountToCall;
	
	public PreMoveInfo(int maxPot, int amountCall)
	{
		maxWinPot = maxPot;
		amountToCall = amountCall;
	}
	
	/**
	 * Returns a String representation of the current table situation.
	 */
	public String toString()
	{
		String str = "";
		
		str += String.format("Match maxWinPot %d\n", maxWinPot);		
		str += String.format("Match amountToCall %d", amountToCall);
		
		return str;
	}
}
