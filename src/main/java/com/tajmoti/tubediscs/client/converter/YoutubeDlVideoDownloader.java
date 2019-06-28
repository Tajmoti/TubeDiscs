package com.tajmoti.tubediscs.client.converter;

import com.sapher.youtubedl.YoutubeDL;
import com.sapher.youtubedl.YoutubeDLException;
import com.sapher.youtubedl.YoutubeDLRequest;
import com.sapher.youtubedl.YoutubeDLResponse;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

@SideOnly(Side.CLIENT)
public class YoutubeDlVideoDownloader implements IVideoDownloader {
    private static final URL EXECUTABLE_URL = url();

    private static URL url() {
        try {
            return new URL("https://youtube-dl.org/downloads/latest/youtube-dl.exe");
        } catch (MalformedURLException e) {
            // Not gonna happen
            return null;
        }
    }

    private final Logger logger;


    public YoutubeDlVideoDownloader(Logger logger) {
        this.logger = logger;
    }

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

    @Override
    public void prepareEnvironment(File modDir) {
        File exec;
        if (SystemUtils.IS_OS_WINDOWS) {
            exec = new File(modDir, "youtube-dl.exe");
        } else {
            exec = new File(modDir, "youtube-dl");
        }

        if (exec.exists()) {
            // update here
        } else {
            try (InputStream in = EXECUTABLE_URL.openStream()) {
                Files.copy(in, exec.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                logger.error(e);
            }
        }
        if (exec.exists())
            YoutubeDL.setExecutablePath(exec.getAbsolutePath());
    }
}
