/**
    C++ client example using sockets.
    This programs can be compiled in linux and with minor modification in 
	   mac (mainly on the name of the headers)
    Windows requires extra lines of code and different headers
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <winsock2.h>
#include <ws2tcpip.h>

// Need to link with Ws2_32.lib, Mswsock.lib, and Advapi32.lib
#pragma comment(lib, "Ws2_32.lib")
...
WSADATA wsaData;
iResult = WSAStartup(MAKEWORD(2,2), &wsaData);
...
*/
#include <iostream>    //cout
#include <string>
#include <stdio.h> //printf
#include <stdlib.h>
#include <string.h>    //strlen
#include <sys/socket.h>    //socket
#include <arpa/inet.h> //inet_addr
#include <netinet/in.h>
#include <sys/types.h>
#include <unistd.h>
#include <netdb.h>

#define BUFFER_LENGTH 2048

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
        std::cout << "by ip";
        sockaddr.sin_addr.s_addr =  inet_addr(host.c_str());
    }
    else {
        std::cout << "by name";
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

int request(int sock, std::string message)
{
    return send(sock, message.c_str(), message.size(), 0);
}

std::string reply(int s)
{
    std::string strReply;
    int count;
    char buffer[BUFFER_LENGTH+1];
    
    usleep(1000);
    do {
        count = recv(s, buffer, BUFFER_LENGTH, 0);
        buffer[count] = '\0';
        strReply += buffer;
    }while (count ==  BUFFER_LENGTH);
    return strReply;
}

std::string requestReply(int s, std::string message)
{
	if (request(s, message) > 0)
    {
    	return reply(s);
	}
	return "";
}


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
    
    
    strReply = requestReply(sockpi, "USER Anonymous\r\n");
//    std::cout << "This is the strReply: " << strReply << std::endl;
    
    //  TODO parse srtReply to obtain the status.
    //  Let the system act according to the status and display
    //  friendly message to the user
    //  You can see the ouput using std::cout << strReply  << std::endl;
    
    //  Returns 500 if login not ok
    if (strReply.find("331") == std::string::npos) {
        std::cout << "Error login not ok. Message: " << strReply << std::endl;
        return -1;
    }
    //  Returns 331 if login ok (search string for 331)
    else{
        std::cout << "Login ok. Message: " << strReply << std::endl;
    }
    
    strReply = requestReply(sockpi, "PASS asa@asas.com\r\n");
        
    //TODO implement PASV, LIST, RETR.
    // Hint: implement a function that set the SP in passive mode and accept commands.
    return 0;
}
