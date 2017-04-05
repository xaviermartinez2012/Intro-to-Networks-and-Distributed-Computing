import java.io.*;
import java.rmi.*;

public interface ChordMessageInterface extends Remote {
public ChordMessageInterface getPredecessor()  throws RemoteException;
ChordMessageInterface locateSuccessor(long key) throws RemoteException;
ChordMessageInterface closestPrecedingNode(long key) throws RemoteException;
public void joinRing(String Ip, int port)  throws RemoteException;
public void notify(ChordMessageInterface j) throws RemoteException;
public boolean isAlive() throws RemoteException;
public long getId() throws RemoteException;
public void setSuccessor(ChordMessageInterface s) throws RemoteException;
public void setPredecessor(ChordMessageInterface p) throws RemoteException;
public void cancelTimer() throws RemoteException;
public void put(long guidObject, InputStream file) throws IOException,
RemoteException;
public InputStream get(long guidObject) throws IOException, RemoteException;
public void delete(long guidObject) throws IOException, RemoteException;
}
