package pl.wsei.pam.lab03

import android.animation.*
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.wsei.pam.lab01.R
import java.util.Random

class Lab03Activity : AppCompatActivity() {

    private lateinit var mBoard: GridLayout
    private lateinit var mBoardModel: MemoryBoardView

    private lateinit var completionPlayer: MediaPlayer
    private lateinit var negativePlayer: MediaPlayer

    private var isSound: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab03)

        mBoard = findViewById(R.id.main)

        val rows = intent.getIntExtra("rows", 3)
        val cols = intent.getIntExtra("columns", 4)
        mBoard.rowCount = rows
        mBoard.columnCount = cols

        if (savedInstanceState == null) {
            mBoardModel = MemoryBoardView(mBoard, cols, rows)
        } else {
            val savedTileOrder = savedInstanceState.getIntArray("tilesOrder")
            val savedRevealed = savedInstanceState.getBooleanArray("tilesRevealed")
            val savedMatched = savedInstanceState.getBooleanArray("tilesMatched")

            mBoardModel = MemoryBoardView(
                gridLayout = mBoard,
                cols = cols,
                rows = rows,
                savedTileResources = savedTileOrder,
                savedRevealedState = savedRevealed,
                savedMatchedState = savedMatched
            )
        }

        mBoardModel.setOnGameChangeListener { event ->
            when (event.state) {
                GameStates.Matching -> {
                    event.tiles.forEach { tile ->
                        tile.revealed = true
                    }
                }
                GameStates.Match -> {
                    if (isSound) completionPlayer.start()
                    event.tiles.forEach { tile ->
                        tile.revealed = true
                        tile.matched = true
                    }
                    event.tiles.forEach { tile ->
                        animatePairedButton(tile.button) {
                            tile.removeOnClickListener()
                        }
                    }
                }
                GameStates.NoMatch -> {
                    if (isSound) negativePlayer.start()

                    event.tiles.forEach { it.revealed = true }

                    event.tiles.forEach { tile ->
                        animateNoMatchButton(tile.button) {
                            tile.revealed = false
                        }
                    }
                }
                GameStates.Finished -> {
                    event.tiles.forEach { tile ->
                        tile.matched = true
                        animatePairedButton(tile.button) {
                            tile.removeOnClickListener()
                        }
                    }
                    Toast.makeText(this, "Game finished!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("tilesOrder", mBoardModel.getTileResources())
        outState.putBooleanArray("tilesRevealed", mBoardModel.getRevealedState())
        outState.putBooleanArray("tilesMatched", mBoardModel.getMatchedState())
    }

    private fun animatePairedButton(button: ImageButton, action: Runnable) {
        val set = AnimatorSet()
        val random = Random()
        button.pivotX = random.nextFloat() * button.width
        button.pivotY = random.nextFloat() * button.height

        val rotation = ObjectAnimator.ofFloat(button, "rotation", 0f, 1080f)
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 3f)
        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 3f)
        val fadeOut = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f)

        set.playTogether(rotation, scaleX, scaleY, fadeOut)
        set.duration = 1500
        set.interpolator = DecelerateInterpolator()

        set.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                button.scaleX = 1f
                button.scaleY = 1f
                button.alpha = 0f
                button.rotation = 0f
                action.run()
            }
        })

        set.start()
    }

    private fun animateNoMatchButton(button: ImageButton, action: Runnable) {
        val shake = AnimatorSet()
        val rotateLeft = ObjectAnimator.ofFloat(button, "rotation", 0f, -15f)
        val rotateRight = ObjectAnimator.ofFloat(button, "rotation", -15f, 15f)
        val rotateCenter = ObjectAnimator.ofFloat(button, "rotation", 15f, 0f)

        shake.playSequentially(rotateLeft, rotateRight, rotateCenter)
        shake.duration = 200
        shake.interpolator = DecelerateInterpolator()

        shake.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                button.rotation = 0f
                action.run()
            }
        })

        shake.start()
    }

    override fun onResume() {
        super.onResume()
        completionPlayer = MediaPlayer.create(applicationContext, R.raw.completion)
        negativePlayer = MediaPlayer.create(applicationContext, R.raw.negative_guitar)
    }

    override fun onPause() {
        super.onPause()
        completionPlayer.release()
        negativePlayer.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.board_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.board_activity_sound -> {
                if (item.icon?.constantState == getDrawable(R.drawable.baseline_volume_up_24)?.constantState) {
                    Toast.makeText(this, "Sound turned off", Toast.LENGTH_SHORT).show()
                    item.icon = getDrawable(R.drawable.baseline_volume_mute_24)
                    isSound = false
                } else {
                    Toast.makeText(this, "Sound turned on", Toast.LENGTH_SHORT).show()
                    item.icon = getDrawable(R.drawable.baseline_volume_up_24)
                    isSound = true
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}