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
Hashtable<Long, Date> lastRead;
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
			    long guidObject = md5(fileName);
			    long guidObject1 = md5(fileName + "1");
			    long guidObject2 = md5(fileName + "2");
			    long guidObject3 = md5(fileName + "3");
			    ChordMessageInterface peer1 =
				chord.locateSuccessor(guidObject);
			    ChordMessageInterface peer2 =
				chord.locateSuccessor(guidObject);
			    ChordMessageInterface peer3 =
				chord.locateSuccessor(guidObject);
			    Date localLastRead;
			    if (lastRead.contains(guidObject))
				localLastRead = lastRead.get(guidObject);
			    else
				localLastRead = new Date(System.currentTimeMillis());
			    if (peer1.canCommit(guidObject1, localLastRead)) {
				if (peer2.canCommit(guidObject2, localLastRead)) {
				    if (peer3.canCommit(guidObject3, localLastRead)) {
					/*
					 * To-do commit file:
					 * - update last written (server-side),
					 * - unlock file (server-side),
					 * - update lastRead (client-side)
					 */
				    } else {
					// peer2.abort();
					// peer1.abort();
				    }
				} else {
					// peer1.abort();
				}
			    }
			    try {
				FileStream file = new FileStream(
					path);
				ChordMessageInterface peer =
				    chord.locateSuccessor(guidObject);
				peer.put(guidObject, file);	// put file into ring
			    } catch (IOException e) {
				e.printStackTrace();
			    }
			}
			if (tokens[0].equals("read") && (tokens.length == 2)) {
			    String path;
			    String fileName = tokens[1];
			    path = "./" + guid + "/" + fileName;
			    long guidObject = md5(
				    fileName);
			    try {
				ChordMessageInterface peer =
				    chord.locateSuccessor(guidObject);
				InputStream stream = peer.get(guidObject);
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
