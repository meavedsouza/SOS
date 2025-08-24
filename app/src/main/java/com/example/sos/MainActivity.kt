package com.examplesos  // Changed to match your error message path

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class MainActivity : AppCompatActivity() {

    private lateinit var sosButton: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val permissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.CALL_PHONE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val permissionRequestCode = 100  // Fixed: removed underscores

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sosButton = findViewById(R.id.sosButton)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)  // Fixed: initialized here

        // Check and request permissions
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(this, permissions, permissionRequestCode)  // Fixed: used correct variable name
        }

        sosButton.setOnClickListener {
            if (hasPermissions()) {
                triggerEmergency()
            } else {
                Toast.makeText(this, "Please grant permissions first", Toast.LENGTH_SHORT).show()
            }
        }

        // Long press for settings
        sosButton.setOnLongClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            true
        }
    }

    private fun hasPermissions(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {  // Fixed: used correct variable name
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getLocation(callback: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            callback("Location permission needed")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->  // Fixed: specified type explicitly
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                callback("https://maps.google.com/?q=$lat,$lng")
            } else {
                callback("Location unavailable")
            }
        }.addOnFailureListener {
            callback("Location error")
        }
    }

    private fun triggerEmergency() {
        // Get emergency contacts from SharedPreferences
        val prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE)
        val contacts = prefs.getStringSet("emergency_contacts", setOf()) ?: setOf()
        val baseMessage = prefs.getString("emergency_message", "I need help! My location is: ") ?: "I need help! My location is: "

        if (contacts.isEmpty()) {
            Toast.makeText(this, "No emergency contacts set", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            return
        }

        // Get location and send messages
        getLocation { location ->
            val message = "$baseMessage $location"

            // Send SMS to all emergency contacts
            val smsManager = SmsManager.getDefault()  // Using default for now to avoid complexity
            for (contact in contacts) {
                try {
                    smsManager.sendTextMessage(contact, null, message, null, null)
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to send SMS to $contact", Toast.LENGTH_SHORT).show()
                }
            }

            // Make emergency call to first contact
            val firstContact = contacts.firstOrNull()
            firstContact?.let {
                val callIntent = Intent(Intent.ACTION_CALL)
                callIntent.data = Uri.parse("tel:$it")
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startActivity(callIntent)
                }
            }

            Toast.makeText(this, "Emergency alert sent with location!", Toast.LENGTH_SHORT).show()
        }
    }
}