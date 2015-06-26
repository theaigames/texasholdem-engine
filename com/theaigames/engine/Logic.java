package com.theaigames.engine;

import com.theaigames.engine.io.IOPlayer;
import java.util.ArrayList;

/**
 * Logic interface
 * 
 * Interface to implement when creating games.
 * 
 * @author Jackie Xu <jackie@starapple.nl>, Jim van Eeden <jim@starapple.nl>
 */
public interface Logic {
    public void setupGame(ArrayList<IOPlayer> players) throws Exception;
    public void playRound(int roundNumber) throws Exception;
    public boolean isGameWon();
    public void finish() throws Exception;
}
