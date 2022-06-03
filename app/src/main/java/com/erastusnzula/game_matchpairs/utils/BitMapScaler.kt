package com.erastusnzula.game_matchpairs.utils

import android.graphics.Bitmap

object BitMapScaler {
    fun scaleToFitHeight(bitmap:Bitmap, height:Int): Bitmap {
        val factor = height/bitmap.height.toFloat()
        return Bitmap.createScaledBitmap(bitmap,(bitmap.width*factor).toInt(),height, true)
    }

    fun scaleToFitWidth(bitmap:Bitmap, width:Int): Bitmap {
        val factor = width/bitmap.width.toFloat()
        return Bitmap.createScaledBitmap(bitmap,width,(bitmap.width*factor).toInt(), true)
    }

}
