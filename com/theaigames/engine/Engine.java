package com.theaigames.engine;

import com.theaigames.engine.io.BotCommunication;
import com.theaigames.engine.io.IOPlayer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Engine class
 * 
 * A general engine to implement IO for bot classes
 * All game logic is handled by implemented Logic interfaces.
 * 
 * @author Jackie Xu <jackie@starapple.nl>, Jim van Eeden <jim@starapple.nl>
 */
public class Engine implements BotCommunication {
    
    // Boolean representing current engine running state
    private boolean isRunning;
    
    // Class implementing Logic interface; handles all data
    private Logic logic;
    
    // ArrayList containing player handlers
    private ArrayList<IOPlayer> players;
    
    // Engine constructor 
    public Engine() {
        this.isRunning = false;
        this.players = new ArrayList<IOPlayer>();
    }
    
    // Sets game logic
    public void setLogic(Logic logic) {
        this.logic = logic;
    }
    
    // Determines whether game has ended
    public boolean hasEnded() {
        return this.logic.isGameWon();
    }
    
    @Override
    // Adds a player to the game
    public void addPlayer(String command) throws IOException {

        // Create new process
        Process process = Runtime.getRuntime().exec(command);

        // Attach IO to process
        IOPlayer player = new IOPlayer(process);
        
        // Add player
        this.players.add(player);

        // Start running
        player.run();
    }
    
    @Override
    // Method to start engine
    public void start() throws Exception {
    	
    	int round = 0;
        
        // Set engine to running
        this.isRunning = true;
        
        // Set up game settings
        this.logic.setupGame(this.players);

        // Keep running
        while (this.isRunning) {
        
        	round++;

            // Play a round
            this.logic.playRound(round);
            
            // Check if win condition has been met
            if (this.hasEnded()) {

                System.out.println("stopping...");
                
                // Stop running
                this.isRunning = false;
                
                // Close off everything
                try {
                	this.logic.finish();
                } catch (Exception ex) {
                    System.out.println(ex);
                	Logger.getLogger(Engine.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            
        }
        
    }
    
}
