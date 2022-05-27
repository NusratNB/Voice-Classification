package com.example.spectoclassifier118.spectoimage;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecordWavMaster {
    private static final int samplingRates[] = {16000, 11025, 11000, 8000, 6000};
    public static int SAMPLE_RATE = 16000;
    private AudioRecord mRecorder;
    MediaPlayer mediaPlayer;
    AudioTrack audioTrack;
    File waveFile;
    private File mRecording;
    private short[] mBuffer;
    private String audioFilePath;
    private boolean mIsRecording = false;
    int BUFFER_SIZE_PLAYING;
    boolean isPlayingAudio = false;
    boolean isPlayingMedia = false;
    public static final String TAG = "RecordWavMaster";
    private Thread playingThread;
    String fileNameMedia;
    String fileNameAudio;

    private String RECORD_WAV_PATH; //= Environment.getExternalStorageDirectory() + File.separator + "AudioRecord";
    short threshold=500;
    /* Initializing AudioRecording MIC */
    public RecordWavMaster(Context ctx,  String path) {
        initRecorder(ctx, path);
    }

    /* Get Supported Sample Rate */
    public static int getValidSampleRates() {
        for (int rate : samplingRates) {
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                return rate;
            }
        }
        return SAMPLE_RATE;
    }



    /* Start AudioRecording */
    public void recordWavStart() {
        mIsRecording = true;
        mRecorder.startRecording();
        mRecording = getFile("raw");
        startBufferedWrite(mRecording);
    }



    /* Stop AudioRecording */
    public String recordWavStop() {
        try {
            mIsRecording = false;
            mRecorder.stop();
            waveFile = getFile("wav");
            rawToWave(mRecording, waveFile);
            Log.e("path_audioFilePath", audioFilePath);
            return audioFilePath;
        } catch (Exception e) {
            Log.e("Error saving file : ", e.getMessage());
        }
        return null;
    }

    int searchThreshold(short[]arr,short thr){
        int peakIndex;
        int arrLen=arr.length;
        for (peakIndex=0;peakIndex<arrLen;peakIndex++){
            if ((arr[peakIndex]>=thr) || (arr[peakIndex]<=-thr)){
                //se supera la soglia, esci e ritorna peakindex-mezzo kernel.

                return peakIndex;
            }
        }
        return -1; //not found
    }
    public void startPlaying(Context ctx, int id, File fileName) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() { // release resources when end of file is reached
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.reset();
                    mp.release();
                    mediaPlayer = null;
                    isPlayingMedia = false;
                }
            });

            try {
                mediaPlayer.setDataSource(String.valueOf(fileName));
                mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()); // optional step
                mediaPlayer.prepare();
                mediaPlayer.start();
                Log.d(TAG, "playback started with MediaPlayer");
            } catch (IOException e) {
                Toast.makeText(ctx, "Couldn't prepare MediaPlayer, IOException", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "error reading from file while preparing MediaPlayer" + e.toString());
            } catch (IllegalArgumentException e) {
                Toast.makeText(ctx, "Couldn't prepare MediaPlayer, IllegalArgumentException", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "illegal argument given " + e.toString());
            }
        }


        }
//    }

    private void readAudioDataFromFile(Context ctx, File filename) { // called inside Runnable of playingThread

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(filename);
        }
        catch (IOException e) {
            Toast.makeText(ctx, "Couldn't open file input stream, IOException", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "could not create input stream before using AudioTrack " + e.toString());
            e.printStackTrace();
            return;
        }
        byte[] data = new byte[BUFFER_SIZE_PLAYING/2];
        int i = 0;

        while (isPlayingAudio && (i != -1)) { // continue until run out of data or user stops playback
            try {
                i = fileInputStream.read(data);
                audioTrack.write(data, 0, i);
            }
            catch (IOException e) {
                Toast.makeText(ctx, "Couldn't read from file while playing audio, IOException", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Could not read data " + e.toString());
                e.printStackTrace();
                return;
            }

        }
        try { // finish file operations
            fileInputStream.close();
        }
        catch (IOException e) {
            Log.e(TAG, "Could not close file input stream " + e.toString());
            e.printStackTrace();
            return;
        }

        // clean up resources
        isPlayingAudio = false;
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;
        playingThread = null;

    }

    private void stopPlaying(int id) {
            isPlayingAudio = false; // will trigger playingThread to exit while loop
//        }
    }

    /* Release device MIC */
    public void releaseRecord() {
        mRecorder.release();
    }

    /* Initializing AudioRecording MIC */
    private void initRecorder(Context ctx, String path) {
        RECORD_WAV_PATH = path;
        SAMPLE_RATE = getValidSampleRates();
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        BUFFER_SIZE_PLAYING = bufferSize;
        mBuffer = new short[bufferSize];

        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

        }
        mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        new File(RECORD_WAV_PATH).mkdir();

    }

    /* Writing RAW file */
    private void startBufferedWrite(final File file) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataOutputStream output = null;
                try {
                    output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
                    while (mIsRecording) {
                        double sum = 0;
                        int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
                        for (int i = 0; i < readSize; i++) {
                            int foundPeak=searchThreshold(mBuffer,threshold);
                            if (foundPeak>-1){
                                output.writeShort(mBuffer[i]);
                                sum += mBuffer[i] * mBuffer[i];
                            }
                            else {

                            }

                        }
                        if (readSize > 0) {
                            final double amplitude = sum / readSize;
                        }
                    }
                } catch (IOException e) {
                    Log.e("Error writing file : ", e.getMessage());
                } finally {

                    if (output != null) {
                        try {
                            output.flush();
                        } catch (IOException e) {
                            Log.e("Error writing file : ", e.getMessage());
                        } finally {
                            try {
                                output.close();
                            } catch (IOException e) {
                                Log.e("Error writing file : ", e.getMessage());
                            }
                        }
                    }
                }
            }
        }).start();
    }

    /* Converting RAW format To WAV Format*/
    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }
        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, SAMPLE_RATE); // sample rate
            writeInt(output, SAMPLE_RATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }
            output.write(bytes.array());
        } finally {
            if (output != null) {
                output.close();
                rawFile.delete();
            }
        }


    }

    /* Get file name */
    private File getFile(final String suffix) {
        Time time = new Time();
        time.setToNow();
        audioFilePath = time.format("%Y%m%d%H%M%S");
        return new File(RECORD_WAV_PATH, time.format("%Y%m%d%H%M%S") + "." + suffix);
    }

    public File getAudioName(){
        return waveFile;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    public String getFileName (final String time_suffix) {
        return (RECORD_WAV_PATH+time_suffix+ "." + "wav");
    }

    public Boolean getRecordingState () {
        if(  mRecorder.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) {
            return false;
        }
        return true;
    }
}

