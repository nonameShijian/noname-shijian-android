package net.sf.sevenzipjbinding;

/**
 *
 * Enumeration of possible operation results by extracting operations.
 *
 * @author Boris Brodski
 * @since 1.0
 */
public enum ExtractOperationResult {
    /**
     * Extraction was a success
     */
    OK,

    /**
     * Extraction failed: unknown compression method
     */
    UNSUPPORTEDMETHOD,

    /**
     * Extraction failed: data error
     */
    DATAERROR,

    /**
     * Extraction failed: CRC-check failed
     */
    CRCERROR,

    /**
     * Archive data is unavailable. Like data can't be read from archive.
     */
    UNAVAILABLE,

    /**
     * Archive ends unexpected. Archive may be truncated or the next volume is missing.
     */
    UNEXPECTED_END,

    /**
     * There are some data after the end of the payload data.
     */
    DATA_AFTER_END,

    /**
     *
     */
    IS_NOT_ARC,

    /**
     * The data doesn't look like an archive.
     */
    HEADERS_ERROR,

    /**
     * Wrong password.
     */
    WRONG_PASSWORD,

    /**
     * Unknown extract operation result
     */
    UNKNOWN_OPERATION_RESULT;

    /**
     * Return extract operations enumeration item by index
     *
     * @param index
     *            index of enumeration item
     * @return extract operations enumeration item by index
     */
    public static ExtractOperationResult getOperationResult(int index) {
        if (index >= 0 && index < values().length) {
            return values()[index];
        }

        return UNKNOWN_OPERATION_RESULT;
    }
}
