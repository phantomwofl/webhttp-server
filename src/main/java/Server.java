import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    final List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");


    int defaultPort = 9999;
    int defaultNumberOfThreads = 64;
    int port;
    int numberOfThreads;

    Server(int port, int numberOfThreads) {
        this.port = port;
        this.numberOfThreads = numberOfThreads;
    }

    Server() {
        this.port = defaultPort;
        this.numberOfThreads = defaultNumberOfThreads;
    }

    public void start() throws IOException {

        final var serverSocket = new ServerSocket(port);
        ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);

        try {
            while (true) {

                final var socket = serverSocket.accept();
                pool.submit(() -> handle(socket));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectionProcessing(String path, BufferedOutputStream out) throws IOException {


        if (!validPaths.contains(path)) {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
            return;
        }

        final var filePath = Path.of(".", "public", path);
        final var mimeType = Files.probeContentType(filePath);

        if (path.equals("/classic.html")) {
            final var template = Files.readString(filePath);
            final var content = template.replace(
                    "{time}",
                    LocalDateTime.now().toString()
            ).getBytes();
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.write(content);
            out.flush();
            return;
        }

        final var length = Files.size(filePath);
        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mimeType + "\r\n" +
                        "Content-Length: " + length + "\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private void handle(Socket socket) {

        try {
            final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            final var out = new BufferedOutputStream(socket.getOutputStream());

            final var requestLine = in.readLine();
            final var parts = requestLine.split(" ");

            if (parts.length != 3) {
                return;
            }

            final var path = parts[1];
            connectionProcessing(path, out);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
