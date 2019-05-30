package com.tajmoti.tubediscs.client.converter;

import com.github.axet.vget.VGet;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class VgetVideoDownloader implements IVideoDownloader {

    @Override
    public File downloadVideo(URL videoUrl, File targetDir) throws IOException {
        VGet vGet = new VGet(videoUrl);
        vGet.setTarget(targetDir);
        vGet.download();

        return targetDir;
    }
}
