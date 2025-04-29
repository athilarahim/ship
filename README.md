# ship

This project demonstrates a simple HTTP Proxy setup using Java sockets. It consists of two components:

**Client Proxy** (Ship Proxy) – Runs on port 8080, accepts requests from your browser or tools like curl, and forwards them to the server.

**Server Proxy** (Offshore Proxy) – Runs on port 9000, receives requests from the client, fetches the response from the target URL, and returns it to the client.

How it works:

**Initial Setup** -
-Client
1. Establishes a TCP connection to the proxy server(offshore) at PORT 9000
2. Creates DataInputStream and DataOutputStream for communication

-Server
1. Listens on PORT 9000 for incoming client connections
2. It uses thread pool to handle multiple requests

**When Browser requests** -
1. When browser makes a request, it connects to clien't local proxy at PORT 8080
2. Client accepts the request and send to handle the request (handleRequest() function)

**Client processes browser request (inside handleRequest() function)** -
1. Reads the data in 4KB chunks (eg: a 10KB request would take 3 reads (4096 + 4096 + 1808 bytes)) and stores into a growing buffer
2. Checks if all the bytes are received and breaks the loop if no more bytes remaining

**Client forwards request to Server** - 
1. synchronized() prevents concurrent writes to the server (ensures sequential writes)
2. Send the request as 4 byte length prefix and then sends the actual request bytes. (Length prefixes is used to mark where one message ends and the next begins)

**Server receives request** -
1. If the request is larger than the maximum request size allocated, then throws exception.
2. Otherwise, first reads the length prefix
3. Then reads the actual request bytes(complete HTTP request)
4. Server then processes the request(processRequest()) and sends the response back to the client

**How server processes the request(ProcessRequest())** - 
1. From the full request, extracts the first line and then from that it extracts HTTP method and URL.
2. Then sends the target URL to the internet to connect.
3. Then it forwards all headers except the request line (already handled)
   _______________________________________________________
   Eg: curl.exe -x http://localhost:8080 http://httpforever.com/
    Raw HTTP request will be like:
    GET http://httpforever.com/ HTTP/1.1   - this is request line and all the below are header lines
    Host: httpforever.com                  
    User-Agent: curl/8.11.1
    Accept: */*
    Proxy-Connection: Keep-Alive
   _______________________________________________________________
5. Server receives the remote response and writes everything to a response buffer(including response header and body).
6. Returns the response buffer.

**Server sends response to client** - 
1. Server sends back the processed response to client which includes the status line, headers and response body.

**Client forwards the response to browser** -
1. Reads reponse from the server
2. Sends error message to the browser (or curl) if response length exceeds maximum response size.
3. Otherwise sends the reponse to the browser.

