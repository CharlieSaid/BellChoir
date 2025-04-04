package src;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;


public class Player implements Runnable {

    // Class variables.
    private final AudioFormat af;
    private String name;
    private Note note;

    private boolean playing = false;
    private NoteLength length;

    // Lock object for synchronization
    private final Object lock = new Object();

    /**
     * Getter for playing status.
     * @return True if the player is currently playing a note, false otherwise.
     */
    public boolean isPlaying() {
        return playing;
    }
    
    /**
     * Constructor for the Player class.
     * @param name Name of the player, used as an identifier.
     * @param note The note that this player can play.
     * @param af The audio format to be used for playback.
     */
    public Player(String name, Note note, AudioFormat af) {
        this.name = name;
        this.note = note;
        this.af = af;

        // Start the player thread
        Thread t = new Thread(this);
        t.start();
    }

    /**
     * The run method for the Player thread.
     * Triggered when the Player is created.
     */
    public void run() {
        System.out.println("Player " + name + " started with note " + note);
        
        // Run loop.
        while (Conductor.isRunning()) {
            
            synchronized (lock) {
                
                // If the Conductor is done, release the lock and break out of the run loop.
                if (!Conductor.isRunning()) {
                    lock.notify();
                    break;
                }
                
                // Wait until the player is signaled to play a note
                while (!playing && Conductor.isRunning()) {
                    try {
                        lock.wait(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (!Conductor.isRunning()) {
                    lock.notify();
                    break; // Exit outer loop if running is false
                }
                
                // Play the note with the specified length
                System.out.println("Player " + name + " is starting its note.");
                try (final SourceDataLine line = AudioSystem.getSourceDataLine(af)) {
                    line.open();
                    line.start();

                    playNote(line, new BellNote(note, length));

                    line.drain();
                } catch (LineUnavailableException e) {
                    e.printStackTrace();
                }

                System.out.println("Player " + name + " is done playing its note.");
                
                // Turn off playing status
                playing = false;
        
                // Notify the lock that this player is no longer playing atomically.
                lock.notifyAll();

                // if (!Conductor.isRunning()) {
                //     break; // Exit outer loop if running is false
                // }
            }
        }
        
        System.out.println("Player " + name + " is stopping.");
    }

    /**
     * Method to play a note with a specified length.
     * The note is defined in the Player constructor.
     * @param length The length of the note to be played.
     */
    public void play(NoteLength length) {
        // Acquire the lock to ensure thread safety
        synchronized (lock) {
            // Set playing status to true.
            playing = true;
            this.length = length;
    
            lock.notify();
        }
    }

    /**
     * Getter for the Player name.
     * @return String name of the player.
     */
    public String getName() {
        return name;
    }

    /**
     * Method to play a note using the SourceDataLine.
     * 
     * Adapted from teacher-provided code from Moodle.
     * @param line SourceDataLine to play the note on.
     * @param bn BellNote object (contains Note and Length info).
     */
    private void playNote(SourceDataLine line, BellNote bn) {
        final int ms = Math.min(bn.length.timeMs(), Note.MEASURE_LENGTH_SEC * 1000);
        final int length = Note.SAMPLE_RATE * ms / 1000;
        line.write(bn.note.sample(), 0, length);
        line.write(Note.REST.sample(), 0, 50);
    }
    
    
}
