package com.lasuak.smartnotes

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.lasuak.smartnotes.data.model.Note
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.is_in_action_mode
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.is_select_all
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.selectedItem
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class NoteListAdapter(
    var mContext: Context,
    note: LiveData<List<Note>>,
    var listener: NoteListener
) :
    RecyclerView.Adapter<NoteListAdapter.NoteViewHolder>() {

    companion object {
        @JvmStatic
        var item = ArrayList<Note>()
    }

    fun getNoteAt(position: Int): Note {
        return item[position]
    }

    fun updateList(newList: ArrayList<Note>) {
        //item = ArrayList<Note>()
        item.clear()
        item.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val pref = mContext.getSharedPreferences("DATA", AppCompatActivity.MODE_PRIVATE)
        val checkbox = pref.getString("HEY", "True")

        val viewHolder: NoteViewHolder
        if (checkbox.equals("True")) {
            viewHolder = NoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.note_item, parent, false)
            )
        } else {
            viewHolder = NoteViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.note_item_grid, parent, false)
            )
        }
        return viewHolder
    }


    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        if (!is_in_action_mode) {
            holder.cardView.strokeWidth = 2
            //  holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_500))
        } else {
            if (is_select_all) {
                holder.cardView.strokeWidth = 6
                //       holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_700))
                if (selectedItem != null) {
                    selectedItem!![position] = true
                }
            } else {
                holder.cardView.strokeWidth = 2
                // holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_500))
                if (selectedItem != null) {
                    selectedItem!![position] = false
                }
            }
        }
        holder.copybtn.setOnClickListener() {
            listener.onCopyClicked(
                item[position],
                item[position].note
            );
        }
        holder.cardView.setOnClickListener() {
            //when in action mode OnClick Listener don't want to work
            if (is_in_action_mode) {
                if (!selectedItem!![position]) {
                    holder.cardView.strokeWidth = 6
//                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext,R.color.blue_700))
                } else {
                    holder.cardView.strokeWidth = 2
//                    holder.cardView.setCardBackgroundColor( ContextCompat.getColor( mContext, R.color.blue_500))
                }
            }
            listener.onItemClicked(
                position,
                item[position]
            )
        }
        holder.cardView.setOnLongClickListener {
            //holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_700))
            listener.onAnyItemLongClicked(position)
            if (selectedItem == null) {
            } else {
                if (selectedItem!![position]) {
                    Log.d("Item", "value if long : ${selectedItem!![position]}")
                    // holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_700))
                    holder.cardView.strokeWidth = 6
                } else {
                    Log.d("Item", "value else long : ${selectedItem!![position]}")
                    //holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_500))
                    holder.cardView.strokeWidth = 2
                }
            }
            return@setOnLongClickListener true
        }

        //here new code deleted
        val current = item[position]
        if (current.title.isEmpty()) {
            holder.txtNote.text = current.note
        } else if (current.title.isNotEmpty()) {
            if (current.note!!.isNotEmpty())
                holder.txtNote.text = (current.title + "\n" + current.note).toString()
            else
                holder.txtNote.text = current.title
        }

        val time = getTimeAgo("${current.time},${current.date}");
        holder.textTime.text = time//current.time + ","+ current.date

        when (current.priority) {
            3 -> holder.imgPriority.setImageResource(R.drawable.priority_green)
            2 -> holder.imgPriority.setImageResource(R.drawable.priority_yellow)
            1 -> {
                holder.imgPriority.setImageResource(R.drawable.priority_red)
                val txt = "${holder.txtNote.text.toString().substring(0, 1)} *****"
                holder.txtNote.text = txt
            }
        }
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNote: TextView = itemView.findViewById(R.id.txtNote)
        val copybtn: ImageView = itemView.findViewById(R.id.copybtn)
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val imgPriority: ImageView = itemView.findViewById(R.id.imgPriority)
        var cardView: com.google.android.material.card.MaterialCardView =
            itemView.findViewById(R.id.cardView)
    }

    override fun getItemCount(): Int {
        return item.size
    }

    private fun getTimeAgo(dateTime: String): String {
        Log.d("TIME", "time : $dateTime")
        var timeStatus = "time"
        try {
            val format =
                SimpleDateFormat(
                    "hh:mm:ss aa,d MMM yyyy",
                    Locale.getDefault()
                )//"hh:mm:ss a, d MMM yyyy")//"EEE, d MMM,hh:mm a")//"yyyy.MM.dd G 'at' HH:mm:ss");
            val past = format.parse(dateTime)
            val now = Date();
            Log.d(
                "TIME",
                "Seconds with ${TimeUnit.MILLISECONDS.toSeconds(now.time)} and ${
                    TimeUnit.MILLISECONDS.toSeconds(past!!.time)
                }"
            )
            val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - past!!.time);
            val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time);
            val hours = TimeUnit.MILLISECONDS.toHours(now.time - past.time);
            val days = TimeUnit.MILLISECONDS.toDays(now.time - past.time);

            Log.d(
                "TIME",
                "$dateTime"
            ) //second $seconds min $minutes and hour $hours and day $days" )
//            var h=dateTime.substring(0,2).toInt()
//            var amOrPm = " am"
//            if(h>12){
//                h -= 12
//               amOrPm = " pm"
//            }
            when {
                seconds < 60 -> {
//                    timeStatus = "$dateTime $seconds seconds ago"
                    timeStatus = "${dateTime.substring(0, 11)}, $seconds seconds ago"
                    //     timeStatus = "$h${dateTime.substring(2,8)}$amOrPm, $seconds seconds ago"
                }
                minutes < 60 -> {
                    timeStatus = "${
                        dateTime.substring(
                            0,
                            11
                        )
                    }, $minutes minute ago "//, $minutes minutes ago"
                    // timeStatus = "$h${dateTime.substring(2,8)}$amOrPm, $minutes minutes ago"
                }
                hours < 24 -> {
                    timeStatus = "${dateTime.substring(0, 11)}, $hours hours ago"
//                    timeStatus = "$h${dateTime.substring(2,8)}$amOrPm, $hours hours ago"
                }
                days in 1..7 -> {
                    timeStatus = if (days == 1.toLong()) {
                        //      "$h${dateTime.substring(2, 8)}$amOrPm, Yesterday"
                        "${dateTime.substring(0, 11)}, Yesterday"
                    } else
                        "${dateTime.substring(12, dateTime.length)}, $days days ago"
                    //  "${dateTime.substring(12)}, $days days ago"
                }
                else -> {
                    timeStatus = dateTime.substring(12);
                }
            }
        } catch (e: Exception) {
            e.printStackTrace();
        }
        return timeStatus;
    }

}

interface NoteListener {
    fun onItemClicked(position: Int, note: Note)
    fun onCopyClicked(note: Note, text: String?)
    fun onAnyItemLongClicked(position: Int)
}
