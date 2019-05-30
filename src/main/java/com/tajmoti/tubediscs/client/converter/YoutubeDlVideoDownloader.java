package com.tajmoti.tubediscs.client.converter;

import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class YoutubeDlVideoDownloader implements IVideoDownloader {
    @Override
    public File downloadVideo(URL videoUrl, File targetDir) throws IOException {
        // Build request
        YoutubeDLRequest request = new YoutubeDLRequest(videoUrl.toString(), targetDir.getAbsolutePath());
        request.setOption("ignore-errors");
        request.setOption("output", "%(id)s");
        request.setOption("retries", 10);

        // Make request and return response
        YoutubeDLResponse response = null;
        try {
            response = YoutubeDL.execute(request);
        } catch (YoutubeDLException e) {
            throw new IOException(e);
        }

        File original = new File(targetDir, extractVideoId(videoUrl));
        File renamed = new File(targetDir, original.getName() + ".webm");

        original.renameTo(renamed);
        return renamed;
    }
}
