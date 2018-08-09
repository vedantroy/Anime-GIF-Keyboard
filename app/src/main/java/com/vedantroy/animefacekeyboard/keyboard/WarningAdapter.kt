package com.vedantroy.animefacekeyboard.keyboard

import android.content.Context
import android.support.v7.widget.AppCompatTextView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.vedantroy.animefacekeyboard.R
import kotlinx.android.synthetic.main.warningrecycler_item_row.view.*

class WarningAdapter(private val items: MutableList<String>, private val context: Context) : RecyclerView.Adapter<WarningMessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningMessageViewHolder {
        return WarningMessageViewHolder(LayoutInflater.from(context).inflate(R.layout.warningrecycler_item_row, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: WarningMessageViewHolder, position: Int) {
        holder.warningMessage.text = items[position]
    }

}

class WarningMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val warningMessage: AppCompatTextView = view.warningMessage
}
