package net.sf.sevenzipjbinding.impl;

import java.io.IOException;
import java.io.RandomAccessFile;

import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Implementation of {@link IInStream} using {@link RandomAccessFile}.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public class RandomAccessFileInStream implements IInStream {
    private final RandomAccessFile randomAccessFile;

    /**
     * Constructs instance of the class from random access file.
     * 
     * @param randomAccessFile
     *            random access file to use
     */
    public RandomAccessFileInStream(RandomAccessFile randomAccessFile) {
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
    public synchronized int read(byte[] data) throws SevenZipException {
        try {
            int read = randomAccessFile.read(data);
            if (read == -1) {
                return 0;
            } else {
                return read;
            }

        } catch (IOException e) {
            throw new SevenZipException("Error reading random access file", e);
        }
    }

    /**
     * Closes random access file. After this call no more methods should be called.
     * 
     * @throws IOException
     *             see {@link RandomAccessFile#close()}
     */
    public synchronized void close() throws IOException {
        randomAccessFile.close();
    }
}
