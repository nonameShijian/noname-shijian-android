package net.sf.sevenzipjbinding;

import java.io.File;
import java.util.Date;

/**
 * Enumeration for possible archive and archive item properties
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public enum PropID {
    /**
     * Dummy property1. First real property should have an item 2. So skip the position 2.
     */
    NO_PROPERTY1,

    /**
     * Dummy property2. First real property should have an item 2. So skip the position 1.
     */
    NO_PROPERTY2,

    /**
     * Unknown purpose
     */
    HANDLER_ITEM_INDEX, // = 2

    /**
     * Full path, name and extension of the file inside the archive. Example. <code>'dir/file.ext'</code>. Use
     * {@link File#separator} for better cross-platform compatibility. Please note, that stream archive formats such as
     * gzip does not support this property, since it is always a single file (or stream) being compressed.<br>
     * <br>
     * Type: {@link String}. <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    PATH, //

    /**
     * In {@link IArchiveOpenCallback}: Name of the volume to get
     */
    NAME, //

    EXTENSION,

    /**
     * Flag either a item represents a folder or not. Please note, that some archive formats doesn't define special
     * items for folders. In this case you may get a item with a path <code>'dir/file'</code> without having an item for
     * <code>'dir'</code> at all. <br>
     * <br>
     * Type: {@link Boolean}. <code>true</code> if item is a folder, otherwise <code>false</code>.
     * <code>Boolean.FALSE</code> is returned, if archive format doesn't support this property. This property is never
     * <code>null</code>.
     */
    IS_FOLDER,

    /**
     * Size of the original file. <code>-1</code> is returned, if no size known for this item.
     * 
     * Type: {@link Long} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    SIZE,

    /**
     * Size of the packed item in archive. Sometimes <code>0</code> will be returned. It means either <i>unknown</i> or
     * the item shares compressed data with other items and so take no additional space in archive.<br>
     * <br>
     * Type: {@link Long} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    PACKED_SIZE, //

    /**
     * Archive item or file attributes. See {@link AttributesBitMask} for the supported bit masks. <br>
     * <br>
     * Type: {@link Integer} <code>null</code> will be returned, if current archive type doesn't support this property.<br>
     * <br>
     * <i>Compression:</i> The flag {@link AttributesBitMask#FILE_ATTRIBUTE_DIRECTORY} should be consistent with the
     * {@link IOutItemAllFormats#getPropertyIsDir()} property.
     */
    ATTRIBUTES, //

    /**
     * Date and time of the file creation. <br>
     * <br>
     * Type: {@link Date} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    CREATION_TIME, //

    /**
     * Date and time of last access of the file. <br>
     * <br>
     * Type: {@link Date} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    LAST_ACCESS_TIME, //

    /**
     * Date and time the file was last time modified.<br>
     * <br>
     * Type: {@link Date} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    LAST_MODIFICATION_TIME, //

    SOLID, //
    COMMENTED,

    /**
     * Flag either a item encrypted or not. <br>
     * <br>
     * Type: {@link Boolean}. <code>true</code> if item is encrypted, otherwise <code>false</code>.
     * <code>Boolean.FALSE</code> is returned, if archive format doesn't support this property. This property is never
     * <code>null</code>.
     */
    ENCRYPTED, //
    SPLIT_BEFORE, //
    SPLIT_AFTER, //
    DICTIONARY_SIZE, // 
    CRC, //
    TYPE, //

    /**
     * If <code>true</code> delete corresponding file or directory during extraction. Supported only by
     * {@link ArchiveFormat#SEVEN_ZIP}. <br>
     * <br>
     * Type: {@link Boolean} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    IS_ANTI, //

    METHOD, //
    HOST_OS, //
    FILE_SYSTEM, //

    /**
     * User (owner) of the file.<br>
     * <br>
     * Type: {@link String} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    USER, //

    /**
     * Group (owner) of the file.<br>
     * <br>
     * Type: {@link String} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    GROUP, //

    BLOCK, //
    COMMENT, //
    POSITION, //
    PREFIX, //
    NUM_SUB_DIRS, //
    NUM_SUB_FILES, //
    UNPACK_VER, //
    VOLUME, //
    IS_VOLUME, //
    OFFSET, //
    LINKS, //
    NUM_BLOCKS, //
    NUM_VOLUMES, //
    TIME_TYPE, //
    BIT64, //
    BIG_ENDIAN, //
    CPU, //
    PHY_SIZE, //
    HEADERS_SIZE, //
    CHECKSUM, //
    CHARACTS, //
    VA, //
    ID, //
    SHORT_NAME, //
    CREATOR_APP, //
    SECTOR_SIZE, //

    /**
     * Posix attributes.<br>
     * <br>
     * Type: {@link Integer} <code>null</code> will be returned, if current archive type doesn't support this property.
     */
    POSIX_ATTRIB, //

    /**
     * Represent a symbolic link. Used for example in Tar archives.
     */
    SYM_LINK, //

    ERROR, //
    TOTAL_SIZE, //
    FREE_SPACE, //
    CLUSTER_SIZE, //
    VOLUME_NAME, //
    LOCAL_NAME, //
    PROVIDER, //
    NT_SECURE, //
    IS_ALT_STREAM, //
    IS_AUX, //
    IS_DELETED, //
    IS_TREE, //
    SHA1, //
    SHA256, //
    ERROR_TYPE, //
    NUM_ERRORS, //
    ERROR_FLAGS, //
    WARNING_FLAGS, //
    WARNING, //
    NUM_STREAMS, //
    NUM_ALT_STREAMS, //
    ALT_STREAMS_SIZE, //
    VIRTUAL_SIZE, //
    UNPACK_SIZE, //
    TOTAL_PHY_SIZE, //
    VOLUME_INDEX, //
    SUB_TYPE, //
    SHORT_COMMENT, //
    CODE_PAGE, //
    IS_NOT_ARC_TYPE, //
    PHY_SIZE_CANT_BE_DETECTED, //
    ZEROS_TAIL_IS_ALLOWED, //
    TAIL_SIZE, //
    EMBEDDED_STUB_SIZE, //
    NT_REPARSE, //

    /**
     * Represent a hard link. Used for example in Tar archives.
     */
    HARD_LINK, //
    INODE, //
    STREAM_ID, //
    READ_ONLY, //
    OUT_NAME, //
    COPY_LINK, //

    NUM_DEFINED, //

    // TODO Add test to ensure "kpidLink"(c++) == "LINK" (java)

    USER_DEFINED(0x10000), //

    /**
     * Unknown PropID. This PropID shouldn't be used.
     */
    UNKNOWN(-1);

    /**
     * Windows file attribute bit mask.
     * 
     * @see PropID#ATTRIBUTES
     */
    public static class AttributesBitMask {
        /**
         * Bit-mask for attribute: read only.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_READONLY = 1;

        /**
         * Bit-mask for attribute: hidden.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_HIDDEN = 2;

        /**
         * Bit-mask for attribute: system.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_SYSTEM = 4;

        /**
         * Bit-mask for attribute: directory.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_DIRECTORY = 16;

        /**
         * Bit-mask for attribute: archive.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_ARCHIVE = 32;

        /**
         * Bit-mask for attribute: device.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_DEVICE = 64;

        /**
         * Bit-mask for attribute: normal.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_NORMAL = 128;

        /**
         * Bit-mask for attribute: temporary.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_TEMPORARY = 256;

        /**
         * Bit-mask for attribute: sparse file.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_SPARSE_FILE = 512;

        /**
         * Bit-mask for attribute: reparse point.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_REPARSE_POINT = 1024;

        /**
         * Bit-mask for attribute: compressed.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_COMPRESSED = 2048;

        /**
         * Bit-mask for attribute: offline.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_OFFLINE = 0x1000;

        /**
         * Bit-mask for attribute: encrypted.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_ENCRYPTED = 0x4000;

        /**
         * Bit-mask for attribute: unix extension.
         * 
         * @see PropID#ATTRIBUTES
         */
        public static final int FILE_ATTRIBUTE_UNIX_EXTENSION = 0x8000; /* trick for Unix */
    }

    private final int propIDIndex;

    private PropID(int propIDIndex) {
        this.propIDIndex = propIDIndex;
    }

    private PropID() {
        this.propIDIndex = ordinal();
    }

    /**
     * Native index of the property
     * 
     * @return Native index of the property
     */
    public int getPropIDIndex() {
        return propIDIndex;
    }

    /**
     * Get enumeration element by native property index
     * 
     * @param propIDIndex
     *            native property index
     * @return enumeration element
     */
    public static PropID getPropIDByIndex(int propIDIndex) {
        if (propIDIndex >= 0 && propIDIndex < values().length && values()[propIDIndex].getPropIDIndex() == propIDIndex) {
            return values()[propIDIndex];
        }

        for (int i = values().length - 1; i != -1; i--) {
            if (values()[i].getPropIDIndex() == propIDIndex) {
                return values()[i];
            }
        }

        return UNKNOWN;
    }
}
