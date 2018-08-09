package com.vedantroy.animefacekeyboard.keyboard

import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v13.view.inputmethod.EditorInfoCompat
import android.support.v13.view.inputmethod.InputConnectionCompat
import android.support.v13.view.inputmethod.InputContentInfoCompat
import android.support.v4.content.FileProvider
import android.util.Log
import android.view.inputmethod.EditorInfo
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.crashlytics.android.Crashlytics
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.BitmapFactory
import com.vedantroy.animefacekeyboard.R


class ContentCommitter(private val context: Context, private val localDirectory: File) {

    private var AUTHORITY : String = context.getString(R.string.content_provider_authority)
    private lateinit var supportedTypes: Array<out String>

    init {
        localDirectory.mkdirs()
    }

    fun supportedTypes(editorInfo: EditorInfo): Array<out String> {
        supportedTypes = EditorInfoCompat.getContentMimeTypes(editorInfo)
        return supportedTypes
    }

    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(context, AUTHORITY, file)
    }

    fun deleteAllStoredFiles() {
        FileUtils.cleanDirectory(localDirectory)
    }

    private fun commitGeneric(file: File, fileType: String) {

        //val contentURI = FileProvider.getUriForFile(context, AUTHORITY, file)
        val contentURI = getUriForFile(file)

        val editorInfo = (context as AnimeFaceKeyboard).currentInputEditorInfo
        val flag: Int

        if (Build.VERSION.SDK_INT >= 25) {
            flag = InputConnectionCompat.INPUT_CONTENT_GRANT_READ_URI_PERMISSION
        } else {
            flag = 0

            context.grantUriPermission(
                    editorInfo.packageName,
                    contentURI,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val inputConnection = context.currentInputConnection

        //No Description is Temporary
        val inputContentInfo = InputContentInfoCompat(contentURI, ClipDescription("No Description", arrayOf("image/$fileType")), null)

        InputConnectionCompat.commitContent(inputConnection, editorInfo, inputContentInfo, flag, null)

    }

    private fun findStaticImageFileType(): String {

        return if (supportedTypes.contains("image/png")) {
            "png"
        } else if (supportedTypes.contains("image/jpg") || supportedTypes.contains("image/jpeg")) {
            "jpeg"
        } else if (supportedTypes.contains("image/webp")) {
            "webp"
        } else {
            "png"
        }
    }

    fun commitBitmapDrawable(bitmapDrawable: BitmapDrawable, fileName: String) {
        val imageFile = File(localDirectory, fileName)

        val fileType = findStaticImageFileType()

        if (!imageFile.exists()) {
            val clonedBitmapDrawable = bitmapDrawable.constantState.newDrawable().mutate() as BitmapDrawable
            bitmapDrawableToFile(clonedBitmapDrawable, imageFile, fileType)
        }

        commitGeneric(imageFile, fileType)
    }

    private fun bitmapDrawableToFile(bitmapDrawable: BitmapDrawable, imageFile: File, fileType: String) {
        val outputStream = FileOutputStream(imageFile)

        when (fileType) {
            "png" -> {
                bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
            "jpeg" -> {
                bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.JPEG, 15, outputStream)
            }
            "webp" -> {
                bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.WEBP, 30, outputStream)
            }
            else -> {
                bitmapDrawable.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        }

        outputStream.close()
    }

    fun commitGifDrawable(gifDrawable: Drawable, fileName: String) {
        val gifFile = File(localDirectory, fileName)

        if (!gifFile.exists()) {
            val clonedGifDrawable = (gifDrawable.constantState.newDrawable().mutate()) as GifDrawable
            gifDrawableToFile(clonedGifDrawable, gifFile)
        }

        commitGeneric(gifFile, "gif")
    }

    private fun gifDrawableToFile(gifDrawable: GifDrawable, gifFile: File) {

        val byteBuffer = gifDrawable.buffer
        val output = FileOutputStream(gifFile)
        val bytes = ByteArray(byteBuffer.capacity())
        (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
        output.write(bytes, 0, bytes.size)
        output.close()
    }

    fun urlToName(string: String): String {
        //Space = untested regex change
        return string.replace(Regex("[- /:\\\\]"), "_")
    }


}