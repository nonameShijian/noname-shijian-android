package net.sf.sevenzipjbinding.impl;

import net.sf.sevenzipjbinding.IOutCreateArchive7z;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.IOutUpdateArchive7z;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * 7z specific archive create and update class.
 *
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public class OutArchive7zImpl extends OutArchiveImpl<IOutItem7z> implements IOutCreateArchive7z, IOutUpdateArchive7z {
    private boolean solid = true;
    private int countOfFilesPerBlock = -1;
    private long countOfBytesPerBlock = -1;
    private boolean solidExtension;

    public void setLevel(int compressionLevel) {
        featureSetLevel(compressionLevel);
    }

    public void setHeaderEncryption(boolean enabled) throws SevenZipException {
        featureSetHeaderEncryption(enabled);
    }

    /**
     * {@inheritDoc}
     */
    public void setSolid(boolean solid) {
        this.solid = solid;
    }

    /**
     * {@inheritDoc}
     */
    public void setSolidFiles(int countOfFilesPerBlock) {
        this.countOfFilesPerBlock = countOfFilesPerBlock;
    }

    /**
     * {@inheritDoc}
     */
    public void setSolidSize(long countOfBytesPerBlock) {
        this.countOfBytesPerBlock = countOfBytesPerBlock;
    }

    /**
     * {@inheritDoc}
     */
    public void setSolidExtension(boolean solidExtension) {
        this.solidExtension = solidExtension;
    }

    @Override
    protected void applyFeatures() throws SevenZipException {
        super.applyFeatures();

        StringBuilder stringBuilder = new StringBuilder();
        if (solidExtension) {
            stringBuilder.append("E");
        }
        if (countOfFilesPerBlock != -1) {
            stringBuilder.append(countOfFilesPerBlock);
            stringBuilder.append("F");
        }
        if (countOfBytesPerBlock != -1) {
            stringBuilder.append(countOfBytesPerBlock);
            stringBuilder.append("B");
        }
        if (stringBuilder.length() > 0) {
            nativeSetSolidSpec(stringBuilder.toString());
        }

        // Set solid block configuration
        if (!solid) {
            nativeSetSolidSpec(null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setThreadCount(int threadCount) {
        featureSetThreadCount(threadCount);
    }
}
