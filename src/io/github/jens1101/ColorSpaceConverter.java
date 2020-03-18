package io.github.jens1101;

// TODO: add documentation to this
public class ColorSpaceConverter {
    /**
     * Private constructor to prevent directly instantiating this class
     */
    private ColorSpaceConverter() {
    }

    public static float[] RGBtoXYZ(float[] rgb) {
        if (rgb.length != 3) {
            throw new RuntimeException("RGB array must be an array with " +
                    "length 3");
        }

        double var_R = rgb[0];
        double var_G = rgb[1];
        double var_B = rgb[2];

        if (var_R > 0.04045) var_R = Math.pow((var_R + 0.055) / 1.055, 2.4);
        else var_R = var_R / 12.92;
        if (var_G > 0.04045) var_G = Math.pow((var_G + 0.055) / 1.055, 2.4);
        else var_G = var_G / 12.92;
        if (var_B > 0.04045) var_B = Math.pow((var_B + 0.055) / 1.055, 2.4);
        else var_B = var_B / 12.92;

        var_R = var_R * 100;
        var_G = var_G * 100;
        var_B = var_B * 100;

        float X = (float) (var_R * 0.4124 + var_G * 0.3576 + var_B * 0.1805);
        float Y = (float) (var_R * 0.2126 + var_G * 0.7152 + var_B * 0.0722);
        float Z = (float) (var_R * 0.0193 + var_G * 0.1192 + var_B * 0.9505);

        return new float[]{X, Y, Z};
    }

    public static float[] RGBtoCIELab(float[] rgb, float[] referenceXyz) {
        return XYZtoCIELab(RGBtoXYZ(rgb), referenceXyz);
    }

    public static float[] XYZtoCIELab(float[] xyz, float[] referenceXyz) {
        double var_X = xyz[0] / referenceXyz[0];
        double var_Y = xyz[1] / referenceXyz[1];
        double var_Z = xyz[2] / referenceXyz[2];

        if (var_X > 0.008856) var_X = Math.pow(var_X, 1.0 / 3.0);
        else var_X = (7.787 * var_X) + (16.0 / 116.0);
        if (var_Y > 0.008856) var_Y = Math.pow(var_Y, 1.0 / 3.0);
        else var_Y = (7.787 * var_Y) + (16.0 / 116.0);
        if (var_Z > 0.008856) var_Z = Math.pow(var_Z, 1.0 / 3.0);
        else var_Z = (7.787 * var_Z) + (16.0 / 116.0);

        float l = (float) (116 * var_Y - 16);
        float a = (float) (500 * (var_X - var_Y));
        float b = (float) (200 * (var_Y - var_Z));

        return new float[]{l, a, b};
    }
}
