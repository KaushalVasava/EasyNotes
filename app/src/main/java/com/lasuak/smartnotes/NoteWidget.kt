package com.lasuak.smartnotes

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.lasuak.smartnotes.R

class NoteWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        for (appWidgetId in appWidgetIds!!) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity (context, 0, intent, 0);

            val views = RemoteViews(context!!.packageName, R.layout.example_widget);
            views.setOnClickPendingIntent(R.id.example_widget_button, pendingIntent);

            appWidgetManager!!.updateAppWidget(appWidgetId, views);
        }
    }

}