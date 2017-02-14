import java.net.*;
import java.io.*;
import java.util.*;
/*****************************//**
* \brief It implements a distributed chat. 
* It creates a ring and delivers messages
* using flooding 
**********************************/
public class Chat   {
  
/*
   Json Messages:
 
  {
        "type" :  "JOIN",
        "parameters" :
               {   
                    "myAlias" : string,
                    "myPort"  : number
               }
   }
 
   {
        "type" :  "ACCEPT",
        "parameters" :
               {   
                   "ipPred"    : string,
                   "portPred"  : number
               }
    }
 
    {
         "type" :  "LEAVE",
         "parameters" :
         {
             "ipPred"    : string,
             "portPred"  : number
         }
    }

   {
         "type" :  "Put",
        "parameters" :
         {
             "aliasSender"    : string,
             "aliasReceiver"  : string,
             "message"        : string
        }
   }
 
 {
        "type" :  "NEWSUCCESSOR",
        "parameters" :
        {
            "ipSuccessor"    : string,
            "portSuccessor"  : number
        }
 }
 */
    
// My info
    public String alias;
    public int myPort;
// Successor
    public String ipSuccessor;
    public int portSuccessor;
// Predecessor
    public String ipPredecessor;
    public int portPredecessor;
    
 
  
/*****************************//**
* \class Server class "chat.java" 
* \brief It implements the server
**********************************/ 
  private class Server implements Runnable 
  {
    public Server()
    {
    }
/*****************************//**
* \brief It allows the system to interact with the participants. 
**********************************/   
    public void run() {
        try {
            ServerSocket servSock = new ServerSocket(myPort);
            while (true)
            {
                /*
                 Socket clntSock = servSock.accept(); // Get client connections
                 Create a new thread to handle the connection
           
                 ObjectInputStream  ois = new
                 ObjectInputStream(clntSock.getInputStream());
                 ObjectOutputStream oos = new
                 ObjectOutputStream(clntSock.getOutputStream());
                 ois.read();    reads the message using JsonParser and handle the messages
          
                 oos.write(m);   only if the message requires a response
                 clntSock.close();
                 */
            }
            } catch (IOException e)
            {
                // Handle the exception
            }
        }
  }

    
    
/*****************************//*
* \brief It implements the client
**********************************/
  private class Client implements Runnable 
  {       
    
    public Client()
    {
    }

/*****************************//**
* \brief It allows the user to interact with the system. 
**********************************/    
    public void run()
    {
      while (true)
      {
          /*
          Create a simple user interface
        
          The first thing to do is to join
             ask the ip and port when joining and set ipSuccessor = ip, portSuccessor = port
          Socket socket = new Socket(ipSuccessor, portSuccessor);
          
          
          // Create the mssages m using JsonWriter and send it as stream
          
          ObjectOutputStream oos = new
          ObjectOutputStream(socket.getOutputStream());
          ObjectInputStream ois = new
          ObjectInputStream(socket.getInputStream());
          oos.write(m);   this sends the message
            ois.read();    reads the response and parse it using JsonParser
          socket.close();
           
           Use mutex to handle race condition when reading and writing the global variable (ipSuccessor, 
                portSuccessor, ipPredecessor, portPredecessor)
           
           */
      }
    }
  }
  
/*****************************//**
* Starts the threads with the client and server:
* \param Id unique identifier of the process
* \param port where the server will listen
**********************************/  
  public Chat(String alias, int myPort) {
     
      this.alias = alias;
      this.myPort = myPort;
      // Initialization of the peer
      Thread server = new Thread(new Server());
      Thread client = new Thread(new Client());
      server.start();
      client.start();
      try {
          client.join();
          server.join();
      } catch (InterruptedException e)
      {
            // Handle Exception
      
      }
  }
  
  public static void main(String[] args) {
      
      if (args.length < 2 ) {  
          throw new IllegalArgumentException("Parameter: <alias> <myPort>");
      }
      Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
  }
}
