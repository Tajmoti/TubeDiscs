package com.tajmoti.tubediscs.client.sound.codec;

import com.tajmoti.tubediscs.TubeDiscs;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundBuffer;
import paulscode.sound.codecs.CodecJOrbis;


public class CodecJOrbisSeekable extends CodecJOrbis {
    private final Logger logger;
    private int offset;


    public CodecJOrbisSeekable() {
        this.logger = TubeDiscs.getInstance().getLogger();
        this.offset = -1;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    @Override
    public SoundBuffer readAll() {
        SoundBuffer buffer = super.readAll();
        if (offset == -1) return buffer;
        // Final length
        int dataLen = buffer.audioData.length;
        int retLen = dataLen - offset;
        // If the offset is larger than the data,
        // it is an error, log it and return the original data.
        if (retLen < 0) {
            logger.error("Offset [" + offset + "] larger than data [" + dataLen + "]!");
            return buffer;
        }
        byte[] newData = new byte[retLen];
        System.arraycopy(buffer.audioData, offset, newData, 0, retLen);
        buffer.audioData = newData;
        return buffer;
    }
}
