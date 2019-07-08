package com.tajmoti.tubediscs.audio.client;

import javax.sound.sampled.AudioFormat;

public interface IAudioProvider {
    AudioFormat MC_AUDIO_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);


    /**
     * Asynchronously loads the track identified by {@link PositionedAudioPlayer.ActiveRequest}
     * and pushes the loaded bytes into {@link PositionedAudioPlayer.ActiveRequest#trimAndFeedBytes(byte[])}.
     * If an error occurs, calls {@link PositionedAudioPlayer.ActiveRequest#notifyFailed()}.
     */
    void fetchAndPlayAsync(PositionedAudioPlayer.ActiveRequest request);


    /**
     * Calculates the byte offset into the PCM sound
     * from the milliseconds to skip.
     */
    static int millisToBytes(long totalOffsetMillis) {
        int frameSize = MC_AUDIO_FORMAT.getFrameSize();
        float totalOffsetBytes = (totalOffsetMillis * MC_AUDIO_FORMAT.getFrameRate() / 1000) * frameSize;
        // Offset must be multiple of a frame!
        return (int) totalOffsetBytes / frameSize * frameSize;
    }
}
