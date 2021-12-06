package com.lasuak.smartnotes.fragment

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.lasuak.smartnotes.R
import com.lasuak.smartnotes.broadcast.AlarmReceiver
import com.lasuak.smartnotes.data.model.Note
import com.lasuak.smartnotes.databinding.FragmentOpenNoteBinding
import com.lasuak.smartnotes.ui.activities.MainActivity
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

class OpenNoteFragment : Fragment(R.layout.fragment_open_note) {

    private lateinit var binding: FragmentOpenNoteBinding
    private val args: OpenNoteFragmentArgs by navArgs()

    private lateinit var textToSpeech: TextToSpeech
    private var txtId: Int = 0
    private var txtNote: String? = null
    private var txtTitle: String? = null
    private lateinit var noteTime: String
    private var reminderTime: String? = null
    private val mCalendar = Calendar.getInstance()
    private lateinit var noteViewModel: NoteViewModel

    companion object {
        private var Hour = 0
        private var Minute = 0
        private var milliSeconds = 0;
        private var count = 0
        private var priority = 3

        @JvmStatic
        private var isValid = false
        private var check = false
        private var checkPriority = 3
    }

    @SuppressLint("QueryPermissionsNeeded")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentOpenNoteBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        noteViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(NoteViewModel::class.java)

        //val pref= requireActivity().getSharedPreferences("NOTE",MODE_PRIVATE)
        txtTitle = args.noteTitle //pref.getString("note_title",null)//args.noteTitle
        txtId = args.noteId //pref.getString("note_text",null)//args.noteId
        txtNote = args.noteDetail //pref.getInt("note_id",0)//args.noteDetail
        noteTime = args.time

        reminderTime = args.reminderTime //pref.getString("note_reminder",null)//args.reminderTime
        priority = args.priority //pref.getInt("note_priority",3)//args.priority
        checkPriority = priority

        if (savedInstanceState != null) {
            val rTime = savedInstanceState.getString("remTime")
            binding.txtReminder.text = rTime
            priority = savedInstanceState.getInt("save_priority")
            Log.d("NEW", "Priority : $priority")
            isValid = savedInstanceState.getBoolean("validPassword")
        }
        //NOTE PREFERENCE FOR GETTING DATA WHEN REMINDER NOTIFICATION CLICK
        val notePref =
            requireActivity().getSharedPreferences("NOTE_DATA", AppCompatActivity.MODE_PRIVATE)
        val editor = notePref!!.edit()

        binding.txtReminder.setText(reminderTime)
        binding.editTitle.setText(txtTitle)
        binding.editText.setText(txtNote)
        if (binding.txtReminder.text.toString().isNotEmpty()) {
            binding.cancelReminder.visibility = View.VISIBLE
            Log.d("R", "Reminder not null")
        }

//Priority based notes
        val preferences =
            requireActivity().getSharedPreferences("PASSWORD", AppCompatActivity.MODE_PRIVATE)
        val password = preferences!!.getString("password", null);

        //Set Priority of notes
        setPriority(password, preferences)
        Log.d("NEW", "after set Priority : $priority")


        //onClick listeners
        //Priority
        binding.priorityGreen.setOnClickListener {
            binding.priorityGreen.setImageResource(R.drawable.ic_done)
            binding.priorityRed.setImageResource(0)
            binding.priorityYellow.setImageResource(0)
            priority = 3
            Log.d("P", "priority green : $priority")
        }
        binding.priorityYellow.setOnClickListener {
            binding.priorityYellow.setImageResource(R.drawable.ic_done)
            binding.priorityRed.setImageResource(0)
            binding.priorityGreen.setImageResource(0)
            priority = 2
            Log.d("P", "priority yellow : $priority")
        }
        binding.priorityRed.setOnClickListener {
            binding.priorityRed.setImageResource(R.drawable.ic_done)
            binding.priorityYellow.setImageResource(0)
            binding.priorityGreen.setImageResource(0)
            priority = 1

            //val password = preferences!!.getString("password", "");

            if (password == null) {
                val title = "Set New Password"
                val description = "Secure your note : " +
                        "\n - Minimum 4 character required \n - Maximum 15 characters allow" +
                        "\n - You can use characters AbC..." +
                        "\n - Numbers ie. 1,2,3..." +
                        "\n - Special symbols ie. #$@..."
                setPassword(null, preferences, title, description, false)
            }
        }

        /** REMINDER FOR NOTE */
        createNotification()
        binding.imgReminder.setOnClickListener {

            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.reminder_layout)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(false)

            val setTime = dialog.findViewById<TextView>(R.id.btnTime)
            val setDate = dialog.findViewById<TextView>(R.id.btnDate)
            val okay = dialog.findViewById<Button>(R.id.btnOk)
            val cancel = dialog.findViewById<Button>(R.id.btnCancel)
            val timePicker = dialog.findViewById<Spinner>(R.id.timePicker)
            val reminderStatus = dialog.findViewById<Spinner>(R.id.reminderStatus)

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            setDate.text = dateFormatter.format(mCalendar.time)
            val formatter = SimpleDateFormat("hh:mm a, dd/MM/yyyy", Locale.getDefault())
            setTime.text = formatter.format(mCalendar.time)


            reminderSelection(resources.getStringArray(R.array.time_list), timePicker, 2, setTime)
            reminderSelection(
                resources.getStringArray(R.array.reminder_type_list),
                reminderStatus,
                1,
                setTime
            )

            //TIME PICKER LOGIC
            setTime.setOnClickListener {
                // val formatter1 = SimpleDateFormat("hh:mm a")
                val hour = formatter.format(mCalendar.time).substring(0, 2).trim()
                val min = formatter.format(mCalendar.time).substring(3, 5).trim()
                Log.d("R", " hour $hour and min $min ")
                val materialTimePicker: MaterialTimePicker = MaterialTimePicker.Builder()
                    .setTitleText("SET TIME")
                    .setHour(hour.toInt())
                    .setMinute(min.toInt())
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .build()

                materialTimePicker.show(requireActivity().supportFragmentManager, "TIME")
                // on clicking the positive button of the time picker
                // dialog update the TextView accordingly
                materialTimePicker.addOnPositiveButtonClickListener {
                    val pickedHour: Int = materialTimePicker.hour
                    val pickedMinute: Int = materialTimePicker.minute
                    val formattedTime: String = when {
                        pickedHour > 12 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour - 12}:0${materialTimePicker.minute} pm"
                            } else {
                                "${materialTimePicker.hour - 12}:${materialTimePicker.minute} pm"
                            }
                        }
                        pickedHour == 12 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour}:0${materialTimePicker.minute} pm"
                            } else {
                                "${materialTimePicker.hour}:${materialTimePicker.minute} pm"
                            }
                        }
                        pickedHour == 0 -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour + 12}:0${materialTimePicker.minute} am"
                            } else {
                                "${materialTimePicker.hour + 12}:${materialTimePicker.minute} am"
                            }
                        }
                        else -> {
                            if (pickedMinute < 10) {
                                "${materialTimePicker.hour}:0${materialTimePicker.minute} am"
                            } else {
                                "${materialTimePicker.hour}:${materialTimePicker.minute} am"
                            }
                        }
                    }
                    mCalendar.set(Calendar.HOUR_OF_DAY, pickedHour)
                    mCalendar.set(Calendar.MINUTE, pickedMinute)
                    mCalendar.set(Calendar.SECOND, 0)
                    // then update the preview TextView
                    setTime.text = formattedTime
                }
            }

            //DATE PICKER LOGIC
            val dateListener =
                DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                    mCalendar.set(Calendar.YEAR, year)
                    mCalendar.set(Calendar.MONTH, monthOfYear)
                    mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    val temp = "$dayOfMonth/${monthOfYear + 1}/$year"
                    setDate.text = temp
                }

            setDate.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    requireContext(),
                    dateListener,
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }
            dialog.show()

            okay.setOnClickListener() {
                val time = setTime.text.toString() + "," + setDate.text.toString()
                setReminder(editor, time) //,setTime.text.toString())
                Toast.makeText(requireContext(), "Reminder Set at $time", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            cancel.setOnClickListener() {
                dialog.dismiss()
            }
        }

        //text to speech
        textToSpeech = TextToSpeech(requireActivity().applicationContext) {
            fun onInit(i: Int) {
                if (i == TextToSpeech.SUCCESS) {
                    textToSpeech.language = Locale.ENGLISH;

                }
            }
        }

        //replaced by activity result
        val speakLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult())
            { result: ActivityResult ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    val result1 = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    var getText = binding.editText.text.toString()
                    getText += result1!![0]
                    binding.editText.setText(getText)
                }
            }

        binding.micImg.setOnClickListener() {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                //startActivityForResult(intent, 1)
                speakLauncher.launch(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Your device not supported for speech input",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.copyNote.setOnClickListener {
            val clipboard =
                requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText("simple note", txtTitle + "\n" + txtNote)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Note copied", Toast.LENGTH_SHORT).show()
        }
        binding.cancelReminder.setOnClickListener {
            cancelReminder()
            binding.cancelReminder.visibility = View.GONE
            binding.txtReminder.visibility = View.GONE
            //  binding.txtReminder.text = null
        }
        return binding.root
    }

    private fun setPriority(password: String?, preferences: SharedPreferences) {
        when (priority) {
            3 -> {
                binding.priorityGreen.setImageResource(R.drawable.ic_done)
                isValid = true
            }
            2 -> {
                binding.priorityYellow.setImageResource(R.drawable.ic_done)
                isValid = true
            }
            1 -> {
                binding.priorityRed.setImageResource(R.drawable.ic_done)

                if (checkPriority == priority) {
                    Log.d("NEW", "ONLY when opening an app")
                    Log.d("R", "nothing red :  $isValid")

                    if (isValid) {
                        Log.d("R", "nothing red")
                    } else {
                        //Hides contains before password
                        binding.openNoteLayout.visibility = View.INVISIBLE
                        binding.micImg.visibility = View.INVISIBLE
                        binding.speakImg.visibility = View.INVISIBLE

                        val title = "Enter Password"
                        val description = "Your note is secure"
                        setPassword(password, preferences, title, description, true)
                    }
                } else {
                    Log.d("NEW", "Wen an app changed priority")
                    if (password == null) {
                        val title = "Set New Password"
                        val description = "Secure your note : " +
                                "\n - Minimum 4 character required \n - Maximum 15 characters allow" +
                                "\n - You can use characters AbC..." +
                                "\n - Numbers ie. 1,2,3..." +
                                "\n - Special symbols ie. #$@..."
                        setPassword(null, preferences, title, description, false)
                    }
                }
            }
        }
    }

    private fun setPassword(
        password: String?,
        preferences: SharedPreferences,
        title: String,
        description: String,
        isSecure: Boolean
    ) {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.custom_dialog)
        dialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.back_dialog
            )
        )
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        val okay = dialog.findViewById<Button>(R.id.yesbtn)
        val cancel = dialog.findViewById<Button>(R.id.cancelbtn)
        val dialogTitle = dialog.findViewById<TextView>(R.id.textAlert)
        val message = dialog.findViewById<TextView>(R.id.alertDescription)
        val password2 = dialog.findViewById<EditText>(R.id.txtPassword)

        dialogTitle.text = title
        message.text = description

        dialog.show()

        okay.setOnClickListener() {
            val pass = password2.text.toString()
            if (isSecure) {
                Log.d("PW", "Check password")
                if (pass == password) {
                    isValid = true
                    binding.openNoteLayout.visibility = View.VISIBLE
                    binding.micImg.visibility = View.VISIBLE
                    binding.speakImg.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Your password is incorrect,Please try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().popBackStack()
                }
            } else {
                val editor1 = preferences.edit()
                if (pass.length in 4..15) {
                    editor1!!.putString("password", pass)
                    editor1.apply()

                    Toast.makeText(
                        requireContext(),
                        "Your password is set successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        requireContext(),
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
            if (priority == checkPriority)
                findNavController().popBackStack()
        }
    }

    //REMINDER METHODS
    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = "Reminder"
            val desc = "Note Reminder"

            val channel =
                NotificationChannel(
                    MainActivity.CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_HIGH
                )
            channel.description = desc

            val notificationManager =
                requireActivity().getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setReminder(editor: SharedPreferences.Editor, time: String) {
        editor.putString("text", binding.editText.text.toString())
        editor.putString("title", binding.editTitle.text.toString())
        editor.putString("time", time)
        editor.putInt("priority", priority)
        editor.putInt("reminder", 1);
        editor.apply()

        val intent = Intent(requireActivity().baseContext, AlarmReceiver::class.java)
        intent.putExtra("noteTitle", "${binding.editTitle.text} ${binding.editText.text}")

        val pendingIntent = PendingIntent.getBroadcast(
            requireActivity().baseContext, 12, intent, 0
        )
        val alarmManager =
            requireActivity().getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        when (count) {
            0 -> {
                //Only once reminder
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    mCalendar.timeInMillis + milliSeconds,
                    pendingIntent
                )
            }
            1 -> {
                //repeating reminder
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP, mCalendar.timeInMillis + milliSeconds,
                    5000, pendingIntent
                )
            }
            2 -> { //repeat after day interval
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, mCalendar.timeInMillis + milliSeconds,
                    AlarmManager.INTERVAL_DAY, pendingIntent
                )
            }
        }
        binding.txtReminder.visibility = View.VISIBLE
        binding.cancelReminder.visibility = View.VISIBLE
        binding.txtReminder.setText(time)//(mCalendar.time).toString()
        Minute = 0;
        Hour = 0;
        Log.d("R", "inside reminder ${binding.txtReminder.text} ")
    }

    private fun reminderSelection(
        items: Array<String>,
        spinner: Spinner,
        flag: Int,
        setDate: TextView
    ) {
        val adapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            items
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spinner.adapter = adapter

        if (flag == 1) {
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View?, i: Int, l: Long
                ) {
                    count = i
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        } else if (flag == 2) {
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>,
                    view: View?,
                    i: Int,
                    l: Long
                ) {

                    Minute = 0
                    Hour = 0
                    val time = adapterView.getItemAtPosition(i).toString();
                    if (time.substring(time.length - 6, time.length) == "Minute") {
                        Minute = time.substring(0, 2).trim().toInt()
                        Hour = 0
                        milliSeconds = Minute * 60 * 1000

                    } else if (time.substring(time.length - 4, time.length) == "Hour") {
                        Hour = time.substring(0, 2).trim().toInt()
                        Minute = 0
                        milliSeconds = Hour * 60 * 60 * 1000
                    }

                    var minute = mCalendar.get(Calendar.MINUTE) + Minute
                    if (minute > 60) {
                        Hour += minute / 60
                        minute %= 60
                    }
                    val hourOfDay = mCalendar.get(Calendar.HOUR_OF_DAY) + Hour
                    val xTime = when {
                        hourOfDay == 0 -> {
                            if (minute < 10) {
                                "${hourOfDay + 12}:0${minute} am"
                            } else {
                                "${hourOfDay + 12}:${minute} am"
                            }
                        }
                        hourOfDay > 12 -> {
                            if (minute < 10) {
                                "${hourOfDay - 12}:0${minute} pm"
                            } else {
                                "${hourOfDay - 12}:${minute} pm"
                            }
                        }
                        hourOfDay == 12 -> {
                            if (minute < 10) {
                                "${hourOfDay}:0${minute} pm"
                            } else {
                                "${hourOfDay}:${minute} pm"
                            }
                        }
                        else -> {
                            if (minute < 10) {
                                "${hourOfDay}:${minute} am"
                            } else {
                                "${hourOfDay}:${minute} am"
                            }
                        }
                    }
                    setDate.text = xTime
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        }
    }

    private fun cancelReminder() {
        val intent = Intent(requireActivity().baseContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireActivity().baseContext, 12, intent, 0
        )
        val alarmManager =
            requireActivity().getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Toast.makeText(requireContext(), "Reminder Cancelled", Toast.LENGTH_SHORT).show()
        binding.txtReminder.text = null
    }

    private fun deleteNote() {
        Log.d("P", "valid $isValid")
        if (isValid) {
            val getText = binding.editText.text.toString()
            val getTitle = binding.editTitle.text.toString()
            if (getText.isNotEmpty() || getTitle.isNotEmpty()) {
                val note = Note(
                    getTitle, getText, System.currentTimeMillis().toString(), noteTime, priority,
                    binding.txtReminder.text.toString(), txtId
                )

                val materialAlertDialog = MaterialAlertDialogBuilder(requireContext())
                materialAlertDialog.setTitle("Delete Note")
                    .setMessage("Delete this note?")
                    .setCancelable(false)
                    .setIcon(R.drawable.ic_delete)
                    .setPositiveButton("Delete") { dialog, id ->
                        noteViewModel.delete(note)
                        Toast.makeText(requireContext(), "Note Deleted", Toast.LENGTH_SHORT).show()
                        check = true
                        dialog.dismiss()
                        findNavController().popBackStack()
                    }
                    .setNegativeButton("No") { dialog, id ->
                        dialog.cancel()
                    }
                    .show()
            }
        }
    }

    private fun shareNote() {
        if (isValid) {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                val getText = binding.editText.text.toString()
                val getTitle = binding.editTitle.text.toString()
                if (getText.isNotEmpty() || getTitle.isNotEmpty()) {
                    putExtra(Intent.EXTRA_TEXT, getTitle + "\n" + getText)
                    type = "text/plain"
                }
            }
            try {
                startActivity(sendIntent)
            } catch (e: ActivityNotFoundException) {
                // Define what your app should do if no activity can handle the intent.
                Toast.makeText(requireContext(), "Not is Empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNote(isSave: Boolean) {
        val preferences =
            requireActivity().getSharedPreferences("PASSWORD", AppCompatActivity.MODE_PRIVATE)
        val oldPassword = preferences!!.getString("password", null)

        if (check == false) {
            val textName = binding.editText.text.toString().trim()
            val textTitle = binding.editTitle.text.toString().trim()

            if ((textName.isNotEmpty() || textTitle.isNotEmpty())
                && (txtNote != textName || txtTitle != textTitle)
                || (checkPriority != priority && priority != 1 ||
                        (oldPassword != null && priority == 1))
                || (reminderTime != binding.txtReminder.text.toString())
            ) {
                val formatter = SimpleDateFormat(
                    "hh:mm:ss aa,d MMM yyyy",
                    Locale.getDefault()
                )//getString(R.string.date_format))
                val date: String = formatter.format(Date()).substring(12)
                val time: String = formatter.format(Date()).substring(0, 11)
                Log.d("NEW", "Save iknside")
                val updateNote = Note(
                    textTitle,
                    textName,
                    time,
                    date,
                    priority,
                    binding.txtReminder.text.toString(),
                    txtId
                )
                noteViewModel.update(updateNote)
            } else {
                findNavController().popBackStack()
            }
            if (isSave) {
                check = true
                findNavController().popBackStack()
            }
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        savedInstanceState.putString("remTime", binding.txtReminder.text.toString())
        savedInstanceState.putInt("save_priority", priority)
        savedInstanceState.putBoolean("validPassword", isValid)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.open_item_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                saveNote(true)
            }
            R.id.action_save -> {
                saveNote(true)
            }
            R.id.action_share -> {
                shareNote()
            }
            R.id.delete -> {
                deleteNote()
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onStop() {
        super.onStop()
        textToSpeech.shutdown()
        hideKeyboard()
    }

    fun Fragment.hideKeyboard() {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(requireView().windowToken, 0)
    }
}