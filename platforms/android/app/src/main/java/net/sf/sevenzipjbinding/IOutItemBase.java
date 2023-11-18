package net.sf.sevenzipjbinding;

import net.sf.sevenzipjbinding.impl.OutItem;

/**
 * Base interface of the archive item data interfaces:
 * <ul>
 * <li>archive format specific interfaces <code>IOutItemXxx</code>, like {@link IOutItem7z}
 * <li>archive format independent interface {@link IOutItemAllFormats}
 * </ul>
 * The single known implementation is {@link OutItem}. This base interface provides access to the getters and setters
 * methods, that are shared by all archive formats.<br>
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
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutItemBase {
    /**
     * Return corresponding IOutArchive object.
     * 
     * @return IOutArchive
     */
    public IOutArchive<?> getOutArchive();

    /**
     * Return current archive format
     * 
     * @return archive format
     */
    public ArchiveFormat getArchiveFormat();

    /**
     * Return the index of the item being described in the new archive.
     * 
     * @return the index of the item in the archive (0-based)
     */
    public int getIndex();

    /**
     * Get property {@link PropID#SIZE}.
     * 
     * @see PropID#SIZE
     * @return size.
     */
    public Long getDataSize();

    /**
     * Set property {@link PropID#SIZE}.
     * 
     * @see PropID#SIZE
     * @param size
     *            see {@link PropID#SIZE}
     */
    public void setDataSize(Long size);

    /**
     * Get whether the archive item data (content) has changed. If <code>isNewData</code> is <code>true</code>,
     * <code>isNewProperties</code> must also be <code>true</code><br>
     * <br>
     * <i>Note:</i> only relevant for archive update operations!
     * 
     * @return <code>true</code> - the archive item has new data (always true for new archives),<br>
     *         <code>false</code> - the archive item data from the old archive can be reused.
     */
    public Boolean getUpdateIsNewData();

    /**
     * Set whether the archive item data (content) has changed. If <code>isNewData</code> is <code>true</code>,
     * <code>isNewProperties</code> must also be <code>true</code>.<br>
     * <br>
     * <i>Note:</i> only relevant for archive update operations!
     * 
     * @param updateIsNewData
     *            <code>true</code> - the archive item has new data (always true for new archives),<br>
     *            <code>false</code> - the archive item data from the old archive can be reused.
     */
    public void setUpdateIsNewData(Boolean updateIsNewData);

    /**
     * Get whether the archive item properties have changed. If <code>isNewData</code> is <code>true</code>,
     * <code>isNewProperties</code> must also be <code>true</code><br>
     * <br>
     * <i>Note:</i> only relevant for archive update operations!
     * 
     * @return <code>true</code> - the archive item has new properties (always true for new archives).<br>
     *         <code>false</code> - the archive item properties from the old archive should be reused.
     */
    public Boolean getUpdateIsNewProperties();

    /**
     * Set whether the archive item properties have changed. If <code>isNewData</code> is <code>true</code>,
     * <code>isNewProperties</code> must also be <code>true</code><br>
     * <br>
     * <i>Note:</i> only relevant for archive update operations!
     * 
     * @param updateIsNewProperties
     *            <code>true</code> - the archive item has new properties (always true for new archives).<br>
     *            <code>false</code> - the archive item properties from the old archive should be reused.
     */
    public void setUpdateIsNewProperties(Boolean updateIsNewProperties);

    /**
     * Get the index of the archive item in the archive being updated (old archive). Mandatory, if
     * <code>isNewData</code> or <code>isNewProperties</code> set.<br>
     * <br>
     * <i>Note:</i> only relevant for archive update operations!
     * 
     * @return corresponding index of the archive item in alsothe old archive (starting from 0).<br>
     *         <code>-1</code> if both <code>isNewData</code> and <code>isNewProperties</code> are <code>true</code>.
     */
    public Integer getUpdateOldArchiveItemIndex();

    /**
     * Set the index of the archive item in the archive being updated (old archive). <br>
     * <br>
     * <i>Note:</i> only relevant for archive update operations!
     * 
     * @param updateOldArchiveItemIndex
     *            corresponding index of the archive item in the old archive (starting from 0).<br>
     *            <code>-1</code> if there is no corresponding archive item in the old archive or if doesn't matter (for
     *            new archives).
     */
    public void setUpdateOldArchiveItemIndex(Integer updateOldArchiveItemIndex);
}
