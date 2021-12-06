package com.lasuak.smartnotes.ui.activities

import android.annotation.SuppressLint
import android.app.*
import android.app.DatePickerDialog.OnDateSetListener
import android.content.*
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.lasuak.smartnotes.broadcast.AlarmReceiver
import com.lasuak.smartnotes.ui.viewmodel.NoteViewModel
import com.lasuak.smartnotes.R
import com.lasuak.smartnotes.ui.activities.MainActivity.Companion.CHANNEL_ID
import com.lasuak.smartnotes.data.model.Note
import com.lasuak.smartnotes.databinding.ActivityNewNoteBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class NewNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewNoteBinding

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var noteViewModel: NoteViewModel
    private val mCalendar = Calendar.getInstance()
    private var priorityNew = 3

    companion object {
        private var milliSeconds = 0;
        private var count = 1
        private var Hour = 0
        private var Minute = 0
        private var isShare = false

        @JvmStatic
        private var check = false
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("ClickableViewAccessibility", "QueryPermissionsNeeded", "SimpleDateFormat")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        noteViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(NoteViewModel::class.java)

        binding.editText.requestFocus()
        val actionBar = actionBar
        title = getString(R.string.new_note)
        actionBar?.setDisplayHomeAsUpEnabled(true)

        //shared text received from other apps
        if (intent?.action == Intent.ACTION_SEND) {
            if ("text/plain" == intent.type) {
                val txt = intent.getStringExtra(Intent.EXTRA_TEXT)
                binding.editText.setText(txt)
            }
        }
        val intent: Intent = intent

        val message: String = intent.getStringExtra("message").toString()
        Log.d("BR","message $message")

        var rTime: String? = null
        if (savedInstanceState != null) {
            rTime = savedInstanceState.getString("remTime")
            binding.txtReminder.text = rTime
            priorityNew = savedInstanceState.getInt("save_priorityNew")
        }

        //Priority based notes
        setPriority()

        if (rTime != null) {
            binding.cancelReminder.visibility = View.VISIBLE
        }

        /** REMINDER FOR NOTE */
        createNotification()
        binding.imgReminder.setOnClickListener {

            val dialog = Dialog(this)
            dialog.setContentView(R.layout.reminder_layout)
            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            dialog.setCancelable(false)

            val timePicker = dialog.findViewById<Spinner>(R.id.timePicker)
            val setTime = dialog.findViewById<TextView>(R.id.btnTime)
            val setDate = dialog.findViewById<TextView>(R.id.btnDate)
            val okay = dialog.findViewById<Button>(R.id.btnOk)
            val cancel = dialog.findViewById<Button>(R.id.btnCancel)
            val reminderStatus = dialog.findViewById<Spinner>(R.id.reminderStatus)

            val dateFormatter = SimpleDateFormat("dd/MM/yyyy")
            setDate.text = dateFormatter.format(mCalendar.time)
            val formatter = SimpleDateFormat("hh:mm a")
            setTime.text = formatter.format(mCalendar.time)

            reminderSelection(resources.getStringArray(R.array.time_list), timePicker, 2, setTime)
            reminderSelection(
                resources.getStringArray(R.array.reminder_type_list),
                reminderStatus,
                1,
                setTime
            )

            setTime.setOnClickListener {
                val hour= formatter.format(mCalendar.time).substring(0,2).trim()
                val min = formatter.format(mCalendar.time).substring(3,5).trim()

                val materialTimePicker: MaterialTimePicker = MaterialTimePicker.Builder()
                    .setTitleText("SET TIME")
                    .setHour(hour.toInt())
                    .setMinute(min.toInt())
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .build()

                materialTimePicker.show(supportFragmentManager, "TIME")
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
            val dateListener = OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                mCalendar.set(Calendar.YEAR, year)
                mCalendar.set(Calendar.MONTH, monthOfYear)
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                Log.d("TIME", "date : $dayOfMonth/${monthOfYear + 1}/$year")
                val temp = "$dayOfMonth/${monthOfYear + 1}/$year"
                setDate.text = temp
            }

            setDate.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    this,
                    dateListener,
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH)
                )
                datePickerDialog.show()
            }

            dialog.show()
            okay.setOnClickListener() {
                val notePref = getSharedPreferences("NOTE_DATA", MODE_PRIVATE)
                val editor = notePref!!.edit()
                val time = setTime.text.toString() + "," + setDate.text.toString()
                setReminder(editor, time)
                Toast.makeText(this, "Reminder Set at $time", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            cancel.setOnClickListener() {
                dialog.dismiss()
            }
        }

        //text to speech
        textToSpeech = TextToSpeech(applicationContext) {
            fun onInit(i: Int) {
                if (i == TextToSpeech.SUCCESS) {
                    /** Changed ENGLISH TO getDefault() */
                    textToSpeech.language = Locale.getDefault();
                }
            }
        }

        //SPEECH To Text
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
            if (intent.resolveActivity(packageManager) != null) {
                //startActivityForResult(intent, 1)
                speakLauncher.launch(intent)
            } else {
                Toast.makeText(
                    this,
                    "Your device not supported for speech input",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }

        binding.copyNote.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData = ClipData.newPlainText(
                "simple note",
                "${binding.editTitle.text} ${binding.editText.text}"
            )
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Note copied", Toast.LENGTH_SHORT).show()
        }
        binding.cancelReminder.setOnClickListener {
            cancelReminder()
            binding.cancelReminder.visibility = View.GONE
            binding.txtReminder.visibility = View.GONE

        }
    }

    private fun setPriority() {
        when (priorityNew) {
            3 -> binding.priorityGreen.setImageResource(R.drawable.ic_done)
            2 -> binding.priorityYellow.setImageResource(R.drawable.ic_done)
            1 -> {
                binding.priorityRed.setImageResource(R.drawable.ic_done)
                setPassword()
            }

        }
        //priorityNew = 3
        binding.priorityGreen.setOnClickListener {
            binding.priorityGreen.setImageResource(R.drawable.ic_done)
            binding.priorityRed.setImageResource(0)
            binding.priorityYellow.setImageResource(0)
            priorityNew = 3
        }
        binding.priorityYellow.setOnClickListener {
            binding.priorityYellow.setImageResource(R.drawable.ic_done)
            binding.priorityRed.setImageResource(0)
            binding.priorityGreen.setImageResource(0)
            priorityNew = 2
        }
        binding.priorityRed.setOnClickListener {
            binding.priorityRed.setImageResource(R.drawable.ic_done)
            binding.priorityYellow.setImageResource(0)
            binding.priorityGreen.setImageResource(0)
            priorityNew = 1

            //password set
            setPassword()
        }
    }

    //LOCK METHOD
    private fun setPassword() {
        val preferences = getSharedPreferences("PASSWORD", MODE_PRIVATE)
        val oldPassword = preferences!!.getString("password", null);

        if (oldPassword == null) {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.custom_dialog)
            dialog.window?.setBackgroundDrawable(
                ContextCompat.getDrawable(
                    this,
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
            val dialogTitle = dialog.findViewById<TextView>(R.id.textAlert)
            val password2 = dialog.findViewById<EditText>(R.id.txtPassword)

            dialogTitle.text = "Set New Password "

            dialog.show()

            okey.setOnClickListener() {
                val password = password2.text.toString()
                if (password.length in 4..15) {
                    val editor = preferences.edit()
                    editor!!.putString("password", password)
                    editor.apply()

                    Toast.makeText(
                        this@NewNoteActivity,
                        "Your password is set successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else{
                    Toast.makeText(
                        this@NewNoteActivity,
                        "Please set password of min of 4 character and maximum 15 character",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            cancel.setOnClickListener() {
                dialog.dismiss()
            }
        }
    }

    //Reminder methods
    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val name = "Reminder"
            val desc = "Note Reminder"

            val channel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH)
            channel.description = desc

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setReminder(editor: SharedPreferences.Editor, time: String) {
        editor.putString("text", binding.editText.text.toString())
        editor.putString("title", binding.editTitle.text.toString())
        //editor.putInt("priorityNew", priorityNew)
        editor.putString("time", time)
        editor.putInt("reminder", 1);
        editor.apply()

        val intent = Intent(baseContext, AlarmReceiver::class.java)
        intent.putExtra("noteTitle", "${binding.editTitle.text} ${binding.editText.text}")
        val pendingIntent = PendingIntent.getBroadcast(
            baseContext, 12, intent, 0
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
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
            2 -> {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP, mCalendar.timeInMillis + milliSeconds,
                    AlarmManager.INTERVAL_DAY, pendingIntent
                )
            }
        }
        binding.txtReminder.visibility = View.VISIBLE
        binding.cancelReminder.visibility = View.VISIBLE
        binding.txtReminder.text = time
    }

    private fun reminderSelection(
        items: Array<String>,
        spinner: Spinner,
        flag: Int,
        setDate: TextView
    ) {
        val adapter: ArrayAdapter<*> = ArrayAdapter<Any?>(
            this,
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
        val intent = Intent(baseContext, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            baseContext, 12, intent, 0
        )
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this@NewNoteActivity, "Reminder Cancelled", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.open_item_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                saveNote(true)
            }
            R.id.action_save -> {
                Log.d("P", "in save Save")
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

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)

        if (!binding.txtReminder.text.isNullOrEmpty()) {
            savedInstanceState.putString("remTime", binding.txtReminder.text.toString())
        }
        savedInstanceState.putInt("save_priorityNew", priorityNew)
        Log.d("NEW", "saved instance store $priorityNew")
    }

    @SuppressLint("SimpleDateFormat")
    private fun saveNote(isSave: Boolean) {

        Log.d("R", "Save note $check")
        val preferences = getSharedPreferences("PASSWORD", MODE_PRIVATE)
        val oldPassword = preferences!!.getString("password", null)


        if (check == false) {
            val textName = binding.editText.text.toString().trim()
            val textTitle = binding.editTitle.text.toString().trim()

            if ((textName.isNotEmpty() || textTitle.isNotEmpty()) && ((priorityNew == 1 && oldPassword != null) || priorityNew != 1)) {
                Log.d("R", "Save inside note")
                val formatter = SimpleDateFormat("hh:mm:ss aa,d MMM yyyy")//getString(R.string.date_format))
                val date: String = formatter.format(Date()).substring(12)
                val time: String = formatter.format(Date()).substring(0,11)

                lateinit var updateNote: Note
                if (binding.txtReminder.text.toString().isEmpty()) {
                    updateNote = Note(
                        textTitle,
                        textName,
                        time,
                        date,
                        priorityNew,
                        "",
                        0
                    )
                    noteViewModel.insert(updateNote)
                } else {
                    updateNote = Note(
                        textTitle,
                        textName,
                        time,
                        date,
                        priorityNew, binding.txtReminder.text.toString(), 0
                    )
                    noteViewModel.insert(updateNote)
                }
            }
            check = true
        } else {
            //finishAfterTransition()
            setResult(Activity.RESULT_CANCELED, intent)
        }
        if (isSave) {
            //finishAfterTransition()
            finish()
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun deleteNote() {
        val getText = binding.editText.text.toString()
        val getTitle = binding.editTitle.text.toString()

        val note = Note(
            getTitle, getText,System.currentTimeMillis().toString(),
            "anydate", priorityNew, binding.txtReminder.text.toString(), 0
        )
        check = true

        noteViewModel.delete(note)
        Toast.makeText(this, "Note Deleted", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun shareNote() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            val getText = binding.editText.text.toString()
            val getTitle = binding.editTitle.text.toString()
            if (getText.isNotEmpty() || getTitle.isNotEmpty()) {
                putExtra(Intent.EXTRA_TEXT, getTitle + "\n" + getText)
                type = "text/plain"
            }
            isShare = true
        }
        try {
            startActivity(sendIntent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Note is Empty", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        textToSpeech.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        check = false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveNote(false)
    }
}

