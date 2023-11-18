package net.sf.sevenzipjbinding;

import java.util.Date;

/**
 * 7-zip specific archive item data class. It contains all information about a single archive item, that is needed for a
 * create or an update archive operation. See {@link IOutItemBase} for details.
 * 
 * @see IOutItemBase
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutItem7z extends IOutItemBase {

    /**
     * Get property {@link PropID#ATTRIBUTES}.
     * 
     * @see PropID#ATTRIBUTES
     * @return attributes
     */
    public Integer getPropertyAttributes();

    /**
     * Set property {@link PropID#ATTRIBUTES}.
     * 
     * @see PropID#ATTRIBUTES
     * @param attributes
     *            see {@link PropID#ATTRIBUTES}
     */
    public void setPropertyAttributes(Integer attributes);

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
     * Get property {@link PropID#IS_FOLDER}.
     * 
     * @see PropID#IS_FOLDER
     * @return dir
     */
    public Boolean getPropertyIsDir();

    /**
     * Set property {@link PropID#IS_FOLDER}.
     * 
     * @see PropID#IS_FOLDER
     * @param dir
     *            see {@link PropID#IS_FOLDER}
     */
    public void setPropertyIsDir(Boolean dir);

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

    /**
     * Get property {@link PropID#IS_ANTI}.
     * 
     * @see PropID#IS_ANTI
     * @return isAnti flag
     */
    public Boolean getPropertyIsAnti();

    /**
     * Set property {@link PropID#IS_ANTI}.
     * 
     * @see PropID#IS_ANTI
     * @param isAnti
     *            see {@link PropID#IS_ANTI}.
     */
    public void setPropertyIsAnti(Boolean isAnti);
}
