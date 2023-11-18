package com.noname.shijian.zip;

import android.text.TextUtils;
import android.util.Pair;

import com.noname.shijian.R;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.ISequentialOutStream;
import net.sf.sevenzipjbinding.SevenZip;
import net.sf.sevenzipjbinding.SevenZipException;
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream;
import net.sf.sevenzipjbinding.simple.ISimpleInArchive;
import net.sf.sevenzipjbinding.simple.ISimpleInArchiveItem;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ZipUtil {

    public interface Callback{
        void setTotal(long total);
        void setCompleted(long completed);
        void onError(String filePath,Exception e);
    }

    public static void extractAll(String filePath, File dest,String charset,String password,Callback callback) throws Exception{
        IInArchive inArchive = null;
        RandomAccessFile randomAccessFile = null;
        try{
            randomAccessFile = new RandomAccessFile(new File(filePath), "r");
            if(TextUtils.isEmpty(password)) {
                inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
            }else{
                inArchive = SevenZip.openInArchive(null,new RandomAccessFileInStream(randomAccessFile),password);
            }
            inArchive.extract(null, false, new ExtractCallback(inArchive,dest.getPath(),charset,password) {
                @Override
                public void setTotal(long total) throws SevenZipException {
                    if(callback!=null){
                        callback.setTotal(total);
                    }
                }

                @Override
                void onError(String filePath, Exception e) {
                    if(callback!=null){
                        callback.onError(filePath,e);
                    }
                }

                @Override
                public void setCompleted(long complete) throws SevenZipException {
                    if(callback!=null){
                        callback.setCompleted(complete);
                    }
                }
            });
        } finally {
            if(inArchive != null){
                inArchive.close();
            }
            if(randomAccessFile != null){
                randomAccessFile.close();
            }
        }
    }

    public static void extractAllMultiThread(String filePath, File dest,String charset,String password,Callback callback) throws Exception{
        IInArchive inArchive = null;
        RandomAccessFile randomAccessFile = null;
        try{
            randomAccessFile = new RandomAccessFile(new File(filePath), "r");
            if(TextUtils.isEmpty(password)) {
                inArchive = SevenZip.openInArchive(null, new RandomAccessFileInStream(randomAccessFile));
            }else{
                inArchive = SevenZip.openInArchive(null,new RandomAccessFileInStream(randomAccessFile),password);
            }
            int threadCount = Runtime.getRuntime().availableProcessors() - 2;
            if(threadCount <= 1){
                extractAll(filePath,dest,charset,password,callback);
                return;
            }
            ArrayList<ArrayList<Integer>> indexGroups = new ArrayList<>(threadCount);
            for(int i=0;i<threadCount;i++){
                indexGroups.add(new ArrayList<>());
            }
            ISimpleInArchive simpleInArchive = inArchive.getSimpleInterface();
            int count = 0;
            for(ISimpleInArchiveItem item:simpleInArchive.getArchiveItems()){
                if(item.isFolder())continue;
                indexGroups.get(count%threadCount).add(item.getItemIndex());
                count++;
            }
            ArrayList<Thread> threads = new ArrayList<>(threadCount);
            Double[] ms = new Double[threadCount];
            Arrays.fill(ms, 0.0);
            CopyOnWriteArrayList<Double> progressList = new CopyOnWriteArrayList<>(ms);
            if(callback!=null){
                callback.setTotal(100);
            }
            for(int i=0;i<indexGroups.size();i++){
                ArrayList<Integer> group = indexGroups.get(i);
                final IInArchive archive = inArchive;
                final int tindex = i;
                Thread thread = new Thread(){
                    @Override
                    public void run() {
                        try {
                            archive.extract(arrayListToArray(group), false, new ExtractCallback(archive, dest.getPath(), charset,password) {

                                private long total = -1;

                                @Override
                                public void setTotal(long total) throws SevenZipException {
                                    this.total = total;
                                }

                                @Override
                                void onError(String filePath, Exception e) {

                                }

                                @Override
                                public void setCompleted(long complete) throws SevenZipException {
                                    if(this.total!=-1){
                                        progressList.set(tindex,(double)complete/this.total);
                                        double sum = 0;
                                        for(Double d:progressList){
                                            sum += (d/threadCount);
                                        }
                                        if(callback!=null){
                                            callback.setCompleted((int)(100*sum));
                                        }
                                    }
                                }
                            });
                        }catch (Throwable e){

                        }
                    }
                };
                thread.start();
                threads.add(thread);
            }
            for(Thread thread:threads){
                thread.join();
            }
        } finally {
            if(inArchive != null){
                inArchive.close();
            }
            if(randomAccessFile != null){
                randomAccessFile.close();
            }
        }
    }

    private static int[] arrayListToArray(List<Integer> list){
        int[] ret = new int[list.size()];
        for(int i=0;i<list.size();i++){
            ret[i] = list.get(i);
        }
        return ret;
    }

}
