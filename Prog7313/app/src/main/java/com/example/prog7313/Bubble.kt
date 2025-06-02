package com.example.prog7313

data class Bubble(var x: Float, var y: Float, val radius: Float) {
    fun contains(touchX: Float, touchY: Float): Boolean {
        val dx = touchX - x
        val dy = touchY - y
        return dx * dx + dy * dy <= radius * radius
    }
}
