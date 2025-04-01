package pl.wsei.pam.lab03

import android.os.Bundle
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import pl.wsei.pam.lab01.R
import kotlin.concurrent.schedule

class Lab03Activity : AppCompatActivity() {

    private lateinit var mBoardModel: MemoryBoardView
    private var rows: Int = 3
    private var columns: Int = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lab03)

        rows = intent.getIntExtra("rows", 3)
        columns = intent.getIntExtra("columns", 3)

        initBoard()
    }

    private fun initBoard() {
        val boardLayout = findViewById<GridLayout>(R.id.main)

        boardLayout.rowCount = rows
        boardLayout.columnCount = columns

        mBoardModel = MemoryBoardView(boardLayout, columns, rows)

        val savedState = loadBoardState(rows, columns)
        if (savedState != null) {
            mBoardModel.setState(savedState)
        }

        mBoardModel.setOnGameChangeListener { event ->
            when (event.state) {
                GameStates.Matching -> {
                    event.tiles.forEach { it.revealed = true }
                }
                GameStates.Match -> {
                    event.tiles.forEach { it.revealed = true }
                }
                GameStates.NoMatch -> {
                    event.tiles.forEach { it.revealed = true }
                    java.util.Timer().schedule(2000) {
                        runOnUiThread {
                            event.tiles.forEach { it.revealed = false }
                        }
                    }
                }
                GameStates.Finished -> {
                    Toast.makeText(this, "Game finished", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val currentState = mBoardModel.getState()
        saveBoardState(rows, columns, currentState)
    }

    private fun saveBoardState(rows: Int, columns: Int, state: IntArray) {
        val sp = getSharedPreferences("MemoryState", MODE_PRIVATE)
        val key = "${rows}x${columns}"
        val arrayString = state.joinToString(",")
        sp.edit()
            .putString(key, arrayString)
            .apply()
    }

    private fun loadBoardState(rows: Int, columns: Int): IntArray? {
        val sp = getSharedPreferences("MemoryState", MODE_PRIVATE)
        val key = "${rows}x${columns}"
        val arrayString = sp.getString(key, null) ?: return null
        return arrayString.split(",").mapNotNull { it.toIntOrNull() }.toIntArray()
    }
}