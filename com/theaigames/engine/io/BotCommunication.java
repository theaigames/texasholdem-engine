package com.theaigames.engine.io;

import java.io.IOException;

/**
 * Bot Communication interface
 * 
 * Handles callback between players and the engine.
 * 
 * @author Jackie Xu <jackie@starapple.nl>, Jim van Eeden <jim@starapple.nl>
 */
public interface BotCommunication {
    
    public void addPlayer(String command) throws IOException;
    
    public void start() throws Exception;
}
