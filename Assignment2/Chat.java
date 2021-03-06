import java.net.*;
import java.io.*;
import java.util.*;
import javax.json.*;
import java.nio.charset.StandardCharsets;

/*****************************/
/**
* \brief It implements a distributed chat. 
* It creates a ring and delivers messages
* using flooding 
**********************************/
public class Chat {

    // My info
    public String alias;
    public int myPort;
    // Successor
    public String ipSuccessor;
    public int portSuccessor;
    // Predecessor
    public String ipPredecessor;
    public int portPredecessor;

    private Object lock1 = new Object();

    /*!
    \brief Creates connection via sockets using the provided port.
    \param port The port to use.
    \return The socket to use.
    */
    public Socket GetSocket(int port) throws IOException {
        Socket socket = new Socket("127.0.0.1", port);
        return socket;
    }

    /*****************************/
    /**
    * \class Server class "chat.java" 
    * \brief It implements the server
    **********************************/
    public class Server implements Runnable {
        public Server() {
        }

        /*   
            {
            "type" :  "ACCEPT",
            "parameters" :
                {   
                "ipPred"    : string,
                "portPred"  : number
                }
            }
        */
        /*!
        \brief Sends "ACCEPT" message via sockets.
        \param clientSocket The Socket to use.
        \param ipPred The IP of the Predecessor.
        \param portPred The port of the Predecessor.
        \param myPort The port of the sender.
        */
        public void accept(Socket clientSocket, String ipPred, int portPred, int myPort) throws IOException {
            JsonObject jsonAcceptMessageObject = Json.createObjectBuilder().add("type", "ACCEPT").add("parameters", Json
                    .createObjectBuilder().add("ipPred", ipPred).add("portPred", portPredecessor).add("myPort", myPort))
                    .build();
            OutputStreamWriter out = new OutputStreamWriter(clientSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(jsonAcceptMessageObject.toString());
            out.close();
        }

        /*   
            {
            "type" :  "ACCEPTED",
            "parameters" :
                {   
                "ipPred"    : string,
                "portPred"  : number
                }
            }
        */
        /*!
        \brief Sends "ACCEPTED" message via sockets.
        \param peerSocket The Socket to use.
        \param updatedPredIP The updated Predecessor IP of the sender.
        \param updatedPortPred The updated port of the sender.
        \param myPort The port of the sender.
        */
        public void accepted(Socket peerSocket, String updatedPredIP, int updatedPortPred, int myPort)
                throws IOException {
            JsonObject jsonAcceptMessageObject = Json
                    .createObjectBuilder().add("type", "ACCEPTED").add("parameters", Json.createObjectBuilder()
                            .add("ipPred", updatedPredIP).add("portPred", updatedPortPred).add("myPort", myPort))
                    .build();
            OutputStreamWriter out = new OutputStreamWriter(peerSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(jsonAcceptMessageObject.toString());
            out.close();
        }

        /*
            {
            "type" :  "NEWSUCCESSOR",
            "parameters" :
                {
                "ipSuccessor"    : string,
                "portSuccessor"  : number
                }
            }
        */
        /*!
        \brief Sends "NEWSUCCESSOR" message via sockets.
        \param succSocket The Socket to use.
        \param ipSucc The IP of the Successor to update on the Receiver.
        \param portPred The port of the Predecessor.
        \param myPort The port of the sender.
        */
        public void NewSuccessor(Socket succSocket, String ipSucc, int portSucc, int myPort) throws IOException {
            JsonObject jsonAcceptMessageObject = Json.createObjectBuilder().add("type", "NEWSUCCESSOR")
                    .add("parameters", Json.createObjectBuilder().add("ipSuccessor", ipSucc)
                            .add("portSuccessor", portSucc).add("myPort", myPort))
                    .build();
            OutputStreamWriter out = new OutputStreamWriter(succSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(jsonAcceptMessageObject.toString());
            out.close();
        }
        
        /*!
        \brief Forwards Json message to Successor via sockets.
        \param succSocket The Socket to use.
        \param message The Json message.
        */
        public void forward(Socket succSocket, JsonObject message) throws IOException{
            OutputStreamWriter out = new OutputStreamWriter(succSocket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(message.toString());
            out.close();
        }

        /*****************************/
        /**
        * \brief It allows the system to interact with the participants. 
        **********************************/
        public void run() {
            try {
                ServerSocket servSock = new ServerSocket(myPort);
                while (true) {
                    Socket clientSock = servSock.accept();
                    new Thread() {
                        public void run() {
                            try {
                                JsonReader reader = Json.createReader(clientSock.getInputStream());
                                JsonObject message = reader.readObject();
                                reader.close();
                                clientSock.close();
                                JsonObject parameters = message.getJsonObject("parameters");
                                if (message.getString("type").equals("JOIN")) {
                                    int joinPort = parameters.getInt("myPort");
                                    System.out.println();
                                    System.out.println("\n-- Peer at port " + joinPort + " is requesting to join.");
                                    System.out.println("-- Sending request to accept to peer at port " + joinPort + ".");
                                    Socket acceptSocket = GetSocket(joinPort);
                                    accept(acceptSocket, ipPredecessor, portPredecessor, myPort);
                                    Socket predSocket = GetSocket(portPredecessor);
                                    System.out.println("-- Sending successor information to predecessor at port "
                                            + portPredecessor + ".");
                                    NewSuccessor(predSocket, "127.0.0.1", joinPort, myPort);
                                    synchronized (lock1) {
                                        System.out.println("-- Lock set --");
                                        System.out.println("-- Updating portPredecessor -> " + joinPort);
                                        portPredecessor = joinPort;
                                        System.out.println("-- Unlocking --");
                                    }
                                } else if (message.getString("type").equals("ACCEPT")) {
                                    int acceptPredPort = parameters.getInt("portPred");
                                    int peerPort = parameters.getInt("myPort");
                                    System.out.println();
                                    System.out.println("\n-- Peer at port " + peerPort + " is requesting to accept.");
                                    synchronized (lock1) {
                                        System.out.println("-- Lock set --");
                                        System.out.println("-- Updating portPredecessor -> " + acceptPredPort);
                                        portPredecessor = acceptPredPort;
                                        System.out.println("-- Updating portSuccessor -> " + peerPort);
                                        portSuccessor = peerPort;
                                        System.out.println("-- Unlocking --");
                                    }
                                    System.out.println("-- Accepting peer at port " + peerPort + ".");
                                    Socket acceptedSocket = GetSocket(peerPort);
                                    accepted(acceptedSocket, ipPredecessor, portPredecessor, myPort);
                                } else if (message.getString("type").equals("ACCEPTED")) {
                                    int peerPort = parameters.getInt("myPort");
                                    System.out.println("\n-- Peer at port " + peerPort + " accepted connection.");
                                } else if (message.getString("type").equals("NEWSUCCESSOR")) {
                                    int newSuccessorPort = parameters.getInt("portSuccessor");
                                    int peerPort = parameters.getInt("myPort");
                                    System.out.println("\n-- Peer at port " + peerPort
                                            + " is requesting to update portSuccessor information.");
                                    synchronized (lock1) {
                                        System.out.println("-- Lock set --");
                                        System.out.println("-- Updating portSuccessor -> " + newSuccessorPort);
                                        portSuccessor = newSuccessorPort;
                                        System.out.println("-- Unlocking --");
                                    }
                                } else if (message.getString("type").equals("PUT")) {
                                    String senderAlias = parameters.getString("aliasSender");
                                    String receiverAlias = parameters.getString("aliasReceiver");
                                    int peerPort = parameters.getInt("myPort");
                                    System.out.println("\n-- Peer at port " + peerPort + " has sent a message.");
                                    if (senderAlias.equals(alias)) {
                                        System.out.println("-- ERROR: The alias you specified does match a node within this network.");
                                    }
                                    else if (receiverAlias.equals(alias)) {
                                        System.out.println("-- Alias confirmed. Displaying message:");
                                        synchronized (lock1) {
                                            System.out.println("-- \"" + parameters.getString("message") + "\"");
                                        }
                                    }
                                    else {
                                        System.out.println("-- Alias not confirmed. Forwarding to successor.");
                                        Socket forwardSocket = GetSocket(portSuccessor);
                                        forward(forwardSocket, message);
                                    }
                                } else if (message.getString("type").equals("LEAVE")) {
                                    int newPortPredecessor = parameters.getInt("portPred");
                                    int peerPort = parameters.getInt("myPort");
                                    System.out.println("\n-- Peer at port " + peerPort + " is leaving.");
                                    synchronized (lock1) {
                                        System.out.println("-- Lock set --");
                                        System.out.println("-- Updating portPredecessor -> " + newPortPredecessor);
                                        portPredecessor = newPortPredecessor;
                                        System.out.println("-- Unlocking --");
                                    }
                                    Socket predSocket = GetSocket(portPredecessor);
                                    System.out.println("-- Sending successor information to predecessor at port "
                                            + portPredecessor + ".");
                                    NewSuccessor(predSocket, "127.0.0.1", myPort, myPort);
                                }			
                            } catch (JsonException j) {
                                System.out.println("Json exception exiting...");
                                System.exit(-1);
                            } catch (IOException io) {
                                System.out.println("IOException in inner thread block. Exiting...");
                                System.exit(-1);
                            } finally {
                                synchronized (lock1) {
                                    System.out.println("-- State of system --");
                                    System.out.println("-- myAlias         : " + alias);
                                    System.out.println("-- myPort          : " + myPort);
                                    System.out.println("-- portPredecessor : " + portPredecessor);
                                    System.out.println("-- portSuccessor   : " + portSuccessor);
                                    System.out.print("> ");
                                }
                            }
                        }
                    }.start();
                }
            } catch (IOException e) {
                System.out.println("IOException in server block...");
                System.exit(-1);
            }
        }
    }

    /*****************************/
    /** 
    * \brief It implements the client.
    *********************************/
    public class Client implements Runnable {

        public Client() {
        }

        /*
            {
            "type" :  "JOIN",
            "parameters" :
                {   
                "myAlias" : string,
                "myPort"  : number
                }
            }
        */
        /*!
        \brief Sends "JOIN" message via sockets.
        \param socket The Socket to use.
        */
        public void join(Socket socket) throws IOException {
            JsonObject jsonJoinMessageObject = Json.createObjectBuilder().add("type", "JOIN")
                    .add("parameters", Json.createObjectBuilder().add("myAlias", alias).add("myPort", myPort)).build();
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(jsonJoinMessageObject.toString());
            out.close();
        }

        /*
            {
        "type" :  "Put",
        "parameters" :
            {
            "aliasSender"    : string,
            "aliasReceiver"  : string,
            "message"        : string,
            "myPort"  		 : number
            }
        }
        */
        /*!
        \brief Sends "PUT" message via sockets.
        \param socket The Socket to use.
        \param aliasSender The alias of the sender.
        \param aliasReceiver The alias of the reciever.
        \param text The message.
        */
        public void put(Socket socket, String aliasSender, String aliasReceiver, String text) throws IOException {
            JsonObject jsonPutMessageObject = Json.createObjectBuilder().add("type", "PUT")
                    .add("parameters",
                            Json.createObjectBuilder().add("aliasSender", aliasSender)
                                    .add("aliasReceiver", aliasReceiver).add("message", text).add("myPort", myPort))
                    .build();
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(jsonPutMessageObject.toString());
            out.close();
        }

        /*
        {
        "type" :  "LEAVE",
        "parameters" :
            {
            "ipPred"    : string,
            "portPred"  : number
            }
        }
        */
        /*!
        \brief Sends "LEAVE" message via sockets.
        \param socket The Socket to use.
        */
        public void leave(Socket socket) throws IOException {
            JsonObject jsonJoinMessageObject = Json
                    .createObjectBuilder().add("type", "LEAVE").add("parameters", Json.createObjectBuilder()
                            .add("ipPred", ipPredecessor).add("portPred", portPredecessor).add("myPort", myPort))
                    .build();
            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
            out.write(jsonJoinMessageObject.toString());
            out.close();
        }

        /*!
        \brief Provides a text-based user menu.
        \param in The scanner to use for user input.
        */
        public int SelectionMenu(Scanner in) {
            boolean correct_input = false;
            int selection = 0;
            while (!correct_input) {
                System.out.println("Please select an option:");
                System.out.println("(1) Join");
                System.out.println("(2) Put");
                System.out.println("(3) Leave");
                System.out.print("> ");
                try {
                    selection = in.nextInt();
                    if (selection != 1 && selection != 2 && selection != 3) {
                        System.out.println("Invalid choice. Try again...");
                    } else {
                        correct_input = true;
                    }
                } catch (InputMismatchException input) {
                    System.out.println("This menu only accepts integers. Try again...");
                } catch (NoSuchElementException element) {
                    element.printStackTrace();
                    System.out.println("Scanner error, quitting...");
                    System.exit(-1);
                }
            }
            return selection;
        }

        /*!
        \brief Provides a text-based user menu for the JOIN functionality.
        \param in The scanner to use for user input.
        */
        public int JoinMenu(Scanner in) {
            boolean correct_port = false;
            int port = myPort;
            while (!correct_port) {
                System.out.println("Please enter a port to connect to.");
                System.out.print("> ");
                try {
                    port = in.nextInt();
                    correct_port = true;
                } catch (InputMismatchException input) {
                    System.out.println("Enter an interger port number.");
                } catch (NoSuchElementException element) {
                    element.printStackTrace();
                    System.out.println("Scanner error, quitting...");
                    System.exit(-1);
                }
            }
            System.out.println();
            return port;
        }
        /*!
        \brief Provides a text-based user menu for the PUT functionality.
        \param in The scanner to use for user input.
        */
        public String[] putMenu(Scanner in) {
            boolean correct_input = false;
            String[] putReturn = new String[2];
            String aliasReciev = ""; 
            String msg = "";
            while (!correct_input) {
                try {
                    System.out.println("Please enter reciever's alias.");
                    System.out.print("> ");
                    in.nextLine();
                    aliasReciev = in.nextLine();
                    System.out.println("Please enter message.");
                    System.out.print("> ");
                    msg = in.nextLine();
                    correct_input = true;
                } catch (NoSuchElementException element) {
                    element.printStackTrace();
                    System.out.println("Scanner error, quitting...");
                    System.exit(-1);
                }
            }
            putReturn[0] = aliasReciev;
            putReturn[1] = msg;
            System.out.println();
            return putReturn;
        }

        /*****************************/
        /**
        * \brief It allows the user to interact with the system. 
        **********************************/
        public void run() {
            synchronized (lock1) {
                ipSuccessor = "127.0.0.1";
                ipPredecessor = "127.0.0.1";
                portSuccessor = myPort;
                portPredecessor = myPort;
            }
            Scanner in = new Scanner(System.in);
            while (true) {
                int userSelection = SelectionMenu(in);
                switch (userSelection) {
                case 1:
                    int port = JoinMenu(in);
                    try {
                        Socket sock = GetSocket(port);
                        join(sock);
                    } catch (IOException io) {
                        io.printStackTrace();
                        System.out.println("IO Exception in Case 1.");
                    }
                    break;
                case 2:
                    String putParameter[] = putMenu(in);
                    try {
                        Socket sock = GetSocket(portSuccessor);
                        put(sock, alias, putParameter[0], putParameter[1]);
                    } catch (IOException io) {
                        System.out.println("IO Exception in Case 2.");
                    }
                    break;
                case 3:
                    try {
                        Socket sock = GetSocket(portSuccessor);
                        leave(sock);
                    } catch (IOException io) {
                        io.printStackTrace();
                        System.out.println("IO Exception in Case 3.");
                    }
                    System.out.println("Exiting...");
                    in.close();
                    System.exit(0);
                }
            }
        }
    }

    /*****************************/
    /**
    * Starts the threads with the client and server.
    * \param alias unique identifier of the process.
    * \param myPort where the server will listen.
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
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("InterruptedException in Chat class. Exiting...");
            System.exit(-1);
        }
    }

    /*!
    \brief The main.
    \param args The arguments: <alias> <myPort>.
    */
    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Parameter: <alias> <myPort>");
        }
        Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
    }
}
