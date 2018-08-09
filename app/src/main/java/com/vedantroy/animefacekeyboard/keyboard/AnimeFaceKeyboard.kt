package com.vedantroy.animefacekeyboard.keyboard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.net.ConnectivityManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import com.crashlytics.android.Crashlytics
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.vedantroy.animefacekeyboard.R
import com.yqritc.recyclerviewflexibledivider.HorizontalDividerItemDecoration
import com.yqritc.recyclerviewflexibledivider.VerticalDividerItemDecoration
import kotlinx.android.synthetic.main.keyboardmain.view.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import kotlin.coroutines.experimental.suspendCoroutine


class AnimeFaceKeyboard : InputMethodService() {

    val targetImageURLs: ArrayList<String> = ArrayList()
    private val loadedImageURLs: ArrayList<String> = ArrayList()

    private val tabs: ArrayList<Pair<String, ArrayList<String>>> = ArrayList()

    private val warningMessages = mutableListOf<String>()

    private lateinit var mainview: View

    private val Gson = Gson()
    private val stringArrayListType = object : TypeToken<ArrayList<String>>() {

    }.type

    private lateinit var tabAdapter: TabAdapter
    var isFavoritesTabSelected: Boolean = false
    var favorites = ArrayList<String>()
    private lateinit var favoritesData: File

    lateinit var contentCommitter: ContentCommitter
    private lateinit var supportedTypes: Array<out String>

    private val DATABASE_LOCATION = "https://github.com/vedantroy/Image-Database/raw/master/"

    private val networkReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            val activeNetworkInfo = (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo

            if (activeNetworkInfo != null) {
                if (activeNetworkInfo.isConnectedOrConnecting) {
                    launch {
                        try {
                            val connectionStatus = fetchUrlAsString(DATABASE_LOCATION + "Utilities/connectionmonitor.txt")
                            if (connectionStatus == "Connected To Image Database") {
                                hideConnectionWarning()
                            } else {
                                showConnectionWarning()
                            }
                        } catch (e: Exception) {
                            showConnectionWarning()
                        }
                    }
                } else {
                    showConnectionWarning()
                }
            } else {
                showConnectionWarning()
            }
        }
    }

    private fun showConnectionWarning() {
        Crashlytics.setBool("Internet Connection", true)

        if (!warningMessages.contains(getString(R.string.warning_message_no_database_connection))) {
            warningMessages.add(getString(R.string.warning_message_no_database_connection))
        }

        refreshRecyclerView(mainview.warningRecyclerView)
    }

    private fun hideConnectionWarning() {
        Crashlytics.setBool("Internet Connection", false)

        warningMessages.remove(getString(R.string.warning_message_no_database_connection))

        refreshRecyclerView(mainview.warningRecyclerView)
    }

    override fun onCreateInputView(): View {
        super.onCreateInputView()
        Crashlytics.log("AnimeFaceKeyboard|onCreateInputView()")

        contentCommitter = ContentCommitter(this, File(filesDir, "images"))

        mainview = layoutInflater.inflate(R.layout.keyboardmain, null)

        mainview.imageRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mainview.imageRecyclerView.adapter = ImageAdapter(loadedImageURLs, this)


        mainview.tabRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        mainview.tabRecyclerView.addItemDecoration(
                VerticalDividerItemDecoration.Builder(this)
                        .margin(22, 22)
                        .build())


        /*
        mainview.tabRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView?, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    mainview.tabRecyclerView.isScrollbarFadingEnabled = true
                }
            }
        })
        */

        mainview.warningRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        mainview.warningRecyclerView.adapter = WarningAdapter(warningMessages, this)

        mainview.warningRecyclerView.addItemDecoration(
                HorizontalDividerItemDecoration.Builder(this)
                        .margin(50, 50)
                        .build()
        )

        val parser = JsonParser()

        val keyboardData = File(filesDir, "keyboard_data.json")

        if (!keyboardData.exists()) {
            resources.openRawResource(R.raw.keyboard_data).copyTo(FileOutputStream(keyboardData))
        }

        favoritesData = File(filesDir, "favorites_data.json")

        if (favoritesData.exists()) {
            favorites = Gson.fromJson(parser.parse(FileReader(favoritesData)), stringArrayListType)
        }

        updateGlobalKeyboardDataSet(parser.parse(FileReader(keyboardData)).asJsonObject.entrySet())

        tabAdapter = TabAdapter(tabs, this)
        mainview.tabRecyclerView.adapter = tabAdapter

        //refreshRecyclerView(mainview.tabRecyclerView)

        launch(UI) {
            try {
                val globalVersion = fetchUrlAsString(DATABASE_LOCATION + "Curated/version.txt").toFloat()

                hideConnectionWarning()

                val sharedPrefs = getSharedPreferences("com.vedantroy.animefacekeyboard.preferences", Context.MODE_PRIVATE)
                val localVersion = sharedPrefs.getFloat("version", 0.1f)
                Crashlytics.setFloat("layout_version",localVersion)

                if (globalVersion > localVersion) {
                    Crashlytics.setFloat("layout_version",globalVersion)
                    Crashlytics.log("AnimeFaceKeyboard|onCreateInputView()|Updating Version")

                    val newLayout = fetchUrlAsString(DATABASE_LOCATION + "Curated/index.json")

                    updateGlobalKeyboardDataSet(parser.parse(newLayout).asJsonObject.entrySet())

                    keyboardData.writeText(newLayout)

                    refreshRecyclerView(mainview.tabRecyclerView)

                    sharedPrefs.edit().putFloat("version", globalVersion).apply()
                }
            } catch (e: Exception) {
                showConnectionWarning()
            }
        }
        return mainview
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        Crashlytics.log("AnimeFaceKeyboard|onStartInputView()")

        val filter = IntentFilter()
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(networkReceiver, filter)


        val connectionWarning = getString(R.string.warning_message_no_database_connection)

        val iterator = warningMessages.listIterator()
        while (iterator.hasNext()) {
            val warningMessage = iterator.next()
            if (warningMessage != connectionWarning) {
                iterator.remove()
            }
        }


        supportedTypes = if (info != null) {
            contentCommitter.supportedTypes(info)
        } else {
            arrayOf("")
        }

        val gifSupport = supportedTypes.contains("image/gif")
        val pngSupport = supportedTypes.contains("image/png")
        val jpegSupport = supportedTypes.contains("image/jpg") || supportedTypes.contains("image/jpeg")
        val webpSupport = supportedTypes.contains("image/webp")

        val staticImageSupport = pngSupport || jpegSupport || webpSupport

        val warningString = if (!gifSupport && !staticImageSupport) {
            getString(R.string.warning_message_two_no_app_name)
        } else if (!gifSupport) {
            getString(R.string.warning_message_one_no_app_name, getString(R.string.GIF))
        } else if (!staticImageSupport) {
            getString(R.string.warning_message_one_no_app_name, getString(R.string.static_image))
        } else {
            "Status-No-Warning"
        }

        if (warningString != "Status-No-Warning") {
            if (!warningMessages.contains(warningString)) {
                warningMessages.add(warningString)
                //mainview.warningRecyclerView.adapter.notifyItemInserted(warningMessages.size - 1)
                refreshRecyclerView(mainview.warningRecyclerView)
            }
        }

    }

    override fun onFinishInputView(finishingInput: Boolean) {
        unregisterReceiver(networkReceiver)
        super.onFinishInputView(finishingInput)
    }

    private suspend fun fetchUrlAsString(url: String): String = suspendCoroutine { cont ->
        url.httpGet().header(Pair("pragma", "no-cache"), Pair("cache-control", "no-cache")).responseString { _, _, result ->

            Crashlytics.log("AnimeFaceKeyboard|fetchUrlAsString(): " + url.removePrefix(DATABASE_LOCATION))

            when (result) {
                is Result.Failure -> {
                    cont.resumeWithException(result.getException())
                }
                is Result.Success -> {
                    cont.resume(result.value)
                }
            }

        }
    }

    fun updateFavoritesFile() {
        favoritesData.writeText(Gson.toJson(favorites))
    }

    private fun refreshRecyclerView(recyclerView: RecyclerView) {
        recyclerView.post {
            recyclerView.adapter.notifyDataSetChanged()
        }
    }

    private fun updateGlobalKeyboardDataSet(layoutData: Set<Map.Entry<String, JsonElement>>) {
        Crashlytics.log("AnimeFaceKeyboard|updateGlobalKeyboardDataSet()")

        tabs.clear()

        tabs.add(Pair(getString(R.string.favorites_tab_title), favorites))

        for (entry: Map.Entry<String, JsonElement> in layoutData) {
            tabs.add(Pair(entry.key, Gson.fromJson(entry.value, stringArrayListType)))
        }

    }

    fun updateCurrentImageLayout(imageURLList: ArrayList<String>) {
        Crashlytics.log("AnimeFaceKeyboard|updateCurrentImageLayout()")

        targetImageURLs.clear()
        loadedImageURLs.clear()

        contentCommitter.deleteAllStoredFiles()

        updateFavoritesMessageView(imageURLList)

        //Without copying the list the second value in Pair<String, ArrayList<String>>, which is
        // part of ArrayList "tabs" will get changed, thus modifying the global layout.
        val copiedList = ArrayList<String>()
        copiedList.addAll(imageURLList)



        //Need to copy a list
        val iterator = copiedList.listIterator()
        while (iterator.hasNext()) {
            val imageURL = iterator.next()
            if (imageURL.endsWith(".gif",true)) {
                targetImageURLs.add(imageURL)
                iterator.remove()
            }
        }

        targetImageURLs.addAll(copiedList)

        loadedImageURLs.addAll(targetImageURLs)
        refreshRecyclerView(mainview.imageRecyclerView)

        //recursivelyLoadURLs()
    }

    fun recursivelyLoadURLs() {

        if (loadedImageURLs.size >= targetImageURLs.size) {
            return
        }

        loadedImageURLs.add(targetImageURLs[loadedImageURLs.size])

        Log.d("VED-APP","Loaded image url size: " + loadedImageURLs.size)

        mainview.imageRecyclerView.adapter.notifyItemInserted(loadedImageURLs.size - 1)

        //refreshRecyclerView(mainview.imageRecyclerView)
    }

    fun updateFavoritesMessageView(imageURLList: ArrayList<String>) {
        if (isFavoritesTabSelected && imageURLList.size == 0) {
            mainview.add_favorites_message.visibility = View.VISIBLE
        } else {
            mainview.add_favorites_message.visibility = View.GONE
        }
    }

}