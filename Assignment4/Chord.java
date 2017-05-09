import java.io.*;
import java.io.FileNotFoundException;
import java.net.*;
import java.nio.file.*;
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;
import java.util.*;

/*****************************/

/**
 * \brief Functions for the Chord of
 * the ring.
 *
 **********************************/
public class Chord extends java.rmi.server.UnicastRemoteObject implements
    ChordMessageInterface {
public static final int M = 2;

Registry registry;			// rmi registry for lookup the remote objects.
ChordMessageInterface successor;
ChordMessageInterface predecessor;
ChordMessageInterface[] finger;
int nextFinger;
long guid;				// GUID (i)
Hashtable<Long, Date> lastWritten;
Hashtable<Long, Boolean> fileBusy;
Timer timer;

/*!
 * \brief get the last written time of an file
 * \param guid guid of the file
 * \return time of the last written
 */
public Date getLastWritten(Long guid) {
    Date written;

    if (lastWritten.containsKey(guid)) {
	written = lastWritten.get(guid);
    } else {
	// Subtract an hour from current time of the object
	written = new Date(System.currentTimeMillis() - 3600 * 1000);
	lastWritten.put(guid, written);
    }
    return lastWritten.get(guid);
}

/*!
 * \brief add values/state to the HashMaps
 * \param ip ip of the other user
 * \param port of other user
 * \return the chord
 */
public void transferKey(Long guid, Date log) {
    lastWritten.put(guid, log);
    fileBusy.put(guid, false);
}

/*!
 * \brief check if file exists
 * \param guid guid of the file
 * \return true if file exists/false if it does not
 */
public boolean fileExists(Long guid) {
    return lastWritten.containsKey(guid);
}

/*!
 * \brief check if file can be committed
 * \param guid guid of the file
 * \param Date the last date/time the user read the file
 * \return true if file can commit/false if it can not
 */
public boolean canCommit(Long guid, Date userLastRead) {
    boolean	vote = false;
    Date	written = getLastWritten(guid);

    if (!fileBusy.containsKey(guid))
	fileBusy.put(guid, false);
    if ((written.before(userLastRead) || written.equals(userLastRead)) && !fileBusy.get(guid)) {
	vote = true;
	fileBusy.replace(guid, true);
    }
    return vote;
}

/*!
 * \brief abort the commit
 * \param guid guid of the file
 */
public void abort(Long guid) {
    fileBusy.replace(guid, false);
}

/*!
 * \brief commit the file
 * \param guid guid of the file
 * \param Date the date/time that the commit occurs
 */
public void commit(Long guid, Date write) {
    lastWritten.replace(guid, write);
    fileBusy.replace(guid, false);
}

/*!
 * \brief create an chord between user
 * \param ip ip of the other user
 * \param port of other user
 * \return the chord
 */
public ChordMessageInterface rmiChord(String ip, int port) {
    ChordMessageInterface chord = null;

    try {
	Registry registry = LocateRegistry.getRegistry(ip, port);
	chord = (ChordMessageInterface)(registry.lookup("Chord"));
    } catch (RemoteException | NotBoundException e) {
	e.printStackTrace();
    }
    return chord;
}

/*!
 * \brief change the sucessor
 * \param s sucessor
 */
public void setSuccessor(ChordMessageInterface s) throws RemoteException {
    successor = s;
    if (successor.getId() == guid) {
	for (int j = 0; j < M; j++)
	    finger[j] = null;
	cancelTimer();
    }
}

/*!
 * \brief change the predecessor
 * \param p predecessor
 */
public void setPredecessor(ChordMessageInterface p) throws RemoteException {
    if (p.getId() == guid) {
	predecessor = null;
	cancelTimer();
    } else {
	predecessor = p;
    }
}

/*!
 * \brief cancels the timer
 */
public void cancelTimer() {
    timer.cancel();
    timer.purge();
}

/*!
 * \brief restarts the timer
 */
public void restartTimer() {
    timer.cancel();
    timer.purge();
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
		stabilize();
		fixFingers();
		checkPredecessor();
	    }
	}, 500, 500);
}

/*!
 * \brief               check if key is the semi close interval (]
 * \param key	key of the file
 * \param key1	key of user1
 * \param key2	key of user2
 * \return      true or false
 */
public Boolean isKeyInSemiCloseInterval(long key, long key1, long key2) {
    if (key1 < key2)
	return key > key1 && key <= key2;
    else
	return key > key1 || key <= key2;
}

/*!
 * \brief		check if key is in the open interval (not including end)
 * \param key	key of the file
 * \param key1	key of user1
 * \param key2	key of user2
 * \return      true or false
 */
public Boolean isKeyInOpenInterval(long key, long key1, long key2) {
    if (key1 < key2)
	return key > key1 && key < key2;
    else
	return key > key1 || key < key2;
}

/*!
 * \brief write the file
 * \param guidObject	guid of the file
 * \param stream		the file
 */
public void put(long guidObject, InputStream stream) throws RemoteException {
    String fileName = "./" + guid + "/repository/" + guidObject;

    try {
	FileOutputStream output = new FileOutputStream(fileName);
	while (stream.available() > 0)
	    output.write(stream.read());
	output.close();
    } catch (IOException e) {
	System.out.println(e);
    }
}

/*!
 * \brief retrieve a file in the ring
 * \param guidobject the file to retrieve
 * \return the file
 */
public FileStream get(long guidObject) throws RemoteException {
    String fileName = "./" + guid + "/repository/" + guidObject;

    try {
	FileStream file = new FileStream(fileName);
	return file;
    } catch (FileNotFoundException fnf) {
	throw (new RemoteException(
		   "-- ERROR: The file specified does not exist!"));
    } catch (IOException io) {
	throw (new RemoteException("-- ERROR: IO Exception"));
    }
}

/*!
 * \brief delete a file in the ring
 * \param guidobject the file to delete
 */
public void delete(long guidObject) throws RemoteException {
    try {
	Path fileName = Paths.get("./" + guid + "/repository/" + guidObject);
	Files.delete(fileName);
	lastWritten.remove(guidObject);
	fileBusy.remove(guidObject);
    } catch (NoSuchFileException x) {
	throw (new RemoteException("-- ERROR: The file specified does not exist!"));
    } catch (IOException x) {
	throw (new RemoteException("-- ERROR: IO Exception"));
    }
}

/*!
 * \brief get guid oh node
 * \return guid
 */
public long getId() throws RemoteException {
    return guid;
}

/*!
 * \brief checks for status of this chord
 * \return state of the chord in boolean
 */
public boolean isAlive() throws RemoteException {
    return true;
}

/*!
 * \brief gets the prefedd
 * \return the predecessor
 */
public ChordMessageInterface getPredecessor() throws RemoteException {
    return predecessor;
}

/*!
 * \brief	find the successor of an obj
 * \param       key key of the obj
 * \return the sucessor
 */
public ChordMessageInterface locateSuccessor(long key) throws RemoteException {
    if (key == guid)
	throw new IllegalArgumentException("Key must be distinct that  " + guid);
    if (successor.getId() != guid) {
	if (isKeyInSemiCloseInterval(key, guid, successor.getId()))
	    return successor;
	ChordMessageInterface j = closestPrecedingNode(key);
	if (j == null)
	    return null;
	return j.locateSuccessor(key);
    }
    return successor;
}

/*!
 * \brief	func to find the closest preceding node
 * \param       key	key of the obj
 * \return the successor of the obj
 */
public ChordMessageInterface closestPrecedingNode(long key) throws
RemoteException {
    return successor;
}

/*!
 * \brief	func to join the ring
 * \param ip	ip of the connecting predessor
 * \param port	port of the connecting prefessor
 */
public void joinRing(String ip, int port) throws RemoteException {
    try {
	System.out.println("Get Registry to join ring");
	Registry		registry = LocateRegistry.getRegistry(ip, port);
	ChordMessageInterface	chord =
	    (ChordMessageInterface)(registry.lookup("Chord"));
	predecessor = null;
	successor = chord.locateSuccessor(this.getId());
	successor.restartTimer();
	System.out.println("Joining ring");
    } catch (RemoteException | NotBoundException e) {
	successor = this;
    }
}

/*!
 * \brief	func to find the next successor
 */
public void findingNextSuccessor() {
    int i;

    successor = this;
    for (i = 0; i < M; i++) {
	try {
	    if (finger[i].isAlive())
		successor = finger[i];
	} catch (RemoteException | NullPointerException e) {
	    finger[i] = null;
	}
    }
}

/*!
 * \brief	func to stabalize when user leaves
 */
public void stabilize() {
    try {
	if (successor != null) {
	    ChordMessageInterface x = successor.getPredecessor();
	    if ((x != null &&x.getId() != this.getId()) &&
		isKeyInOpenInterval(x.getId(), this.getId(), successor.getId()))
		successor = x;
	    if (successor.getId() != getId()) {
		successor.notify(this);
		if (successor.getPredecessor().getId() == guid) {
		    for (int j = 0; j < M; j++)
			finger[j] = successor;
		}
	    }
	    File	folder = new File("./" + guid + "/repository");
	    File[]	files = folder.listFiles();
	    for (File file: files) {
		String fileName = file.getName();
		if (fileName.startsWith("."))
		    continue;
		long			fileGuid = Long.parseLong(fileName);
		ChordMessageInterface	peer = locateSuccessor(fileGuid);
		long			destinationGUID = peer.getId();
		if (destinationGUID == guid)
		    continue;
		peer.transferKey(fileGuid, getLastWritten(fileGuid));
		file.renameTo(new File(
			"./" + destinationGUID + "/repository/" + fileName));
	    }
	}
    } catch (RemoteException | NullPointerException e1) {
	findingNextSuccessor();
    }
}

/*!
 * \brief Method to notify successor of new predecessor
 * \param j The successor to notify.
 */
public void notify(ChordMessageInterface j) throws RemoteException {
    if ((predecessor == null) ||
	(predecessor !=
	 null &&isKeyInOpenInterval(j.getId(), predecessor.getId(), guid)))
	predecessor = j;
}

/*!
 * \brief fix the fingers (pointers)
 */
public void fixFingers() {
    long id = guid;

    try {
	long nextId;
	if (nextFinger == 0)
	    nextId = (this.getId() + (1 << nextFinger));
	else
	    nextId = finger[nextFinger - 1].getId();
	finger[nextFinger] = locateSuccessor(nextId);
	if (finger[nextFinger].getId() == guid)
	    finger[nextFinger] = null;
	else
	    nextFinger = (nextFinger + 1) % M;
    } catch (RemoteException | NullPointerException e) {
	finger[nextFinger] = null;
	e.printStackTrace();
    }
}

/*!
 * \brief check the if predecesssor is empty
 */
public void checkPredecessor() {
    try {
	if (predecessor != null && !predecessor.isAlive())
	    predecessor = null;
    } catch (RemoteException e) {
	predecessor = null;
	// e.printStackTrace();
    }
}

/*!
 * \brief	func to create chord
 * \param port	port of the chord
 * \param guid	guid of the chord
 */
public Chord(int port, long guid) throws RemoteException {
    int j;

    finger = new ChordMessageInterface[M];
    for (j = 0; j < M; j++)
	finger[j] = null;
    this.guid = guid;
    lastWritten = new Hashtable<Long, Date>();
    fileBusy = new Hashtable<Long, Boolean>();
    predecessor = null;
    successor = this;
    this.timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
	    @Override
	    public void run() {
		stabilize();
		fixFingers();
		checkPredecessor();
	    }
	}, 500, 500);
    try {
	// create the registry and bind the name and object.
	System.out.println(guid + " is starting RMI at port=" + port);
	registry = LocateRegistry.createRegistry(port);
	registry.rebind("Chord", this);
    } catch (RemoteException e) {
	throw e;
    }
}

/*!
 * \brief	print information of the node
 */
void Print() {
    int i;

    try {
	if (successor != null)
	    System.out.println("successor " + successor.getId());
	if (predecessor != null)
	    System.out.println("predecessor " + predecessor.getId());
	for (i = 0; i < M; i++) {
	    try {
		if (finger != null)
		    System.out.println("Finger " + i + " " + finger[i].getId());
	    } catch (NullPointerException e) {
		finger[i] = null;
	    }
	}
    } catch (RemoteException e) {
	System.out.println("Cannot retrive id");
    }
}
}
