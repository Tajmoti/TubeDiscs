package com.tajmoti.tubediscs.client.converter;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public interface IVideoDownloader {

    File downloadVideo(URL videoUrl, File targetDir) throws IOException;

    void prepareEnvironment(File modDir);

    static String extractVideoId(URL videoUrl) {
        String str = videoUrl.toString();
        int index = str.indexOf("?v=");
        return str.substring(index + 3);
    }
}
