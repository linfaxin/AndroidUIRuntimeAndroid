package org.androidui.runtime.image;

import android.util.Log;

import org.androidui.runtime.BuildConfig;
import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Created by linfaxin on 16/5/15.
 */
public class ImageDownloader {
    private static final int DEFAULT_TIMEOUT = 8 * 1000;
    private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8192;//8K

    public static File reqForDownload(String url, File file){
        try {
            file.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(file);
            reqForDownload(url, fos);
            if(Thread.currentThread().isInterrupted()) return null;
            return file;

        } catch (Exception e) {
            Log.e("fax", "reqForDownload error:" + url);
            if(BuildConfig.DEBUG) e.printStackTrace();
        }
        return null;
    }

    public static void reqForDownload(String url, OutputStream os) throws Exception{
        if(BuildConfig.DEBUG){
            Log.d("fax", "reqForDownload:" + url);
        }

        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setConnectTimeout(DEFAULT_TIMEOUT);

        InputStream is = connection.getInputStream();
        byte[] temp=new byte[DEFAULT_SOCKET_BUFFER_SIZE];
        int length;

        try {
            while ((length = is.read(temp)) != -1) {
                if(Thread.currentThread().isInterrupted()) break;
                os.write(temp, 0, length);
            }
        } finally {
            os.close();
            is.close();
            connection.disconnect();
        }
    }
}
