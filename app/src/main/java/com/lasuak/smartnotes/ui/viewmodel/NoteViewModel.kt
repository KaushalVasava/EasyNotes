package com.lasuak.smartnotes.ui.viewmodel

import android.app.Application
import android.app.Dialog
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.util.Log
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.lasuak.smartnotes.NoteListAdapter
import com.lasuak.smartnotes.NoteListener
import com.lasuak.smartnotes.R
import com.lasuak.smartnotes.data.model.Note
import com.lasuak.smartnotes.data.database.NoteDatabase
import com.lasuak.smartnotes.data.repository.NoteRepository
import com.lasuak.smartnotes.databinding.FragmentHomeBinding
import com.lasuak.smartnotes.fragment.HomeFragment.Companion.editor
import com.lasuak.smartnotes.fragment.HomeFragment.Companion.noteList
import com.lasuak.smartnotes.fragment.HomeFragment.Companion.order
import com.lasuak.smartnotes.fragment.HomeFragment.Companion.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    var appl = application
    var checkSecurity = false
    lateinit var adapter: NoteListAdapter
    private var isLinearLayoutManager = true
    lateinit var colorDrawableBackground: ColorDrawable
    lateinit var deleteIcon: Drawable
    var actionMode: ActionMode? = null

    companion object {
        var selectedItem: Array<Boolean>? = null
        var counter = 0
        var is_in_action_mode = false
        var is_select_all = false
    }

    private var cancellationSignal: CancellationSignal? = null
    private val authenticationCallback: BiometricPrompt.AuthenticationCallback
        get() = @RequiresApi(Build.VERSION_CODES.P)
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                notifyUser("Error : $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                checkSecurity = true
            }
        }

    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
    }

    val allNotes: LiveData<List<Note>> = repository.allNotes
    val allNotesById: LiveData<List<Note>> = repository.allNotesById
    val allNotesByHighToLow: LiveData<List<Note>> = repository.allNotesByHighToLow
    val allNotesByLowToHigh: LiveData<List<Note>> = repository.allNotesByLowToHigh
    val allNotesByRed: LiveData<List<Note>> = repository.allNotesByRed
    val allNotesByYellow: LiveData<List<Note>> = repository.allNotesByYellow
    val allNotesByGreen: LiveData<List<Note>> = repository.allNotesByGreen

    fun insert(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(note)
    }

    fun delete(note: Note) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(note)
    }

    fun update(note: Note) = viewModelScope.launch {
        repository.update(note)
    }

    fun deleteAll() = viewModelScope.launch {
        repository.deleteAll()
    }

    fun searchDatabase(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabase(searchQuery)
    }

    fun searchDatabaseById(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseById(searchQuery)
    }

    fun searchDatabaseByHighToLow(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByHighToLow(searchQuery)
    }

    fun searchDatabaseByLowToHigh(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByLowToHigh(searchQuery)
    }

    fun searchDatabaseByRed(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByRed(searchQuery)
    }

    fun searchDatabaseByYellow(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByRed(searchQuery)
    }

    fun searchDatabaseByGreen(searchQuery: String): LiveData<List<Note>> {
        return repository.searchDatabaseByRed(searchQuery)
    }

    private fun notifyUser(message: String) {
        Toast.makeText(
            appl.applicationContext, message, Toast.LENGTH_SHORT
        ).show()
    }

    private fun getCancellationSignal(): CancellationSignal {
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Authentication was cancelled by the user")
        }
        return cancellationSignal as CancellationSignal
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkBiometricSupport(): Boolean {
        val keyguardManager =
            appl.applicationContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isDeviceSecure) {
            notifyUser("Fingerprint authentication has not been enable in settings")
            return false
        }
        if (ActivityCompat.checkSelfPermission(
                appl.applicationContext,
                android.Manifest.permission.USE_BIOMETRIC
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifyUser("Fingerprint authentication permission is not enable")
            return false
        }
        return if (appl.applicationContext.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            true
        } else
            true
    }

    fun layoutCheck(binding: FragmentHomeBinding) {
        val controller =
            AnimationUtils.loadLayoutAnimation(appl.baseContext, R.anim.rcv_layout_animation)
        binding.recyclerView.layoutAnimation = controller
        //binding.recyclerView.adapter!!.notifyDataSetChanged
        binding.recyclerView.scheduleLayoutAnimation()
        if (isLinearLayoutManager) {
            Log.d("L", "list view ")
            //.title = "GridView"
            binding.layoutChoose.setImageResource(R.drawable.ic_listview)
            binding.layoutChoose.contentDescription = "List View"
            editor!!.putString("HEY", "True")
            editor!!.apply()
        } else {
            Log.d("L", "grid view ")
            binding.layoutChoose.setImageResource(R.drawable.ic_gridview)
            binding.layoutChoose.contentDescription = "Grid View"
            editor!!.putString("HEY", "False")
            editor!!.apply()
        }
        isLinearLayoutManager = !isLinearLayoutManager

        // Sets layout and icon
        chooseLayout(binding)
        binding.recyclerView.adapter = adapter
        // adapter.notifyDataSetChanged()
    }

    private fun chooseLayout(binding: FragmentHomeBinding) {
        val checkbox = preferences!!.getString("HEY", "True")

        if (checkbox.equals("True")) {
            Log.d("L", "linear in choose")
            binding.recyclerView.layoutManager = LinearLayoutManager(appl.baseContext)
            binding.layoutChoose.setImageResource(R.drawable.ic_gridview)
            isLinearLayoutManager = false
            // val layout = menuInflater.inflate(R.menu.main_note_menu,Menu menu)findViewById(R.id.action_layout)
        } else {
            Log.d("L", "grid in choose")

            binding.recyclerView.layoutManager = StaggeredGridLayoutManager(2, 1)
            binding.layoutChoose.setImageResource(R.drawable.ic_listview)
            isLinearLayoutManager = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun checkAppSecurity(binding: FragmentHomeBinding, listener: NoteListener) {
        checkBiometricSupport()
        val biometricPrompt = BiometricPrompt.Builder(appl.baseContext)
            .setTitle("FINGER PRINT FOR NOTE")
            .setSubtitle("Authentication is requred")
            .setDescription("This app uses fingerprint protection to keep your note secure")
            .setNegativeButton("Cancel", appl.mainExecutor,
                { _, _ ->
                    val list = ArrayList<Note>()
                    binding.recyclerView.adapter =
                        NoteListAdapter(appl.baseContext, list as LiveData<List<Note>>, listener)

                    //notifyUser("Authentication cancelled")
                }).build()

        biometricPrompt.authenticate(
            getCancellationSignal(),
            appl.mainExecutor,
            authenticationCallback
        )
    }

    fun checkOrder(
        context: Context,
        binding: FragmentHomeBinding,
        viewLifecycleOwner: LifecycleOwner,
        listener: NoteListener
    ) {
        when (order) {
            0 -> {
                allNotes.observe(viewLifecycleOwner, {
                    setAdapter(
                        context,
                        binding, it as ArrayList<Note>, listener
                    )
                    noteList = it as ArrayList<Note>
                })
            }
            1 -> {
                allNotesById.observe(viewLifecycleOwner,
                    {
                        setAdapter(context, binding, it as ArrayList<Note>, listener)
                        noteList = it
                    })
            }

            2 -> {
                allNotesByLowToHigh.observe(viewLifecycleOwner, {
                    setAdapter(context, binding, it as ArrayList<Note>, listener)
                    noteList = it
                })
            }
            3 -> {
                allNotesByHighToLow.observe(viewLifecycleOwner, {
                    setAdapter(context, binding, it as ArrayList<Note>, listener)
                    noteList = it
                })
            }
            4 -> {
                allNotesByRed.observe(viewLifecycleOwner, {
                    setAdapter(context, binding, it as ArrayList<Note>, listener)
                    noteList = it
                })
            }
            5 -> {
                allNotesByYellow.observe(viewLifecycleOwner, {
                    setAdapter(context, binding, it as ArrayList<Note>, listener)
                    noteList = it
                })
            }
            6 -> {
                allNotesByGreen.observe(viewLifecycleOwner, {
                    setAdapter(context, binding, it as ArrayList<Note>, listener)
                    noteList = it
                })
            }
        }
    }

    fun sortNote(
        context: Context,
        binding: FragmentHomeBinding,
        viewLifecycleOwner: LifecycleOwner,
        listener: NoteListener
    ) {
        val sortAdapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            appl.baseContext,
            android.R.layout.simple_spinner_dropdown_item,
            appl.resources.getStringArray(R.array.sorting_list)
        )
        sortAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        binding.sortingOptions.adapter = sortAdapter

        //we need by default selected sort when app start
        binding.sortingOptions.setSelection(order)

        //pass view:View? for null reference
        binding.sortingOptions.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View?, item: Int, l: Long
                ) {
                    if (order != item) {
                        order = item
                        when (order) {
                            0 -> {
                                allNotes.observe(
                                    viewLifecycleOwner,
                                    {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,
                                            listener
                                        )
                                        noteList = it
                                    })
                                order = 0
                            }
                            1 -> {
                                allNotesById.observe(
                                    viewLifecycleOwner,
                                    {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,
                                            listener
                                        )
                                        noteList = it
                                    })
                                order = 1
                            }
                            2 -> {
                                allNotesByLowToHigh.observe(
                                    viewLifecycleOwner, {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,
                                            listener
                                        )
                                        order = 2
                                    })
                            }
                            3 -> {
                                allNotesByHighToLow.observe(
                                    viewLifecycleOwner,
                                    {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,
                                            listener
                                        )
                                        noteList = it
                                    })
                                order = 3
                            }
                            4 -> {
                                allNotesByRed.observe(
                                    viewLifecycleOwner,
                                    {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,

                                            listener
                                        )
                                        noteList = it
                                    })
                                order = 4
                            }
                            5 -> {
                                allNotesByYellow.observe(
                                    viewLifecycleOwner,
                                    {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,
                                            listener
                                        )
                                        noteList = it
                                    })
                                order = 5
                            }
                            6 -> {
                                allNotesByGreen.observe(
                                    viewLifecycleOwner,
                                    {
                                        setAdapter(
                                            context,
                                            binding,
                                            it as ArrayList<Note>,
                                            listener
                                        )
                                        noteList = it
                                    })
                                order = 6
                            }
                        }
                    }
                    Log.d("Order", " Inside sort $order")
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
    }

    fun setAdapter(
        context: Context,
        binding: FragmentHomeBinding,
        notes: ArrayList<Note>,
        listener: NoteListener
    ) {
        chooseLayout(binding)

        when (order) {
            0 -> {
                adapter = NoteListAdapter(context, allNotes, listener)
            }
            1 -> {
                adapter = NoteListAdapter(context, allNotesById, listener)
            }
            2 -> {
                adapter = NoteListAdapter(context, allNotesByLowToHigh, listener)
            }
            3 -> {
                adapter = NoteListAdapter(context, allNotesByHighToLow, listener)
            }
            4 -> {
                adapter = NoteListAdapter(context, allNotesByRed, listener)
            }
            5 -> {
                adapter = NoteListAdapter(context, allNotesByYellow, listener)
            }
            6 -> {
                adapter = NoteListAdapter(context, allNotesByGreen, listener)
            }

        }
        //store order of sorting into shared preference
        editor!!.putInt("Order", order)
        editor!!.apply()

        binding.recyclerView.adapter = adapter
        //words.let { adapter.updateList(it as ArrayList<Note>) }
        adapter.updateList(notes)
        adapter.notifyDataSetChanged()
    }


    fun setPassword(context: Context) {

        val preferences =
            appl.getSharedPreferences("PASSWORD", AppCompatActivity.MODE_PRIVATE)
        val password = preferences!!.getString("password", null)
        var isSecure: Boolean

        isSecure = password != null

        val dialog = Dialog(context)

        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.back_dialog
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
                        context,
                        "Tap again to change password",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Your password is incorrect,Please try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                if (pass.length in 4..15) {
                    val editor = preferences.edit()
                    editor!!.putString("password", pass)
                    editor.apply()
                    Toast.makeText(
                        context,
                        "Your password is change successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
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

    fun swipeToDelete(binding: FragmentHomeBinding) {
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

                    delete(adapter.getNoteAt(viewHolder.adapterPosition))
                    if (adapter.itemCount - 1 == 0) {
                        //binding.noteTitle.visibility = View.VISIBLE
                    }
                    Snackbar.make(viewHolder.itemView, "${item.note} Deleted", Snackbar.LENGTH_LONG)
                        .setAction("UNDO") {
                            insert(item)
                            adapter.notifyDataSetChanged()
                            //  binding.noteTitle.visibility = View.INVISIBLE
                        }.show()
                    Snackbar.ANIMATION_MODE_FADE
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val iconMarginVertical =
                    (viewHolder.itemView.height - deleteIcon.intrinsicHeight) / 2
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
        }).attachToRecyclerView(binding.recyclerView)
    }

    fun deleteAllNote(binding: FragmentHomeBinding, context: Context) {
        if (binding.recyclerView.isEmpty()) {
            Toast.makeText(context, "Note List is Empty", Toast.LENGTH_SHORT).show()
        } else {
            val materialAlertDialog = MaterialAlertDialogBuilder(context)
            materialAlertDialog.setTitle("Delete all notes")
                .setMessage("Are you conform to delete these notes?")
                .setCancelable(false)
                .setIcon(R.drawable.ic_delete)
                .setPositiveButton("Delete") { dialog, id ->
                    deleteAll()
                    // mainBinding.noteTitle.visibility = View.VISIBLE
                    Toast.makeText(
                        context,
                        "Notes deleted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    actionMode!!.finish()
                    onActionMode(false, binding)
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, id ->
                    //  mainBinding.toolbar.textSelected.text = "${counter} items selected"
                    dialog.cancel()
                }
                .show()
        }

    }

    fun onActionMode(actionModeOn: Boolean, binding: FragmentHomeBinding) {
        //selectionList.clear()
        if (actionModeOn) {
            selectedItem = Array(noteList.size) { false }
            is_in_action_mode = true
            binding.sortingOptions.visibility = View.GONE
            binding.fab.visibility = View.GONE
            binding.layoutChoose.visibility = View.GONE
            //  adapter.notifyDataSetChanged()
        } else {
            is_in_action_mode = false
            is_select_all = false //select all item disable
            binding.sortingOptions.visibility = View.VISIBLE
            binding.fab.visibility = View.VISIBLE
            binding.layoutChoose.visibility = View.VISIBLE
            adapter.notifyDataSetChanged()
        }
    }

    fun searchDatabase(query: String, viewLifecycleOwner: LifecycleOwner) {
        val searchQuery = "%$query%"
        Log.d("SEARCH", "order $order and query $query")
        when (order) {
            0 -> {
                searchDatabase(searchQuery).observe(viewLifecycleOwner,
                    androidx.lifecycle.Observer {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            1 -> {
                searchDatabaseById(searchQuery).observe(viewLifecycleOwner,
                    {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            2 -> {
                searchDatabaseByLowToHigh(searchQuery).observe(viewLifecycleOwner,
                    {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            3 -> {
                searchDatabaseByHighToLow(searchQuery).observe(viewLifecycleOwner,
                    {
                        adapter.updateList(it as ArrayList<Note>)
                    })
            }
            4 -> {
                searchDatabaseByRed(searchQuery).observe(viewLifecycleOwner,
                    {
                        // setAdapter(it as ArrayList<Note>)
                        adapter.updateList(it as ArrayList<Note>)
                        //         adapter.notifyDataSetChanged()
                    })
            }
            5 -> {
                searchDatabaseByYellow(searchQuery).observe(viewLifecycleOwner,
                    {
                        adapter.updateList(it as ArrayList<Note>)
                        // adapter.notifyDataSetChanged()
                    })
            }
            6 -> {
                searchDatabaseByGreen(searchQuery).observe(viewLifecycleOwner,
                    {
                        adapter.updateList(it as ArrayList<Note>)
//                        adapter.notifyDataSetChanged()
                    })
            }
        }
    }

    fun copyClicked(text: String) {
        val clipboard =
            appl.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip: ClipData = ClipData.newPlainText("simple text", text)
        clipboard.setPrimaryClip(clip)

    }

    fun showDeleteDialog(context: Context, binding: FragmentHomeBinding) {
        val materialAlertDialog = MaterialAlertDialogBuilder(context)
        materialAlertDialog.setTitle("Delete Note")
            .setMessage("Delete these notes?")
            .setCancelable(false)
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton("Delete") { dialog, id ->
                //loop running to size but we want to reduce time using counter
                for (i in selectedItem!!.indices) {
                    if (selectedItem!![i]) {
                        //Log.d("ACTION","Selected $i ${selectedItem!!.get(i)}")
                        delete(noteList[i])
                    }
                }
                onActionMode(false, binding)
                actionMode!!.finish()

                adapter.updateList(noteList)
                // selectionList.clear()
                Toast.makeText(
                    context,
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
