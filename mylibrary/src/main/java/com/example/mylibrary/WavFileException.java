package com.example.mylibrary;

/**
 * Custom Exception Class to handle errors occurring while loading/reading Wav file.
 *
 *
 */
public class WavFileException extends Exception {

    private static final long serialVersionUID = 1L;

    public WavFileException(final String message) {
        super(message);
    }

}
