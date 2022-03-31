package code.transform;

import code.types.Distance;
import code.types.DrawMode;
import code.types.Result;
import code.ui.Applet;
import code.ui.Surface;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;

import static code.transform.TransformHash.*;
import static code.ui.Applet.HASH_H;
import static code.ui.Applet.HASH_W;

public class CompressPairRunnable implements Runnable {
    Distance dist;
    String refHash;
    Surface drawer;
    DrawMode mode;
    Result[] out;
    int iter;

    public CompressPairRunnable(Result[] out, Distance dist, String refHash, Surface drawer, DrawMode mode, int iter) {
        this.out = out;
        this.refHash = refHash;
        this.drawer = drawer;
        this.mode = mode;
        this.dist = dist;
        this.iter = iter;
    }

    @Override
    public void run() {
        int size = Integer.parseInt(mode.toString().substring(mode.toString().length() - 3));
        BufferedImage res = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics g = res.createGraphics();
        double similarity;
        int[] refHashPixels;
        for (int i = 0; i < out.length; i++) {
            refHashPixels = drawer.drawFourierHash(g, refHash, 0, mode, dist, (i + 5) / 10.0);
            similarity = psiDist(refHashPixels, drawer.drawFourierHash(g, flipBit(refHash, mode.worstBit()), 0, mode, dist, (i + 5) / 10.0), "imageThread" + this.hashCode());
            // COMPRESSION
            try {
                int[] bitMasks = new int[]{0xFF0000, 0xFF00, 0xFF};
                SinglePixelPackedSampleModel sm = new SinglePixelPackedSampleModel(
                        DataBuffer.TYPE_INT, size, size, bitMasks);
                DataBufferInt db = new DataBufferInt(refHashPixels, refHashPixels.length);
                WritableRaster wr = Raster.createWritableRaster(sm, db, new Point());
                //System.out.println(ColorModel.getRGBdefault().hasAlpha());
                res = new BufferedImage(new DirectColorModel(24, bitMasks[0], bitMasks[1],
                        bitMasks[2]), wr, false, null);
                File compressedImageFile = new File("/Users/laya/Documents/VisualHashApplet/code/temp/" + this.hashCode() + "compress.jpg");
                OutputStream os = new FileOutputStream(compressedImageFile);
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
                ImageWriter writer = writers.next();
                File notcompressedImageFile = new File("/Users/laya/Documents/VisualHashApplet/code/temp/" + this.hashCode() + "notcompress.jpg");
                OutputStream notos = new FileOutputStream(notcompressedImageFile);
                ImageOutputStream nios = ImageIO.createImageOutputStream(notos);
                writer.setOutput(nios);
                writer.write(null, new IIOImage(res, null, null), null);
                ImageOutputStream ios = ImageIO.createImageOutputStream(os);
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.05f);
                writer.write(null, new IIOImage(res, null, null), param);
                ios.close();
                double compression = (double) compressedImageFile.length() / notcompressedImageFile.length();
                out[i] = (out[i] == null) ? new Result(dist, (i + 5) / 10.0, compression, similarity) :
                        out[i].plus(new Result(dist, (i + 5) / 10.0, compression, similarity), iter);
            } catch (Exception e) {
                System.err.println("Error in compresspair runnable :" + e);
            }
        }

    }
}

