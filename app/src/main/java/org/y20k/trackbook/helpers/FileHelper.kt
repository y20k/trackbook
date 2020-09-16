/*
 * FileHelper.kt
 * Implements the FileHelper object
 * A FileHelper provides helper methods for reading and writing files from and to device storage
 *
 * This file is part of
 * TRACKBOOK - Movement Recorder for Android
 *
 * Copyright (c) 2016-20 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 *
 * Trackbook uses osmdroid - OpenStreetMap-Tools for Android
 * https://github.com/osmdroid/osmdroid
 */


package org.y20k.trackbook.helpers

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.y20k.trackbook.Keys
import org.y20k.trackbook.core.Track
import org.y20k.trackbook.core.Tracklist
import org.y20k.trackbook.core.TracklistElement
import java.io.*
import java.text.NumberFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/*
 * FileHelper object
 */
object FileHelper {

    /* Define log tag */
    private val TAG: String = LogHelper.makeLogTag(FileHelper::class.java)


    /* Return an InputStream for given Uri */
    fun getTextFileStream(context: Context, uri: Uri): InputStream? {
        var stream : InputStream? = null
        try {
            stream = context.contentResolver.openInputStream(uri)
        } catch (e : Exception) {
            e.printStackTrace()
        }
        return stream
    }


    /* Get file size for given Uri */
    fun getFileSize(context: Context, uri: Uri): Long {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val sizeIndex: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            val size: Long = cursor.getLong(sizeIndex)
            cursor.close()
            return size
        } else {
            return 0L
        }
    }


    /* Get file name for given Uri */
    fun getFileName(context: Context, uri: Uri): String {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        if (cursor != null) {
            val nameIndex: Int = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            val name: String = cursor.getString(nameIndex)
            cursor.close()
            return name
        } else {
            return String()
        }
    }


    /* Clears given folder - keeps given number of files */
    fun clearFolder(folder: File?, keep: Int, deleteFolder: Boolean = false) {
        if (folder != null && folder.exists()) {
            val files = folder.listFiles()
            val fileCount: Int = files.size
            files.sortBy { it.lastModified() }
            for (fileNumber in files.indices) {
                if (fileNumber < fileCount - keep) {
                    files[fileNumber].delete()
                }
            }
            if (deleteFolder && keep == 0) {
                folder.delete()
            }
        }
    }


    /* Reads tracklist from storage using GSON */
    fun readTracklist(context: Context): Tracklist {
        LogHelper.v(TAG, "Reading Tracklist - Thread: ${Thread.currentThread().name}")
        // get JSON from text file
        val json: String = readTextFile(context, getTracklistFileUri(context))
        var tracklist: Tracklist = Tracklist()
        when (json.isNotBlank()) {
            // convert JSON and return as tracklist
            true -> try {
                tracklist = getCustomGson().fromJson(json, Tracklist::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return tracklist
    }


    /* Reads track from storage using GSON */
    fun readTrack(context: Context, fileUri: Uri): Track {
        // get JSON from text file
        val json: String = readTextFile(context, fileUri)
        var track: Track = Track()
        when (json.isNotEmpty()) {
            // convert JSON and return as track
            true -> try {
                track = getCustomGson().fromJson(json, Track::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return track
    }


    /* Deletes temp track file */
    fun deleteTempFile(context: Context) {
        getTempFileUri(context).toFile().delete()
    }


    /* Checks if temp track file exists */
    fun tempFileExists(context: Context): Boolean {
        return getTempFileUri(context).toFile().exists()
    }


    /* Creates Uri for Gpx file of a track */
    fun getGpxFileUri(context: Context, track: Track): Uri = File(context.getExternalFilesDir(Keys.FOLDER_GPX), getGpxFileName(track)).toUri()


    /* Creates file name for Gpx file of a track */
    fun getGpxFileName(track: Track): String = DateTimeHelper.convertToSortableDateString(track.recordingStart) + Keys.GPX_FILE_EXTENSION


    /* Creates Uri for json track file */
    fun getTrackFileUri(context: Context, track: Track): Uri {
        val fileName: String = DateTimeHelper.convertToSortableDateString(track.recordingStart) + Keys.TRACKBOOK_FILE_EXTENSION
        return File(context.getExternalFilesDir(Keys.FOLDER_TRACKS), fileName).toUri()
    }


    /* Creates Uri for json temp track file */
    fun getTempFileUri(context: Context): Uri {
        return File(context.getExternalFilesDir(Keys.FOLDER_TEMP), Keys.TEMP_FILE).toUri()
    }


    /* Suspend function: Wrapper for saveTracklist */
    suspend fun addTrackAndSaveTracklistSuspended(context: Context, track: Track, modificationDate: Date = track.recordingStop) {
        return suspendCoroutine { cont ->
            val tracklist: Tracklist = readTracklist(context)
            tracklist.tracklistElements.add(track.toTracklistElement(context))
            cont.resume(saveTracklist(context, tracklist, modificationDate))
        }
    }


    /* Suspend function: Wrapper for renameTrack */
    suspend fun renameTrackSuspended(context: Context, track: Track, newName: String) {
        return suspendCoroutine { cont ->
            cont.resume(renameTrack(context, track, newName))
        }
    }


    /* Suspend function: Wrapper for saveTracklist */
    suspend fun saveTracklistSuspended(context: Context, tracklist: Tracklist, modificationDate: Date) {
        return suspendCoroutine { cont ->
            cont.resume(saveTracklist(context, tracklist, modificationDate))
        }
    }


    /* Suspend function: Wrapper for saveTrack */
    suspend fun saveTrackSuspended(track: Track, saveGpxToo: Boolean) {
        return suspendCoroutine { cont ->
            cont.resume(saveTrack(track, saveGpxToo))
        }
    }


    /* Suspend function: Wrapper for saveTempTrack */
    suspend fun saveTempTrackSuspended(context: Context, track: Track) {
        return suspendCoroutine { cont ->
            cont.resume(saveTempTrack(context, track))
        }
    }


    /* Suspend function: Wrapper for deleteTrack */
    suspend fun deleteTrackSuspended(context: Context, position: Int, tracklist: Tracklist): Tracklist {
        return suspendCoroutine { cont ->
            cont.resume(deleteTrack(context, position, tracklist))
        }
    }


    /* Suspend function: Deletes tracks that are not starred using deleteTracks */
    suspend fun deleteNonStarredSuspended(context: Context, tracklist: Tracklist): Tracklist {
        return suspendCoroutine { cont ->
            val tracklistElements = mutableListOf<TracklistElement>()
            tracklist.tracklistElements.forEach { tracklistElement ->
                if (!tracklistElement.starred) {
                    tracklistElements.add(tracklistElement)
                }
            }
            cont.resume(deleteTracks(context, tracklistElements, tracklist))
        }
    }


    /* Suspend function: Wrapper for readTracklist */
    suspend fun readTracklistSuspended(context: Context): Tracklist {
        return suspendCoroutine {cont ->
            cont.resume(readTracklist(context))
        }
    }


    /* Suspend function: Wrapper for copyFile */
    suspend fun saveCopyOfFileSuspended(context: Context, originalFileUri: Uri, targetFileUri: Uri, deleteOriginal: Boolean = false) {
        return suspendCoroutine { cont ->
            cont.resume(copyFile(context, originalFileUri, targetFileUri, deleteOriginal))
        }
    }


    /* Save Track as JSON to storage */
    private fun saveTrack(track: Track, saveGpxToo: Boolean) {
        val jsonString: String = getTrackJsonString(track)
        if (jsonString.isNotBlank()) {
            // write track file
            writeTextFile(jsonString, Uri.parse(track.trackUriString))
        }
        if (saveGpxToo) {
            val gpxString: String = TrackHelper.createGpxString(track)
            if (gpxString.isNotBlank()) {
                // write GPX file
                writeTextFile(gpxString, Uri.parse(track.gpxUriString))
            }
        }
    }


    /* Save Temp Track as JSON to storage */
    private fun saveTempTrack(context: Context, track: Track) {
        val json: String = getTrackJsonString(track)
        if (json.isNotBlank()) {
            writeTextFile(json, getTempFileUri(context))
        }
    }


    /* Saves track tracklist as JSON text file */
    private fun saveTracklist(context: Context, tracklist: Tracklist, modificationDate: Date) {
        tracklist.modificationDate = modificationDate
        // convert to JSON
        val gson: Gson = getCustomGson()
        var json: String = String()
        try {
            json = gson.toJson(tracklist)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (json.isNotBlank()) {
            // write text file
            writeTextFile(json, getTracklistFileUri(context))
        }
    }


    /* Creates Uri for tracklist file */
    private fun getTracklistFileUri(context: Context): Uri {
        return File(context.getExternalFilesDir(""), Keys.TRACKLIST_FILE).toUri()
    }



    /* Renames track */
    private fun renameTrack(context: Context, track: Track, newName: String) {
        // search track in tracklist
        val tracklist: Tracklist = readTracklist(context)
        var trackUriString: String = String()
        tracklist.tracklistElements.forEach { tracklistElement ->
            if (tracklistElement.getTrackId() == track.getTrackId()) {
                // rename tracklist element
                tracklistElement.name = newName
                trackUriString = tracklistElement.trackUriString
            }
        }
        if (trackUriString.isNotEmpty()) {
            // save tracklist
            saveTracklist(context, tracklist, GregorianCalendar.getInstance().time)
            // rename track
            track.name = newName
            // save track
            saveTrack(track, saveGpxToo = true)
        }
    }


    /* Deletes multiple tracks */
    private fun deleteTracks(context: Context, tracklistElements: MutableList<TracklistElement>, tracklist: Tracklist): Tracklist {
        tracklistElements.forEach { tracklistElement ->
            // delete track files
            tracklistElement.trackUriString.toUri().toFile().delete()
            tracklistElement.gpxUriString.toUri().toFile().delete()
        }
        tracklist.tracklistElements.removeAll{ tracklistElements.contains(it) }
        saveTracklist(context, tracklist, GregorianCalendar.getInstance().time)
        return tracklist
    }


    /* Deletes one track */
    private fun deleteTrack(context: Context, position: Int, tracklist: Tracklist): Tracklist {
        val tracklistElement: TracklistElement = tracklist.tracklistElements[position]
        // delete track files
        tracklistElement.trackUriString.toUri().toFile().delete()
        tracklistElement.gpxUriString.toUri().toFile().delete()
        // remove track element from list
        tracklist.tracklistElements.removeIf { TrackHelper.getTrackId(it) == TrackHelper.getTrackId(tracklistElement) }
        saveTracklist(context, tracklist, GregorianCalendar.getInstance().time)
        return tracklist
    }


    /* Copies file to specified target */
    private fun copyFile(context: Context, originalFileUri: Uri, targetFileUri: Uri, deleteOriginal: Boolean = false) {
        val inputStream = context.contentResolver.openInputStream(originalFileUri)
        val outputStream = context.contentResolver.openOutputStream(targetFileUri)
        if (outputStream != null) {
            inputStream?.copyTo(outputStream)
        }
        if (deleteOriginal) {
            context.contentResolver.delete(originalFileUri, null, null)
        }
    }


    /* Converts track to JSON */
    private fun getTrackJsonString(track: Track): String {
        val gson: Gson = getCustomGson()
        var json: String = String()
        try {
            json = gson.toJson(track)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return json
    }


    /* Creates a Gson object */
    private fun getCustomGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.setDateFormat("M/d/yy hh:mm a")
        gsonBuilder.excludeFieldsWithoutExposeAnnotation()
        return gsonBuilder.create()
    }



    /* Converts byte value into a human readable format */
    // Source: https://programming.guide/java/formatting-byte-size-to-human-readable-format.html
    fun getReadableByteCount(bytes: Long, si: Boolean = true): String {

        // check if Decimal prefix symbol (SI) or Binary prefix symbol (IEC) requested
        val unit: Long = if (si) 1000L else 1024L

        // just return bytes if file size is smaller than requested unit
        if (bytes < unit) return "$bytes B"

        // calculate exp
        val exp: Int = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()

        // determine prefix symbol
        val prefix: String = ((if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i")

        // calculate result and set number format
        val result: Double = bytes / Math.pow(unit.toDouble(), exp.toDouble())
        val numberFormat = NumberFormat.getNumberInstance()
        numberFormat.maximumFractionDigits = 1

        return numberFormat.format(result) + " " + prefix + "B"
    }


    /* Reads InputStream from file uri and returns it as String */
    private fun readTextFile(context: Context, fileUri: Uri): String {
        // todo read https://commonsware.com/blog/2016/03/15/how-consume-content-uri.html
        // https://developer.android.com/training/secure-file-sharing/retrieve-info
        val file: File = fileUri.toFile()
        // check if file exists
        if (!file.exists()) {
            return String()
        }
        // read until last line reached
        val stream: InputStream = file.inputStream()
        val reader: BufferedReader = BufferedReader(InputStreamReader(stream))
        val builder: StringBuilder = StringBuilder()
        reader.forEachLine {
            builder.append(it)
            builder.append("\n") }
        stream.close()
        return builder.toString()
    }


    /* Writes given text to file on storage */
    private fun writeTextFile(text: String, fileUri: Uri) {
        if (text.isNotEmpty()) {
            val file: File = fileUri.toFile()
            file.writeText(text)
        } else {
            LogHelper.w(TAG, "Writing text file $fileUri failed. Empty text string text was provided.")
        }
    }


    /* Writes given bitmap as image file to storage */
    private fun writeImageFile(context: Context, bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 75) {
        if (file.exists()) file.delete ()
        try {
            val out = FileOutputStream(file)
            bitmap.compress(format, quality, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}