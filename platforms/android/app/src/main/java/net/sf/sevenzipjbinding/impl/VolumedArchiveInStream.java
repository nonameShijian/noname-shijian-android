package net.sf.sevenzipjbinding.impl;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import net.sf.sevenzipjbinding.IArchiveOpenVolumeCallback;
import net.sf.sevenzipjbinding.IInStream;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * This helper class allows merging multiple instances of {@link IInStream} interface into one. This is helpful for
 * accessing 7z volumed archives. 7z splits its archives into volumes on the byte layout. Each archive volume gets
 * extension <code>.7z.nnn</code> (<code>.7z.001</code>, <code>.7z.002</code>, <code>.7z.003</code>, ...). Such archives
 * can be reassembled into single archive file using simple concatenation: <br>
 * <blockquote> {@code cat name.7z.* > name.7z} </blockquote>
 *
 * To use this you need to implement {@link IArchiveOpenVolumeCallback} interface.
 * <ul>
 * <li>{@link IArchiveOpenVolumeCallback#getProperty(PropID)} with the <code>propID</code>={@link PropID#NAME} will be
 * called ones to get the file name of the first volume in case it was not given to constructor. The file name should
 * ends with <code>.7z.001</code> or SevenZipException will be thrown.
 * <li>{@link IArchiveOpenVolumeCallback#getStream(String)} will be called multiple times to get instance of
 * {@link IInStream} representing required volume. The implementation of {@link IArchiveOpenVolumeCallback} should close
 * file associated with the old {@link IInStream}, if a new {@link IInStream} was successfully instantiated.
 * </ul>
 *
 * @author Boris Brodski
 * @since 4.65-1
 */
public class VolumedArchiveInStream implements IInStream {
    private static final String SEVEN_ZIP_FIRST_VOLUME_POSTFIX = ".7z.001";

    private long absoluteOffset;
    private long absoluteLength = -1;

    private int currentIndex = -1;
    private IInStream currentInStream;
    private long currentVolumeOffset;
    private long currentVolumeLength;
    private List<Long> volumePositions = new ArrayList<Long>();

    private final IArchiveOpenVolumeCallback archiveOpenVolumeCallback;
    private String cuttedVolumeFilename;

    /**
     * Creates instance of {@link VolumedArchiveInStream} using {@link IArchiveOpenVolumeCallback}. The name of the
     * first archive volume will be asked using {@link IArchiveOpenVolumeCallback#getProperty(PropID)} with the
     * <code>propID</code>={@link PropID#NAME}. The file name should ends with <code>.7z.001</code> or SevenZipException
     * will be thrown.
     *
     * @param archiveOpenVolumeCallback
     *            call back implementation used to access different volumes of archive.
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public VolumedArchiveInStream(IArchiveOpenVolumeCallback archiveOpenVolumeCallback) throws SevenZipException {
        this((String) archiveOpenVolumeCallback.getProperty(PropID.NAME), archiveOpenVolumeCallback);
    }

    /**
     * Creates instance of {@link VolumedArchiveInStream} using {@link IArchiveOpenVolumeCallback}.
     *
     * @param firstVolumeFilename
     *            the file name of the first volume.
     * @param archiveOpenVolumeCallback
     *            call back implementation used to access different volumes of archive. The file name should ends with
     *            <code>.7z.001</code> or SevenZipException will be thrown.
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public VolumedArchiveInStream(String firstVolumeFilename, IArchiveOpenVolumeCallback archiveOpenVolumeCallback)
            throws SevenZipException {
        this.archiveOpenVolumeCallback = archiveOpenVolumeCallback;
        volumePositions.add(Long.valueOf(0));

        if (!firstVolumeFilename.endsWith(SEVEN_ZIP_FIRST_VOLUME_POSTFIX)) {
            throw new SevenZipException("The first 7z volume filename '" + firstVolumeFilename
                    + "' don't ends with the postfix: '" + SEVEN_ZIP_FIRST_VOLUME_POSTFIX + "'. Can't proceed");

        }

        cuttedVolumeFilename = firstVolumeFilename.substring(0, firstVolumeFilename.length() - 3);
        openVolume(1, true);
    }

    private void openVolume(int index, boolean seekToBegin) throws SevenZipException {
        if (currentIndex == index) {
            return;
        }

        for (int i = volumePositions.size(); i < index && absoluteLength == -1; i++) {
            openVolume(i, false);
        }

        if (absoluteLength != -1 && volumePositions.size() <= index) {
            return;
        }

        String volumeFilename = cuttedVolumeFilename + MessageFormat.format("{0,number,000}", Integer.valueOf(index));

        // Get new IInStream
        IInStream newInStream = archiveOpenVolumeCallback.getStream(volumeFilename);

        if (newInStream == null) {
            absoluteLength = volumePositions.get(volumePositions.size() - 1).longValue();
            return;
        }

        currentInStream = newInStream;

        if (volumePositions.size() == index) {
            // Determine volume size
            currentVolumeLength = currentInStream.seek(0, SEEK_END);
            if (currentVolumeLength == 0) {
                throw new RuntimeException("Volume " + index + " is empty");
            }
            volumePositions.add(Long.valueOf(volumePositions.get(index - 1).longValue() + currentVolumeLength));

            if (seekToBegin) {
                currentInStream.seek(0, SEEK_SET);
            }
        } else {
            currentVolumeLength = volumePositions.get(index).longValue() - volumePositions.get(index - 1).longValue();
        }

        if (seekToBegin) {
            currentVolumeOffset = 0;
            absoluteOffset = volumePositions.get(index - 1).longValue();
        }

        currentIndex = index;
    }

    // 0------X------Y------Z length=4
    // 0______1______2______3 - list index
    // 1______2______3______4 - volume
    private void openVolumeToAbsoluteOffset() throws SevenZipException {
        int index = volumePositions.size() - 1;
        if (absoluteLength != -1 && absoluteOffset >= absoluteLength) {
            return;
        }
        while (volumePositions.get(index).longValue() > absoluteOffset) {
            index--;
        }

        if (index < volumePositions.size() - 1) {
            openVolume(index + 1, false);
            return;
        }

        do {
            index++;
            openVolume(index, false);
        } while ((absoluteLength == -1 || absoluteOffset < absoluteLength)
                && volumePositions.get(index).longValue() <= absoluteOffset);

    }

    /**
     * ${@inheritDoc}
     */

    public synchronized long seek(long offset, int seekOrigin) throws SevenZipException {
        long newOffset;
        boolean proceedWithSeek = false;
        switch (seekOrigin) {
        case SEEK_SET:
            newOffset = offset;
            break;

        case SEEK_CUR:
            newOffset = absoluteOffset + offset;
            break;

        case SEEK_END:
            if (absoluteLength == -1) {
                openVolume(Integer.MAX_VALUE, false);
                proceedWithSeek = true;
            }
            newOffset = absoluteLength + offset;
            break;

        default:
            throw new RuntimeException("Seek: unknown origin: " + seekOrigin);
        }

        if (newOffset == absoluteOffset && !proceedWithSeek) {
            return newOffset;
        }
        absoluteOffset = newOffset;

        openVolumeToAbsoluteOffset();

        if (absoluteLength != -1 && absoluteLength <= absoluteOffset) {
            absoluteOffset = absoluteLength;
            return absoluteLength;
        }

        currentVolumeOffset = absoluteOffset - volumePositions.get(currentIndex - 1).longValue();
        currentInStream.seek(currentVolumeOffset, SEEK_SET);

        return newOffset;
    }

    /**
     * ${@inheritDoc}
     */

    public synchronized int read(byte[] data) throws SevenZipException {
        if (absoluteLength != -1 && absoluteOffset >= absoluteLength) {
            return 0;
        }

        int read = currentInStream.read(data);

        absoluteOffset += read;
        currentVolumeOffset += read;

        if (currentVolumeOffset >= currentVolumeLength) {
            openVolume(currentIndex + 1, true);
        }

        return read;
    }

    public void close() throws IOException {
        throw new RuntimeException(
                "close() method not supported. " + "The user should implement its own caching and closing stratagies "
                        + "within the IArchiveOpenVolumeCallback.getStream() implementation.");
    }
}
