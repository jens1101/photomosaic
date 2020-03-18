package io.github.jens1101;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class PhotoMosaic {
    public static void main(String[] args) {
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
            System.out.println("Source image '" + sourceImage.getPath()
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
        try (Stream<Path> paths = Files.walk(imageLibraryDirectory.toPath())) {
            // Get all library images
            List<LibraryImage> allLibraryImages = paths
                    // Only traverse all regular files
                    .filter(Files::isRegularFile)
                    // Map each file to a library image instance
                    .map(path -> {
                        try {
                            return new LibraryImage(path.toFile());
                        } catch (IOException e) {
                            // The file could not be read as an image. Print an
                            // error and continue
                            System.err.println("The file at '" + path +
                                    "' could not be read as an image.");
                            return null;
                        }
                    })
                    // Remove all null values from the stream
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            // An error occurred while reading a file
            e.printStackTrace();
        }
    }

    /**
     * Class used to represent a single image in the image library that's used
     * to create the photo mosaic.
     */
    private static class LibraryImage {
        /**
         * The file reference to the image itself.
         */
        private final File imageFile;

        /**
         * The average colour of the image.
         */
        private final Color averageColour;

        /**
         * @param imageFile The file where the image file is located.
         * @throws IOException When the given file instance cannot be
         *                     interpreted as an image.
         */
        LibraryImage(File imageFile) throws IOException {
            this.imageFile = imageFile;

            BufferedImage img = ImageIO.read(imageFile);
            this.averageColour = LibraryImage.averageColour(img);
        }

        /**
         * Returns the average colour for the given buffered image.
         *
         * @param image The image to analyse.
         * @return The average colour of the image.
         */
        public static Color averageColour(BufferedImage image) {
            long sumRed = 0;
            long sumGreen = 0;
            long sumBlue = 0;
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    Color pixel = new Color(image.getRGB(x, y));
                    sumRed += pixel.getRed();
                    sumGreen += pixel.getGreen();
                    sumBlue += pixel.getBlue();
                }
            }

            long resolution = image.getWidth() * image.getHeight();

            return new Color((int) (sumRed / resolution),
                    (int) (sumGreen / resolution),
                    (int) (sumBlue / resolution));
        }

        public Color getAverageColour() {
            return averageColour;
        }

        public File getImageFile() {
            return imageFile;
        }
    }
}
