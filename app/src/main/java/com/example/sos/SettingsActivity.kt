import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var contactInput: EditText
    private lateinit var addButton: Button
    private lateinit var contactsList: ListView
    private lateinit var messageInput: EditText
    private lateinit var saveMessageButton: Button

    private val contacts = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        contactInput = findViewById(R.id.contactInput)
        addButton = findViewById(R.id.addButton)
        contactsList = findViewById(R.id.contactsList)
        messageInput = findViewById(R.id.messageInput)
        saveMessageButton = findViewById(R.id.saveMessageButton)

        // Load saved contacts and message
        val prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE)
        contacts.addAll(prefs.getStringSet("emergency_contacts", setOf()) ?: setOf())
        messageInput.setText(prefs.getString("emergency_message", "I need help! My location is: "))

        updateContactsList()

        addButton.setOnClickListener {
            val contact = contactInput.text.toString().trim()
            if (contact.isNotEmpty()) {
                contacts.add(contact)
                contactInput.text.clear()
                updateContactsList()
                saveContacts()
            }
        }

        contactsList.setOnItemClickListener { _, _, position, _ ->
            contacts.remove(contacts.elementAt(position))
            updateContactsList()
            saveContacts()
        }

        saveMessageButton.setOnClickListener {
            val message = messageInput.text.toString()
            val prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE)
            prefs.edit().putString("emergency_message", message).apply()
            Toast.makeText(this, "Message saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateContactsList() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, contacts.toList())
        contactsList.adapter = adapter
    }

    private fun saveContacts() {
        val prefs = getSharedPreferences("SOSPrefs", MODE_PRIVATE)
        prefs.edit().putStringSet("emergency_contacts", contacts).apply()
    }
}