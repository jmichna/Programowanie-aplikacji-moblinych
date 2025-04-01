package pl.wsei.pam.lab03

import MemoryGameEvent
import MemoryGameLogic
import Tile
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import java.util.Stack
import pl.wsei.pam.lab01.R

class MemoryBoardView(
    private val gridLayout: GridLayout,
    private val cols: Int,
    private val rows: Int
) {
    private val tiles: MutableMap<String, Tile> = mutableMapOf()

    private val icons: List<Int> = listOf(
        R.drawable.baseline_rocket_24,
        R.drawable.baseline_audiotrack_24,
        R.drawable.baseline_launch_24,
        R.drawable.baseline_360_24,
        R.drawable.baseline_add_24,
        R.drawable.baseline_airplane_ticket_24,
        R.drawable.baseline_airplay_24,
        R.drawable.baseline_announcement_24,
        R.drawable.baseline_arrow_drop_down_circle_24,
        R.drawable.baseline_assistant_navigation_24,
        R.drawable.baseline_back_hand_24,
        R.drawable.baseline_batch_prediction_24,
        R.drawable.baseline_battery_0_bar_24,
        R.drawable.baseline_bike_scooter_24,
        R.drawable.baseline_call_end_24,
        R.drawable.baseline_cast_24,
        R.drawable.baseline_check_box_24,
        R.drawable.baseline_church_24

    )

    private val deckResource: Int = R.drawable.baseline_deck_24

    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = {}

    private val matchedPair: Stack<Tile> = Stack()

    private val logic: MemoryGameLogic = MemoryGameLogic((rows * cols) / 2)

    init {
        val neededPairs = (rows * cols) / 2

        if (neededPairs > icons.size) {
            throw IllegalStateException(
                "Potrzeba co najmniej $neededPairs ikon, a mamy tylko ${icons.size}"
            )
        }

        val selectedIcons = icons.subList(0, neededPairs).toMutableList()

        val allCards = selectedIcons + selectedIcons

        var index = 0
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val button = ImageButton(gridLayout.context).also {
                    it.tag = "$i x $j"

                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = 0
                        setGravity(Gravity.CENTER)
                        columnSpec = GridLayout.spec(j, 1, 1f)
                        rowSpec = GridLayout.spec(i, 1, 1f)
                    }
                    it.layoutParams = params

                    it.setOnClickListener(::onClickTile)
                }

                gridLayout.addView(button)

                val tileResource = allCards[index]
                val tile = Tile(button, tileResource, deckResource)
                tiles[button.tag.toString()] = tile

                index++
            }
        }
    }

    private fun onClickTile(v: View) {
        val tile = tiles[v.tag.toString()] ?: return
        matchedPair.push(tile)

        val matchResult = logic.process {
            tile.tileResource
        }

        onGameChangeStateListener(MemoryGameEvent(matchedPair.toList(), matchResult))

        if (matchResult != GameStates.Matching) {
            matchedPair.clear()
        }
    }

    fun setOnGameChangeListener(listener: (MemoryGameEvent) -> Unit) {
        onGameChangeStateListener = listener
    }

    fun getState(): IntArray {
        val total = rows * cols
        val array = IntArray(total) { -1 }
        var index = 0

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = tiles["$i x $j"]
                if (tile?.revealed == true) {
                    array[index] = tile.tileResource
                }
                index++
            }
        }
        return array
    }

    fun setState(state: IntArray) {
        if (state.size != rows * cols) return
        var idx = 0

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tile = tiles["$i x $j"] ?: continue
                val resource = state[idx]
                tile.revealed = (resource >= 0)
                idx++
            }
        }
    }
}