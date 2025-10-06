package comp3949.bcit.a3;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

public class HttpOverTcp {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: java HttpOverTcp <host> <port> [<filepath>]");
            System.err.println("Example: java HttpOverTcp localhost 8082 test.png");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String filePathStr = (args.length == 3) ? args[2] : "test.png"; // default

        String caption = "MyPic";
        String date = LocalDate.now().toString();
        String boundary = "----BOUNDARY" + System.currentTimeMillis();
        String charset = "UTF-8";
        String CRLF = "\r\n";

        try {
            Path filePath = Paths.get(filePathStr);
            byte[] fileBytes = Files.readAllBytes(filePath);
            String fileName = filePath.getFileName().toString();

            // Build multipart body
            ByteArrayOutputStream body = new ByteArrayOutputStream();

            body.write(("--" + boundary + CRLF).getBytes(charset));
            body.write(("Content-Disposition: form-data; name=\"caption\"" + CRLF + CRLF).getBytes(charset));
            body.write((caption + CRLF).getBytes(charset));

            body.write(("--" + boundary + CRLF).getBytes(charset));
            body.write(("Content-Disposition: form-data; name=\"date\"" + CRLF + CRLF).getBytes(charset));
            body.write((date + CRLF).getBytes(charset));

            body.write(("--" + boundary + CRLF).getBytes(charset));
            body.write(("Content-Disposition: form-data; name=\"fileName\"; filename=\"" + fileName + "\"" + CRLF).getBytes(charset));
            body.write(("Content-Type: application/octet-stream" + CRLF + CRLF).getBytes(charset));
            body.write(fileBytes);
            body.write(CRLF.getBytes(charset));

            body.write(("--" + boundary + "--" + CRLF).getBytes(charset));
            byte[] bodyBytes = body.toByteArray();

            // Send HTTP request
            try (Socket socket = new Socket(hostName, portNumber)) {
                OutputStream out = socket.getOutputStream();
                InputStream in   = socket.getInputStream();

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, charset));
                pw.print("POST / HTTP/1.1\r\n");
                pw.print("Host: " + hostName + "\r\n");
                pw.print("Connection: close\r\n");
                pw.print("Content-Type: multipart/form-data; boundary=" + boundary + "\r\n");
                pw.print("Content-Length: " + bodyBytes.length + "\r\n\r\n");
                pw.flush();

                out.write(bodyBytes);
                out.flush();

                // Print response
                BufferedReader br = new BufferedReader(new InputStreamReader(in, charset));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}

