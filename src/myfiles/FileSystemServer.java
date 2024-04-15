package myfiles;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class FileSystemServer {
    private static final int PORT = 6789;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("LIST")) {
                        listFiles(out, inputLine.substring(5));
                    } else if (inputLine.startsWith("READ")) {
                        readFile(out, inputLine.substring(5));
                    } else if (inputLine.startsWith("WRITE")) {
                        writeFile(in, out, inputLine.substring(6));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void listFiles(PrintWriter out, String path) {
            try {
                Files.list(Paths.get(path))
                        .map(Path::toString)
                        .forEach(out::println);
                out.println("END");
            } catch (IOException e) {
                out.println("ERROR: Unable to list directory");
            }
        }

        private void readFile(PrintWriter out, String path) {
            try {
                Files.lines(Paths.get(path)).forEach(out::println);
                out.println("END");
            } catch (IOException e) {
                out.println("ERROR: Unable to read file");
            }
        }

        private void writeFile(BufferedReader in, PrintWriter out, String path) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path), StandardOpenOption.CREATE)) {
                String line;
                while (!(line = in.readLine()).equals("END")) {
                    writer.write(line);
                    writer.newLine();
                }
                out.println("WRITE COMPLETE");
                out.println("END");
            } catch (IOException e) {
                out.println("ERROR: Unable to write file");
            }
        }
    }
}
