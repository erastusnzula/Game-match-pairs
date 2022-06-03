package com.erastusnzula.game_matchpairs

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.erastusnzula.game_matchpairs.models.BoardSize
import kotlin.math.min

class NewGameAdapter(
    private val context: Context,
    private val chosenImagesListUri: List<Uri>,
    private val boardSize: BoardSize,
    private val imageViewClicked: ImageViewClicked
) :
    RecyclerView.Adapter<NewGameAdapter.ViewHolder>() {
    companion object {
        const val margin = 10
    }

    interface ImageViewClicked{
       fun onImageViewClick()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val width = parent.width / boardSize.getColumns() - (2 * margin)
        val height = parent.height / boardSize.getRows() - (2 * margin)
        val size = min(width, height)
        val view = LayoutInflater.from(context).inflate(R.layout.new_game, parent, false)
        val imageView = view.findViewById<ImageView>(R.id.imageViewUpload)
        val params = imageView.layoutParams as ViewGroup.MarginLayoutParams
        params.width = size
        params.height = size
        params.setMargins(margin, margin, margin, margin)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < chosenImagesListUri.size) {
            holder.bind(chosenImagesListUri[position])
        } else {
            holder.bind()
        }
    }

    override fun getItemCount(): Int {
        return boardSize.getPairs()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.imageViewUpload)
        fun bind(uri: Uri) {
            imageView.setImageURI(uri)
            imageView.setOnClickListener(null)
        }

        fun bind() {
            imageView.setOnClickListener {
                imageViewClicked.onImageViewClick()

            }

        }
    }

}
