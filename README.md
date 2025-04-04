# Bell Choir Threading Project
 - by Charlie Said

 ## Requirements

 This program orchestrates a "Bell Choir" of threads, each playing a note when signaled to by the Conductor thread.

 ### The Conductor Thread
  - Reads the song
  - Starts the Players.
  - Iterates through the song, signaling the correct Player to play for a specified duration when their note is next.
  - Stops itself when the song is over.

 ### The Player Thread(s)
  - Start themselves when initialized.
  - Initialized with one note.  They only play their single note.
  - Wait until signaled, then play their note for the signaled duration.
  - Check if the Conductor thread is still alive; if it is not, terminate.  Otherwise, wait for the next signal.

  ## Challenges
  The main challenge was devising a system for signaling to the Players without obtaining their lock.  It would be easier and simpler to have the Conductor obtain the lock for the specific Player it was signaling, wait for the note to finish, then release the lock.  This system would have also enforced that the Conductor could hold no more than one Player lock simultaneously.  However, this system would have made the Player's lock public, which is sloppy code.  A more protected system is the one I ultimately used.

  A second challenge was passing arguments in to Ant.  It took some external research to find how to code my build.xml file properly