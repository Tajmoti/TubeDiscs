package com.tajmoti.tubediscs.client.converter;

import com.github.axet.vget.VGet;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import ws.schild.jave.EncoderException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

@SideOnly(Side.CLIENT)
public class AudioCacheProcessor {
    private static final File VIDEO_CACHE = new File("tubediscs/video");
    private static final File AUDIO_CACHE = new File("tubediscs/audio");

    private final VGet vGet;
    private final AtomicBoolean cancel;


    public AudioCacheProcessor(URL url) {
        mkdirs();
        this.vGet = new VGet(url, VIDEO_CACHE);
        this.cancel = new AtomicBoolean();
    }

    public File obtainAudio() throws EncoderException, IOException {
        return new File(AUDIO_CACHE, "10 second video FAIL.ogg");
        /*
        // Download the video metadata
        vGet.extract();

        String title = vGet.getVideo().getTitle();
        File audio = new File(AUDIO_CACHE, title + ".ogg");

        // If already downloaded, play it right away
        if (audio.exists())
            return audio;

        // Download it
        File video = new File(VIDEO_CACHE, title + ".webm");
        vGet.setTarget(video);
        vGet.download();

        // Convert it
        AudioUtil.convertToOgg(video, audio);
        video.delete();
        return audio;*/
    }

    public void cancel() {
        cancel.set(true);
    }

    private static void mkdirs() {
        VIDEO_CACHE.mkdirs();
        AUDIO_CACHE.mkdirs();
    }
}
