package com.atomx.genericprinter;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class EscPosImageHelper {

    public static byte[] convertBitmap(Bitmap bitmap) {
        Bitmap resizedBitmap = resizeBitmap(bitmap, 384); // Resize to printer width (adjust as needed)
        Bitmap monochromeBitmap = convertToMonochrome(resizedBitmap);

        return generateRasterImage(monochromeBitmap);
    }

    private static Bitmap resizeBitmap(Bitmap bitmap, int width) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        float ratio = (float) originalHeight / originalWidth;
        int height = (int) (width * ratio);
        return Bitmap.createScaledBitmap(bitmap, width, height, false);
    }

    private static Bitmap convertToMonochrome(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap monoBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                int gray = (r + g + b) / 3;
                int newPixel = (gray < 128) ? 0xFF000000 : 0xFFFFFFFF; // black or white
                monoBitmap.setPixel(x, y, newPixel);
            }
        }
        return monoBitmap;
    }

    private static byte[] generateRasterImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bytesPerRow = (width + 7) / 8;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        // ESC/POS command for raster image: GS v 0
        stream.write(0x1D);
        stream.write('v');
        stream.write('0');
        stream.write(0x00); // Normal mode

        // Width and height in little-endian
        stream.write(bytesPerRow % 256); // xL
        stream.write(bytesPerRow / 256); // xH
        stream.write(height % 256);      // yL
        stream.write(height / 256);      // yH

        for (int y = 0; y < height; y++) {
            for (int xByte = 0; xByte < bytesPerRow; xByte++) {
                byte b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int x = xByte * 8 + bit;
                    if (x < width) {
                        int pixel = bitmap.getPixel(x, y);
                        int r = (pixel >> 16) & 0xff;
                        // Since it's monochrome, red channel is enough
                        if (r == 0) {
                            b |= (1 << (7 - bit));
                        }
                    }
                }
                stream.write(b);
            }
        }

        return stream.toByteArray();
    }
}
