package comp3949.bcit.a3;

import java.io.OutputStream;

public class HttpServletResponse {
    private final OutputStream outputStream;
    public HttpServletResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
    }
    public OutputStream getOutputStream() { return outputStream; }
}
