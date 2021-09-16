package com.lasuak.smartnotes

import android.app.Dialog
import android.content.*
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lasuak.smartnotes.database.Note
import com.lasuak.smartnotes.databinding.ActivityMainBinding
import android.content.Intent


class MainActivity : AppCompatActivity(), NoteListener, SearchView.OnQueryTextListener {
    private lateinit var mainBinding: ActivityMainBinding
    private var actionMode: ActionMode? = null
    private lateinit var adapter: NoteListAdapter

    companion object {
        var selectedItem: Array<Boolean>? = null
        var noteList = ArrayList<Note>()
        var counter = 0
        var order = 0
        var is_in_action_mode = false
        var is_select_all = false
        const val CHANNEL_ID = "com.kmv.easynote.notificationID"
    }

    private lateinit var dialog: Dialog
    private var editor: SharedPreferences.Editor? = null
    private var preferences: SharedPreferences? = null
    private var isLinearLayoutManager = true
    private lateinit var colorDrawableBackground: ColorDrawable
    private lateinit var deleteIcon: Drawable
    private lateinit var noteViewModel: NoteViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        noteViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(NoteViewModel::class.java)

        //shared preference for save layout configuration
        preferences = getSharedPreferences("DATA", MODE_PRIVATE)
        editor = preferences!!.edit()
        editor!!.apply()

        //Shared preference for sort order
        order = preferences!!.getInt("Order", 0);

        //swipe to delete color and icon
        colorDrawableBackground = ColorDrawable(Color.parseColor("#FFF67373"))
        deleteIcon = ContextCompat.getDrawable(this, R.drawable.ic_delete)!!

        //GET DEFAULT SORTING ORDER AND SORT NOTES ACCORDING ORDER
        //   chooseLayout()
        when (order) {
            0 -> {
                noteViewModel.allNotes.observe(this@MainActivity,
                    androidx.lifecycle.Observer {
                        setAdapter(it as ArrayList<Note>)
                        noteList = it
                    })
            }
            1 -> {
                noteViewModel.allNotesById.observe(this@MainActivity,
                    androidx.lifecycle.Observer {
                        setAdapter(it as ArrayList<Note>)
                        noteList = it
                    })
            }

            2 -> {
                noteViewModel.allNotesByLowToHigh.observe(this, androidx.lifecycle.Observer {
                    setAdapter(it as ArrayList<Note>)
                    noteList = it as ArrayList<Note>
                })
            }
            3 -> {
                noteViewModel.allNotesByHighToLow.observe(this, androidx.lifecycle.Observer {
                    setAdapter(it as ArrayList<Note>)
                    noteList = it
                })
            }
            4 -> {
                noteViewModel.allNotesByRed.observe(this, androidx.lifecycle.Observer {
                    setAdapter(it as ArrayList<Note>)
                    noteList = it
                })
            }
            5 -> {
                noteViewModel.allNotesByYellow.observe(this, androidx.lifecycle.Observer {
                    setAdapter(it as ArrayList<Note>)
                    noteList = it
                })
            }
            6 -> {
                noteViewModel.allNotesByGreen.observe(this, androidx.lifecycle.Observer {
                    setAdapter(it as ArrayList<Note>)
                    noteList = it
                })
            }
        }
        //Sort Note functionality
        sortNote()
        //NEW NOTE
        mainBinding.fab.setOnClickListener {
            mainBinding.recyclerView.adapter = adapter
            val intent = Intent(this@MainActivity, NewNoteActivity::class.java)
            startActivity(intent)
            // startActivityForResult(intent, newNoteActivityRequestCode)
        }
        swipeToDelete()
    }

    private fun swipeToDelete() {
        //Swipe to delete functionality
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getNoteAt(viewHolder.adapterPosition)

                if (direction == ItemTouchHelper.LEFT) {

                    noteViewModel.delete(adapter.getNoteAt(viewHolder.adapterPosition))
                    if (adapter.itemCount - 1 == 0) {
                        //mainBinding.noteTitle.visibility = View.VISIBLE
                    }
                    Snackbar.make(viewHolder.itemView, "${item.note} Deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            noteViewModel.insert(item)
                            adapter.notifyDataSetChanged()
                          //  mainBinding.noteTitle.visibility = View.INVISIBLE
                        }.show()
                    Snackbar.ANIMATION_MODE_FADE
                }
//                else if(direction==ItemTouchHelper.RIGHT){
//                    val sendIntent = Intent().apply {
//                        action = Intent.ACTION_SEND
//                        val gettext = item.note
//                        if(gettext.isNotEmpty())
//                        {
//                            putExtra(Intent.EXTRA_TEXT,gettext)
//                            type = "text/plain"
//                        }
//                    }
//                    startActivity(sendIntent)
//                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMarginVertical =
                    (viewHolder.itemView.height - deleteIcon.intrinsicHeight) / 2
                //val iconMarginVertical2 = (viewHolder.itemView.height - shareIcon.intrinsicHeight) / 2

//               if (dX > 0) {
//                    Log.d("E","top")
//                    colorDrawableBackground.setBounds(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
//                    shareIcon.setBounds(itemView.left + iconMarginVertical2, itemView.top + iconMarginVertical2,
//                        itemView.left + iconMarginVertical2 + shareIcon.intrinsicWidth, itemView.bottom - iconMarginVertical)
//                    shareIcon.level = 0
//               }
                //else {
                colorDrawableBackground.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top + 30,
                    itemView.right,
                    itemView.bottom
                )
                deleteIcon.setBounds(
                    itemView.right - iconMarginVertical - deleteIcon.intrinsicWidth,
                    itemView.top + iconMarginVertical + 10,
                    itemView.right - iconMarginVertical,
                    itemView.bottom - iconMarginVertical
                )
                deleteIcon.level = 0
                //      }
                colorDrawableBackground.draw(c)
                c.save()

                if (dX < 0) {
                    c.clipRect(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteIcon.draw(c)
                }
//               else {
//                    c.clipRect(itemView.left, itemView.top, dX.toInt(), itemView.bottom)
//                    shareIcon.draw(c)
//               }
                c.restore()

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }).attachToRecyclerView(mainBinding.recyclerView)

    }

    private fun setAdapter(notes: ArrayList<Note>) {
        chooseLayout()
        when (order) {
            0 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotes, this)
            }
            1 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotesById, this)
            }
            2 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotesByLowToHigh, this)
            }
            3 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotesByHighToLow, this)
            }
            4 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotesByRed, this)
            }
            5 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotesByYellow, this)
            }
            6 -> {
                adapter = NoteListAdapter(this, noteViewModel.allNotesByGreen, this)
            }

        }
        //store order of sorting into shared preference
        editor!!.putInt("Order", order)
        editor!!.apply()

        mainBinding.recyclerView.adapter = adapter
        //words.let { adapter.updateList(it as ArrayList<Note>) }
        adapter.updateList(notes)
        adapter.notifyDataSetChanged()
//        if (adapter.itemCount == 0) {
//            mainBinding.noteTitle.visibility = View.VISIBLE
//        } else {
//            mainBinding.noteTitle.visibility = View.INVISIBLE
//        }
    }

    private fun chooseLayout() {
        val checkbox = preferences!!.getString("HEY", "True")

        if (checkbox.equals("True")) {
            mainBinding.recyclerView.layoutManager = LinearLayoutManager(this)
            // val layout = menuInflater.inflate(R.menu.main_note_menu,Menu menu)findViewById(R.id.action_layout)
        } else {
            mainBinding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, 1)
        }
    }

    private fun deleteAllNote() {
        if (mainBinding.recyclerView.isEmpty()) {
            Toast.makeText(this, "Note List is Empty", Toast.LENGTH_SHORT).show()
        } else {
            val materialAlertDialog = MaterialAlertDialogBuilder(this)
            materialAlertDialog.setTitle("Delete all notes")
                .setMessage("Are you conform to delete these notes?")
                .setCancelable(false)
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton("Delete") { dialog, id ->
                    noteViewModel.deleteAll()
                   // mainBinding.noteTitle.visibility = View.VISIBLE
                    Toast.makeText(
                        this@MainActivity,
                        "Notes deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    actionMode!!.finish()
                    onActionMode(false)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, id ->
                    //  mainBinding.toolbar.textSelected.text = "${counter} items selected"
                    dialog.cancel()
                }
                .show()
           }

    }

    private fun setPassword() {

        val preferences = getSharedPreferences("PASSWORD", MODE_PRIVATE)
        val password = preferences!!.getString("password", null);
        var isSecure = false

        isSecure = password != null

        dialog = Dialog(this)

        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.back_dailog
            )
        )
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        val okey = dialog.findViewById<Button>(R.id.yesbtn)
        val cancel = dialog.findViewById<Button>(R.id.cancelbtn)
        val title = dialog.findViewById<TextView>(R.id.textAlert)
        val message = dialog.findViewById<TextView>(R.id.alertDescription)
        val password2 = dialog.findViewById<EditText>(R.id.txtPassword)

        if (isSecure) {
            "Enter Password".also { title.text = it }
            "Your note is secure".also { message.text = it }
        } else {
            "Set New Password".also { title.text = it }
        }
        dialog.show()

        okey.setOnClickListener() {
            val pass = password2.text.toString()
            if (isSecure) {
                if (pass == password) {
                    val editor = preferences.edit()
                    editor!!.putString("password", null)
                    editor.apply()
                    Toast.makeText(
                        this@MainActivity,
                        "Tap again to change password",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Your password is incorrect,Please try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else{
                if(pass.length in 4..15) {
                    val editor = preferences.edit()
                    editor!!.putString("password", pass)
                    editor.apply()
                    Toast.makeText(
                        this@MainActivity,
                        "Your password is change successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                else{
                    Toast.makeText(
                        this@MainActivity,
                        "Please set password of min of 4 character and maximum 15 character",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialog.dismiss()
        }
        cancel.setOnClickListener() {
            // Toast.makeText(this,"CANCEL",Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_note_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(this)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_layout -> {
                val controller =
                    AnimationUtils.loadLayoutAnimation(this, R.anim.rcv_layout_animation)
                mainBinding.recyclerView.layoutAnimation = controller
                mainBinding.recyclerView.adapter!!.notifyDataSetChanged()
                mainBinding.recyclerView.scheduleLayoutAnimation()
                if (isLinearLayoutManager) {
                    item.title = "GridView"
                    item.setIcon(R.drawable.ic_gridview)
                    editor!!.putString("HEY", "True")
                    editor!!.apply()
                } else {
                    item.title = "ListView"
                    item.setIcon(R.drawable.ic_listview)
                    editor!!.putString("HEY", "False")
                    editor!!.apply()
                }
                isLinearLayoutManager = !isLinearLayoutManager
                // Sets layout and icon
                chooseLayout()
                mainBinding.recyclerView.adapter = adapter
                return true
            }
            R.id.action_security -> {
                setPassword()
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    //Search note methods
    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null) {
            searchDatabase(query)
        }
        return true
    }

    private fun sortNote() {
        val sortAdapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.sorting_list)
        )
        sortAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        mainBinding.sortingOptions.adapter = sortAdapter

        //we need by default selected sort when app start
        mainBinding.sortingOptions.setSelection(order)

        //pass view:View? for null reference
        mainBinding.sortingOptions.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View?, item: Int, l: Long
                ) {
                    if (order != item) {
                        order = item
                        when (order) {
                            0 -> {
                                noteViewModel.allNotes.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 0
                            }
                            1 -> {
                                noteViewModel.allNotesById.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 1
                            }
                            2 -> {
                                noteViewModel.allNotesByLowToHigh.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 2
                            }
                            3 -> {
                                noteViewModel.allNotesByHighToLow.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 3
                            }
                            4 -> {
                                noteViewModel.allNotesByRed.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 4
                            }
                            5 -> {
                                noteViewModel.allNotesByYellow.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 5
                            }
                            6 -> {
                                noteViewModel.allNotesByGreen.observe(
                                    this@MainActivity,
                                    androidx.lifecycle.Observer {
                                        setAdapter(it as ArrayList<Note>)
                                        noteList = it
                                    })
                                order = 6
                            }
                        }
                    }
                    Log.d("Order", " Inside sort ${order}")
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
    }

    private fun searchDatabase(query: String) {
        val searchQuery = "%$query%"
        Log.d("SEARCH", "order ${order} and query $query")
        when (order) {
            0 -> {
                noteViewModel.searchDatabase(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            1 -> {
                noteViewModel.searchDatabaseById(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            2 -> {
                noteViewModel.searchDatabaseByLowToHigh(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            3 -> {
                noteViewModel.searchDatabaseByHighToLow(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            4 -> {
                noteViewModel.searchDatabaseByRed(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                        adapter.notifyDataSetChanged()
                    })
            }
            5 -> {
                noteViewModel.searchDatabaseByYellow(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                        adapter.notifyDataSetChanged()

                    })
            }
            6 -> {
                noteViewModel.searchDatabaseByGreen(searchQuery).observe(this,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                        adapter.notifyDataSetChanged()

                    })
            }
        }
    }


    override fun onItemClicked(
        position: Int,
        note: Note
    ) {
        if (is_in_action_mode) {
            if (selectedItem!![position]) {
                selectedItem!![position] = false
                counter--
                actionMode!!.title = "$counter/${noteList.size} Selected"
            } else {
                selectedItem!![position] = true
                counter++
                actionMode!!.title = "$counter/${noteList.size} Selected"
            }
            Log.d("ACTION", "Counter = $counter")
        } else if (!is_in_action_mode) {
            val intent = Intent(this, OpenNoteActivity::class.java)
            intent.putExtra("Title", note.title)
            intent.putExtra("Text", note.note)
            intent.putExtra("Time", note.time)
            intent.putExtra("Id", note.id)
            intent.putExtra("Reminder", note.reminderTime)
            intent.putExtra("Priority", note.priority)
            startActivity(intent)
        //            val activityOption  = ActivityOptions.makeSceneTransitionAnimation(this,mainBinding.recyclerView.get(position),"item_card")
//            startActivity(intent,activityOption.toBundle())
            //        startActivityForResult(intent, fullScreenRequestCode)
            //overridePendingTransition(R.anim.top_in,0)
        }
    }

    override fun onCopyClicked(note: Note, text: String?) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("simple text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }

    val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            menuInflater.inflate(R.menu.main_note__action_menu, menu)
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode?,
            item: MenuItem?
        ): Boolean {
            return when (item?.itemId) {
                R.id.action_delete -> {
                    Log.d("ACTION", "DELETE CLICKED")
                    if(counter==0) {
                        Toast.makeText(this@MainActivity,"Please select a note",Toast.LENGTH_SHORT).show()
                    }
                    else if (selectedItem!!.isNotEmpty()) {
                        if (counter == noteList.size) {
                            deleteAllNote()
                            adapter.updateList(noteList)
                        } else {
                            val materialAlertDialog = MaterialAlertDialogBuilder(this@MainActivity)
                            materialAlertDialog.setTitle("Delete Note")
                                .setMessage("Delete these notes?")
                                .setCancelable(false)
                                .setIcon(R.drawable.ic_delete)
                                .setPositiveButton("Delete") { dialog, id ->
                                    for (i in selectedItem!!) {
                                        Log.d(
                                            "ACTION",
                                            "Selected pos #${noteList[selectedItem!!.indexOf(i)]}"
                                        )
                                        val delete =
                                            noteViewModel.delete(noteList[selectedItem!!.indexOf(i)])
                                    }
                                    onActionMode(false)
                                    actionMode!!.finish()

                                    adapter.updateList(noteList)
                                    // selectionList.clear()
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Notes deleted successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                }
                                .setNegativeButton("No") { dialog, id ->
                                    dialog.cancel()
                                }
                                .show()
                        }
                    }
                    true
                }
                R.id.action_selectAll -> {
                    if(!is_select_all) {
                       item.setIcon(R.drawable.ic_select_all_on)
                        for (i in 0 until noteList.size)
                            selectedItem!![i] == true

                        counter = noteList.size
                        actionMode!!.title = "${counter}/${noteList.size} Selected"
                        is_select_all = true
                    }else {
                        item.setIcon(R.drawable.ic_select_all_off)
                        for (i in 0 until noteList.size)
                            selectedItem!![i] == false

                        counter = 0
                        is_select_all = false
                        actionMode!!.title = "${counter}/${noteList.size} Selected"
                    }
                    adapter.notifyDataSetChanged()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            onActionMode(false)
            actionMode = null
        }
    }

    override fun onAnyItemLongClicked(position: Int) {
        if (!is_in_action_mode) {
            onActionMode(true)
            counter = 1
            Log.d("Item", "onLongCLick action mode if : $counter")
            selectedItem!![position] = true
        } else {
            if (selectedItem!![position]) {
                selectedItem!![position] = false
                counter--
                Log.d("Item", "onLongCLick action mode else -- : $counter")
            } else {
                selectedItem!![position] = true
                counter++
                Log.d("Item", "onLongCLick action mode else ++ : $counter")
            }
        }
        if (actionMode == null) {
            actionMode = startSupportActionMode(callback)!!
        }
        actionMode!!.title = "$counter/${noteList.size} Selected"
    }

    private fun onActionMode(actionModeOn: Boolean) {
        //selectionList.clear()
        if (actionModeOn) {
            selectedItem = Array(noteList.size) { false }
            is_in_action_mode = true
            mainBinding.sortingOptions.visibility = View.GONE
            mainBinding.fab.visibility = View.GONE
            //  adapter.notifyDataSetChanged()
        } else {
            is_in_action_mode = false
            is_select_all = false //select all item disable
            mainBinding.sortingOptions.visibility = View.VISIBLE
            mainBinding.fab.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }
}