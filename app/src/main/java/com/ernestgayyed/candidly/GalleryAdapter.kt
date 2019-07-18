package com.ernestgayyed.candidly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class GalleryAdapter(val context: Context, val galleryAdapterListener: GalleryAdapterListener) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    var imageList = listOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.view_gallery_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageItem = holder.itemView.findViewById<ImageView>(R.id.img_item)

        Glide.with(context).load(imageList[position])
            .placeholder(R.mipmap.ic_launcher).centerCrop()
            .into(imageItem)

        imageItem.setOnClickListener {
            galleryAdapterListener.onItemClicked(imageList[position])
        }

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}