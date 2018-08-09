package com.vedantroy.animefacekeyboard.keyboard

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.crashlytics.android.Crashlytics
import com.vedantroy.animefacekeyboard.R
import io.fabric.sdk.android.services.common.Crash
import kotlinx.android.synthetic.main.tabrecycler_item_column.view.*

class TabAdapter(private val items: ArrayList<Pair<String, ArrayList<String>>>, private val context: Context) : RecyclerView.Adapter<TabViewHolder>() {

    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var isInitialized : Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        return TabViewHolder(LayoutInflater.from(context).inflate(R.layout.tabrecycler_item_column, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {

        context as AnimeFaceKeyboard

        holder.itemView.isSelected = selectedPosition == position


        if (holder.itemView.isSelected) {
            //holder.itemView.tabButton.setTextColor(ContextCompat.getColor(context, R.color.teal))
            holder.button.setTextColor(ContextCompat.getColor(context, R.color.teal))
        } else {
            //holder.itemView.tabButton.setTextColor(ContextCompat.getColor(context, R.color.dark_text_color))
            holder.button.setTextColor(ContextCompat.getColor(context, R.color.dark_text_color))
        }

        val displayMetrics = Resources.getSystem().displayMetrics

        if (itemCount * (120 * displayMetrics.density) < displayMetrics.widthPixels) {
            holder.button.width = displayMetrics.widthPixels / itemCount
        }


        holder.button.text = items[position].first
        holder.button.setOnClickListener {
            Crashlytics.log("TabAdapter|onBindViewHolder()|Tab Clicked")

            //TODO - Figure out how this code works
            notifyItemChanged(selectedPosition)
            selectedPosition = holder.layoutPosition
            notifyItemChanged(selectedPosition)

            //previously last index
            context.isFavoritesTabSelected = position == 0
            context.updateCurrentImageLayout(items[position].second)
        }

        if(!isInitialized) {
            if(context.favorites.isEmpty()) {
                if(position == 1) {
                    holder.button.post {
                        holder.button.performClick()
                        isInitialized = true
                    }
                }
            } else {
                if (position == 0) {
                    holder.button.post {
                        holder.button.performClick()
                        isInitialized = true
                    }
                }
            }
        }


    }

}

class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val button: Button = view.tabButton

}
