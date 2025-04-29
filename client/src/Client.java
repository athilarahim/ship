import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class Client {
    private static final int MAX_RESPONSE_SIZE = 50 * 1024 * 1024; // 50MB
    private static Socket serverSocket; //maintains a persistent connection
    private static DataInputStream serverIn; //streams for communication with the server
    private static DataOutputStream serverOut;
    private static final Object lock = new Object(); // Important for sequential writes/reads
    //ensures thread-safe access to shared streams

    public static void main(String[] args) throws IOException {
        // Connect to offshore server once
        String serverHost = System.getenv("SERVER_HOST") != null ?
                System.getenv("SERVER_HOST") : "localhost";

        serverSocket = new Socket(serverHost, 9000);
        serverIn = new DataInputStream(serverSocket.getInputStream());
        serverOut = new DataOutputStream(serverSocket.getOutputStream());

        System.out.println("Local Proxy started at port 8080, connected to offshore server at 9000");

        ExecutorService executor = Executors.newCachedThreadPool();
        try (ServerSocket localProxy = new ServerSocket(8080)) {
            while (true) {
                Socket browserSocket = localProxy.accept();
                executor.submit(() -> handleRequest(browserSocket));
            }
        }
    }

    private static void handleRequest(Socket browserSocket) {
        try (browserSocket;
             InputStream browserIn = browserSocket.getInputStream();
             OutputStream browserOut = browserSocket.getOutputStream()) {

            ByteArrayOutputStream requestBuffer = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read;
            while ((read = browserIn.read(buf)) != -1) {
                requestBuffer.write(buf, 0, read);
                if (browserIn.available() == 0) break; // No more data
            }

            byte[] requestBytes = requestBuffer.toByteArray();
            System.out.println("[CLIENT] Request size: " + requestBytes.length + " bytes");

            synchronized (lock) {
                // Send request to server
                serverOut.writeInt(requestBytes.length);
                serverOut.write(requestBytes);
                serverOut.flush();

                // Read response from server
                int responseLength = serverIn.readInt();
                if (responseLength > MAX_RESPONSE_SIZE) {
                    sendErrorResponse(browserOut, 502, "Response too large");
                    return;
                }

                byte[] responseBytes = new byte[responseLength];
                serverIn.readFully(responseBytes);

                // Send response to browser
                browserOut.write(responseBytes);
                browserOut.flush();
            }

        } catch (IOException e) {
            System.err.println("Request handling error: " + e.getMessage());
        }
    }

    private static void sendErrorResponse(OutputStream out, int statusCode, String message) throws IOException {
        String response = "HTTP/1.1 " + statusCode + " " + message + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "\r\n" + message;
        out.write(response.getBytes());
        out.flush();
    }
}
