package com.example.spectoclassifier118.wavreader;

/**
 *
 * This Class is an custom exception class to throw exceptions when unsupported files are provided as input for processing.
 *
 *
 */

public class FileFormatNotSupportedException extends Exception {

    public FileFormatNotSupportedException(String message) {
        super(message);
    }

}
