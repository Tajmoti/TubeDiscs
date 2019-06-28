package com.tajmoti.tubediscs.client.converter;

import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.net.URL;

@SideOnly(Side.CLIENT)
public class YoutubeDlVideoDownloader implements IVideoDownloader {
    @Override
    public File downloadVideo(URL videoUrl, File targetDir) throws IOException {
        File original = new File(targetDir, IVideoDownloader.extractVideoId(videoUrl));
        File renamed = new File(targetDir, original.getName() + ".webm");

        original.delete();
        renamed.delete();

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

        original.renameTo(renamed);
        return renamed;
    }
}
