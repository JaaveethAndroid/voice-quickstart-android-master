package com.optisol.twilio_voice

import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.MenuItem
import android.widget.Chronometer
import com.twilio.audioswitch.AudioSwitch
import com.twilio.voice.*
import com.twilio.voice.Call.CallQualityWarning
import java.util.*

class VoiceService(val events: VoiceApplicationListener) : VoiceSession {
    /*
     * Audio device management
     */
    private val audioSwitch: AudioSwitch? = null
    private val savedVolumeControlStream = 0
    private val audioDeviceMenuItem: MenuItem? = null
    private val isReceiverRegistered = false

    // Empty HashMap, never populated for the Quickstart
    var params = HashMap<String, String>()
    private val chronometer: Chronometer? = null
    private val notificationManager: NotificationManager? = null
    private val alertDialog: AlertDialog? = null
    private val activeCallInvite: CallInvite? = null
    private var activeCall: Call? = null
    private val activeCallNotificationId = 0
    var callListener = callListener()
    override fun startCall(context: Context, params: HashMap<String, String>, accessToken: String) {
        val connectOptions = ConnectOptions.Builder(accessToken)
            .params(params)
            .build()
        activeCall = Voice.connect(context, connectOptions, callListener)
    }

    override fun answer(context: Context) {
        activeCallInvite!!.accept(context, callListener)
        if (alertDialog != null && alertDialog.isShowing) {
            alertDialog.dismiss()
        }
    }

    private fun callListener(): Call.Listener {
        return object : Call.Listener {
            /*
             * This callback is emitted once before the Call.Listener.onConnected() callback when
             * the callee is being alerted of a Call. The behavior of this callback is determined by
             * the answerOnBridge flag provided in the Dial verb of your TwiML application
             * associated with this client. If the answerOnBridge flag is false, which is the
             * default, the Call.Listener.onConnected() callback will be emitted immediately after
             * Call.Listener.onRinging(). If the answerOnBridge flag is true, this will cause the
             * call to emit the onConnected callback only after the call is answered.
             * See answeronbridge for more details on how to use it with the Dial TwiML verb. If the
             * twiML response contains a Say verb, then the call will emit the
             * Call.Listener.onConnected callback immediately after Call.Listener.onRinging() is
             * raised, irrespective of the value of answerOnBridge being set to true or false
             */
            override fun onRinging(call: Call) {
                Log.d(TAG, "Ringing")
                /*
                 * When [answerOnBridge](https://www.twilio.com/docs/voice/twiml/dial#answeronbridge)
                 * is enabled in the <Dial> TwiML verb, the caller will not hear the ringback while
                 * the call is ringing and awaiting to be accepted on the callee's side. The application
                 * can use the `SoundPoolManager` to play custom audio files between the
                 * `Call.Listener.onRinging()` and the `Call.Listener.onConnected()` callbacks.
                 */
                events.onRinging(call)
            }

            override fun onConnectFailure(call: Call, error: CallException) {
                audioSwitch!!.deactivate()
                Log.d(TAG, "Connect failure")
                val message = String.format(
                    Locale.US,
                    "Call Error: %d, %s",
                    error.errorCode,
                    error.message
                )
                Log.e(TAG, message)
                events.onConnectFailure(message)
            }

            override fun onConnected(call: Call) {
                audioSwitch!!.activate()
                Log.d(TAG, "Connected")
                activeCall = call
                events.onConnected(call)
            }

            override fun onReconnecting(call: Call, callException: CallException) {
                Log.d(TAG, "onReconnecting")
                events.onReconnecting(call,callException.message)
            }

            override fun onReconnected(call: Call) {
                Log.d(TAG, "onReconnected")
                events.onReconnected(call)
            }

            override fun onDisconnected(call: Call, error: CallException?) {
                audioSwitch!!.deactivate()
                Log.d(TAG, "Disconnected")
                if (error != null) {
                    val message = String.format(
                        Locale.US,
                        "Call Error: %d, %s",
                        error.errorCode,
                        error.message
                    )
                    Log.e(TAG, message)
                    events.onDisconnected(call,message)

                }
            }

            /*
             * currentWarnings: existing quality warnings that have not been cleared yet
             * previousWarnings: last set of warnings prior to receiving this callback
             *
             * Example:
             *   - currentWarnings: { A, B }
             *   - previousWarnings: { B, C }
             *
             * Newly raised warnings = currentWarnings - intersection = { A }
             * Newly cleared warnings = previousWarnings - intersection = { C }
             */
            override fun onCallQualityWarningsChanged(
                call: Call,
                currentWarnings: MutableSet<CallQualityWarning>,
                previousWarnings: MutableSet<CallQualityWarning>
            ) {
                if (previousWarnings.size > 1) {
                    val intersection: MutableSet<CallQualityWarning> = HashSet(currentWarnings)
                    currentWarnings.removeAll(previousWarnings)
                    intersection.retainAll(previousWarnings)
                    previousWarnings.removeAll(intersection)
                }
                val message = String.format(
                    Locale.US,
                    "Newly raised warnings: $currentWarnings Clear warnings $previousWarnings"
                )
                events.onCallWarning(call,message)
                Log.e(TAG, message)
            }
        }
    }

    override fun onDestroy() {
        /*
         * Tear down audio device management and restore previous volume stream
         */
        audioSwitch!!.stop()
    }

    private fun handleCancel() {
        if (alertDialog != null && alertDialog.isShowing) {
            alertDialog.cancel()
        }
    }

    /*
     * Disconnect from Call
     */
    override fun disconnect() {
        if (activeCall != null) {
            activeCall!!.disconnect()
            activeCall = null
        }
    }

    override fun hold() {
        if (activeCall != null) {
            val hold = !activeCall!!.isOnHold
            activeCall!!.hold(hold)
        }
    }

    override fun mute() {
        if (activeCall != null) {
            val mute = !activeCall!!.isMuted
            activeCall!!.mute(mute)
        }
    }

    /*
     * Show the current available audio devices.
     */
    private fun showAudioDevices(context: Context) {
        val selectedDevice = audioSwitch!!.selectedAudioDevice
        val availableAudioDevices = audioSwitch.availableAudioDevices
        if (selectedDevice != null) {
            val selectedDeviceIndex = availableAudioDevices.indexOf(selectedDevice)
            val audioDeviceNames = ArrayList<String>()
            for (a in availableAudioDevices) {
                audioDeviceNames.add(a.name)
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.select_device)
                .setSingleChoiceItems(
                    audioDeviceNames.toTypedArray<CharSequence>(),
                    selectedDeviceIndex
                ) { dialog: DialogInterface, index: Int ->
                    dialog.dismiss()
                    val selectedAudioDevice = availableAudioDevices[index]
                    audioSwitch.selectDevice(selectedAudioDevice)
                }.create().show()
        }
    }

    companion object {
        private const val TAG = "VoiceActivity"
    }
}