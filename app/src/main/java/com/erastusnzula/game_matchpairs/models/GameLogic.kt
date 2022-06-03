package com.erastusnzula.game_matchpairs.models

import com.erastusnzula.game_matchpairs.utils.DEFAULT_ICONS

class GameLogic(private val boardSize: BoardSize, private val customGameImages: List<String>?) {

    var pairsFound = 0
    var numberOfFlippedCards = 0
    var clickedCardIndex: Int? = null

    val imageListDoubled: List<SingleCard> = if (customGameImages == null){
        val imagesList = DEFAULT_ICONS.shuffled().take(boardSize.getPairs())
        (imagesList + imagesList).shuffled().map { SingleCard(it) }
    }else{
        val customImages = (customGameImages+customGameImages).shuffled()
        customImages.map { SingleCard(it.hashCode(), it) }
    }


    fun flipCard(position: Int): Boolean {
        numberOfFlippedCards++
        var matchFound = false
        val cardImage = imageListDoubled[position]
        if (clickedCardIndex == null) {
            restoreCards(position)

        } else {
            matchFound = checkForMatch(clickedCardIndex!!, position)
        }
        cardImage.isFaceUp = !cardImage.isFaceUp
        return matchFound

    }

    private fun checkForMatch(clickedCardIndexPos: Int, position: Int): Boolean {
        return if (imageListDoubled[clickedCardIndexPos].identifier != imageListDoubled[position].identifier) {
            restoreCards(position)
            false
        } else {
            imageListDoubled[clickedCardIndexPos].isMatched = true
            imageListDoubled[position].isMatched = true
            pairsFound++
            clickedCardIndex = null
            true
        }


    }

    private fun restoreCards(position: Int) {
        for (imageCard in imageListDoubled) {
            if (!imageCard.isMatched) {
                imageCard.isFaceUp = false
                clickedCardIndex = position
            }

        }
    }

    fun gameOver(): Boolean {
        return pairsFound == boardSize.getPairs()
    }

    fun cardIsFaceUp(position: Int): Boolean {
        return imageListDoubled[position].isFaceUp
    }

    fun getNumberMoves(): Int {
       return numberOfFlippedCards/2
    }

}