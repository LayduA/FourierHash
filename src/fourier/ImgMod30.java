package src.fourier;

/*
Copyright 2005, R.G.Baldwin
The purpose of this program is to provide 2D
Fourier Transform capability to be used for image
processing and other purposes.  The class
provides three static methods:
xform2D: Performs a forward 2D Fourier transform
 on a surface described by a 2D array of double
 values in the space domain to produce a spectrum
 in the wavenumber domain.  The method returns
 the real part, the imaginary part, and the
 amplitude spectrum, each in its own 2D array of
 double values.
inverseXform2D:  Performs an inverse 2D Fourier
 transform from the wavenumber domain into the
 space domain using the real and imaginary parts
 of the wavenumber spectrum as input.  Returns
 the surface in the space domain in a 2D array of
 double values.
shiftOrigin:  The wavenumber spectrum produced
 by xform2D has its origin in the upper left
 corner with the Nyquist folding wave numbers
 near the center.  This is not a very suitable
 format for visual analysis.  This method
 rearranges the data to place the origin at the
 center with the Nyquist folding wave numbers
 along the edges.
Tested using J2SE 5.0 and WinXP
************************************************/
import static java.lang.Math.*;

public class ImgMod30 {

    // This method computes a forward 2D Fourier
    // transform from the space domain into the
    // wavenumber domain. The number of points
    // produced for the wavenumber domain matches
    // the number of points received for the space
    // domain in both dimensions. Note that the
    // input data must be purely real. In other
    // words, the program assumes that there are
    // no imaginary values in the space domain.
    // Therefore, it is not a general-purpose 2D
    // complex-to-complex transform.
    public static void xform2D(double[][] inputData,
            double[][] realOut,
            double[][] imagOut,
            double[][] amplitudeOut) {
        int height = inputData.length;
        int width = inputData[0].length;

        System.out.println("height = " + height);
        System.out.println("width = " + width);
        // Two outer loops iterate on output data.
        for (int yWave = 0; yWave < height; yWave++) {
            System.out.println(yWave);
            for (int xWave = 0; xWave < width; xWave++) {
                // Two inner loops iterate on input data.
                for (int ySpace = 0; ySpace < height; ySpace++) {
                    for (int xSpace = 0; xSpace < width; xSpace++) {
                        // Compute real, imag, and ampltude. Note that it
                        // was necessary to sacrifice indentation to
                        // force these very long equations to be
                        // compatible with this narrow publication format
                        // and still be somewhat readable.
                        realOut[yWave][xWave] += (inputData[ySpace][xSpace] * cos(2 * PI * ((1.0 *
                                xWave * xSpace / width) + (1.0 * yWave * ySpace / height))))
                                / sqrt(width * height);
                        imagOut[yWave][xWave] -= (inputData[ySpace][xSpace] * sin(2 * PI * ((1.0 * xWave *
                                xSpace / width) + (1.0 * yWave * ySpace / height))))
                                / sqrt(width * height);

                        amplitudeOut[yWave][xWave] = sqrt(
                                realOut[yWave][xWave] * realOut[yWave][xWave] +
                                        imagOut[yWave][xWave] * imagOut[yWave][xWave]);
                    } // end xSpace loop
                } // end ySpace loop
            } // end xWave loop
        } // end yWave loop
    }// end xform2D method
     // -------------------------------------------//

    // This method computes an inverse 2D Fourier
    // transform from the wavenumber domain into
    // the space domain. The number of points
    // produced for the space domain matches
    // the number of points received for the wave-
    // number domain in both dimensions. Note that
    // this method assumes that the inverse
    // transform will produce purely real values in
    // the space domain. Therefore, in the
    // interest of computational efficiency, it
    // does not compute the imaginary output
    // values. Therefore, it is not a general
    // purpose 2D complex-to-complex transform. For
    // correct results, the input complex data must
    // match that obtained by performing a forward
    // transform on purely real data in the space
    // domain.
    public static void inverseXform2D(double[][] real,
            double[][] imag,
            double[][] dataOut) {
        int height = real.length;
        int width = real[0].length;

        System.out.println("height = " + height);
        System.out.println("width = " + width);
        // Two outer loops iterate on output data.
        for (int ySpace = 0; ySpace < height; ySpace++) {
            for (int xSpace = 0; xSpace < width; xSpace++) {
                // Two inner loops iterate on input data.
                for (int yWave = 0; yWave < height; yWave++) {
                    for (int xWave = 0; xWave < width; xWave++) {
                        // Compute real output data. Note that it was
                        // necessary to sacrifice indentation to force
                        // this very long equation to be compatible with
                        // this narrow publication format and still be
                        // somewhat readable.
                        dataOut[ySpace][xSpace] += (real[yWave][xWave] * cos(2 * PI * ((1.0 * xSpace *
                                xWave / width) + (1.0 * ySpace * yWave / height))) -
                                imag[yWave][xWave] * sin(2 * PI * ((1.0 * xSpace *
                                        xWave / width) + (1.0 * ySpace * yWave / height))))
                                / sqrt(width * height);
                    } // end xWave loop
                } // end yWave loop
            } // end xSpace loop
        } // end ySpace loop
    }// end inverseXform2D method
     // -------------------------------------------//

    // Method to shift the wavenumber origin and
    // place it at the center for a more visually
    // pleasing display. Must be applied
    // separately to the real part, the imaginary
    // part, and the amplitude spectrum for a wave-
    // number spectrum.
    public static double[][] shiftOrigin(double[][] data) {
        int numberOfRows = data.length;
        int numberOfCols = data[0].length;
        int newRows;
        int newCols;

        double[][] output = new double[numberOfRows][numberOfCols];

        // Must treat the data differently when the
        // dimension is odd than when it is even.

        if (numberOfRows % 2 != 0) {// odd
            newRows = numberOfRows +
                    (numberOfRows + 1) / 2;
        } else {// even
            newRows = numberOfRows + numberOfRows / 2;
        } // end else

        if (numberOfCols % 2 != 0) {// odd
            newCols = numberOfCols +
                    (numberOfCols + 1) / 2;
        } else {// even
            newCols = numberOfCols + numberOfCols / 2;
        } // end else

        // Create a temporary working array.
        double[][] temp = new double[newRows][newCols];

        // Copy input data into the working array.
        for (int row = 0; row < numberOfRows; row++) {
            for (int col = 0; col < numberOfCols; col++) {
                temp[row][col] = data[row][col];
            } // col loop
        } // row loop

        // Do the horizontal shift first
        if (numberOfCols % 2 != 0) {// shift for odd
            // Slide leftmost (numberOfCols+1)/2 columns
            // to the right by numberOfCols columns
            for (int row = 0; row < numberOfRows; row++) {
                for (int col = 0; col < (numberOfCols + 1) / 2; col++) {
                    temp[row][col + numberOfCols] = temp[row][col];
                } // col loop
            } // row loop
              // Now slide everything back to the left by
              // (numberOfCols+1)/2 columns
            for (int row = 0; row < numberOfRows; row++) {
                for (int col = 0; col < numberOfCols; col++) {
                    temp[row][col] = temp[row][col + (numberOfCols + 1) / 2];
                } // col loop
            } // row loop

        } else {// shift for even
            // Slide leftmost (numberOfCols/2) columns
            // to the right by numberOfCols columns.
            for (int row = 0; row < numberOfRows; row++) {
                for (int col = 0; col < numberOfCols / 2; col++) {
                    temp[row][col + numberOfCols] = temp[row][col];
                } // col loop
            } // row loop

            // Now slide everything back to the left by
            // numberOfCols/2 columns
            for (int row = 0; row < numberOfRows; row++) {
                for (int col = 0; col < numberOfCols; col++) {
                    temp[row][col] = temp[row][col + numberOfCols / 2];
                } // col loop
            } // row loop
        } // end else
          // Now do the vertical shift
        if (numberOfRows % 2 != 0) {// shift for odd
            // Slide topmost (numberOfRows+1)/2 rows
            // down by numberOfRows rows.
            for (int col = 0; col < numberOfCols; col++) {
                for (int row = 0; row < (numberOfRows + 1) / 2; row++) {
                    temp[row + numberOfRows][col] = temp[row][col];
                } // row loop
            } // col loop
              // Now slide everything back up by
              // (numberOfRows+1)/2 rows.
            for (int col = 0; col < numberOfCols; col++) {
                for (int row = 0; row < numberOfRows; row++) {
                    temp[row][col] = temp[row + (numberOfRows + 1) / 2][col];
                } // row loop
            } // col loop

        } else {// shift for even
            // Slide topmost (numberOfRows/2) rows down
            // by numberOfRows rows
            for (int col = 0; col < numberOfCols; col++) {
                for (int row = 0; row < numberOfRows / 2; row++) {
                    temp[row + numberOfRows][col] = temp[row][col];
                } // row loop
            } // col loop

            // Now slide everything back up by
            // numberOfRows/2 rows.
            for (int col = 0; col < numberOfCols; col++) {
                for (int row = 0; row < numberOfRows; row++) {
                    temp[row][col] = temp[row + numberOfRows / 2][col];
                } // row loop
            } // col loop
        } // end else

        // Shifting of the origin is complete. Copy
        // the rearranged data from temp to output
        // array.
        for (int row = 0; row < numberOfRows; row++) {
            for (int col = 0; col < numberOfCols; col++) {
                output[row][col] = temp[row][col];
            } // col loop
        } // row loop
        return output;
    }// end shiftOrigin method
}// end class ImgMod30
