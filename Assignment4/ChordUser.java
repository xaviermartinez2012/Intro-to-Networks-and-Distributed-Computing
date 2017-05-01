import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.file.*;
import java.rmi.*;
import java.security.*;
import java.util.*;

/*****************************/

/**
 * \brief The functions for users
 * Includes user joining/leaving of chord
 * and reading/writing of files
 *
 **********************************/
public class ChordUser {
int port;
long guid;
Chord chord;

/*****************************/

/**
 * \brief Functions for md5 which
 *	hashes the filename
 *
 **********************************/
private long md5(String objectName) {
    try {
	MessageDigest m = MessageDigest.getInstance("MD5");
	m.reset();
	m.update(objectName.getBytes());
	BigInteger bigInt = new BigInteger(1, m.digest());
	return Math.abs(bigInt.longValue());
    } catch (NoSuchAlgorithmException e) {
	e.printStackTrace();
    }
    return 0;
}

/*!
 * \brief return the guid of the user
 * \return guid
 */
public long getGuid() {
    return guid;
}

/*!
 * \brief get chord of User
 * \return chord
 */
public Chord getChord() {
    return chord;
}

/*!
 * \brief function to start program with desired port
 * \param p port
 */
public ChordUser(int p) {
    port = p;

    Timer timer1 = new Timer();
    timer1.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
		try {
		    guid = md5("" + port);
		    chord = new Chord(port, guid);
		    try {
			Files.createDirectories(Paths.get(
				guid + "/repository"));
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		    System.out.println(
			"Usage: \n\tjoin <ip> <port>\n\twrite <file> (the file must be an integer stored in the working directory, i.e, ./"
			+
			guid + "/file");
		    System.out.println(
			"\tread <file>\n\tdelete <file>\n\tprint\n\tleave");

		    Scanner scan = new Scanner(System.in);
		    String delims = "[ ]+";
		    String command = "";
		    while (true) {
			System.out.print("> ");
			String text = scan.nextLine();
			String[] tokens = text.split(delims);
			if (tokens[0].equals("join") && (tokens.length == 3)) {
			    try {
				int portToConnect = Integer.parseInt(tokens[2]);

				chord.joinRing(tokens[1], portToConnect);
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
			if (tokens[0].equals("print"))
			    chord.Print();
			if (tokens[0].equals("write") && (tokens.length == 2)) {
			    String path;
			    String fileName = tokens[1];
			    path = "./" + guid + "/" + fileName;
			    long guidObject1 = md5(fileName + "1");
			    long guidObject2 = md5(fileName + "2");
			    long guidObject3 = md5(fileName + "3");
			    try {
					// File 1
				FileStream file1 = new FileStream(
					path);
				ChordMessageInterface peer1 =
				    chord.locateSuccessor(guidObject1);
				peer1.put(guidObject1, file1);	// put file into ring
				// File 2
				FileStream file2 = new FileStream(
					path);
				ChordMessageInterface peer2 =
				    chord.locateSuccessor(guidObject2);
				peer2.put(guidObject2, file2);	// put file into ring
				// File 3
				FileStream file3 = new FileStream(
					path);
				ChordMessageInterface peer3 =
				    chord.locateSuccessor(guidObject3);
				peer3.put(guidObject3, file3);	// put file into ring
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
			if (tokens[0].equals("read") && (tokens.length == 2)) {
			    String path;
			    String fileName = tokens[1];
			    path = "./" + guid + "/" + fileName;
			    long guidObject1 = md5(fileName + "1");
			    long guidObject2 = md5(fileName + "2");
			    long guidObject3 = md5(fileName + "3");
			    try {
				ChordMessageInterface peer1 =
				    chord.locateSuccessor(guidObject1);
				ChordMessageInterface peer2 =
				    chord.locateSuccessor(guidObject2);
				ChordMessageInterface peer3 =
				    chord.locateSuccessor(guidObject3);

				boolean skip = false;
				int cwHops = 0;
				int ccwHops = 0;
				long closestCWPeer = 0;
				long closestCCWPeer = 0;
				long closestPeer = 0;
				ChordMessageInterface currentPeer = chord.successor;
				long currentGUID = currentPeer.getId();
				if (peer1.getId() == guid) {
				    skip = true;
				    closestPeer = peer1.getId();
				} else if (peer2.getId() == guid) {
				    skip = true;
				    closestPeer = peer2.getId();
				} else if (peer3.getId() == guid) {
				    skip = true;
				    closestPeer = peer3.getId();
				}
					// Clockwise measurement
				while (currentGUID != guid && !skip) {
				    cwHops++;
				    if (currentGUID == peer1.getId()) {
					closestCWPeer = currentGUID;
					break;
				    } else if (currentGUID == peer2.getId()) {
					closestCWPeer = currentGUID;
					break;
				    } else if (currentGUID == peer3.getId()) {
					closestCWPeer = currentGUID;
					break;
				    } else {
					currentPeer = currentPeer.locateSuccessor(currentGUID);
					currentGUID = currentPeer.getId();
				    }
				}
				if (cwHops == 1) {
				    closestPeer = closestCWPeer;
				    skip = true;
				}
				currentPeer = chord.predecessor;
				currentGUID = currentPeer.getId();
				while (currentGUID != guid && !skip) {
				    ccwHops++;
				    if (currentGUID == peer1.getId()) {
					closestCCWPeer = currentGUID;
					break;
				    } else if (currentGUID == peer2.getId()) {
					closestCCWPeer = currentGUID;
					break;
				    } else if (currentGUID == peer3.getId()) {
					closestCCWPeer = currentGUID;
					break;
				    } else {
					currentPeer = currentPeer.getPredecessor();
					currentGUID = currentPeer.getId();
				    }
				}
				if ((cwHops == ccwHops) && !skip)
				    closestPeer = closestCWPeer;
				else if ((cwHops < ccwHops) && !skip)
				    closestPeer = closestCWPeer;
				else if ((ccwHops < cwHops) && !skip)
				    closestPeer = closestCCWPeer;
				InputStream stream = peer.get(closestPeer);
				FileOutputStream output =
				    new FileOutputStream(path);
				while (stream.available() > 0)
				    output.write(stream.read());
				output.close();
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
			if (tokens[0].equals("delete") && (tokens.length == 2)) {
			    String fileName = tokens[1];
			    long guidObject = md5(
				    fileName);
			    try {
				ChordMessageInterface peer =
				    chord.locateSuccessor(guidObject);
				peer.delete(guidObject);
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
			if (tokens[0].equals("leave") && (tokens.length == 1))
			    System.exit(0);
		    }
		} catch (RemoteException e) {
		    System.out.println(e);
		}
	    }
	}, 1000, 1000);
}

/*!
 * \brief exit command
 * \param args[] user input
 */
static public void main(String args[]) {
    if (args.length < 1)
	throw new IllegalArgumentException("Parameter: <port>");
    try {
	ChordUser chordUser = new ChordUser(Integer.parseInt(args[0]));
	Runtime.getRuntime().addShutdownHook(new Thread() {
		@Override
		public void run() {
		    System.out.println("\nExiting...");
		    Chord chord = chordUser.getChord();
		    long guid = chordUser.getGuid();
		    try {
			if (chord.successor.getId() != guid) {
			    File folder = new File("./" + guid + "/repository");
			    File[] files = folder.listFiles();
			    for (File file: files)
				file.renameTo(new File(
					"./" + chord.successor.getId() + "/repository/" + file.getName()));
			    chord.cancelTimer();
			    chord.successor.setPredecessor(chord.predecessor);
			    chord.predecessor.setSuccessor(chord.successor);
			}
		    } catch (RemoteException remote) {
			System.out.println(remote);
		    }
		}
	    });
    } catch (Exception e) {
	e.printStackTrace();
	System.exit(1);
    }
}
}
