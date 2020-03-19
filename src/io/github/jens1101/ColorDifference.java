package io.github.jens1101;

/**
 * Class used to calculate colour differences.
 * <p>
 * This is not my own work. Credit goes to
 * <a href="https://github.com/wuchubuzai">wuchubuzai on GitHub</a>
 *
 * @author wuchubuzai
 * @see <a href="https://github.com/wuchubuzai/OpenIMAJ">Source library</a>
 */
public class ColorDifference {
    /**
     * Private constructor to prevent directly instantiating this class
     */
    private ColorDifference() {
    }

    /**
     * Calculate the colour difference value between two colours in lab space.
     *
     * @param lab1 The first colour to compare. An array of doubles with a
     *             length of 3. The 1st index represents the L component, the
     *             2nd represents the a component, and the 3rd represents the b
     *             component.
     * @param lab2 The second colour to compare. An array of doubles with a
     *             length of 3. The 1st index represents the L component, the
     *             2nd represents the a component, and the 3rd represents the b
     *             component.
     * @return A double in the range between 0 - 100 that represents the
     * difference between the two colours. 0 means the colours are identical,
     * 100 means the colours are polar opposites.
     */
    public static double calculateDeltaE2000(double[] lab1, double[] lab2) {
        return calculateDeltaE2000(lab1[0], lab1[1], lab1[2], lab2[0], lab2[1],
                lab2[2]);
    }

    /**
     * Calculate the colour difference value between two colours in lab space.
     *
     * @param L1 first colour's L component
     * @param a1 first colour's a component
     * @param b1 first colour's b component
     * @param L2 second colour's L component
     * @param a2 second colour's a component
     * @param b2 second colour's b component
     * @return the CIE 2000 colour difference
     * @see <a href="https://github.com/wuchubuzai/OpenIMAJ/blob/master/image/image-processing/src/main/java/org/openimaj/image/analysis/colour/CIEDE2000.java">
     * Original implementation</a>
     */
    public static double calculateDeltaE2000(double L1, double a1, double b1,
                                             double L2, double a2, double b2) {
        double LMean = (L1 + L2) / 2.0;
        double C1 = Math.sqrt(a1 * a1 + b1 * b1);
        double C2 = Math.sqrt(a2 * a2 + b2 * b2);
        double CMean = (C1 + C2) / 2.0;

        double G = (1 - Math.sqrt(Math.pow(CMean, 7) / (Math.pow(CMean, 7) + Math.pow(25, 7)))) / 2;
        double a1prime = a1 * (1 + G);
        double a2prime = a2 * (1 + G);

        double C1prime = Math.sqrt(a1prime * a1prime + b1 * b1);
        double C2prime = Math.sqrt(a2prime * a2prime + b2 * b2);
        double CMeanPrime = (C1prime + C2prime) / 2;

        double h1prime = Math.atan2(b1, a1prime) + 2 * Math.PI * (Math.atan2(b1, a1prime) < 0 ? 1 : 0);
        double h2prime = Math.atan2(b2, a2prime) + 2 * Math.PI * (Math.atan2(b2, a2prime) < 0 ? 1 : 0);
        double HMeanPrime = ((Math.abs(h1prime - h2prime) > Math.PI) ? (h1prime + h2prime + 2 * Math.PI) / 2 : (h1prime + h2prime) / 2);

        double T = 1.0 - 0.17 * Math.cos(HMeanPrime - Math.PI / 6.0) + 0.24 * Math.cos(2 * HMeanPrime) + 0.32 * Math.cos(3 * HMeanPrime + Math.PI / 30) - 0.2 * Math.cos(4 * HMeanPrime - 21 * Math.PI / 60);

        double deltaHPrime = ((Math.abs(h1prime - h2prime) <= Math.PI) ? h2prime - h1prime : (h2prime <= h1prime) ? h2prime - h1prime + 2 * Math.PI : h2prime - h1prime - 2 * Math.PI);

        double deltaLPrime = L2 - L1;
        double deltaCPrime = C2prime - C1prime;
        deltaHPrime = 2.0 * Math.sqrt(C1prime * C2prime) * Math.sin(deltaHPrime / 2.0);
        double SL = 1.0 + ((0.015 * (LMean - 50) * (LMean - 50)) / (Math.sqrt(20 + (LMean - 50) * (LMean - 50))));
        double SC = 1.0 + 0.045 * CMeanPrime;
        double SH = 1.0 + 0.015 * CMeanPrime * T;

        double deltaTheta = (30 * Math.PI / 180) * Math.exp(-((180 / Math.PI * HMeanPrime - 275) / 25) * ((180 / Math.PI * HMeanPrime - 275) / 25));
        double RC = (2 * Math.sqrt(Math.pow(CMeanPrime, 7) / (Math.pow(CMeanPrime, 7) + Math.pow(25, 7))));
        double RT = (-RC * Math.sin(2 * deltaTheta));

        double KL = 1;
        double KC = 1;
        double KH = 1;

        return Math.sqrt(
                ((deltaLPrime / (KL * SL)) * (deltaLPrime / (KL * SL))) +
                        ((deltaCPrime / (KC * SC)) * (deltaCPrime / (KC * SC))) +
                        ((deltaHPrime / (KH * SH)) * (deltaHPrime / (KH * SH))) +
                        (RT * (deltaCPrime / (KC * SC)) * (deltaHPrime / (KH * SH)))
        );
    }
}
