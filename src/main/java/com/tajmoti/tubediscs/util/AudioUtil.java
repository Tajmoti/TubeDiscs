package com.tajmoti.tubediscs.util;

import ws.schild.jave.*;

import java.io.File;

public class AudioUtil {

    /**
     * Converts the input video file
     * into an ogg vorbis file.
     */
    public static void convertToOgg(File source, File target) throws EncoderException {
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libvorbis");

        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("ogg");
        attrs.setAudioAttributes(audio);

        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(source), target, attrs);
    }
}
