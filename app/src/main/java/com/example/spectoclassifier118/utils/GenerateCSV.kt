package com.example.spectoclassifier118.utils

import android.os.Build
import androidx.annotation.RequiresApi
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.nio.file.Files
import java.nio.file.Paths

class GenerateCSV {
    
    @RequiresApi(Build.VERSION_CODES.O)
    fun generateCSV(path: String, data: Array<FloatArray> ){

        Files.newBufferedWriter(Paths.get(path)).use { writer ->
            CSVPrinter(
                writer, CSVFormat.DEFAULT

            ).use { csvPrinter ->
                for (element in data){
                    csvPrinter.printRecord(element.asList())
                }
                csvPrinter.flush()
            }
        }
    }

}