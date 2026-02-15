package com.familyalarm.ui

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.familyalarm.databinding.ActivityAlarmBinding
import com.familyalarm.service.AlarmSoundService

/**
 * Full-screen alarm activity that shows on the lock screen.
 * Displays who triggered the alarm and a dismiss button.
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val senderName = intent.getStringExtra(AlarmSoundService.EXTRA_SENDER_NAME) ?: "Family Member"
        binding.tvAlarmSender.text = getString(com.familyalarm.R.string.alarm_from, senderName)

        binding.btnDismiss.setOnClickListener {
            dismissAlarm()
        }
    }

    private fun dismissAlarm() {
        AlarmSoundService.stop(this)
        finish()
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        // Prevent accidental back-press dismiss; must use button
    }
}
