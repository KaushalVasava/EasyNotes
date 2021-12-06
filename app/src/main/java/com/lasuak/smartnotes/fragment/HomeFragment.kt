package com.lasuak.smartnotes.fragment

import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.lasuak.smartnotes.NoteListAdapter
import com.lasuak.smartnotes.NoteListener
import com.lasuak.smartnotes.R
import com.lasuak.smartnotes.data.model.Note
import com.lasuak.smartnotes.databinding.FragmentHomeBinding
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.counter
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.is_in_action_mode
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.is_select_all
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel.Companion.selectedItem

class HomeFragment : Fragment(R.layout.fragment_home), NoteListener,
    SearchView.OnQueryTextListener {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var noteViewModel: NoteViewModel

    companion object {
        var order = 0
        var noteList = ArrayList<Note>()
        var editor: SharedPreferences.Editor? = null
        var preferences: SharedPreferences? = null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        noteViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(NoteViewModel::class.java)

        setHasOptionsMenu(true)

        //swipe to delete color and icon
        noteViewModel.colorDrawableBackground = ColorDrawable(Color.parseColor("#FFF67373"))
        noteViewModel.deleteIcon =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!

//        if (!noteViewModel.checkSecurity) {
//            noteViewModel.checkAppSecurity(binding, this)
//        }

        //shared preference for save layout configuration
        preferences= requireActivity().getSharedPreferences("DATA", AppCompatActivity.MODE_PRIVATE)
        editor = preferences!!.edit()
        editor!!.apply()

        //Shared preference for sort order
        order = preferences!!.getInt("Order", 0)

        binding.layoutChoose.setOnClickListener {
            noteViewModel.layoutCheck(binding)
        }
        noteViewModel.checkOrder(requireContext(), binding, viewLifecycleOwner, this)

        //Sort Note functionality
        noteViewModel.sortNote(requireContext(),binding, viewLifecycleOwner, this)
        //NEW NOTE
        binding.fab.setOnClickListener {
            binding.recyclerView.adapter = noteViewModel.adapter
            val action =HomeFragmentDirections.actionHomeFragmentToNewNoteFragment()
            findNavController().navigate(action)
        }
        if (!is_in_action_mode) {
            noteViewModel.swipeToDelete(binding)
        }
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_note_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Search notes"
        searchView.isSubmitButtonEnabled = true
        searchView.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_security -> {
                noteViewModel.setPassword(requireContext())
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onItemClicked(position: Int, note: Note) {
        if (is_in_action_mode) {
            if (selectedItem!![position]) {
                selectedItem!![position] = false
                counter--
                noteViewModel.actionMode!!.title = "${counter}/${noteList.size} Selected"
            } else {
                selectedItem!![position] = true
                counter++
                noteViewModel.actionMode!!.title = "${counter}/${noteList.size} Selected"
            }
        } else if (!is_in_action_mode) {
            val action =HomeFragmentDirections.actionHomeFragmentToOpenNoteFragment(
                    note.title, note.note, note.id, note.reminderTime, note.priority,note.time
                )
//            val pref = requireActivity().getSharedPreferences("NOTE",MODE_PRIVATE).edit()
//            pref.putString("note_title",note.title)
//            pref.putString("note_text",note.note)
//            pref.putInt("note_id",note.id)
//            pref.putString("note_reminder",note.reminderTime)
//            pref.putInt("note_priority",note.priority)
//            pref.apply()
            findNavController().navigate(action)
        }
    }

    override fun onCopyClicked(note: Note, text: String?) {
        noteViewModel.copyClicked(text!!)
        Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
    }

    private val callback = object : ActionMode.Callback {
        override fun onCreateActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            val menuInflater = MenuInflater(requireContext())
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
                    if (counter == 0) {
                        Toast.makeText(
                            requireContext(),
                            "Please select a note",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (selectedItem!!.isNotEmpty()) {
                        if (counter == noteList.size) {
                            noteViewModel.deleteAllNote(binding, requireContext())
                            noteViewModel.adapter.updateList(noteList)
                        } else {
                            noteViewModel.showDeleteDialog(requireContext(), binding)
                        }
                    }
                    true
                }
                R.id.action_selectAll -> {
                    if (!is_select_all) {
                        item.setIcon(R.drawable.ic_select_all_on)
                        for (i in 0 until noteList.size)
                            selectedItem!![i] == true

                        counter = noteList.size
                        noteViewModel.actionMode!!.title = "${counter}/${noteList.size} Selected"
                        is_select_all = true
                    } else {
                        item.setIcon(R.drawable.ic_select_all_off)
                        for (i in 0 until noteList.size)
                            selectedItem!![i] == false

                        counter = 0
                        is_select_all = false
                        noteViewModel.actionMode!!.title = "${counter}/${noteList.size} Selected"
                    }
                    noteViewModel.adapter.notifyDataSetChanged()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            noteViewModel.onActionMode(false, binding)
            noteViewModel.actionMode = null
        }
    }

    override fun onAnyItemLongClicked(position: Int) {
        if (!is_in_action_mode) {
            noteViewModel.onActionMode(true, binding)
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
        if (noteViewModel.actionMode == null) {
            noteViewModel.actionMode =
                (activity as AppCompatActivity).startSupportActionMode(callback)!!
        }
        noteViewModel.actionMode!!.title = "${counter}/${noteList.size} Selected"
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null) {
            noteViewModel.searchDatabase(query, viewLifecycleOwner)
        }
        return true
    }
}