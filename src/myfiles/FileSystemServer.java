package myfiles;
import java.lang.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class FileSystemServer {
    private static final int PORT = 6789;
    private static final String HOST = "0.0.0.0";

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT, 50, InetAddress.getByName(HOST))) {
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
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.startsWith("LIST")) {
                        listFiles(out, inputLine.substring(5));
                    } else if (inputLine.startsWith("READ")) {
                        readFile(out, inputLine.substring(5));

                    } else if (inputLine.startsWith("WRITE")) {
//                        out.println("Ready to recieve input string:");
                        writeFile(in, out, inputLine.substring(6));
                    } else if (inputLine.startsWith("CREATE_DIR")) {
                        createDirectory(out, inputLine.substring(11));
                    } else if (inputLine.startsWith("COPY")) {
                        copyFile(out, inputLine.substring(5));
                    } else if (inputLine.startsWith("MOVE")) {
                        moveFile(out, inputLine.substring(5));
                    } else {
                        out.println("ERROR: Invalid command.");
                        out.println("END");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Lists all files and directories within the specified path and sends the listing to the client.
         * This method first checks if the provided path exists and is a directory before attempting to list its contents.
         * If the path does not exist or is not a directory, appropriate error messages are sent to the client.
         * If an IOException occurs during directory listing, it is caught and handled, sending an error message to the client.
         *
         * @param out  The PrintWriter object used to send responses to the client.
         * @param path The directory path whose contents are to be listed.
         */
        private void listFiles(PrintWriter out, String path) {
            // Convert the string path to a Path object
            Path directoryPath = Paths.get(path);

            // Check if the path exists
            if (!Files.exists(directoryPath)) {
                out.println("ERROR: Path does not exist");
                out.println("END");
                return;
            }

            // Check if the path is a directory
            if (!Files.isDirectory(directoryPath)) {
                out.println("ERROR: Path is not a directory");
                out.println("END");
                return;
            }
            try {
                // Attempt to list the contents of the directory
                Files.list(Paths.get(path)).map(Path::toString).forEach(out::println); // Stream each path as a string to the client
                out.println("END");
            } catch (IOException e) {
                // Handle potential I/O errors by sending an error message
                out.println("ERROR: Unable to list directory");
                e.printStackTrace();
                out.println("END");
            }
        }

        /**
         * Reads the contents of a specified file and sends the contents to the client.
         * This method first checks if the provided path exists and if it is a regular file.
         * It then attempts to read each line of the file and sends these lines to the client.
         *
         * @param out  the PrintWriter object used to send responses to the client. This is
         *             typically connected to the client's input stream.
         * @param path the path to the file that needs to be read. This should be a valid,
         *             accessible file path on the server's filesystem.
         **/
        private void readFile(PrintWriter out, String path) {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                out.println("ERROR: Path or file does not exist");
                out.println("END");
                return;
            }

            if (!Files.isRegularFile(filePath)) {
                out.println("ERROR: Path is not a file");
                out.println("END");
                return;
            }

            try {
                Files.lines(Paths.get(path)).forEach(out::println);
                out.println("END");
            } catch (IOException e) {
                out.println("ERROR: Unable to read file");
                e.printStackTrace();
                out.println("END");
            }
        }

        /**
         * Writes data received from a client to a file at the specified path. If the file exists,
         * it appends the data to the end of the file. If the file does not exist, it creates the file
         * and then writes the data. The method reads lines from the client until it receives an "END"
         * line, signaling the end of the data transmission.
         *
         * @param in   the BufferedReader to read data from the client
         * @param out  the PrintWriter to send responses back to the client
         * @param path the file system path where the data should be written
         */
        private void writeFile(BufferedReader in, PrintWriter out, String path) {
            OpenOption[] options = {StandardOpenOption.CREATE, StandardOpenOption.APPEND};
            Path filePath = Paths.get(path);

            try (BufferedWriter writer = Files.newBufferedWriter(filePath, options)) {
                String line;
                while ((line = in.readLine()) != null && !line.equals("END")) {
                    writer.write(line);
                    writer.newLine();
                }

                out.println("WRITE COMPLETE");
            } catch (IOException e) {
                out.println("ERROR: Unable to write file");
                e.printStackTrace();
            } finally {
                out.println("END");
            }
        }



        private void createDirectory(PrintWriter out, String path) {
            Path directoryPath = Paths.get(path);
            if (Files.exists(directoryPath)) {
                // Check if the path is indeed a directory and not a file.
                if (Files.isDirectory(directoryPath)) {
                    out.println("DIRECTORY ALREADY EXISTS");
                } else {
                    out.println("ERROR: Path exists but is not a directory");
                }
            } else {
                try {
                    Files.createDirectories(directoryPath);
                    out.println("DIRECTORY CREATED");
                } catch (IOException e) {
                    out.println("ERROR: Unable to create directory");
                    e.printStackTrace();
                }
            }
            out.println("END");
        }

        private void copyFile(PrintWriter out, String data) {
            try {
                String[] paths = data.split(" ");
                if (paths.length != 2) {
                    out.println("ERROR: Invalid arguments for copy command. Missing source or target file");
                    out.println("END");
                    return;
                }
                Path source = Paths.get(paths[0]);
                Path target = Paths.get(paths[1]);
                if (!Files.exists(source)) {
                    out.println("ERROR: Source file does not exist");
                    out.println("END");
                    return;
                }
                if (Files.isDirectory(source)) {
                    out.println("ERROR: Source path is a directory, not a file");
                    out.println("END");
                    return;
                }
                Path targetDirectory = target.getParent();
                if (targetDirectory != null && !Files.exists(targetDirectory)) {
                    out.println("ERROR: Destination directory does not exist");
                    out.println("END");
                    return;
                }
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                out.println("FILE COPIED");
            } catch (IOException e) {
                out.println("ERROR: Unable to copy file");
            }
            out.println("END");
        }

        /**
         * Moves a file from a source directory to a destination directory.
         * The paths for the source and destination are provided as a single string, separated by a space.
         * This method prints output messages to a PrintWriter object to indicate the success or failure of the operation.
         *
         * @param out  the PrintWriter object used to output messages to the user or log.
         * @param data the string containing the source and destination paths separated by a space.
         */
        private void moveFile(PrintWriter out, String data) {
            String[] paths = data.split(" ");
            // Check if exactly two paths are provided; otherwise, print an error and end the method.
            if (paths.length != 2) {
                out.println("ERROR: Invalid move command");
                out.println("END");
                return;
            }

            Path sourcePath = Paths.get(paths[0]);
            Path destinationPath = Paths.get(paths[1]);

            // Check if the source file exists
            if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
                out.println("ERROR: Source file does not exist or is not a file");
                out.println("END");
                return;
            }

            // Extract the file name from the source path.
            Path srcFileName = sourcePath.getFileName();

            // Ensure the destination directory exists
            if (!Files.exists(destinationPath)) {
                out.println("ERROR: Destination directory does not exist");
                out.println("END");
                return;
            }

            try {
                // Move the file with REPLACE_EXISTING option to overwrite if destination exists
                Files.move(sourcePath, destinationPath.resolve(srcFileName), StandardCopyOption.REPLACE_EXISTING);
                out.println("FILE MOVED");
            } catch (IOException e) {
                out.println("ERROR: Unable to move file");
                e.printStackTrace();
            }
            // Print a termination message to indicate the end of the operation.
            out.println("END");
        }
    }
}
