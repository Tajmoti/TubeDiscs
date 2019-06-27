package com.tajmoti.tubediscs.client.sound;

import com.tajmoti.tubediscs.client.converter.IVideoDownloader;
import com.tajmoti.tubediscs.client.sound.library.LibraryLWJGLOpenALSeekable;
import paulscode.sound.FilenameURL;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class OffsetTracker implements LibraryLWJGLOpenALSeekable.SeekAmountGetter {
    private final Map<String, OffsetInfo> offsets;


    public OffsetTracker() {
        this.offsets = new HashMap<>();
    }

    @Override
    public int getSeekMillis(FilenameURL filenameURL) {
        String fileName = filenameURL.getFilename();
        String videoId = fileName.substring(0, fileName.indexOf('.'));
        OffsetInfo info = offsets.get(videoId);
        if (info == null) {
            return -1;
        } else {
            return info.getOffsetNow();
        }
    }

    public void setOffset(URL url, int offset) {
        String videoId = IVideoDownloader.extractVideoId(url);
        offsets.put(videoId, new OffsetInfo(offset));
    }

    private static class OffsetInfo {
        private final long timeReceived;
        private final int offsetMillis;

        private OffsetInfo(int offsetMillis) {
            this.timeReceived = System.currentTimeMillis();
            this.offsetMillis = offsetMillis;
        }

        private int getOffsetNow() {
            return (int) (offsetMillis + (System.currentTimeMillis() - timeReceived));
        }
    }
}
