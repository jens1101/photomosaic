package io.github.jens1101;

import java.awt.Color;
import java.awt.Graphics2D;
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
    // TODO: this can be an array
    private final List<LibraryImage> allLibraryImages;

    // TODO: make the XYZ reference a class variable

    public PhotoMosaic(File imageLibraryDirectory) throws IOException {
        if (!imageLibraryDirectory.exists()) {
            throw new RuntimeException("Image library directory '"
                    + imageLibraryDirectory.getPath() + "' not found");
        }

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
                    .collect(Collectors.toList());
        }
    }

    private static double[] toLabColour(Color color, double[] xyzReference) {
        float[] averageRgbFloat = color.getRGBColorComponents(null);
        double[] averageRgb = new double[]{
                (double) averageRgbFloat[0],
                (double) averageRgbFloat[1],
                (double) averageRgbFloat[2]
        };

        return ColorSpaceConverter.RGBtoCIELab(averageRgb, xyzReference);
    }

    public void createMosaic(File sourceImageFile,
                             File outputFile,
                             int minTilesPerSide,
                             double[] xyzReference) throws IOException {
        if (!sourceImageFile.exists()) {
            throw new RuntimeException("Source image '" +
                    sourceImageFile.getPath() + "' not found");
        }

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

        BufferedImage outputImage = new BufferedImage(numberOfTilesWide * tileSideLength,
                numberOfTilesHigh * tileSideLength,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D outputImageGraphics = outputImage.createGraphics();

        for (int x = 0; x + tileSideLength < sourceImage.getWidth(); x += tileSideLength) {
            for (int y = 0; y + tileSideLength < sourceImage.getHeight(); y += tileSideLength) {
                Rectangle region = new Rectangle(x, y, tileSideLength,
                        tileSideLength);

                Color averageColor = LibraryImage
                        .averageColour(sourceImage, region);

                LibraryImage closestImage =
                        findClosestLibraryImage(averageColor, xyzReference);

                // FIXME: I shouldn't modify this list of images. It will cause
                //  issues if I call this function multiple times in succession.
                this.allLibraryImages.remove(closestImage);

                int closestImageSideLength = Math.min(closestImage.getImage().getWidth(), closestImage.getImage().getHeight());

                outputImageGraphics.drawImage(closestImage.getImage(),
                        x, y, x + tileSideLength, y + tileSideLength,
                        0, 0, closestImageSideLength - 1, closestImageSideLength - 1,
                        null);
            }
        }

        ImageIO.write(outputImage, "png", outputFile);
    }

    private LibraryImage findClosestLibraryImage(Color averageColour,
                                                 double[] xyzReference) {
        double[] averageLabColour = toLabColour(averageColour, xyzReference);

        LibraryImage closestImage = null;
        double closestDeltaE = 0;

        for (LibraryImage currentImage : this.allLibraryImages) {
            double[] currentImageAverageLabColour = toLabColour(
                    currentImage.getAverageColour(), xyzReference);

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
     * Class used to represent a single image in the image library that's used
     * to create the photo mosaic.
     */
    private static class LibraryImage {
        /**
         * The file reference to the image itself.
         */
        private final File imageFile;

        private final BufferedImage image;

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
            this.image = ImageIO.read(imageFile);
            this.averageColour = LibraryImage.averageColour(this.image);
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

        public BufferedImage getImage() {
            return image;
        }

        public Color getAverageColour() {
            // TODO: only calculate the average colour here and then memorize
            //  it. This will make construction faster.
            return averageColour;
        }

        public File getImageFile() {
            return imageFile;
        }
    }
}
