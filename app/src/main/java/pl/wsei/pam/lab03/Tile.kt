package pl.wsei.pam.lab03

import android.widget.ImageButton

data class Tile(
    val button: ImageButton,
    val tileResource: Int,
    val deckResource: Int,
    var matched: Boolean = false
) {
    private var _revealed: Boolean = false
    var revealed: Boolean
        get() = _revealed
        set(value) {
            _revealed = value
            if (!_revealed) {
                button.setImageResource(deckResource)
            } else {
                button.setImageResource(tileResource)
            }
        }

    fun removeOnClickListener() {
        button.setOnClickListener(null)
    }
}