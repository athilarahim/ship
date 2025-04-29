import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int MAX_REQUEST_SIZE = 50 * 1024 * 1024; // 20MB

    public static void main(String[] args) throws IOException {
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(9000)) {
            System.out.println("Proxy Server started on port 9000");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(() -> handleClient(clientSocket));
            }
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (clientSocket;
             DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dataOut = new DataOutputStream(clientSocket.getOutputStream())) {

            System.out.println("Client connected: " + clientSocket.getInetAddress());

            while (!clientSocket.isClosed()) {
                try {
                    // Read request length
                    int requestLength = dataIn.readInt();
                    System.out.println("[SERVER] Received request size: " + requestLength + " bytes");
                    if (requestLength > MAX_REQUEST_SIZE) {
                        throw new IOException("Request too large");
                    }

                    // Read request data
                    byte[] requestBytes = new byte[requestLength];
                    dataIn.readFully(requestBytes);
                    String request = new String(requestBytes);
                    System.out.println("Received request from " + clientSocket.getInetAddress());

                    // Process request and get response
                    byte[] responseBytes = processRequest(request);

                    // Send response
                    dataOut.writeInt(responseBytes.length);
                    dataOut.write(responseBytes);
                    dataOut.flush();

                } catch (EOFException e) {
                    System.out.println("Client disconnected: " + clientSocket.getInetAddress());
                    break;
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
    }

    private static byte[] processRequest(String request) throws IOException {
        try {
            // Parse URL from the request
            String firstLine = request.split("\r\n")[0];
            String[] parts = firstLine.split(" ");
            String method = parts[0];
            String urlStr = parts[1];

            URL url = new URL(urlStr);

            // Forward the request
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            // Copy headers (skip request line and empty lines)
            for (String line : request.split("\r\n")) {
                if (line.isEmpty() || line.startsWith(method)) continue;
                int idx = line.indexOf(":");
                if (idx > 0) {
                    String header = line.substring(0, idx);
                    String value = line.substring(idx + 1).trim();
                    conn.setRequestProperty(header, value);
                }
            }

            // Get response
            int responseCode = conn.getResponseCode();
            InputStream responseStream = (responseCode >= 400) ? conn.getErrorStream() : conn.getInputStream();

            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

            // Write status line
            responseBuffer.write(("HTTP/1.1 " + responseCode + " " + conn.getResponseMessage() + "\r\n").getBytes());

            // Write headers
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null) {
                    try {
                        for (String value : values) {
                            responseBuffer.write((key + ": " + value + "\r\n").getBytes());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            // End of headers
            responseBuffer.write("\r\n".getBytes());

            // Write body if exists
            if (responseStream != null) {
                byte[] buf = new byte[4096];
                int len;
                while ((len = responseStream.read(buf)) != -1) {
                    responseBuffer.write(buf, 0, len);
                }
                responseStream.close();
            }

            return responseBuffer.toByteArray();

        } catch (Exception e) {
            // Return error response if something goes wrong
            String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Connection: close\r\n" +
                    "\r\n" +
                    "Error processing request: " + e.getMessage();
            return errorResponse.getBytes();
        }
    }
}