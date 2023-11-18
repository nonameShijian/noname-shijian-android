package net.sf.sevenzipjbinding.impl;

import java.util.Date;

import net.sf.sevenzipjbinding.IInArchive;
import net.sf.sevenzipjbinding.IOutArchive;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItem7z;
import net.sf.sevenzipjbinding.IOutItemAllFormats;
import net.sf.sevenzipjbinding.IOutItemBZip2;
import net.sf.sevenzipjbinding.IOutItemBase;
import net.sf.sevenzipjbinding.IOutItemGZip;
import net.sf.sevenzipjbinding.IOutItemTar;
import net.sf.sevenzipjbinding.IOutItemZip;
import net.sf.sevenzipjbinding.PropID;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Factory for the {@link OutItem} objects. The <code>E</code> type parameter references one of the archive format
 * specific interfaces <code>IOutItemXxx</code> or the archive format independent interface {@link IOutItemAllFormats}
 * to get access to subset of the {@link OutItem} methods.<br>
 * <br>
 * The purpose of the archive format specific interfaces <code>IOutItemXxx</code> is to hide methods unrelated to the
 * corresponding archive format. For example, GZip format doesn't support the <code>attributes</code> property and so
 * the {@link IOutItemGZip} interface doesn't contain corresponding getters and setter. The Zip archive format on the
 * other hand does support the <code>attributes</code> property defining the methods:
 * <ul>
 * <li> {@link IOutItemZip#getPropertyAttributes()}
 * <li> {@link IOutItemZip#setPropertyAttributes(Integer)}
 * </ul>
 * 
 * @param <T>
 *            the type of the corresponding archive item data class (out item), like {@link IOutItem7z} or
 *            {@link IOutItemZip}. Use {@link IOutItemAllFormats} interface to support all available archive formats.
 *            Return type for of the all create methods of the factory.
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public class OutItemFactory<T extends IOutItemBase> {
    private static final Long ZERO = Long.valueOf(0);
    private int index;
    private IOutArchive<?> outArchive;

    OutItemFactory(IOutArchive<?> outArchive, int index) {
        this.outArchive = outArchive;
        this.index = index;
    }

    /**
     * Create an instance of the <code>E</code> (<code>IOutItemXxx</code> interface).<br>
     * <br>
     * This method set default values for an archive item in create archive operations or a new archive item in update
     * archive operations. For existing items in update archive operations use
     * 
     * <ul>
     * <li>{@link #createOutItem(int)}
     * <li>{@link #createOutItemAndCloneProperties(int)}
     * </ul>
     * 
     * @return a new instance suitable for create archive operations
     */
    @SuppressWarnings("unchecked")
    public T createOutItem() {
        return (T) createOutItemIntern();
    }

    private OutItem createOutItemIntern() {
        OutItem outItem = new OutItem(outArchive, index);
        fillDefaultValues(outItem);
        return outItem;
    }

    private void fillDefaultValues(OutItem outItem) {
        if (outArchive.getConnectedInArchive() != null) {
            outItem.setUpdateOldArchiveItemIndex(Integer.valueOf(-1));
            outItem.setUpdateIsNewData(Boolean.TRUE);
            outItem.setUpdateIsNewProperties(Boolean.TRUE);
        }
        outItem.setDataSize(ZERO);
        outItem.setPropertyLastModificationTime(new Date());

        switch (outItem.getArchiveFormat()) {
        case SEVEN_ZIP:
            fillDefaultValues7z(outItem);
            break;

        case ZIP:
            fillDefaultValuesZip(outItem);
            break;

        case BZIP2:
            fillDefaultValuesBZip2(outItem);
            break;

        case GZIP:
            fillDefaultValuesGZip(outItem);
            break;

        case TAR:
            fillDefaultValuesTar(outItem);
            break;

        default:
            throw new RuntimeException("No default values strategy for the archive format '"
                    + outItem.getArchiveFormat() + "'");
        }
    }

    private void fillDefaultValues7z(IOutItem7z outItem) {
        outItem.setPropertyIsAnti(Boolean.FALSE);
        outItem.setPropertyIsDir(Boolean.FALSE);
        outItem.setPropertyAttributes(Integer.valueOf(0));
    }

    private void fillDefaultValuesZip(IOutItemZip outItem) {
        outItem.setPropertyIsDir(Boolean.FALSE);
        outItem.setPropertyAttributes(Integer.valueOf(0));
    }

    private void fillDefaultValuesBZip2(IOutItemBZip2 outItem) {
    }

    private void fillDefaultValuesGZip(IOutItemGZip outItem) {
    }

    private void fillDefaultValuesTar(IOutItemTar outItem) {
        outItem.setPropertyIsDir(Boolean.FALSE);
    }

    /**
     * Create an instance of the <code>E</code> (<code>IOutItemXxx</code> interface).<br>
     * <br>
     * This method set default values for an existing archive item in update operations. The created object will be
     * configured to remain old data and old properties.<br>
     * <br>
     * Following use cases within the {@link IOutCreateCallback#getItemInformation(int, OutItemFactory)} method are
     * suggested:
     * <ul>
     * <li>Return the created instance without further modification to copy the old archive item into the new archive as
     * is.
     * <li>Setting {@link IOutItemBase#setUpdateIsNewData(Boolean)} to <code>true</code> and providing new data stream
     * and data size.
     * <li>Setting {@link IOutItemBase#setUpdateIsNewProperties(Boolean)} to <code>true</code> and setting <i>all</i>
     * properties supported by the archive format<br>
     * <code>Note:</code> use {@link #createOutItemAndCloneProperties(int)} to change some of the properties
     * </ul>
     * 
     * @param updateOldArchiveItemIndex
     *            index of the existing archive item in the existing (old) archive
     * 
     * @return a new instance
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    @SuppressWarnings("unchecked")
    public T createOutItem(int updateOldArchiveItemIndex) throws SevenZipException {
        IInArchive inArchive = outArchive.getConnectedInArchive();
        if (inArchive == null) {
            throw new RuntimeException("Not an update operation");
        }

        OutItem outItem = createOutItemIntern();

        outItem.setUpdateOldArchiveItemIndex(Integer.valueOf(updateOldArchiveItemIndex));
        outItem.setUpdateIsNewData(Boolean.FALSE);
        outItem.setUpdateIsNewProperties(Boolean.FALSE);

        return (T) outItem;
    }

    /**
     * Create an instance of the <code>E</code> (<code>IOutItemXxx</code> interface).<br>
     * <br>
     * This method set default values for an existing archive item in update operations. Also all properties of the old
     * archive item get copied into the new instance and can be modified later on. This method is suitable to change
     * some but not all of the properties of the old archive item during update operations.
     * 
     * @param updateOldArchiveItemIndex
     *            index of the existing archive item in the existing (old) archive
     * 
     * @return a new instance
     * @throws SevenZipException
     *             7-Zip or 7-Zip-JBinding error occur. Use {@link SevenZipException#printStackTraceExtended()} to get
     *             stack traces of this SevenZipException and of the all thrown 'cause by' exceptions.
     */
    @SuppressWarnings("unchecked")
    public T createOutItemAndCloneProperties(int updateOldArchiveItemIndex) throws SevenZipException {
        IInArchive inArchive = outArchive.getConnectedInArchive();
        if (inArchive == null) {
            throw new RuntimeException("Not an update operation");
        }

        OutItem outItem = createOutItemIntern();

        outItem.setUpdateOldArchiveItemIndex(Integer.valueOf(updateOldArchiveItemIndex));
        outItem.setUpdateIsNewData(Boolean.FALSE);
        outItem.setUpdateIsNewProperties(Boolean.TRUE);

        outItem.setPropertyPath((String) inArchive.getProperty(updateOldArchiveItemIndex, PropID.PATH));

        outItem.setPropertyAttributes((Integer) inArchive.getProperty(updateOldArchiveItemIndex, PropID.ATTRIBUTES));
        outItem.setPropertyPosixAttributes((Integer) inArchive.getProperty(updateOldArchiveItemIndex,
                PropID.POSIX_ATTRIB));

        outItem.setPropertyUser((String) inArchive.getProperty(updateOldArchiveItemIndex, PropID.USER));
        outItem.setPropertyGroup((String) inArchive.getProperty(updateOldArchiveItemIndex, PropID.GROUP));

        outItem.setPropertyCreationTime((Date) inArchive.getProperty(updateOldArchiveItemIndex, PropID.CREATION_TIME));
        outItem.setPropertyLastModificationTime((Date) inArchive.getProperty(updateOldArchiveItemIndex,
                PropID.LAST_MODIFICATION_TIME));
        outItem.setPropertyLastAccessTime((Date) inArchive.getProperty(updateOldArchiveItemIndex,
                PropID.LAST_ACCESS_TIME));

        outItem.setPropertyIsAnti((Boolean) inArchive.getProperty(updateOldArchiveItemIndex, PropID.IS_ANTI));
        outItem.setPropertyIsDir((Boolean) inArchive.getProperty(updateOldArchiveItemIndex, PropID.IS_FOLDER));

        return (T) outItem;
    }
}
