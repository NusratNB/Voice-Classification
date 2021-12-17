package com.example.spectoclassifier118.spectoimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
//import ae.java.awt.Color;



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

  /*
*
* Manipulate ARGB colors from android.graphics.Color

Color.alpha(int value) ===> public static int alpha (int color) ===>
*  Return the alpha component of a color int. This is the same as saying color >>> 24
*  https://developer.android.com/reference/android/graphics/Color#alpha(int)
*
Color.red(int value) ===> public static int red (int color) ===>
*  Return the red component of a color int. This is the same as saying (color >> 16) & 0xFF
*  https://developer.android.com/reference/android/graphics/Color#red(int)
*
Color.green(int value) ===> public static int green (int color) ===>
* Return the green component of a color int. This is the same as saying (color >> 8) & 0xFF
* https://developer.android.com/reference/android/graphics/Color#green(int)
*
Color.blue(int value) ===>  public static int blue (int color) ===>
* Return the blue component of a color int. This is the same as saying color & 0xFF
* https://developer.android.com/reference/android/graphics/Color#blue(int)
*
*
* */

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
//        int a = ave(Color.alpha(c0), Color.alpha(c1), p);
//        Log.d(String.valueOf(Color.alpha(c0)), "This is Color.alpha(c0) ");
//        Log.d(String.valueOf(Color.alpha(c1)), "This is Color.alpha(c1) ");
//        int r = ave(Color.red(c0), Color.red(c1), p);
//        Log.d(String.valueOf(Color.red(c0)), "This is Color.red(c0) ");
//        Log.d(String.valueOf(Color.red(c1)), "This is Color.red(c1) ");
//        int g = ave(Color.green(c0), Color.green(c1), p);
//        Log.d(String.valueOf(Color.green(c0)), "This is Color.green(c0) ");
//        Log.d(String.valueOf(Color.green(c1)), "This is Color.green(c1) ");
//
//        int b = ave(Color.blue(c0), Color.blue(c1), p);
//        Log.d(String.valueOf(Color.blue(c0)), "This is Color.blue(c0) ");
//        Log.d(String.valueOf(Color.blue(c1)), "This is Color.blue(c1) ");


        int a1 = c0 >>> 24;
        int a2 = c1 >>> 24;
        int a = ave(a1, a2, p);

        int r1 = (c0 >> 16) & 0xFF;
        int r2 = (c1 >> 16) & 0xFF;
        int r = ave(r1, r2, p);

        int g1 = (c0 >> 8) & 0xFF;
        int g2 = (c1 >> 8) & 0xFF;
        int g = ave(g1, g2, p);

        int b1 = c0 & 0xFF;
        int b2 = c1 & 0xFF;
        int b = ave(b1, b2, p);

//        Color myColor = new Color(r, g, b, a);
//        int gg = myColor.getGreen();



//        int result = myColor.getRGB();


        return Color.argb(a, r, g, b);
    }


}