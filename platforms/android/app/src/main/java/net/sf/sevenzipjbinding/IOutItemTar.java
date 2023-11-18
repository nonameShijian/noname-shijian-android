package net.sf.sevenzipjbinding;

import java.util.Date;

/**
 * Tar specific archive item data class. It contains all information about a single archive item, that is needed for a
 * create or an update archive operation. See {@link IOutItemBase} for details.
 * 
 * @see IOutItemBase
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public interface IOutItemTar extends IOutItemBase {
    /**
     * Get property {@link PropID#POSIX_ATTRIB}.
     * 
     * @see PropID#POSIX_ATTRIB
     * @return posix attributes
     */
    public Integer getPropertyPosixAttributes();

    /**
     * Set property {@link PropID#POSIX_ATTRIB}.
     * 
     * @see PropID#POSIX_ATTRIB
     * @param posixAttributes
     *            see {@link PropID#POSIX_ATTRIB}
     */
    public void setPropertyPosixAttributes(Integer posixAttributes);

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
     * Get property {@link PropID#USER}.
     * 
     * @see PropID#USER
     * @return user
     */
    public String getPropertyUser();

    /**
     * Set property {@link PropID#USER}.
     * 
     * @see PropID#USER
     * @param user
     *            see {@link PropID#USER}
     */
    public void setPropertyUser(String user);

    /**
     * Get property {@link PropID#GROUP}.
     * 
     * @see PropID#GROUP
     * @return group
     */
    public String getPropertyGroup();

    /**
     * Set property {@link PropID#GROUP}.
     * 
     * @see PropID#GROUP
     * @param group
     *            see {@link PropID#GROUP}
     */
    public void setPropertyGroup(String group);

    /**
     * Get property {@link PropID#SYM_LINK}.
     * 
     * @see PropID#SYM_LINK
     * @return symLink
     */
    public String getPropertySymLink();

    /**
     * Set property {@link PropID#SYM_LINK}.
     * 
     * @see PropID#GROUP
     * @param symLink
     *            see {@link PropID#SYM_LINK}
     */
    public void setPropertySymLink(String symLink);

    /**
     * Get property {@link PropID#HARD_LINK}.
     * 
     * @see PropID#SYM_LINK
     * @return symLink
     */
    public String getPropertyHardLink();

    /**
     * Set property {@link PropID#HARD_LINK}.
     * 
     * @see PropID#GROUP
     * @param symLink
     *            see {@link PropID#HARD_LINK}
     */
    public void setPropertyHardLink(String symLink);
}
