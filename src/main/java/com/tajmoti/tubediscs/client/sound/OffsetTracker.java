package com.tajmoti.tubediscs.client.sound;

import com.tajmoti.tubediscs.client.sound.library.LibraryLWJGLOpenALSeekable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class OffsetTracker implements LibraryLWJGLOpenALSeekable.SeekAmountGetter {
    /**
     * Offset for each "sourcename" (as in MC code).
     */
    private final Map<String, OffsetInfo> offsets;


    public OffsetTracker() {
        this.offsets = new HashMap<>();
    }

    @Override
    public synchronized int getSeekMillis(String sourcename) {
        OffsetInfo info = offsets.get(sourcename);
        if (info == null) {
            return -1;
        } else {
            int offsetNow = info.getOffsetNow();
            offsets.remove(sourcename);
            return offsetNow;
        }
    }

    public synchronized void setOffset(String sourcename, int offset) {
        offsets.put(sourcename, new OffsetInfo(offset));
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
