package com.erastusnzula.game_matchpairs.models

enum class BoardSize(val totalItems: Int) {
    EASY(8),
    MEDIUM(18),
    HARD(24),
    ADVANCED(40);

    companion object{
        fun getByValue(value:Int): BoardSize {
            return values().first{
                it.totalItems==value
            }
        }
    }

    fun getColumns(): Int {
        return when (this) {
            EASY -> 2
            MEDIUM -> 3
            HARD -> 4
            ADVANCED -> 5
        }
    }

    fun getRows(): Int {
        return totalItems / getColumns()
    }

    fun getPairs(): Int {
        return totalItems / 2
    }
}