package com.optisol.twilio_voice

import android.content.Context
import com.twilio.voice.Call
import java.util.HashMap


interface VoiceApplicationListener {
     fun onRinging(call: Call)
     fun onConnectFailure(message: String)
     fun onConnected(call: Call)
     fun onReconnecting(call: Call, message: String?)
     fun onReconnected(call: Call)
     fun onDisconnected(call: Call, message: String)
     fun onCallWarning(call: Call, message: String)

}



interface VoiceSession{

    fun startCall(context: Context, params: HashMap<String, String>, accessToken: String)

    fun answer(context: Context)

    fun onDestroy()

    fun disconnect()

    fun hold()

    fun mute()


}