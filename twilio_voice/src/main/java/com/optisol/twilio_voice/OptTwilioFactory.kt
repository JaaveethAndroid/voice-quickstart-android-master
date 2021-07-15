package com.optisol.twilio_voice



object OptTwilioFactory {
        fun createVoiceSession(
            events: VoiceApplicationListener,
        ): VoiceService {
            return VoiceService(events)
        }
}