package com.tajmoti.tubediscs.client.sound.library;

import org.apache.logging.log4j.Logger;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

import java.lang.reflect.Field;
import java.nio.IntBuffer;
import java.util.HashMap;

public class LibraryLWJGLOpenALFieldAccessor {
    private final Logger logger;
    private final Field fieldBufferMap;
    private final LibraryLWJGLOpenAL target;


    public LibraryLWJGLOpenALFieldAccessor(Logger logger, LibraryLWJGLOpenAL target) throws NoSuchFieldException {
        this.logger = logger;
        this.target = target;
        try {
            this.fieldBufferMap = LibraryLWJGLOpenAL.class.getDeclaredField("ALBufferMap");
            this.fieldBufferMap.setAccessible(true);
        } catch (NoSuchFieldException e) {
            this.logger.error(e);
            throw e;
        }
    }

    public HashMap<String, IntBuffer> getBufferMap() {
        try {
            return (HashMap<String, IntBuffer>) fieldBufferMap.get(target);
        } catch (IllegalAccessException e) {
            logger.error(e);
            return null;
        }
    }

    public void setBufferMap(HashMap<String, IntBuffer> map) {
        try {
            fieldBufferMap.set(target, map);
        } catch (IllegalAccessException e) {
            logger.error(e);
        }
    }
}
