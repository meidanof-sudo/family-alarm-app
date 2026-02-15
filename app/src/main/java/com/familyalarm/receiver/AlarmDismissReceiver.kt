package com.familyalarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.familyalarm.service.AlarmSoundService

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmSoundService.stop(context)
    }
}
