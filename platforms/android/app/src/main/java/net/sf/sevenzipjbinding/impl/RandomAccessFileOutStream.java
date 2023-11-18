package net.sf.sevenzipjbinding.impl;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

import net.sf.sevenzipjbinding.IOutStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Implementation of {@link IOutStream} using {@link RandomAccessFile}.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public class RandomAccessFileOutStream implements IOutStream, Closeable {
    private final RandomAccessFile randomAccessFile;

    /**
     * Constructs instance of the class from random access file.
     * 
     * @param randomAccessFile
     *            random access file to use
     */
    public RandomAccessFileOutStream(RandomAccessFile randomAccessFile) {
        this.randomAccessFile = randomAccessFile;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized long seek(long offset, int seekOrigin) throws SevenZipException {
        try {
            switch (seekOrigin) {
            case SEEK_SET:
                randomAccessFile.seek(offset);
                break;

            case SEEK_CUR:
                randomAccessFile.seek(randomAccessFile.getFilePointer() + offset);
                break;

            case SEEK_END:
                randomAccessFile.seek(randomAccessFile.length() + offset);
                break;

            default:
                throw new RuntimeException("Seek: unknown origin: " + seekOrigin);
            }

            return randomAccessFile.getFilePointer();
        } catch (IOException e) {
            throw new SevenZipException("Error while seek operation", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setSize(long newSize) throws SevenZipException {
        try {
            randomAccessFile.setLength(newSize);
        } catch (IOException exception) {
            throw new SevenZipException("Error setting new length of the file", exception);
        }

    }

    /**
     * {@inheritDoc}
     */
    public synchronized int write(byte[] data) throws SevenZipException {
        try {
            randomAccessFile.write(data);
            return data.length;
        } catch (IOException exception) {
            throw new SevenZipException("Error reading random access file", exception);
        }
    }

    /**
     * Closes random access file. After this call no more methods should be called.
     * 
     * @throws IOException
     *             see {@link RandomAccessFile#close()}
     */
    public void close() throws IOException {
        randomAccessFile.close();
    }
}
