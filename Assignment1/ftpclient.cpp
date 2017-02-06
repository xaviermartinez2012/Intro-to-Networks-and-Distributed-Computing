// C++ client example using sockets.
// This programs can be compiled in linux and with minor modification in mac (mainly on the name of the headers).

// Windows requires extra lines of code and different headers
// #define WIN32_LEAN_AND_MEAN
// #include <windows.h>
// #include <winsock2.h>
// #include <ws2tcpip.h>

// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
// #pragma comment(lib, "Ws2_32.lib")
// ...
// WSADATA wsaData;
// iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
// ...

#include <iostream>
#include <string>
#include <stdio.h>
#include <stdlib.h>
#include <string.h> 
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>

//! The buffer length to use when retrieving messages from DTP server.
#define BUFFER_LENGTH 2048

/*!
\brief Creates connection via sockets using the provided IP and port.
\param host The host to connect to.
\param port The port to use.
\return The socket to use.
*/
int createConnection(std::string host, int port)
{
    int s;
    struct sockaddr_in sockaddr;
    
    memset(&sockaddr,0, sizeof(sockaddr));
    s = socket(AF_INET,SOCK_STREAM,0);
    sockaddr.sin_family=AF_INET;
    sockaddr.sin_port= htons(port);
    
    int a1,a2,a3,a4;
    if (sscanf(host.c_str(), "%d.%d.%d.%d", &a1, &a2, &a3, &a4 ) == 4)
    {
        std::cout << "Connection made by IP.";
        sockaddr.sin_addr.s_addr =  inet_addr(host.c_str());
    }
    else {
        std::cout << "Connection made by hostname.";
        hostent *record = gethostbyname(host.c_str());
        in_addr *addressptr = (in_addr *)record->h_addr;
        sockaddr.sin_addr = *addressptr;
    }
    if(connect(s,(struct sockaddr *)&sockaddr,sizeof(struct sockaddr))==-1)
    {
        perror("connection fail");
        exit(1);
        return -1;
    }
    return s;
}

/*!
\brief Send a request via the socket provided.
\param sock The socket to use.
\param message The message to send.
\return Error code.
*/
int request(int sock, std::string message)
{
    return send(sock, message.c_str(), message.size(), 0);
}

/*!
\brief Function to store data retrieved from the DTP server.
\param s The socket to use.
*/
void store(int s){
    // To-do
}

/*!
\brief Function to accept a reply from server.
\param s The socket to use.
\return The reply.
*/
std::string reply(int s)
{
    std::string strReply;
    int count;
    char buffer[BUFFER_LENGTH+1];
    
    usleep(1000000);
    do {
        count = recv(s, buffer, BUFFER_LENGTH, 0);
        buffer[count] = '\0';
        strReply += buffer;
    }while (count ==  BUFFER_LENGTH);
    return strReply;
}

/*!
\brief Send a request via the socket and recieve a reply.
\param s The socket to use.
\param message The message to send.
\return The reply.
*/
std::string requestReply(int s, std::string message)
{
	if (request(s, message) > 0)
    {
    	return reply(s);
	}
	return "";
}

/*!
\brief The main function.
\param argc The number of strings pointed to by argv.
\param argv The argument vector.
\return Exit code.
*/
int main(int argc , char *argv[])
{
    int sockpi;
    std::string strReply;
    
    std::cout << "We have " << argc << " arguments." << std::endl;
    
//  TODO  arg[1] can be a dns or an IP address.
    
    if (argc > 2)
        sockpi = createConnection(argv[1], atoi(argv[2]));
    if (argc == 2)
        sockpi = createConnection(argv[1], 21);
    else
        sockpi = createConnection("130.179.16.134", 21);
    strReply = reply(sockpi);
    std::cout << strReply  << std::endl;
    
    
    strReply = requestReply(sockpi, "USER anonymous\r\n");
//  std::cout << "This is the strReply: " << strReply << std::endl;
    
//  Let the system act according to the status and display
//  Display friendly message to the user
    
//  Returns 500 if login not ok
    if (strReply.find("331") != std::string::npos) {
        std::cout << "Login ok. Message:" << std::endl << strReply << std::endl;
    }
//  Returns 331 if login ok (search string for 331)
    else{
        std::cout << "Error login not ok. Message:" << std::endl << strReply << std::endl;
        return -1;
    }
    
    strReply = requestReply(sockpi, "PASS asa@asas.com\r\n");
    
//  Returns 230 if password ok
    if (strReply.find("230") != std::string::npos) {
        std::cout << "Password ok. Message:" << std::endl << strReply << std::endl;
    }
//  Returns 530 if password not ok
    else{
        std::cout << "Password not ok. Message:" << std::endl << strReply << std::endl;
        return -1;
    }
    
//  Let the user choose between LIST, RETR, QUIT
//  While loop keeps the user connected to server PI until entering QUIT
    std::string userInput;
    do{
        std::cout << "Enter LIST <Directory/FileName>, RETR [FileName], or QUIT: ";
        std::getline(std::cin,userInput);

	    strReply = requestReply(sockpi, "PASV\r\n");
    //  std::cout << strReply << std::endl;
        int A,B,C,D,a,b,port,sockpj;
        std::string ip;

    //  Returns 227 if passive mode ok
        if (strReply.find("227") != std::string::npos) {
        //  std::cout << "Entering passive mode... Message: " << strReply << std::endl;
            size_t start = strReply.find("(");
            size_t end = strReply.find(")");
            std::string IP_PORT = strReply.substr(start + 1, (end - start) - 1);
        //  std::cout << "This is the IP_PORT: " << IP_PORT << std::endl;
            std::sscanf(IP_PORT.c_str(), "%d,%d,%d,%d,%d,%d", &A, &B, &C, &D, &a, &b);
            port = ((a << 8) | b);
            char buff[100];
            snprintf(buff, sizeof(buff), "%d.%d.%d.%d", A, B, C, D);
            ip = buff;
        //  std::cout << "Converted ip: " << ip << std::endl;
            sockpj = createConnection(ip, port);
        }
    //  Passive mode not ok
        else{
        //  std::cout << "Denied passive mode... Message: " << strReply << std::endl;
            break;
        }
    //  Uppercase the command from userInput (Commands available are a maximum of 4 letters)
        for(unsigned int i = 0; i < 4; i++)
            userInput[i] = toupper(userInput[i]);
        
    //  Do function depending on user choice
        if (userInput.find("LIST") != std::string::npos)
        {
            strReply = requestReply(sockpi, userInput + "\r\n");
        //  std::cout << strReply << std::endl;
            std::string strReply2 = reply(sockpj);
            std::cout << strReply2 << std::endl;
            close(sockpj);
            strReply = reply(sockpi);
            std::cout << strReply << std::endl;
        }
        else if (userInput.find("RETR") == 0)
        {
            strReply = requestReply(sockpi, userInput + "\r\n");
        //  std::cout << std::endl << strReply << std::endl;
            if (strReply.find("550") != std::string::npos){
                close(sockpj);
            }
            else{
                reply(sockpj);
                close(sockpj);
                strReply = reply(sockpi);
                std::cout << std::endl << strReply << std::endl;
            }
        }
        else if (userInput == "QUIT")
        {
            strReply = requestReply(sockpi , "QUIT \r\n");
            if (strReply.find("221") != std::string::npos) {
                std::cout << std::endl << "Quiting..." << std::endl;
            }
            else{
                std::cout << std::endl << "Error quiting. Try Again." << std::endl;
            }
        }
        else{
            std::cout << std::endl << "Invalid option. Try again." << std::endl;
            close(sockpj);
        }
    } while (userInput != "QUIT");
    return EXIT_SUCCESS;
}