import java.io.*;
import java.nio.file.*;

public class FileUtils {
    /**
     * Writes a string to a file
     * @param file The file to write to
     * @param content The content to write
     * @throws IOException If an I/O error occurs
     */
    public static void writeStringToFile(File file, String content) throws IOException {
        Files.write(file.toPath(), content.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Reads a file to a string
     * @param file The file to read
     * @return The content of the file as a string
     * @throws IOException If an I/O error occurs
     */
    public static String readFileToString(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    /**
     * Checks if a file exists
     * @param filePath The path to the file
     * @return true if the file exists, false otherwise
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}