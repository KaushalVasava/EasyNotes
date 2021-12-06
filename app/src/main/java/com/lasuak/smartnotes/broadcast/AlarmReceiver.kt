package com.lasuak.smartnotes.broadcast

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lasuak.smartnotes.R
import com.lasuak.smartnotes.activity.HomeActivity
import com.lasuak.smartnotes.ui.activities.MainActivity
import com.lasuak.smartnotes.ui.activities.MainActivity.Companion.CHANNEL_ID

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        val textTitle: String = intent!!.getStringExtra("noteTitle").toString()

        val intent2 = Intent(context, HomeActivity::class.java)
        intent2.putExtra("Title", textTitle)

        intent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.sendBroadcast(intent2);

        val pendingIntent = PendingIntent.getActivity(context,12, intent2,0)

        val notification = NotificationCompat.Builder(context,CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reminder)
            .setContentTitle(textTitle)
            .setContentText(textTitle)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        val notificationCompat = NotificationManagerCompat.from(context)
        notificationCompat.notify(1,notification)

        Toast.makeText(context, "Alarm received! $textTitle", Toast.LENGTH_LONG).show()
    }
}