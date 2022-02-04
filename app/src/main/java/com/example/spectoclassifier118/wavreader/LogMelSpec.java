package com.example.spectoclassifier118.wavreader;

//import com.example.spectoclassifier118.wavreader.WavFileException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * yangqin
 * This class extracts LogMelSpectrogram based on JLibrosa Audio features for
 * given Wav file.
 *
 */
public class LogMelSpec {

    public static double[][] main(String Path) throws IOException, WavFileException, FileFormatNotSupportedException {
        String audioFilePath = Path;
        int defaultSampleRate = -1; // -1 value implies the method to use default sample rate
        int defaultAudioDuration = -1; // -1 value implies the method to process complete audio duration

        JLibrosa jLibrosa = new JLibrosa();

        /*
         * To read the magnitude values of audio files - equivalent to
         * librosa.load('../audioFiles/0a2b400e_nohash_2_down.wav', sr=None) function
         */

        float audioFeatureValues[] = jLibrosa.loadAndRead(audioFilePath, defaultSampleRate, defaultAudioDuration);
        // System.out.println(audioFeatureValues.length);
        //for (int j = 0; j < 10; j++) {
        //    System.out.printf("%.10f%n", audioFeatureValues[j]);
        //}

        ArrayList<Float> audioFeatureValuesList = jLibrosa.loadAndReadAsList(audioFilePath, defaultSampleRate,
                defaultAudioDuration);
        // System.out.println(audioFeatureValuesList);

        /*
         * yangqin: To add pre-emphasis to audio librosa.effects.preemphasis(y,
         * coef=config_params.preemphasis_coef) function
         */
        float[] audioPreemphasisValue = new float[audioFeatureValues.length];
        audioPreemphasisValue[0] = audioFeatureValues[0];
        //System.out.println(audioPreemphasisValue[0]);
        for (int i = 1; i < audioFeatureValues.length; i++) {
            double pre = 0.0f;
            pre = audioFeatureValues[i] - audioFeatureValues[i - 1] * 0.96875;
            audioPreemphasisValue[i] = (float) pre;
        }
        //for (int j = 0; j < 10; j++) {
        //    System.out.printf("%.10f%n", audioPreemphasisValue[j]);
        //}

        /* To read the no of frames present in audio file */
        //int nNoOfFrames = jLibrosa.getNoOfFrames();

        /* To read sample rate of audio file */
        //int sampleRate = jLibrosa.getSampleRate();

        //float[][] melSpectrogram = jLibrosa.generateMelSpectroGram(audioPreemphasisValue, sampleRate, 512, 40, 160);

        /* yangqin: To get log mel spectrogram */
        double[][] logmelspec = jLibrosa.getLogMelSpec(audioPreemphasisValue);
        //System.out.println(logmelspec.length);
        //System.out.println(logmelspec[0].length);
        //for (int i = 0; i < 1; i++) {
        //    for (int j = 0; j < 10; j++) {
        //        System.out.printf("%.10f%n", logmelspec[i][j]);
        //    }
        //}
        return logmelspec;
    }
}

