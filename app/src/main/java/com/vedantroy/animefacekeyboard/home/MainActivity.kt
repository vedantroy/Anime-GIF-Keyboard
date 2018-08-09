package com.vedantroy.animefacekeyboard.home

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.inputmethod.InputMethodManager
import com.vedantroy.animefacekeyboard.R
import com.vedantroy.animefacekeyboard.keyboard.ContentCommitter
import kotlinx.android.synthetic.main.activity_main.*
import android.support.annotation.RawRes
import com.crashlytics.android.Crashlytics
import java.io.*
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.text.Html
import android.widget.Toast


class MainActivity : AppCompatActivity() {


    private lateinit var imagesDir: File
    private lateinit var contentCommitter: ContentCommitter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imagesDir = File(filesDir, "images")
        contentCommitter = ContentCommitter(this, imagesDir)

        useKeyboardButton.setOnClickListener {

            val samsungRegex = Regex("samsung",RegexOption.IGNORE_CASE)

            if(android.os.Build.MODEL.contains(samsungRegex) || android.os.Build.MANUFACTURER.contains(samsungRegex)) {
                //Check if Galaxy S8/S9
                if(android.os.Build.MODEL.contains(Regex("S[8-9]"))) {
                    displayImage(R.raw.bottom_right_corner_example)
                } else {
                    displayImage(R.raw.notification_bar_example)
                }
            } else {
                displayImage(R.raw.bottom_right_corner_example)
            }
        }

        privacyPolicyButton.setOnClickListener {

            AlertDialog.Builder(this)
                    .setTitle(getString(R.string.privacy_policy_button_title))
                    .setMessage(Html.fromHtml(getString(R.string.privacy_policy_text))
                    )
                    .setNeutralButton(getString(R.string.OK)) { _: DialogInterface?, _: Int ->

                    }
                    .show()
        }

        enableKeyboardButton.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
        }


        rateMeButton.setOnClickListener {
            AlertDialog.Builder(this)
                    .setTitle("Feedback")
                    .setMessage("Are you enjoying this app?")
                    .setNegativeButton(getString(R.string.NO)) {_, _ ->
                        showFeedbackDialog()
                    }
                    .setNeutralButton(getString(R.string.NOT_SURE)) { _: DialogInterface?, _: Int ->
                        showFeedbackDialog()
                    }
                    .setPositiveButton(getString(R.string.YES)) {_,_ ->
                        rateMeDialog()
                    }
                    .show()

        }

    }

    private fun showFeedbackDialog() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.give_feedback))
                .setMessage(getString(R.string.provide_feedback_question))
                .setNegativeButton(getString(R.string.NO)) { _, _ ->
                }.setPositiveButton(getString(R.string.YES)) { _, _ ->
                    sendEmail()
                }.show()
    }

    private fun rateMeDialog() {
        AlertDialog.Builder(this)
                .setTitle(getString(R.string.rate_app))
                .setMessage(getString(R.string.rate_app_question))
                .setNegativeButton(getString(R.string.NO)) {_,_ ->

                }.setPositiveButton(getString(R.string.YES)) {_, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + this.packageName)))
                    } catch (e: android.content.ActivityNotFoundException) {
                        startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + this.packageName)))
                    }

                }.show()

    }

    private fun sendEmail() {
        val emailIntentPrimary = Intent(Intent.ACTION_SEND)
        emailIntentPrimary.apply {
            type = "*/*"
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(R.string.destination_email_address)))
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.email_placeholder_msg))
        }

        if (intentSafetyCheck(emailIntentPrimary)) {
            startActivity(emailIntentPrimary)
        } else {
            val destination = getString(R.string.destination_email_address)
            val subject = getString(R.string.app_name)
            val body = getString(R.string.email_placeholder_msg)
            val mailTo = "mailto:" + destination +
                    "?&subject=" + Uri.encode(subject) +
                    "&body=" + Uri.encode(body)
            val emailIntentSecondary = Intent(Intent.ACTION_VIEW)
            emailIntentSecondary.data = Uri.parse(mailTo)
            if (intentSafetyCheck(emailIntentSecondary)) {
                startActivity(emailIntentSecondary)
            } else {
                Crashlytics.log("MainActivity|Contact Me|Failed to Send Email")
                Toast.makeText(this@MainActivity, getString(R.string.email_error_msg), Toast.LENGTH_LONG).show()
            }
        }
    }


    override fun onResume() {
        super.onResume()



        //Check if keyboard is enabled
        val isKeyboardEnabled = (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).enabledInputMethodList.toString().contains("com.vedantroy.animefacekeyboard")

        keyboardStatusIndicator.isEnabled = false
        keyboardStatusIndicator.isClickable = false

        if (isKeyboardEnabled) {
            enableKeyboardButton.text = getString(R.string.keyboard_enabled_msg)
            keyboardStatusIndicator.isChecked = true
        } else {
            enableKeyboardButton.text = getString(R.string.enable_keyboard_msg)
            keyboardStatusIndicator.isChecked = false
        }
    }



    private fun displayImage(@RawRes res: Int) {
        val image = getFileForResource(this, res, imagesDir, "How to Switch to the Keyboard.png")
        image?.let {
            val launchImageIntent = Intent(Intent.ACTION_VIEW).setDataAndType(contentCommitter.getUriForFile(it), "image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(launchImageIntent)
        }
    }

    private fun intentSafetyCheck(intent: Intent): Boolean {
        return intent.resolveActivity(packageManager) != null
    }

    companion object {


        private fun getFileForResource(
                context: Context, @RawRes res: Int, outputDir: File,
                filename: String): File? {
            val outputFile = File(outputDir, filename)
            val buffer = ByteArray(4096)
            var resourceReader: InputStream? = null
            try {
                try {
                    resourceReader = context.resources.openRawResource(res)
                    var dataWriter: OutputStream? = null
                    try {
                        dataWriter = FileOutputStream(outputFile)
                        while (true) {
                            val numRead = resourceReader!!.read(buffer)
                            if (numRead <= 0) {
                                break
                            }
                            dataWriter.write(buffer, 0, numRead)
                        }
                        return outputFile
                    } finally {
                        if (dataWriter != null) {
                            dataWriter.flush()
                            dataWriter.close()
                        }
                    }
                } finally {
                    resourceReader?.close()
                }
            } catch (e: IOException) {
                Crashlytics.logException(e)
                return null
            }

        }
    }
}
