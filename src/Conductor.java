package src;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import javax.sound.sampled.AudioFormat;

public class Conductor{

    // Class variables
    private final AudioFormat af;
    private static volatile boolean running = false;
    
    // Dict and List to store players.
    private static Map<String, String> noteDict = new HashMap<String, String>();
    private static List<Player> players = new ArrayList<Player>();

    /**
     * Constructor for the Conductor class.
     * 
     * @param af AudioFormat object.
     */
    Conductor(AudioFormat af) {
        this.af = af;
    }

    /**
     * Getter for running status.
     * 
     * @return boolean indicating if the Conductor is running
     */
    public static boolean isRunning() {
        return running;
    }

    public static void main(String[] args) throws Exception {

        // Define the audio format
        final AudioFormat af = new AudioFormat(Note.SAMPLE_RATE, 8, 1, true, false);
        
        // Create the Conductor
        Conductor c = new Conductor(af);
        List<BellNote> song = readSong(args[0]);
        
        if (song == null) {
            System.out.println("Terminating");
            return;
        }

        // Start the run loop.
        running = true;

        // Initialize the Players
        int i = 0;
        for (BellNote bell_note : song){
            Note note = bell_note.note;
            // Check if the note is already in the dictionary
            if (noteDict.containsKey(note.toString())) {
                // If it exists, do nothing.
                System.out.println("Note already indexed: " + note);
            } else {
                //If it doesn't exist, create it and add it to the dictionary.
                System.out.println("New note: " + note);
                players.add(new Player("" + i, note, af));
                noteDict.put(note.toString(), players.get(i).getName());
                i ++;
            }
        }
        
        // Play the song.
        c.playSong(song);
    }

    /**
     * Reads a song from a file and returns a list of BellNotes.
     * @param filename Name of a txt file containing a song.
     * @return List of BellNotes that makes up the song.
     */
    private static List<BellNote> readSong(String filename) {
        // Initialize list of BellNotes
        List<BellNote> song = new ArrayList<>();

        // Open and read file
        try {

            String p = new String("songs/" + filename);
            File f = new File(p);
            Scanner reader = new Scanner(f);

            while (reader.hasNextLine()) {
                String line = reader.nextLine();

                String[] parts = line.split(" ");

                // Validate notes
                List<String> valid_notes = Arrays.asList("REST",
                    "A4",
                    "A4S",
                    "B4",
                    "C4",
                    "C4S",
                    "D4",
                    "D4S",
                    "E4",
                    "F4",
                    "F4S",
                    "G4",
                    "G4S",
                    "A5");
                if (!valid_notes.contains(parts[0])) {
                    System.out.println("Invalid note type!" + parts[0]);
                    reader.close();
                    return null;
                }

                // Convert note length to enum
                if (parts.length > 1) {
                    if (parts[1].equals("4")){
                        parts[1] = "QUARTER";
                    } else if (parts[1].equals("2")){
                        parts[1] = "HALF";
                    } else if (parts[1].equals("1")){
                        parts[1] = "WHOLE";
                    } else if (parts[1].equals("8")){
                        parts[1] = "EIGTH";
                    } else {
                        System.out.println("Invalid note length!");
                        reader.close();
                        return null;
                    }

                    song.add(new BellNote(Note.valueOf(parts[0]), NoteLength.valueOf(parts[1])));

                } else {
                    song.add(new BellNote(Note.valueOf(parts[0]), NoteLength.valueOf("QUARTER")));
                }
            }

            reader.close();
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            return null;
        }
        return song;
    }

    /**
     * Play the song by signaling the player threads.
     * @param song A List of BellNotes that you want to play.
     */
    private void playSong(List<BellNote> song) {
        
        for (BellNote bell_note : song) {

            // Get the note parts - length and note.
            Note note = bell_note.note;
            NoteLength length = bell_note.length;

            if (note == Note.REST) {
                // If the note is a rest, just wait for the duration (never signal players)
                try {
                    Thread.sleep(length.timeMs());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            // Find the right player to play the note.
            int player_id = Integer.parseInt(noteDict.get(note.toString()));
            Player p = players.get(player_id);

            // Play the note.
            p.play(length);
            while (p.isPlaying()) {
                // Do nothing until the player is done playing.
            }
        }
        System.out.println("Song is over.");
        running = false;
    }
    
}

// UTILITY CLASSES - COPIED FROM TEACHER-PROVIDED CODE//

class BellNote {
    final Note note;
    final NoteLength length;

    BellNote(Note note, NoteLength length) {
        this.note = note;
        this.length = length;
    }

    public String toString() {
        // Return a string representation of the BellNote
        // e.g., "A4 QUARTER"
        String noteName = note.name();
        String lengthName = length.name();
        return noteName + " " + lengthName;
    }
}

enum NoteLength {
    WHOLE(1.0f),
    HALF(0.5f),
    QUARTER(0.25f),
    EIGTH(0.125f);

    private final int timeMs;

    private NoteLength(float length) {
        timeMs = (int)(length * Note.MEASURE_LENGTH_SEC * 1000);
    }

    public int timeMs() {
        return timeMs;
    }
}

enum Note {
    // REST Must be the first 'Note'
    REST,
    A4,
    A4S,
    B4,
    C4,
    C4S,
    D4,
    D4S,
    E4,
    F4,
    F4S,
    G4,
    G4S,
    A5;

    public static final int SAMPLE_RATE = 48 * 1024; // ~48KHz
    public static final int MEASURE_LENGTH_SEC = 1;

    // Circumference of a circle divided by # of samples
    private static final double step_alpha = (2.0d * Math.PI) / SAMPLE_RATE;

    private final double FREQUENCY_A_HZ = 440.0d;
    private final double MAX_VOLUME = 127.0d;

    private final byte[] sinSample = new byte[MEASURE_LENGTH_SEC * SAMPLE_RATE];

    private Note() {
        int n = this.ordinal();
        if (n > 0) {
            // Calculate the frequency!
            final double halfStepUpFromA = n - 1;
            final double exp = halfStepUpFromA / 12.0d;
            final double freq = FREQUENCY_A_HZ * Math.pow(2.0d, exp);

            // Create sinusoidal data sample for the desired frequency
            final double sinStep = freq * step_alpha;
            for (int i = 0; i < sinSample.length; i++) {
                sinSample[i] = (byte)(Math.sin(i * sinStep) * MAX_VOLUME);
            }
        }
    }

    public byte[] sample() {
        return sinSample;
    }
}
