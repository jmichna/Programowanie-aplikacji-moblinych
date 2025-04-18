package pl.wsei.pam.lab03

import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import pl.wsei.pam.lab01.R
import java.util.Stack

class MemoryBoardView(
    private val gridLayout: GridLayout,
    private val cols: Int,
    private val rows: Int,
    private val savedTileResources: IntArray? = null,
    private val savedRevealedState: BooleanArray? = null,
    private val savedMatchedState: BooleanArray? = null
) {
    private val tileList = mutableListOf<Tile>()
    private val deckResource = R.drawable.baseline_deck_24

    private val icons = listOf(
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

    private val logic = MemoryGameLogic((cols * rows) / 2)
    private val matchedPair = Stack<Tile>()

    private var tileResources = IntArray(cols * rows)

    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = { }

    init {
        if (savedTileResources == null) {
            val neededPairs = (cols * rows) / 2
            val selected = icons.subList(0, neededPairs)
            val shuffled = (selected + selected).toMutableList()
            shuffled.shuffle()

            for (i in 0 until (cols * rows)) {
                tileResources[i] = shuffled[i]
            }
        } else {
            tileResources = savedTileResources.copyOf()
        }

        for (i in tileResources.indices) {
            val iconRes = tileResources[i]

            val button = ImageButton(gridLayout.context).also {
                val layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = 0
                    setGravity(Gravity.CENTER)
                    columnSpec = GridLayout.spec(i % cols, 1, 1f)
                    rowSpec = GridLayout.spec(i / cols, 1, 1f)
                }
                it.layoutParams = layoutParams
                it.setImageResource(deckResource)
                gridLayout.addView(it)

                it.setOnClickListener(::onClickTile)
            }

            val tile = Tile(button, iconRes, deckResource)
            tileList.add(tile)
        }

        if (savedRevealedState != null) {
            setRevealedState(savedRevealedState)
        }

        if (savedMatchedState != null) {
            setMatchedState(savedMatchedState)
        }
    }

    private fun onClickTile(view: View) {
        val tile = tileList.firstOrNull { it.button == view } ?: return

        matchedPair.push(tile)
        val result = logic.process { tile.tileResource }
        onGameChangeStateListener(MemoryGameEvent(matchedPair.toList(), result))

        if (result != GameStates.Matching) {
            matchedPair.clear()
        }
    }

    fun setOnGameChangeListener(listener: (MemoryGameEvent) -> Unit) {
        onGameChangeStateListener = listener
    }

    fun getTileResources(): IntArray {
        return tileResources.copyOf()
    }

    fun getRevealedState(): BooleanArray {
        val arr = BooleanArray(tileList.size)
        tileList.forEachIndexed { i, tile ->
            arr[i] = tile.revealed
        }
        return arr
    }

    fun getMatchedState(): BooleanArray {
        val arr = BooleanArray(tileList.size)
        tileList.forEachIndexed { i, tile ->
            arr[i] = tile.matched
        }
        return arr
    }

    fun setRevealedState(state: BooleanArray) {
        tileList.forEachIndexed { i, tile ->
            tile.revealed = state[i]
        }
    }

    fun setMatchedState(state: BooleanArray) {
        tileList.forEachIndexed { i, tile ->
            tile.matched = state[i]
            if (tile.matched) {
                tile.removeOnClickListener()
                tile.button.alpha = 0f
            }
        }
    }
}