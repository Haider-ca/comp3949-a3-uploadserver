package comp3949.bcit.a3;

import java.net.ServerSocket;
import java.io.IOException;

public class UploadServer {
    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(8082)) {
            System.out.println("UploadServer listening on http://localhost:8082/");
            while (true) {
                new UploadServerThread(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port: 8082.");
            throw e;
        }
    }
}

