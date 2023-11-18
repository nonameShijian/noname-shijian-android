package net.sf.sevenzipjbinding.impl;

import java.util.Date;

import net.sf.sevenzipjbinding.ArchiveFormat;
import net.sf.sevenzipjbinding.IOutArchive;
import net.sf.sevenzipjbinding.IOutCreateCallback;
import net.sf.sevenzipjbinding.IOutItemAllFormats;
import net.sf.sevenzipjbinding.IOutItemBase;
import net.sf.sevenzipjbinding.IOutItemGZip;
import net.sf.sevenzipjbinding.IOutItemZip;
import net.sf.sevenzipjbinding.SevenZipException;

/**
 * Implementation of the all <code>IOutItemXxx</code> interfaces. Contains information about a single archive item
 * required for a create or update operation. Instances should be created by {@link OutItemFactory}. See
 * {@link IOutCreateCallback#getItemInformation(int, OutItemFactory)} for details.<br>
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
 * @see IOutCreateCallback#getItemInformation(int, OutItemFactory)
 * @see OutItemFactory
 * @see IOutItemBase
 * 
 * @author Boris Brodski
 * @since 9.20-2.00
 */
public final class OutItem implements IOutItemAllFormats {
    private int index;

    private Long dataSize;
    private Integer propertyAttributes;
    private Integer propertyPosixAttributes;
    private String propertyPath;
    private Boolean propertyIsDir;
    private Date propertyLastModificationTime;
    private Date propertyLastAccessTime;
    private Date propertyCreationTime;
    private String propertyUser;
    private String propertyGroup;
    private Boolean propertyIsAnti;
    private String propertySymLink;
    private String propertyHardLink;

    private Boolean updateIsNewData;
    private Boolean updateIsNewProperties;
    private Integer updateOldArchiveItemIndex;

    private IOutArchive<?> outArchive;

    OutItem(IOutArchive<?> outArchive, int index) {
        this.outArchive = outArchive;
        this.index = index;
    }

    /**
     * {@inheritDoc}
     */
    public int getIndex() {
        return index;
    }

    public IOutArchive<?> getOutArchive() {
        return outArchive;
    }

    /**
     * {@inheritDoc}
     */
    public ArchiveFormat getArchiveFormat() {
        return outArchive.getArchiveFormat();
    }

    /**
     * {@inheritDoc}
     */
    public Long getDataSize() {
        return dataSize;
    }

    /**
     * {@inheritDoc}
     */
    public void setDataSize(Long size) {
        this.dataSize = size;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getPropertyAttributes() {
        return propertyAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyAttributes(Integer attributes) {
        this.propertyAttributes = attributes;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getPropertyPosixAttributes() {
        return propertyPosixAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyPosixAttributes(Integer posixAttributes) {
        this.propertyPosixAttributes = posixAttributes;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyPath() {
        return propertyPath;
    }

    /**
     * {@inheritDoc}
     */

    public void setPropertyPath(String path) {
        this.propertyPath = path;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getPropertyIsDir() {
        return propertyIsDir;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyIsDir(Boolean dir) {
        this.propertyIsDir = dir;
    }

    /**
     * {@inheritDoc}
     */
    public Date getPropertyLastModificationTime() {
        return propertyLastModificationTime;
    }

    /**
     * {@inheritDoc}
     */

    public void setPropertyLastModificationTime(Date modificationTime) {
        this.propertyLastModificationTime = modificationTime;
    }

    /**
     * {@inheritDoc}
     */
    public Date getPropertyLastAccessTime() {
        return propertyLastAccessTime;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyLastAccessTime(Date lastAccessTime) {
        this.propertyLastAccessTime = lastAccessTime;
    }

    /**
     * {@inheritDoc}
     */
    public Date getPropertyCreationTime() {
        return propertyCreationTime;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyCreationTime(Date creationTime) {
        this.propertyCreationTime = creationTime;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyUser() {
        return propertyUser;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyUser(String user) {
        this.propertyUser = user;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyGroup() {
        return propertyGroup;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyGroup(String group) {
        this.propertyGroup = group;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertySymLink() {
        return propertySymLink;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertySymLink(String propertySymLink) {
        this.propertySymLink = propertySymLink;
    }

    /**
     * {@inheritDoc}
     */
    public String getPropertyHardLink() {
        return propertyHardLink;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyHardLink(String propertyHardLink) {
        this.propertyHardLink = propertyHardLink;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getPropertyIsAnti() {
        return propertyIsAnti;
    }

    /**
     * {@inheritDoc}
     */
    public void setPropertyIsAnti(Boolean isAnti) {
        this.propertyIsAnti = isAnti;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getUpdateIsNewData() {
        return updateIsNewData;
    }

    /**
     * {@inheritDoc}
     */
    public void setUpdateIsNewData(Boolean updateIsNewData) {
        this.updateIsNewData = updateIsNewData;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getUpdateIsNewProperties() {
        return updateIsNewProperties;
    }

    /**
     * {@inheritDoc}
     */
    public void setUpdateIsNewProperties(Boolean updateIsNewProperties) {
        this.updateIsNewProperties = updateIsNewProperties;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getUpdateOldArchiveItemIndex() {
        return updateOldArchiveItemIndex;
    }

    /**
     * {@inheritDoc}
     */
    public void setUpdateOldArchiveItemIndex(Integer updateOldArchiveItemIndex) {
        this.updateOldArchiveItemIndex = updateOldArchiveItemIndex;
    }

    void verify(boolean update) throws SevenZipException {
        if (update) {
            if (updateIsNewData == null) {
                throw new SevenZipException("updateIsNewData can't be null");
            }
            if (updateIsNewProperties == null) {
                throw new SevenZipException("updateIsNewProperties can't be null");
            }
            if (updateOldArchiveItemIndex == null) {
                throw new SevenZipException("updateOldArchiveItemIndex can't be null");
            }

            if (updateOldArchiveItemIndex.intValue() == -1) {
                if (!updateIsNewData.booleanValue()) {
                    throw new SevenZipException("updateOldArchiveItemIndex must be provided (updateIsNewData is false)");
                }
                if (!updateIsNewProperties.booleanValue()) {
                    throw new SevenZipException(
                            "updateOldArchiveItemIndex must be provided (updateIsNewProperties is false)");
                }
            }
            if (updateIsNewData.booleanValue() && !updateIsNewProperties.booleanValue()) {
                throw new SevenZipException("updateIsNewProperties must be set (updateIsNewData is true)");
            }
        }
    }
}
