package com.erastusnzula.game_matchpairs.models

data class SingleCard(
    val identifier : Int,
    val imageUrl:String?=null,
    var isFaceUp: Boolean=false,
    var isMatched: Boolean=false
)
