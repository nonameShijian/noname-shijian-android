package net.sf.sevenzipjbinding;

/**
 * The interface provides functionality to create new Zip archives.<br>
 * Standard way to get implementation is to use {@link SevenZip#openOutArchiveZip()}. See {@link IOutCreateArchive}
 * -JavaDoc for more information.
 * 
 * <i>NOTE:</i> Each instance should be closed using {@link IOutArchive#close()} method.
 * 
 * @see IOutCreateArchive
 * @see ArchiveFormat#ZIP
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutCreateArchiveZip extends IOutCreateArchive<IOutItemZip>, //
        IOutFeatureSetLevel {

    /**
     * Create new archive. To update an existing archive open it first and then use
     * {@link IInArchive#getConnectedOutArchive()} to get an instance of the {@link IOutUpdateArchive} interface.<br>
     * <br>
     * The <code>outCreateCallback</code> is designed to get necessary information about archive items and provide
     * information about progress of the operation.<br>
     * <br>
     * 
     * @param outStream
     *            output stream to get the new archive
     * @param numberOfItems
     *            number of items in the new archive
     * @param outCreateCallback
     *            callback object to exchange information about archive create operation.
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    public void createArchive(IOutStream outStream, int numberOfItems,
            IOutCreateCallback<? extends IOutItemZip> outCreateCallback) throws SevenZipException;

}
