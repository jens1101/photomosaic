package io.github.jens1101;

import java.awt.Color;
import java.awt.Rectangle;
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
    private static final int MIN_TILES_PER_SIDE = 20;

    /**
     * Standard daylight reference values
     */
    private static final float[] XYZ_REFERENCE = {94.811f, 100.000f, 107.304f};

    public static void main(String[] args) {
        // If anything other than 3 arguments is given then print the usage of
        // this class.
        if (args.length != 3) {
            System.out.println("Usage: PhotoMosaic [SOURCE IMAGE PATH] [IMAGE LIBRARY DIRECTORY] [DESTINATION]");
            System.exit(1);
        }

        File sourceImageFile = new File(args[0]);
        File imageLibraryDirectory = new File(args[1]);

        // Print an error if the source image doesn't exist
        if (!sourceImageFile.exists()) {
            System.err.println("Source image '" + sourceImageFile.getPath()
                    + "' not found");
            System.exit(2);
        }

        // Print an error if the image library directory doesn't exist.
        if (!imageLibraryDirectory.exists()) {
            System.err.println("Image library directory '"
                    + imageLibraryDirectory.getPath() + "' not found");
            System.exit(3);
        }

        // Walk through all files within the image library directory
        try (Stream<Path> paths = Files.walk(imageLibraryDirectory.toPath())) {
            // Interpret the source image file as an image
            BufferedImage sourceImage = ImageIO.read(sourceImageFile);

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

            int tileSideLength = Math.min(sourceImage.getWidth(), sourceImage.getHeight())
                    / MIN_TILES_PER_SIDE;

            int numberOfTiles = (sourceImage.getWidth() / tileSideLength) *
                    (sourceImage.getHeight() / tileSideLength);

            // Print an error and quit if we don't have enough images for the
            // mosaic.
            if (allLibraryImages.size() < numberOfTiles) {
                System.out.println("Not enough images found in the image " +
                        "library to create the mosaic.\n" +
                        numberOfTiles + " images required, but only " +
                        allLibraryImages.size() + " images found");
                return;
            }

            for (int x = 0; x + tileSideLength < sourceImage.getWidth(); x += tileSideLength) {
                for (int y = 0; y + tileSideLength < sourceImage.getHeight(); y += tileSideLength) {
                    Rectangle region = new Rectangle(x, y, tileSideLength,
                            tileSideLength);

                    float[] averageRgbColour = LibraryImage
                            .averageColour(sourceImage, region)
                            .getRGBColorComponents(null);

                    float[] averageLabColour = ColorSpaceConverter
                            .RGBtoCIELab(averageRgbColour, XYZ_REFERENCE);

                    // TODO: for each tile find the closest matching image and remove the
                    //  image from the pool so that images are not re-used.
                    // TODO: add the matching image to the final mosaic
                    // TODO: write the mosaic to the destination file
                }
            }
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
            Rectangle region = new Rectangle(image.getWidth(), image.getHeight());
            return averageColour(image, region);
        }

        /**
         * Returns the average colour for the given region within the given
         * buffered image.
         *
         * @param image  The source image to analyse.
         * @param region The region within the image to analyse for average
         *               colour.
         * @return The average colour of the region within the image.
         */
        public static Color averageColour(BufferedImage image, Rectangle region) {
            long sumRed = 0;
            long sumGreen = 0;
            long sumBlue = 0;

            int startX = (int) region.getX();
            int endX = (int) (region.getX() + region.getWidth());

            int startY = (int) region.getY();
            int endY = (int) (region.getY() + region.getHeight());

            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    Color pixel = new Color(image.getRGB(x, y));
                    sumRed += pixel.getRed();
                    sumGreen += pixel.getGreen();
                    sumBlue += pixel.getBlue();
                }
            }

            double resolution = region.getWidth() * region.getHeight();
            // TODO: it would be better to use a running average calculation.
            //  The current algorithm can suffer from overflow.
            // TODO: it would be better to only use floats, instead of casting
            //  to int.

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
