package comp3949.bcit.a3;

import java.net.Socket;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UploadServerThread extends Thread {
    private final Socket socket;

    public UploadServerThread(Socket socket) {
        super("UploadServerThread");
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream rawIn = socket.getInputStream();
             OutputStream rawOut = socket.getOutputStream()) {

            // 1) Read start line + headers up to CRLFCRLF
            ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
            int b, matched = 0;
            final byte[] CRLFCRLF = "\r\n\r\n".getBytes("ISO-8859-1");
            while ((b = rawIn.read()) != -1) {
                headerBuf.write(b);
                if (b == CRLFCRLF[matched]) {
                    matched++;
                    if (matched == CRLFCRLF.length) break;
                } else {
                    matched = (b == CRLFCRLF[0]) ? 1 : 0;
                }
            }

            byte[] headerBytes = headerBuf.toByteArray();
            String headerText = new String(headerBytes, "ISO-8859-1");
            String[] lines = headerText.split("\r\n");
            if (lines.length == 0) return;

            String requestLine = lines[0]; // e.g., "GET / HTTP/1.1"
            Map<String,String> headers = new HashMap<>();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                if (line.isEmpty()) break;
                int idx = line.indexOf(':');
                if (idx > 0) {
                    headers.put(line.substring(0, idx).trim().toLowerCase(),
                            line.substring(idx + 1).trim());
                }
            }

            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path   = parts.length > 1 ? parts[1] : "/";

            int contentLength = 0;
            if (headers.containsKey("content-length")) {
                try { contentLength = Integer.parseInt(headers.get("content-length")); } catch (NumberFormatException ignored) {}
            }
            String contentType = headers.getOrDefault("content-type", "");

            // 2) Build request/response objects
            HttpServletRequest req = new HttpServletRequest(rawIn, method, path, headers, contentLength, contentType);

            ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
            HttpServletResponse res = new HttpServletResponse(responseBuffer);

            // 3) Dispatch to servlet
            HttpServlet servlet = new UploadServlet();
            if ("GET".equalsIgnoreCase(method)) {
                servlet.doGet(req, res);
            } else if ("POST".equalsIgnoreCase(method)) {
                servlet.doPost(req, res);
            } else {
                // Minimal 405
                PrintWriter pw = new PrintWriter(res.getOutputStream(), true);
                pw.print("HTTP/1.1 405 Method Not Allowed\r\nConnection: close\r\n\r\n");
            }

            // 4) Write servlet response to client
            rawOut.write(responseBuffer.toByteArray());
            rawOut.flush();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}

