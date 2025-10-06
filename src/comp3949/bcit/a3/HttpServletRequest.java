package comp3949.bcit.a3;

import java.io.InputStream;
import java.util.Collections;
import java.util.Map;

public class HttpServletRequest {
    private final InputStream inputStream;
    private final String method;
    private final String path;
    private final Map<String,String> headers;
    private final int contentLength;
    private final String contentType;

    public HttpServletRequest(InputStream inputStream,
                              String method,
                              String path,
                              Map<String,String> headers,
                              int contentLength,
                              String contentType) {
        this.inputStream = inputStream;
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.contentLength = contentLength;
        this.contentType = contentType;
    }

    public InputStream getInputStream() { return inputStream; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public Map<String,String> getHeaders() { return Collections.unmodifiableMap(headers); }
    public int getContentLength() { return contentLength; }
    public String getContentType() { return contentType; }
}

