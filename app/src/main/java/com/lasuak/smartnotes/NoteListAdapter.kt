package com.lasuak.smartnotes

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import com.lasuak.smartnotes.MainActivity.Companion.is_in_action_mode
import com.lasuak.smartnotes.MainActivity.Companion.is_select_all
import com.lasuak.smartnotes.MainActivity.Companion.selectedItem
import com.lasuak.smartnotes.database.Note
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class NoteListAdapter(var mContext: Context, note: LiveData<List<Note>>, var listener: NoteListener) :
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
        val viewHolder = NoteViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.note_item, parent, false)
        )
        return viewHolder
    }


    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        if (!is_in_action_mode) {
            holder.cardView.strokeWidth=0
            //  holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_500))
        }
        else {
            if(is_select_all){
                holder.cardView.strokeWidth=6
                //       holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_700))
                if(selectedItem!=null) {
                    selectedItem!![position]=true
                }
            }
            else{
                holder.cardView.strokeWidth=0
                // holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_500))
                if(selectedItem!=null) {
                    selectedItem!![position]=false
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
            if(is_in_action_mode) {
                if (!selectedItem!![position]) {
                    holder.cardView.strokeWidth=6
//                    holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext,R.color.blue_700))
                } else {
                    holder.cardView.strokeWidth=0
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
            if(selectedItem==null) {
             }
            else{
                if(selectedItem!![position]){
                    Log.d("Item","value if long : ${selectedItem!![position]}")
                   // holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_700))
                    holder.cardView.strokeWidth=6
                }
                else{
                    Log.d("Item","value else long : ${selectedItem!![position]}")
                    //holder.cardView.setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.blue_500))
                   holder.cardView.strokeWidth=0
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

        val time = getTimeAgo(current.time);
        holder.textTime.text = time

        when (current.priority) {
            3 -> holder.imgPriority.setImageResource(R.drawable.priority_green)
            2 -> holder.imgPriority.setImageResource(R.drawable.priority_yellow)
            1 -> {
                holder.imgPriority.setImageResource(R.drawable.priority_red)
                val txt = "${holder.txtNote.text.toString().substring(0, 1)} *****"
                holder.txtNote.text =txt
            }
        }
    }

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtNote: TextView = itemView.findViewById(R.id.txtNote)
        val copybtn: ImageView = itemView.findViewById(R.id.copybtn)
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val imgPriority: ImageView = itemView.findViewById(R.id.imgPriority)
        var cardView: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardview)
    }

    override fun getItemCount(): Int {
        return item.size
    }

    @SuppressLint("SimpleDateFormat")
    fun getTimeAgo(dateTime: String): String {
        var timeStatus = "time"
        try {
            val format =
                SimpleDateFormat("hh:mm:ss a, d MMM yyyy")//"EEE, d MMM,hh:mm a")//"yyyy.MM.dd G 'at' HH:mm:ss");
            val past = format.parse(dateTime);
            val now = Date();
            val seconds = TimeUnit.MILLISECONDS.toSeconds(now.time - past!!.time);
            val minutes = TimeUnit.MILLISECONDS.toMinutes(now.time - past.time);
            val hours = TimeUnit.MILLISECONDS.toHours(now.time - past.time);
            val days = TimeUnit.MILLISECONDS.toDays(now.time - past.time);

            when {
                seconds < 60 -> {
                    timeStatus = "${dateTime.substring(0, 11)} $seconds seconds ago"
                }
                minutes < 60 -> {
                    timeStatus = "${dateTime.substring(0, 11)} , $minutes minute ago "//, $minutes minutes ago"
                }
                hours < 24 -> {
                    timeStatus = "${dateTime.substring(0, 11)}, $hours hours ago"
                }
                days in 1..7 -> {
                    timeStatus = if (days == 1.toLong()) {
                        "${dateTime.substring(0, 11)} yesterday"
                    } else
                        "${dateTime.substring(13, 18)} $days days ago"
                }
                else -> {
                    timeStatus = dateTime.substring(0, 11);
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
