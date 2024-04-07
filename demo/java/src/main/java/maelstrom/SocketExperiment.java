package maelstrom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicBoolean;

public class SocketExperiment {
  private final int port;
  private final BlockingQueue<String> dequeue;

  public SocketExperiment(int port) {
    this.port = port;
    this.dequeue = new ArrayBlockingQueue<>(100);
  }

  public void start() throws IOException {
    try (ServerSocket serverSocket = new ServerSocket(port)) {
      System.out.println(STR."Server started on port \{port}");
      final var running = new AtomicBoolean(true);
      while (running.get()) {
        Socket socket = serverSocket.accept();
        System.out.println("New client connected");
        try (
          final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          final PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {

          try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            scope.fork(() -> {
              String line;
              try {
                while ((line = reader.readLine()) != null && running.get()) {
                  System.out.println(STR."Read: \{line}");
                  dequeue.add(line);
                }
              } catch (IOException e) {
                System.err.println(STR."Error reading from client: \{e.getMessage()}");
              }
              return true;
            });

            scope.fork(() -> {
              try {
                while (running.get()) {
                  // this is deliberately a blocking call on a virtual thread.
                  String line = dequeue.take();
                  //noinspection ConstantValue
                  if (line == null) {
                    continue;
                  }
                  if (line.equals("\\q")) {
                    running.set(false);
                  } else {
                    writer.println(STR."echo:\{line}");
                  }
                }
              } catch (Exception e) {
                System.err.println(STR."Error writing to client: \{e.getMessage()}");
              }
              return true;
            });

            scope.join()            // Join both subtasks
              .throwIfFailed();  // ... and propagate errors

          } catch (Exception e) {
            System.err.println(STR."Error handling client: \{e.getMessage()}");
          }

        } catch (IOException e) {
          System.err.println(STR."Error handling client: \{e.getMessage()}");
        }
      }
    }
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    int port = 10101;
    SocketExperiment server = new SocketExperiment(port);
    server.start();
  }
}
