import java.io.File;
import java.io.IOException;

import io.github.jens1101.PhotoMosaic;

public class Main {
    /**
     * Standard daylight reference values
     */
    private final static double[] XYZ_REFERENCE = {94.811, 100, 107.304};

    private final static int MIN_TILES_PER_SIDE = 20;

    public static void main(String[] args) {
        // If anything other than 3 arguments is given then print the usage of
        // this class.
        if (args.length != 3) {
            System.out.println("Usage: PhotoMosaic [SOURCE IMAGE PATH] " +
                    "[IMAGE LIBRARY DIRECTORY] [DESTINATION]");
            System.exit(1);
        }

        File sourceImageFile = new File(args[0]);
        File imageLibraryDirectory = new File(args[1]);
        File outputFile = new File(args[2]);

        try {
            PhotoMosaic mosaic = new PhotoMosaic(imageLibraryDirectory, XYZ_REFERENCE);
            mosaic.createMosaic(sourceImageFile, outputFile, MIN_TILES_PER_SIDE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
