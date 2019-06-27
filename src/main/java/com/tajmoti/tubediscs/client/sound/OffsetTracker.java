package com.tajmoti.tubediscs.client.sound;

import com.tajmoti.tubediscs.client.sound.library.LibraryLWJGLOpenALSeekable;
import paulscode.sound.FilenameURL;

public class OffsetTracker implements LibraryLWJGLOpenALSeekable.SeekAmountGetter {
    @Override
    public int getAmountToSeek(FilenameURL filenameURL) {
        return 500000;
    }
}
