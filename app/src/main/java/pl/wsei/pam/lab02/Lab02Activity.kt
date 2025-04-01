package pl.wsei.pam.lab02

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.wsei.pam.lab01.R
import pl.wsei.pam.lab03.Lab03Activity

class Lab02Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lab02)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun onBoardSizeClick(view: View) {
        val tagValue = view.tag as? String ?: return
        val tokens = tagValue.split(" ")
        if (tokens.size == 2) {
            val rows = tokens[0].toIntOrNull() ?: return
            val columns = tokens[1].toIntOrNull() ?: return

            val intent = Intent(this, Lab03Activity::class.java)
            intent.putExtra("rows", rows)
            intent.putExtra("columns", columns)

            startActivity(intent)
        }
    }
}