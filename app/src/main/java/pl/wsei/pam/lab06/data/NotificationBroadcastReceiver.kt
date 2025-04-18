package pl.wsei.pam.lab06.data

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import pl.wsei.pam.lab01.R
import pl.wsei.pam.lab06.Lab06Activity
import pl.wsei.pam.lab06.channelID
import pl.wsei.pam.lab06.data.LocalDateConverter.Companion.toEpochMillis
import pl.wsei.pam.lab06.messageExtra
import pl.wsei.pam.lab06.notificationID
import pl.wsei.pam.lab06.titleExtra

class NotificationBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent?) {
        val title   = intent?.getStringExtra(titleExtra) ?: return
        val message = intent.getStringExtra(messageExtra) ?: ""

        // 1) Pokaż powiadomienie
        ctx.showNotification(title, message)         // <- własna funkcja extension

        // 2) Zaplanuj kolejny alarm jeśli termin zadania ciągle nie minął
        val repo = (ctx.applicationContext as TodoApplication)
            .container.todoTaskRepository

        GlobalScope.launch {      // prosty „fire‑and‑forget”
            val task = repo.getItemByTitleOnce(title)     // napisz drobną funkcję DAO
            if (task != null && !task.isDone) {
                val next = System.currentTimeMillis() + 4.hoursToMillis()
                if (next < task.deadline.toEpochMillis()) {
                    (ctx as Lab06Activity).scheduleExactAlarm(next, title)
                }
            }
        }
    }
}

fun Context.showNotification(title: String, message: String) {
    val notification = NotificationCompat.Builder(this, channelID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)   // lub własna ikonka
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    val mgr = getSystemService(NotificationManager::class.java)
    // używamy stałego ID z MainActivity, ale można też hash tytułu:
    mgr.notify(notificationID, notification)
}

fun Int.hoursToMillis(): Long = this * 60L * 60 * 1_000
