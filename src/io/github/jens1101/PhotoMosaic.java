package io.github.jens1101;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class PhotoMosaic {
    public static void main(String[] args) throws IOException {
        // If anything other than 3 arguments is given then print the usage of
        // this class.
        if (args.length != 3) {
            System.out.println("Usage: PhotoMosaic [SOURCE IMAGE PATH] [IMAGE LIBRARY DIRECTORY] [DESTINATION]");
            System.exit(1);
        }

        File sourceImage = new File(args[0]);
        File imageLibraryDirectory = new File(args[1]);

        // Print an error if the source image doesn't exist
        if (!sourceImage.exists()) {
            System.out.println("Source image '" + sourceImage.getCanonicalPath()
                    + "' not found");
            System.exit(2);
        }

        // Print an error if the image library directory doesn't exist.
        if (!imageLibraryDirectory.exists()) {
            System.out.println("Image library directory '"
                    + imageLibraryDirectory.getPath() + "' not found");
            System.exit(3);
        }

        // Walk through all files within the image library directory
        try (Stream<Path> paths = Files.walk(Paths.get(args[1]))) {
            paths.filter(Files::isRegularFile).forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
