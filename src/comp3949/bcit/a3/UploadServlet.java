package comp3949.bcit.a3;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UploadServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String html =
                    "<!doctype html>\n" +
                            "<html><head><meta charset=\"UTF-8\"><title>Upload</title></head>\n" +
                            "<body style=\"font-family:sans-serif\">\n" +
                            "<h1>Upload a file</h1>\n" +
                            "<form method=\"POST\" action=\"/\" enctype=\"multipart/form-data\">\n" +
                            "  Caption: <input type=\"text\" name=\"caption\" required><br><br>\n" +
                            "  Date: <input type=\"date\" name=\"date\" required><br><br>\n" +
                            "  <input type=\"file\" name=\"fileName\" required><br><br>\n" +
                            "  <button type=\"submit\">Upload</button>\n" +
                            "</form>\n" +
                            "</body></html>\n";

            byte[] body = html.getBytes("UTF-8");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
            pw.print("HTTP/1.1 200 OK\r\n");
            pw.print("Content-Type: text/html; charset=UTF-8\r\n");
            pw.print("Content-Length: " + body.length + "\r\n");
            pw.print("Connection: close\r\n\r\n");
            pw.flush();
            response.getOutputStream().write(body);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            // Validate content-type
            String ct = request.getContentType();
            if (ct == null || !ct.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
                send400(response, "Content-Type must be multipart/form-data");
                return;
            }
            // Extract boundary
            String boundary = extractBoundary(ct);
            if (boundary == null) {
                send400(response, "Missing multipart boundary");
                return;
            }

            // Read EXACTLY content-length bytes into memory
            int len = request.getContentLength();
            if (len <= 0) {
                send400(response, "Missing or invalid Content-Length");
                return;
            }
            byte[] body = readNBytes(request.getInputStream(), len);

            // ---- Minimal multipart parsing (binary-safe) ----
            Map<String, Part> parts = parseMultipart(body, boundary);

            String caption = asText(parts.get("caption"));
            String date    = asText(parts.get("date"));
            Part   fileP   = parts.get("fileName");
            if (fileP == null || fileP.filename == null) {
                send400(response, "Missing file part (fileName)");
                return;
            }

            // Save under ./images/<caption>_<date>_<originalName>
            Path dir = Paths.get("images");
            Files.createDirectories(dir);

            String safeCaption = caption.replaceAll("[^a-zA-Z0-9_-]", "_");
            String safeDate    = date.replaceAll("[^0-9-]", "_");
            String original    = Paths.get(fileP.filename).getFileName().toString();
            String saveName    = safeCaption + "_" + safeDate + "_" + original;

            Files.write(dir.resolve(saveName), fileP.content); // binary-safe

            // Build alphabetical HTML listing
            String listing = buildListing(dir);
            byte[] resp = listing.getBytes("UTF-8");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
            pw.print("HTTP/1.1 200 OK\r\n");
            pw.print("Content-Type: text/html; charset=UTF-8\r\n");
            pw.print("Content-Length: " + resp.length + "\r\n");
            pw.print("Connection: close\r\n\r\n");
            pw.flush();
            response.getOutputStream().write(resp);

        } catch (Exception ex) {
            ex.printStackTrace();
            try { send400(response, "Server error: " + ex.getMessage()); } catch (IOException ignored) {}
        }
    }

    // ---------- helpers ----------

    private static String extractBoundary(String ct) {
        int i = ct.toLowerCase(Locale.ROOT).indexOf("boundary=");
        if (i < 0) return null;
        String b = ct.substring(i + 9).trim();
        if (b.startsWith("\"") && b.endsWith("\"")) b = b.substring(1, b.length() - 1);
        return b;
    }

    private static byte[] readNBytes(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r == -1) break;
            off += r;
        }
        if (off < n) {
            byte[] exact = new byte[off];
            System.arraycopy(buf, 0, exact, 0, off);
            return exact;
        }
        return buf;
    }

    // Minimal Part model for this servlet
    private static class Part {
        final String name;
        final String filename; // null for text fields
        final byte[] content;
        Part(String n, String f, byte[] c) { name = n; filename = f; content = c; }
    }

    // Binary-safe multipart parser (sufficient for this assignment)
    private static Map<String, Part> parseMultipart(byte[] body, String boundary) throws IOException {
        byte[] sep = ("--" + boundary).getBytes("ISO-8859-1");
        byte[] end = ("--" + boundary + "--").getBytes("ISO-8859-1");
        Map<String, Part> map = new HashMap<>();

        int pos = indexOf(body, sep, 0);
        if (pos < 0) throw new IOException("Opening boundary not found");
        pos += sep.length;
        pos = skipCRLF(body, pos);

        while (true) {
            // find next boundary (normal or closing)
            int next = indexOf(body, sep, pos);
            int nextEnd = indexOf(body, end, pos);
            boolean isFinal = nextEnd >= 0 && (next < 0 || nextEnd < next);
            int boundaryPos = isFinal ? nextEnd : next;
            if (boundaryPos < 0) break;

            // headers end at CRLFCRLF
            int hdrEnd = indexOf(body, "\r\n\r\n".getBytes("ISO-8859-1"), pos);
            if (hdrEnd < 0 || hdrEnd > boundaryPos) throw new IOException("Headers not found");
            String headers = new String(body, pos, hdrEnd - pos, "ISO-8859-1");
            int contentStart = hdrEnd + 4;
            int contentEnd = boundaryPos;
            // trim last CRLF
            if (contentEnd - contentStart >= 2 && body[contentEnd - 2] == '\r' && body[contentEnd - 1] == '\n') {
                contentEnd -= 2;
            }

            String name = null, filename = null;
            for (String h : headers.split("\r\n")) {
                String lower = h.toLowerCase(Locale.ROOT);
                if (lower.startsWith("content-disposition:")) {
                    for (String token : h.split(";")) {
                        token = token.trim();
                        if (token.startsWith("name=")) {
                            name = unquote(token.substring(5));
                        } else if (token.startsWith("filename=")) {
                            filename = unquote(token.substring(9));
                        }
                    }
                }
            }
            if (name != null) {
                byte[] content = Arrays.copyOfRange(body, contentStart, contentEnd);
                map.put(name, new Part(name, filename, content));
            }

            pos = boundaryPos + (isFinal ? end.length : sep.length);
            pos = skipCRLF(body, pos);
            if (isFinal) break;
        }
        return map;
    }

    private static int indexOf(byte[] hay, byte[] needle, int from) {
        outer:
        for (int i = from; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int skipCRLF(byte[] arr, int i) {
        if (i + 1 < arr.length && arr[i] == '\r' && arr[i+1] == '\n') return i + 2;
        return i;
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length() - 1);
        return s;
    }

    private static String asText(Part p) throws UnsupportedEncodingException {
        return p == null ? "" : new String(p.content, "UTF-8");
    }

    private static void send400(HttpServletResponse response, String msg) throws IOException {
        byte[] b = msg.getBytes("UTF-8");
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), "UTF-8"));
        pw.print("HTTP/1.1 400 Bad Request\r\n");
        pw.print("Content-Type: text/plain; charset=UTF-8\r\n");
        pw.print("Content-Length: " + b.length + "\r\n");
        pw.print("Connection: close\r\n\r\n");
        pw.flush();
        response.getOutputStream().write(b);
    }

    private static String escape(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String buildListing(Path dir) throws IOException {
        if (!Files.exists(dir)) return "<html><body><p>No images folder.</p></body></html>";
        java.util.List<String> names = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) if (Files.isRegularFile(p)) names.add(p.getFileName().toString());
        }
        Collections.sort(names, String::compareToIgnoreCase);

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html>\n")
                .append("<html><head><meta charset=\"UTF-8\"><title>Uploads</title></head>\n")
                .append("<body style=\"font-family:sans-serif\">\n")
                .append("<h1>Uploaded files</h1>\n<ul>\n");
        for (String n : names) sb.append("<li>").append(escape(n)).append("</li>\n");
        sb.append("</ul>\n<p><a href=\"/\">Upload another file</a></p>\n</body></html>\n");
        return sb.toString();
    }
}

