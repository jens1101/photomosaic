package io.github.jens1101;

import java.io.File;
import java.io.IOException;

public class Main {
    /**
     * Standard daylight reference values
     */
    private final static double[] XYZ_REFERENCE = {94.811, 100, 107.304};

    private final static int MIN_TILES_PER_SIDE = 40;

    public static void main(String[] args) {
        // If anything other than 3 arguments is given then print the usage of
        // this class.
        if (args.length != 3) {
            System.out.println("Usage: PhotoMosaic [SOURCE IMAGE PATH] [IMAGE LIBRARY DIRECTORY] [DESTINATION]");
            System.exit(1);
        }

        File sourceImageFile = new File(args[0]);
        File imageLibraryDirectory = new File(args[1]);
        File outputFile = new File(args[2]);

        try {
            PhotoMosaic mosaic = new PhotoMosaic(imageLibraryDirectory);
            mosaic.createMosaic(sourceImageFile, outputFile, MIN_TILES_PER_SIDE,
                    XYZ_REFERENCE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
