import java.io.InputStream;

public class Request {
    String method;
    String header;
    InputStream body;

    Request (String method, String header, InputStream body) {
        this.method = method;
        this.header = header;
        this.body = body;
    }

    Request (String method, String header) {
        this.method = method;
        this.header = header;
    }

}
