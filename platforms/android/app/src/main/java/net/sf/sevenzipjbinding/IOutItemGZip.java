package net.sf.sevenzipjbinding;

import java.util.Date;

/**
 * GZip specific archive item data class. It contains all information about a single archive item, that is needed for a
 * create or an update archive operation. See {@link IOutItemBase} for details.
 * 
 * @see IOutItemBase
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutItemGZip extends IOutItemBase {

    /**
     * Get property {@link PropID#PATH}.
     * 
     * @see PropID#PATH
     * @return path
     */
    public String getPropertyPath();

    /**
     * Set property {@link PropID#PATH}.
     * 
     * @see PropID#PATH
     * @param path
     *            see {@link PropID#PATH}
     */
    public void setPropertyPath(String path);

    /**
     * Get property {@link PropID#LAST_MODIFICATION_TIME}.
     * 
     * @see PropID#LAST_MODIFICATION_TIME
     * @return last modification time
     */
    public Date getPropertyLastModificationTime();

    /**
     * Set property {@link PropID#LAST_MODIFICATION_TIME}.
     * 
     * @see PropID#LAST_MODIFICATION_TIME
     * @param modificationTime
     *            see {@link PropID#LAST_MODIFICATION_TIME}
     */
    public void setPropertyLastModificationTime(Date modificationTime);
}
