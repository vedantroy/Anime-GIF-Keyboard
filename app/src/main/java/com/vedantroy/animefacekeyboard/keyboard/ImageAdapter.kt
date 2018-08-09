package com.vedantroy.animefacekeyboard.keyboard

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.crashlytics.android.Crashlytics
import com.vedantroy.animefacekeyboard.R
import kotlinx.android.synthetic.main.imagerecycler_item_column.view.*
import android.graphics.Bitmap
import com.bumptech.glide.request.target.SimpleTarget
import android.R.attr.path
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.experimental.launch


class ImageAdapter(private val items: ArrayList<String>, private val context: Context) : RecyclerView.Adapter<ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(LayoutInflater.from(context).inflate(R.layout.imagerecycler_item_column, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {

        context as AnimeFaceKeyboard

        //holder.favoriteButton.setFavorite(context.favorites.contains(items[position]), false)
        holder.favoriteButton.isChecked = context.favorites.contains(items[position])

        holder.favoriteButton.setOnClickListener {
            if (holder.favoriteButton.isChecked) {
                Crashlytics.log("ImageAdapter|Adding Favorite")
                context.favorites.add(items[position])
            } else {
                Crashlytics.log("ImageAdapter|Deselecting Favorite")
                context.favorites.remove(items[position])

                if (context.isFavoritesTabSelected) {
                    Crashlytics.log("ImageAdapter|In Favorites Tab")
                    context.targetImageURLs.remove(items[position])


                    items.remove(items[position])
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, items.size)
                }

                context.updateFavoritesMessageView(items)
            }
            context.updateFavoritesFile()
        }

        //Figure out what these return statements actually do
        Glide.with(context)
                .load(items[position])
                .apply(RequestOptions().error(R.drawable.ic_baseline_error_outline_24px))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        holder.progressBar.visibility = View.GONE
                        holder.favoriteButton.visibility = View.GONE
                        //context.recursivelyLoadURLs()
                        return false
                    }

                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        holder.progressBar.visibility = View.GONE
                        holder.favoriteButton.visibility = View.VISIBLE
                        //context.recursivelyLoadURLs()
                        return false
                    }

                })
                .apply(RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC))
                .into(holder.imageView)

        //On Click Listener For Image is Always Same
        holder.imageView.setOnClickListener {

            val fileName = context.contentCommitter.urlToName(items[position])

            if (holder.imageView.drawable is GifDrawable) {
                Crashlytics.log("ImageAdapter|Committing GIF: $fileName")
                context.contentCommitter.commitGifDrawable(holder.imageView.drawable, fileName)
            } else {
                Crashlytics.log("ImageAdapter|Committing Non-GIF: $fileName")
                context.contentCommitter.commitBitmapDrawable(holder.imageView.drawable as BitmapDrawable, fileName)
            }
        }
    }
}

class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val imageView: ImageView = view.itemImage
    val progressBar = view.itemProgressBar
    val favoriteButton = view.itemFavoriteButton
}