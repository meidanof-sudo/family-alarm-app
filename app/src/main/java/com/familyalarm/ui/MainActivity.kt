package com.familyalarm.ui

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.familyalarm.R
import com.familyalarm.data.FamilyRepository
import com.familyalarm.data.PrefsManager
import com.familyalarm.databinding.ActivityMainBinding
import com.familyalarm.service.AlarmTrigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private val repo = FamilyRepository()
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)

        checkDndPermission()
        requestNotificationPermission()
        updateUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (prefs.isInFamily) {
            refreshFamilyMembers()
            updateToken()
        }
    }

    private fun checkDndPermission() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            binding.dndPermissionCard.visibility = View.VISIBLE
            binding.btnGrantDnd.setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        } else {
            binding.dndPermissionCard.visibility = View.GONE
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    private fun updateUI() {
        if (prefs.isInFamily) {
            binding.setupGroup.visibility = View.GONE
            binding.alarmGroup.visibility = View.VISIBLE
            binding.tvFamilyCode.text = "Family Code: ${prefs.familyCode}"
            binding.tvMemberName.text = "Logged in as: ${prefs.memberName}"
            refreshFamilyMembers()
        } else {
            binding.setupGroup.visibility = View.VISIBLE
            binding.alarmGroup.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        binding.btnCreateFamily.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isBlank()) {
                binding.etName.error = "Enter your name"
                return@setOnClickListener
            }
            setLoading(true)
            scope.launch {
                try {
                    val code = withContext(Dispatchers.IO) {
                        repo.createFamily(name)
                    }
                    prefs.familyCode = code
                    prefs.memberName = name
                    updateUI()
                    Toast.makeText(
                        this@MainActivity,
                        "Family created! Share code: $code",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.btnJoinFamily.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val code = binding.etFamilyCode.text.toString().trim().uppercase()
            if (name.isBlank()) {
                binding.etName.error = "Enter your name"
                return@setOnClickListener
            }
            if (code.isBlank()) {
                binding.etFamilyCode.error = "Enter family code"
                return@setOnClickListener
            }
            setLoading(true)
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        repo.joinFamily(code, name)
                    }
                    prefs.familyCode = code
                    prefs.memberName = name
                    updateUI()
                    Toast.makeText(this@MainActivity, "Joined family!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    setLoading(false)
                }
            }
        }

        binding.btnSendAlarm.setOnClickListener {
            val code = prefs.familyCode ?: return@setOnClickListener
            val name = prefs.memberName ?: "Unknown"
            val uid = repo.getCurrentUid() ?: return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Send Alarm?")
                .setMessage("This will trigger a loud alarm on ALL family members' phones, even if they are on Do Not Disturb.")
                .setPositiveButton("SEND ALARM") { _, _ ->
                    binding.btnSendAlarm.isEnabled = false
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                AlarmTrigger.sendAlarm(code, name, uid)
                            }
                            Toast.makeText(
                                this@MainActivity,
                                getString(R.string.alarm_sent),
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Failed to send alarm: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            binding.btnSendAlarm.isEnabled = true
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnLeaveFamily.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Leave Family?")
                .setMessage("You will no longer receive alarms from this family group.")
                .setPositiveButton("Leave") { _, _ ->
                    scope.launch {
                        try {
                            val code = prefs.familyCode ?: return@launch
                            withContext(Dispatchers.IO) {
                                repo.leaveFamily(code)
                            }
                            prefs.clear()
                            updateUI()
                        } catch (e: Exception) {
                            Toast.makeText(
                                this@MainActivity,
                                "Error: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun refreshFamilyMembers() {
        val code = prefs.familyCode ?: return
        scope.launch {
            try {
                val family = withContext(Dispatchers.IO) { repo.getFamily(code) }
                val membersList = family.members.joinToString("\n") { "  \u2022 ${it.name}" }
                binding.tvMembers.text = "Family Members:\n$membersList"
            } catch (_: Exception) { }
        }
    }

    private fun updateToken() {
        val code = prefs.familyCode ?: return
        val name = prefs.memberName ?: return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { repo.updateMyToken(code, name) }
            } catch (_: Exception) { }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnCreateFamily.isEnabled = !loading
        binding.btnJoinFamily.isEnabled = !loading
    }
}
