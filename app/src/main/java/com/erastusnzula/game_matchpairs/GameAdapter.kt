package com.erastusnzula.game_matchpairs

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.erastusnzula.game_matchpairs.models.BoardSize
import com.erastusnzula.game_matchpairs.models.SingleCard
import com.squareup.picasso.Picasso
import kotlin.math.min

class GameAdapter(
    private val context: Context,
    private val boardSize: BoardSize,
    private val imageListDoubled: List<SingleCard>,
    private val imageButtonClicked: ImageButtonClickListener
) :
    RecyclerView.Adapter<GameAdapter.ViewHolder>() {
    companion object {
        const val margin = 10
    }

    interface ImageButtonClickListener {
        fun onImageButtonClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val width = parent.width / boardSize.getColumns() - (2 * margin)
        val height = parent.height / boardSize.getRows() - (2 * margin)
        val size = min(width, height)
        val view = LayoutInflater.from(context).inflate(R.layout.single_card, parent, false)
        val card = view.findViewById<CardView>(R.id.cardview)
        val params = card.layoutParams as ViewGroup.MarginLayoutParams
        params.width = size
        params.height = height
        params.setMargins(margin, margin, margin, margin)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return boardSize.totalItems
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)
        fun bind(position: Int) {
            val card = imageListDoubled[position]
            if (card.isFaceUp){
                if (card.imageUrl !=null){
                    Picasso.get().load(card.imageUrl).placeholder(R.drawable.ic_placeholder).into(imageButton)
                }else{
                    imageButton.setImageResource(card.identifier)
                }
            }else{
                imageButton.setImageResource(R.drawable.ic_launcher_background)
            }

            imageButton.alpha = if (card.isMatched) 0.4f else 1.0f
            val colorStateList = if (card.isMatched) ContextCompat.getColorStateList(
                context,
                R.color.color_gray
            ) else null
            ViewCompat.setBackgroundTintList(imageButton, colorStateList)
            imageButton.setOnClickListener {
                imageButtonClicked.onImageButtonClicked(position)
            }

        }
    }

}
