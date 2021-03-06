package com.github.evreichard.pandorica;

/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

/**
 * Taken and modified from here:
 * http://stackoverflow.com/questions/12057214/jlayer-pause-and-resume-song
 **/

import java.io.InputStream;
import javazoom.jl.player.Player;
import javazoom.jl.decoder.*;

/**
 * This class is the actual player that incorporates the pause feature of the javazoom player. 
 * It is modified from here: http://stackoverflow.com/questions/12057214/jlayer-pause-and-resume-song
 **/
public class PandoraPlayer{
	
	// the player actually doing all the work
    private final Player player;

    // locking object used to communicate with player thread
    private final Object playerLock = new Object();

    // status variable what player thread is doing/supposed to do
    private PlayerStatus playerStatus = PlayerStatus.NOTSTARTED;
	
	public PandoraPlayer(final InputStream inputStream) throws JavaLayerException {
        this.player = new Player(inputStream);
    }

    /**
     * This starts playback of the current song, or resumes if paused. 
     */
    public void play(){
        synchronized (playerLock) {
            switch (playerStatus) {
                case NOTSTARTED:
                    final Runnable r = new Runnable(){
                        public void run() {
                            playInternal();
                        }
                    };
					
                    final Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setPriority(Thread.MAX_PRIORITY);
                    playerStatus = PlayerStatus.PLAYING;
                    t.start();
                    
					break;
                case PAUSED:
                    resume();
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * This pauses playback and returns true if new state is PlayerStatus.PAUSED.
	 *
	 * @return isPaused whether the current song is now paused.
     */
    public boolean pause() {
        synchronized (playerLock) {
            if (playerStatus == PlayerStatus.PLAYING) {
                playerStatus = PlayerStatus.PAUSED;
            }
			boolean isPaused = playerStatus == PlayerStatus.PAUSED;
            return isPaused;
        }
    }

    /**
     * This resumes playback and returns true if the new state is PlayerStatus.PLAYING.
	 *
	 * @return isPlaying whether the current song is not playing.
     */
    public boolean resume() {
        synchronized (playerLock) {
            if (playerStatus == PlayerStatus.PAUSED) {
                playerStatus = PlayerStatus.PLAYING;
                playerLock.notifyAll();
            }
			
			boolean isPlaying = playerStatus == PlayerStatus.PLAYING;
            return isPlaying;
        }
    }

    /**
     * This stops playback. If we're not playing anything, it does nothing
     */
    public void stop() {
        synchronized (playerLock) {
            playerStatus = PlayerStatus.FINISHED;
            playerLock.notifyAll();
        }
    }

	/**
	 * This plays the song internally frame by frame.  It will break through the
	 * loop when either the song is finished or if we play the last frame.
	 **/
    private void playInternal() {
        while (playerStatus != PlayerStatus.FINISHED) {
            try {
				// Plays one frame, breaks if last frame played
                if (!player.play(1)) {
                    break;
                }
            } catch (final JavaLayerException e) {
                break;
            }
			
            // check if paused or terminated
            synchronized (playerLock) {
                while (playerStatus == PlayerStatus.PAUSED) {
                    try {
                        playerLock.wait();
                    } catch (final InterruptedException e) {
                        // terminate player
                        break;
                    }
                }
            }
        }
        close();
    }

    /**
     * This closes the player regardless of current state.
     */
    public void close() {
        synchronized(playerLock){
            playerStatus = PlayerStatus.FINISHED;
        }
		
        try{
            player.close();
        }catch (final Exception e){
            // ignore, we are terminating anyway
        }
    }
}