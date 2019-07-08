package com.tajmoti.tubediscs.audio.client;

public interface IAudioProvider {

    /**
     * Asynchronously loads the track identified by {@link PositionedAudioPlayer.ActiveRequest}
     * and pushes the loaded bytes into {@link PositionedAudioPlayer.ActiveRequest#trimAndFeedBytes(byte[])}.
     * If an error occurs, calls {@link PositionedAudioPlayer.ActiveRequest#notifyFailed()}.
     */
    void fetchAndPlayAsync(PositionedAudioPlayer.ActiveRequest request);
}
