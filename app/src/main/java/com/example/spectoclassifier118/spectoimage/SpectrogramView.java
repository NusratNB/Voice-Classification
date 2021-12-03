package com.example.spectoclassifier118.spectoimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;



public class SpectrogramView extends View {
    public Bitmap bmp;
    private int[] colorRainbow = new int[] {    0xFFFFFFFF, 0xFFFF00FF, 0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFF000000 };
    private int[] colorFire = new int[] {    0xFFFFFFFF, 0xFFFFFF00, 0xFFFF0000, 0xFF000000 };
    private int[] colorIce = new int[] {    0xFFFFFFFF, 0xFF00FFFF, 0xFF0000FF, 0xFF000000 };
    private int[] colorGrey = new int[] {    0xFFFFFFFF, 0xFF000000 };


    public SpectrogramView(Context context, double [][] data) {
        super(context);

        if (data != null) {
            int width = data.length;
            int height = data[0].length;

            int[] arrayCol = new int[width*height];
            int [] altRes = new int[width*height];
            int counter = 0;
            for(int i = 0; i < height; i++) {
                for(int j = 0; j < width; j++) {
                    int value;
                    int color;
                    int coloralt;
                    int value_alt;
                    value = 255-(int)(data[j][i] * 255);
                    value_alt = (int)(data[j][i] * 255);
                    color = (value_alt<<16|value_alt<<8|value_alt|255<<24);

//                    color = ((value_alt & 0xff)<<24|(value_alt & 0xff)<<16|(value_alt & 0xff)<<8|(value_alt & 0xff));
//                    color = ((value & 0xff)|(value & 0xff)<<8|(value & 0xff)<<16|(value & 0xff)<<24);
//                    color =value<<16|value<<8|value;  (value*0.0045555555)
                    int c = getInterpolatedColor(colorFire, (float) (value*0.005));
                    coloralt = (c<<16|c<<8|c|255<<24);
                    arrayCol[counter] = color;
                    altRes[counter] = coloralt;
                    counter ++;
                }

            }
            bmp = Bitmap.createBitmap(altRes, width, height, Bitmap.Config.ARGB_8888);

        } else {
            System.err.println("Data Corrupt");
        }
    }
    public Bitmap getBitmap(){
        return bmp;
    }

    //Nusrat

    private int ave(int s, int d, float p) {
        return s + Math.round(p * (d - s));
    }
    public int getInterpolatedColor(int[] colors, float unit) {
        if (unit <= 0) return colors[0];
        if (unit >= 1) return colors[colors.length - 1];

        float p = unit * (colors.length - 1);
        int i = (int) p;
        p -= i;

        // now p is just the fractional part [0...1) and i is the index
        int c0 = colors[i];
        int c1 = colors[i + 1];
        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
        int r = ave(Color.red(c0), Color.red(c1), p);
        int g = ave(Color.green(c0), Color.green(c1), p);
        int b = ave(Color.blue(c0), Color.blue(c1), p);

        return Color.argb(a, r, g, b);
    }


}