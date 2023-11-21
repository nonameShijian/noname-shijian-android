package com.noname.shijian.zip;

import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import net.sf.sevenzipjbinding.ExtractAskMode;
import net.sf.sevenzipjbinding.ExtractOperationResult;
import net.sf.sevenzipjbinding.IArchiveExtractCallback;
import net.sf.sevenzipjbinding.ICryptoGetTextPassword;
import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;

public abstract class ExtractCallback implements IArchiveExtractCallback, ICryptoGetTextPassword {

    private static final String TAG = "ExtractCallback";

    private final IInArchive inArchive;

    private final String extractPath;

    private String charset;

    private HashMap<String,String> decodeMessyHashMap = new HashMap<>();

    private HashSet<String> extracted = new HashSet<>();

    private String password;

    public ExtractCallback(IInArchive inArchive, String extractPath,String charset,String password) {
        this.inArchive = inArchive;
        if (!extractPath.endsWith("/") && !extractPath.endsWith("\\")) {
            extractPath += File.separator;
        }
        this.password = password;
        this.charset = charset;
        this.extractPath = extractPath;
    }

    abstract void onError(String filePath,Exception e);

    @Override
    public ISequentialOutStream getStream(int index, ExtractAskMode extractAskMode) throws SevenZipException {
        return new ISequentialOutStream() {
            @Override
            public int write(byte[] data) throws SevenZipException {
                String filePath = inArchive.getStringProperty(index, PropID.PATH);
                //onError("Before:"+filePath,new Exception());
                filePath = decodeMessy(filePath);
                //onError("After:"+filePath,new Exception());
                FileOutputStream fos = null;
                try {
                    File path = new File(extractPath + filePath);
                    if(!path.getParentFile().exists()){
                        path.getParentFile().mkdirs();
                    }
                    if(!path.exists()){
                        path.createNewFile();
                        extracted.add(path.getAbsolutePath());
                    }else if(!extracted.contains(path.getAbsolutePath())){
                        path.delete();
                        path.createNewFile();
                        extracted.add(path.getAbsolutePath());
                    }
                    fos = new FileOutputStream(path, true);
                    fos.write(data);
                } catch (IOException e) {
                    Log.e(TAG,"IOException while extracting "+filePath,e);
                    onError(filePath,e);
                }catch (Throwable e){
                    onError(filePath,new Exception(e));
                }
                finally{
                    try {
                        if(fos != null){
                            fos.flush();
                            fos.close();
                        }
                    } catch (IOException e) {
                        onError(filePath,e);
                        Log.e(TAG,"Could not close FileOutputStream "+filePath,e);
                    }
                }
                return data.length;
            }
        };
    }

    //移除乱码。
    private String decodeMessy(String origin){
        if("utf-8".equalsIgnoreCase(charset))return origin;
        String ret = decodeMessyHashMap.get(origin);
        if(TextUtils.isEmpty(ret)){
            ret = new String(origin.getBytes(StandardCharsets.ISO_8859_1), Charset.forName(charset));
            decodeMessyHashMap.put(origin,ret);
        }
        return ret;
    }

    @Override
    public void prepareOperation(ExtractAskMode extractAskMode) throws SevenZipException {

    }

    @Override
    public String cryptoGetTextPassword() throws SevenZipException {
        return password;
    }

    @Override
    public void setOperationResult(ExtractOperationResult extractOperationResult) throws SevenZipException {

    }

}
