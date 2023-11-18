package net.sf.sevenzipjbinding.impl;

import java.io.IOException;
import java.io.InputStream;

import net.sf.sevenzipjbinding.ISequentialInStream;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Implementation of {@link ISequentialInStream} based on {@link InputStream}.
 * 
 * @deprecated use {@link InputStreamSequentialInStream}
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
@Deprecated
// TODO Remove in the next release
public class SequentialInStreamImpl implements ISequentialInStream {
    private InputStream inputStream;

    /**
     * Create instance of {@link SequentialInStreamImpl} based on {@link InputStream}
     * 
     * @param inputStream
     *            input stream to use
     */
    public SequentialInStreamImpl(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * {@inheritDoc}
     */
    public int read(byte[] data) throws SevenZipException {
        if (data.length == 0) {
            return 0;
        }

        try {
            int read = inputStream.read(data);
            if (read < 0) {
                return 0;
            }
            return read;
        } catch (IOException e) {
            throw new SevenZipException("Error reading input stream", e);
        }
    }

    /**
     * Get underlaying input stream
     * 
     * @return input stream
     */
    public InputStream getInputStream() {
        return inputStream;
    }

    public void close() throws IOException {
        // TODO Auto-generated method stub

    }
}
