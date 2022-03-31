import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    ConcurrentHashMap<String, ConcurrentHashMap<String, Handler>> handlers = new ConcurrentHashMap<>();

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

    private void handle(Socket socket) {
        while (true) {
            try {
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream());

                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    continue;
                }

                var method = parts[0];
                var path = parts[1].split("\\?")[0];
// path.split("?")[0]

                Request request = new Request(method, path);


                if (handlers.containsKey(method) && handlers.get(method).containsKey(path)) {
                    handlers.get(method).get(path).handle(request, out);
                } else {
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Length: 0\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addHandler(String method, String endPoint, Handler handler) {
        var methodMap = handlers.get(method);

        if (methodMap == null) {
            methodMap = new ConcurrentHashMap<>();
            handlers.put(method, methodMap);
        }

        methodMap.put(endPoint, handler);
    }
}