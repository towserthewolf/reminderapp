package com.example.reminderapp

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.Notification
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.get
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import androidx.work.workDataOf
import com.example.reminderapp.databinding.ActivityMainBinding
import com.example.reminderapp.databinding.ReminderDialogBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.addReminder.setOnClickListener{
            addReminder()
        }
    }

    private fun addReminder() {
        if (Build.VERSION.SDK_INT >= 33 && !NotificationManagerCompat.from(this)
                .areNotificationsEnabled()
        ) {
            showNotificationPermissionDialog()
        } else {
            addReminderDialog()
        }
    }
    private fun addReminderDialog() {
        val dialogBinding =
            ReminderDialogBinding.bind(layoutInflater.inflate(R.layout.reminder_dialog, null))
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.remindertype.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.ReminderTypes)
        )
        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        val pickedDate = Calendar.getInstance()
        dialogBinding.selectButton.setOnClickListener {

            val year = pickedDate.get(Calendar.YEAR)
            val month = pickedDate.get(Calendar.MONTH)
            val day = pickedDate.get(Calendar.DAY_OF_MONTH)
            val hour = pickedDate.get(Calendar.HOUR_OF_DAY)
            val minute = pickedDate.get(Calendar.MINUTE)
            DatePickerDialog(
                this,
                DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
                    TimePickerDialog(
                        this,
                        TimePickerDialog.OnTimeSetListener { view, hourOfDay, minute ->
                            pickedDate.set(year, month, dayOfMonth, hourOfDay, minute)
                            Log.d("Date and Time", "Picked Date and Time $pickedDate")
                            dialogBinding.pickDateAndTime.text =
                                getCurrentDateAndTime(pickedDate.timeInMillis)
                        },
                        hour,
                        minute,
                        false
                    ).show()
                },
                year,
                month,
                day
            )
                .show()
        }
        dialogBinding.submitButton.setOnClickListener {
            if (dialogBinding.eyTitle.text.isNullOrEmpty()) {
                dialogBinding.eyTitle.requestFocus()
                dialogBinding.eyTitle.error = "Please Provide Title"
            } else if (dialogBinding.pickDateAndTime.text == resources.getString(R.string.date_and_time)) {
                dialogBinding.pickDateAndTime.error = "Please select Date and Time"
            } else {
                val timeDelayinSeconds =
                    (pickedDate.timeInMillis / 1000L) - (Calendar.getInstance().timeInMillis / 1000L)
                if (timeDelayinSeconds < 0) {
                    Toast.makeText(this, "Cant set reminders for past", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                createWorkRequest(
                    dialogBinding.eyTitle.text.toString(),
                    resources.getStringArray(R.array.ReminderTypes)[dialogBinding.remindertype.selectedItemPosition],
                    timeDelayinSeconds
                )
                Toast.makeText(this, "ReminderAdded", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
        }
    }
            private fun createWorkRequest(title: String, reminderType: String, delay: Long) {
                val reminderWorkRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.SECONDS)
                    .setInputData(workDataOf("Title" to "Todo: $reminderType", "Message" to title))
                    .build()
                WorkManager.getInstance(this).enqueue(reminderWorkRequest)
}

    private fun getCurrentDateAndTime(millis:Long):String{
return SimpleDateFormat("dd-MM-yyy hh:mm a", Locale.getDefault()).format(Date(millis))
    }

    private fun showNotificationPermissionDialog() {
        MaterialAlertDialogBuilder(
            this,
            com.google.android.material.R.style.MaterialAlertDialog_Material3
        )
            .setTitle("Notification Permission")
            .setMessage("Notification permission required to show notifications")
            .setPositiveButton("OK")
            { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(
                        android.Manifest,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private val notificationPermissionLauncher=registerForActivityResult(ActivityResultContracts.RequestPermission()){
        isGranted ->
        if(!isGranted){
            if(Build.VERSION.SDK_INT>=33){
                if(shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)){
                    showNotificationPermissionDialog()
                }else{
                    showSettingsDialog()
                }
                }
        }else{
            Toast.makeText(this,"Notification permission granted",Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettingsDialog() {
        MaterialAlertDialogBuilder(this,com.google.android.material.R.style.MaterialAlertDialog_Material3)
            .setTitle("Notification Permission")
            .setMessage("Notification permission required to show notifications")
            .setPositiveButton("OK")
            { _, _ ->
                val intent= Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data= Uri.parse("package:${this.applicationContext?.packageName}")
                startActivity(intent)
            }
            .setNegativeButton("Cancel",null)
            .show()


    }
}