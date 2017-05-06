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
    lastRead = new Hashtable<Long, Date>();
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
			    File f = new File(path);
			    if (f.exists()) {
				long guidObject = md5(fileName);
				long guidObject1 = md5(fileName + "1");
				long guidObject2 = md5(fileName + "2");
				long guidObject3 = md5(fileName + "3");
				ChordMessageInterface peer1 =
				    chord.locateSuccessor(guidObject1);
				ChordMessageInterface peer2 =
				    chord.locateSuccessor(guidObject2);
				ChordMessageInterface peer3 =
				    chord.locateSuccessor(guidObject3);
				Date localLastRead = new Date(System.currentTimeMillis());
				boolean noHistory = false;
				if (lastRead.containsKey(guidObject)) {
				    localLastRead = lastRead.get(guidObject);
				} else {
				    if (peer1.fileExists(guidObject1) || peer2.fileExists(guidObject2) ||
					peer3.fileExists(guidObject3))
					noHistory = true;
				}
				if (noHistory) {
				    System.out.println(
					"-- ERROR: Conflict due to no \"lastRead\" history! Please read the latest version of \""
					+
					fileName + "\" and try again.");
				} else {
				    System.out.println("-- Opening transaction on \"" + fileName + "\"");
				    if (peer1.canCommit(guidObject1, localLastRead)) {
					if (peer2.canCommit(guidObject2, localLastRead)) {
					    if (peer3.canCommit(guidObject3, localLastRead)) {
						try {
						    FileStream file = new FileStream(
							    path);
						    Date read = new Date(System.currentTimeMillis());
						    peer1.put(guidObject1, file);
						    peer1.commit(guidObject1, read);
						    peer2.put(guidObject2, file);
						    peer2.commit(guidObject2, read);
						    peer3.put(guidObject3, file);
						    peer3.commit(guidObject3, read);
						    lastRead.replace(guidObject, read);
						    lastRead.put(guidObject, read);
						    System.out.println("-- \"" + fileName + "\" committed on " +
							read.toString());
						} catch (IOException e) {
						    e.printStackTrace();
						}
					    } else {
						System.out.println(
						    "-- ERROR: Peer3 rejected commit request. Aborting transaction...");
						peer2.abort(guidObject2);
						peer1.abort(guidObject1);
					    }
					} else {
					    System.out.println(
						"-- ERROR: Peer2 rejected commit request. Aborting transaction...");
					    peer1.abort(guidObject1);
					}
				    } else {
					System.out.println(
					    "-- ERROR: Peer1 rejected commit request. Aborting transaction...");
				    }
				}
			    } else {
				System.out.println("-- ERROR: \"" + fileName + "\" does not exist!");
			    }
			}
			if (tokens[0].equals("read") && (tokens.length == 2)) {
			    String path;
			    String fileName = tokens[1];
			    path = "./" + guid + "/" + fileName;
			    long guidObject = md5(
				    fileName);
			    long guidObject1 = md5(fileName + "1");
			    try {
				ChordMessageInterface peer =
				    chord.locateSuccessor(guidObject1);
				InputStream stream = peer.get(guidObject1);
				FileOutputStream output =
				    new FileOutputStream(path);
				while (stream.available() > 0)
				    output.write(stream.read());
				output.close();
				Date read = new Date(System.currentTimeMillis());
				if (lastRead.containsKey(guidObject))
				    lastRead.replace(guidObject, read);
				else
				    lastRead.put(guidObject, read);
			    } catch (IOException e) {
				e.printStackTrace();
				lastRead.remove(guidObject);
			    }
			}
			if (tokens[0].equals("delete") && (tokens.length == 2)) {
			    String fileName = tokens[1];
			    long guidObject = md5(
				    fileName);
			    long guidObject1 = md5(
				    fileName + "1");
			    long guidObject2 = md5(
				    fileName + "2");
			    long guidObject3 = md5(
				    fileName + "3");
			    try {
				ChordMessageInterface peer1 =
				    chord.locateSuccessor(guidObject1);
				peer1.delete(guidObject1);
				ChordMessageInterface peer2 =
				    chord.locateSuccessor(guidObject2);
				peer2.delete(guidObject2);
				ChordMessageInterface peer3 =
				    chord.locateSuccessor(guidObject3);
				peer3.delete(guidObject3);
				if (lastRead.containsKey(guidObject))
				    lastRead.remove(guidObject);
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
			    for (File file: files) {
				String fileName = file.getName();
				if (fileName.startsWith("."))
				    continue;
				Long fileGUID = Long.parseLong(fileName);
				ChordMessageInterface peer = chord.successor;
				peer.transferKey(fileGUID, chord.getLastWritten(fileGUID));
				file.renameTo(new File(
					"./" + peer.getId() + "/repository/" + fileName));
			    }
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
