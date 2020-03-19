package io.github.jens1101;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

public class PhotoMosaic {
    /**
     * Array of all usable images within the image library folder.
     */
    private final LibraryImage[] allLibraryImages;

    /**
     * The XYZ reference illumination to use for the mosaic. This is used to
     * determine the correct CIE L*ab values when creating the mosaic.
     *
     * @see <a href="http://www.easyrgb.com/en/math.php">Details about colour
     * spaces and reference illumination values</a>
     */
    private final double[] xyzReference;

    /**
     * @param imageLibraryDirectory The directory which contains all library
     *                              images that should be used for the mosaic.
     *                              This may contain nested directories.
     * @param xyzReference          The reference illumination to be used when
     *                              creating a mosaic.
     * @throws IOException When the image library directory cannot be traversed.
     */
    public PhotoMosaic(File imageLibraryDirectory, double[] xyzReference) throws IOException {
        if (!imageLibraryDirectory.exists()) {
            throw new RuntimeException("Image library directory '"
                    + imageLibraryDirectory.getPath() + "' not found");
        }

        this.xyzReference = xyzReference;

        try (Stream<Path> paths = Files.walk(imageLibraryDirectory.toPath())) {
            // Get all library images
            this.allLibraryImages = paths
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
                    // Finally convert the stream to a list
                    .toArray(LibraryImage[]::new);
        }
    }

    /**
     * Finds the library image that closest matches the given colour.
     *
     * @param averageColour    The colour for which to search.
     * @param xyzReference     The reference illumination to use to convert the
     *                         average colours to CIE L*ab which is in turn used
     *                         to calculate colour differences.
     * @param allLibraryImages A list of all library images to search through
     *                         to find the one most similar to the specified
     *                         average colour.
     * @return The library image with the most similar average colour to the
     * specified average colour.
     */
    private static LibraryImage findClosestLibraryImage(Color averageColour,
                                                        double[] xyzReference,
                                                        List<LibraryImage> allLibraryImages) {
        double[] averageLabColour = ColorSpaceConverter
                .colorToCIELab(averageColour, xyzReference);

        LibraryImage closestImage = null;
        double closestDeltaE = 0;

        for (LibraryImage currentImage : allLibraryImages) {
            double[] currentImageAverageLabColour = ColorSpaceConverter
                    .colorToCIELab(currentImage.getAverageColour(), xyzReference);

            double currentDeltaE = ColorDifference.calculateDeltaE2000(
                    averageLabColour, currentImageAverageLabColour);

            if (closestImage == null || currentDeltaE < closestDeltaE) {
                closestImage = currentImage;
                closestDeltaE = currentDeltaE;
            }
        }

        return closestImage;
    }

    /**
     * Creates a mosaic for the given source image using the image library
     * that was created during construction.
     * <p>
     * Each tile in the mosaic is a square.
     * <p>
     * The resolution of the final mosaic will not necessarily match the
     * resolution of the source image.
     * <p>
     * Each image in the mosaic is unique. A runtime exception will be thrown
     * when there are not enough images in the source image library.
     *
     * @param sourceImageFile The source image for which to create a mosaic.
     * @param outputFile      The file where the final mosaic will be saved. The
     *                        file will be overwritten if it already exists.
     * @param minTilesPerSide The number of sections into which the shortest
     *                        side of the image will be divided into. The longer
     *                        side will be divided into a proportional number of
     *                        sections so that the mosaic tiles will always be
     *                        square.
     * @throws IOException When the source image file could not be read.
     * @throws IOException When the destination image file could not be written
     *                     to.
     */
    public void createMosaic(File sourceImageFile,
                             File outputFile,
                             int minTilesPerSide) throws IOException {
        // Throw a runtime exception if the source image doesn't exist.
        if (!sourceImageFile.exists()) {
            throw new RuntimeException("Source image '" +
                    sourceImageFile.getPath() + "' not found");
        }

        String outputFileName = outputFile.getName();

        // Throw a runtime exception when the output image file doesn't have
        // an extension.
        if (!outputFileName.contains(".")) {
            throw new RuntimeException("Output image file name must contain " +
                    "an extension");
        }

        String extension = outputFileName
                .substring(outputFileName.lastIndexOf(".") + 1);

        boolean isValidExtension = Arrays
                .asList(ImageIO.getReaderFileSuffixes())
                .contains(extension);

        // Throw a runtime exception when the output image file has an
        // unsupported extension.
        if (!isValidExtension) {
            throw new RuntimeException("Unsupported extension '" +
                    extension + "'");
        }

        // Copy the array of library images into a list. We do this so that we
        // can remove images from the list once they are used in the mosaic.
        // This is done to ensure that no images are used twice.
        List<LibraryImage> allLibraryImages = Arrays
                .stream(this.allLibraryImages)
                .collect(Collectors.toList());

        // Interpret the source image file as an image
        BufferedImage sourceImage = ImageIO.read(sourceImageFile);

        int tileSideLength = Math.min(sourceImage.getWidth(), sourceImage.getHeight())
                / minTilesPerSide;

        int numberOfTilesWide = sourceImage.getWidth() / tileSideLength;
        int numberOfTilesHigh = sourceImage.getHeight() / tileSideLength;
        int totalNumberOfTiles = numberOfTilesHigh * numberOfTilesWide;

        // Print an error and quit if we don't have enough images for the
        // mosaic.
        if (allLibraryImages.size() < totalNumberOfTiles) {
            throw new RuntimeException("Not enough images found in the image " +
                    "library to create the mosaic." + totalNumberOfTiles +
                    " images required, but only " + allLibraryImages.size() +
                    " images found");
        }

        // Create the output image buffer where the mosaic will be written to.
        BufferedImage outputImage = new BufferedImage(numberOfTilesWide * tileSideLength,
                numberOfTilesHigh * tileSideLength,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D outputImageGraphics = outputImage.createGraphics();

        // Iterate through the source image tile by tile.
        for (int x = 0; x + tileSideLength < sourceImage.getWidth(); x += tileSideLength) {
            for (int y = 0; y + tileSideLength < sourceImage.getHeight(); y += tileSideLength) {
                Rectangle region = new Rectangle(x, y, tileSideLength,
                        tileSideLength);

                // Find the average colour of the current tile
                Color averageColor = LibraryImage
                        .averageColour(sourceImage, region);

                // From the available pool of library images find the image
                // that closest matches the current tile's average colour.
                LibraryImage closestImage = findClosestLibraryImage(
                        averageColor, xyzReference, allLibraryImages);

                // Remove the current closest image from the list of library
                // images. This is done to prevent duplication.
                allLibraryImages.remove(closestImage);

                // The length of the shortest side of the current closest image.
                // This is used below to crop the image to a square when
                // creating the mosaic.
                int closestImageSideLength = Math.min(closestImage.getImage().getWidth(), closestImage.getImage().getHeight());

                // Add the current closest image to the mosaic.
                outputImageGraphics.drawImage(closestImage.getImage(),
                        x, y, x + tileSideLength, y + tileSideLength,
                        0, 0, closestImageSideLength - 1, closestImageSideLength - 1,
                        null);
            }
        }

        // Finally save the image to file.
        ImageIO.write(outputImage, extension, outputFile);
    }

    /**
     * Class used to represent a single image in the image library that's used
     * to create the photo mosaic.
     */
    private static class LibraryImage {
        /**
         * The image object of this library image.
         */
        private final BufferedImage image;

        /**
         * The average colour of the image.
         */
        private Color averageColour = null;

        /**
         * @param imageFile The file where the image file is located.
         * @throws IOException When the given file instance cannot be
         *                     interpreted as an image.
         */
        LibraryImage(File imageFile) throws IOException {
            this.image = ImageIO.read(imageFile);
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
            IntSummaryStatistics redStats = new IntSummaryStatistics();
            IntSummaryStatistics greenStats = new IntSummaryStatistics();
            IntSummaryStatistics blueStats = new IntSummaryStatistics();

            int startX = (int) region.getX();
            int endX = (int) (region.getX() + region.getWidth());

            int startY = (int) region.getY();
            int endY = (int) (region.getY() + region.getHeight());

            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    Color pixel = new Color(image.getRGB(x, y));
                    redStats.accept(pixel.getRed());
                    greenStats.accept(pixel.getGreen());
                    blueStats.accept(pixel.getBlue());
                }
            }

            return new Color((int) redStats.getAverage(),
                    (int) greenStats.getAverage(),
                    (int) blueStats.getAverage());
        }

        public BufferedImage getImage() {
            return image;
        }

        /**
         * Gets the average colour of this image. The calculation only happens
         * once and is this result is then returned each time this function is
         * called.
         * @return The average colour of this image.
         */
        public Color getAverageColour() {
            if (averageColour == null) {
                this.averageColour = LibraryImage.averageColour(this.image);
            }

            return this.averageColour;
        }
    }
}
