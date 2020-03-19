package io.github.jens1101;

import java.awt.Color;

/**
 * This class contains helper functions to convert colours from one colour space
 * representation to another.
 */
public class ColorSpaceConverter {
    /**
     * Private constructor to prevent directly instantiating this class
     */
    private ColorSpaceConverter() {
    }

    /**
     * Converts the given array of RGB values to XYZ
     *
     * @param rgb An array of doubles with a length of 3. The 1st index
     *            represents the red component, the 2nd represents the green
     *            component, and the 3rd represents the blue component.
     * @return The colour in the XYZ format.
     */
    public static double[] RGBtoXYZ(double[] rgb) {
        double red = rgb[0];
        double green = rgb[1];
        double blue = rgb[2];

        if (red > 0.04045) red = Math.pow((red + 0.055) / 1.055, 2.4);
        else red = red / 12.92;
        if (green > 0.04045) green = Math.pow((green + 0.055) / 1.055, 2.4);
        else green = green / 12.92;
        if (blue > 0.04045) blue = Math.pow((blue + 0.055) / 1.055, 2.4);
        else blue = blue / 12.92;

        red = red * 100;
        green = green * 100;
        blue = blue * 100;

        double x = red * 0.4124 + green * 0.3576 + blue * 0.1805;
        double y = red * 0.2126 + green * 0.7152 + blue * 0.0722;
        double z = red * 0.0193 + green * 0.1192 + blue * 0.9505;

        return new double[]{x, y, z};
    }

    /**
     * Converts the given array of RGB values to CIE L*ab
     *
     * @param color        The colour to convert.
     * @param referenceXyz The reference illumination to be used when creating
     *                     a mosaic.
     * @return The colour in the CIE L*ab format.
     */
    public static double[] colorToCIELab(Color color, double[] referenceXyz) {
        float[] averageRgbFloat = color.getRGBColorComponents(null);
        double[] averageRgb = new double[]{
                (double) averageRgbFloat[0],
                (double) averageRgbFloat[1],
                (double) averageRgbFloat[2]
        };

        return XYZtoCIELab(RGBtoXYZ(averageRgb), referenceXyz);
    }

    /**
     * Converts the given array of XYZ values to CIE L*ab
     *
     * @param xyz          An array of doubles with a length of 3. The 1st index
     *                     represents the X component, the 2nd represents the
     *                     Y component, and the 3rd represents the Z component.
     * @param referenceXyz The reference illumination to be used when creating
     *                     a mosaic.
     * @return The colour in the CIE L*ab format.
     */
    public static double[] XYZtoCIELab(double[] xyz, double[] referenceXyz) {
        double var_X = xyz[0] / referenceXyz[0];
        double var_Y = xyz[1] / referenceXyz[1];
        double var_Z = xyz[2] / referenceXyz[2];

        if (var_X > 0.008856) var_X = Math.pow(var_X, 1.0 / 3.0);
        else var_X = (7.787 * var_X) + (16.0 / 116.0);
        if (var_Y > 0.008856) var_Y = Math.pow(var_Y, 1.0 / 3.0);
        else var_Y = (7.787 * var_Y) + (16.0 / 116.0);
        if (var_Z > 0.008856) var_Z = Math.pow(var_Z, 1.0 / 3.0);
        else var_Z = (7.787 * var_Z) + (16.0 / 116.0);

        double l = 116 * var_Y - 16;
        double a = 500 * (var_X - var_Y);
        double b = 200 * (var_Y - var_Z);

        return new double[]{l, a, b};
    }
}
