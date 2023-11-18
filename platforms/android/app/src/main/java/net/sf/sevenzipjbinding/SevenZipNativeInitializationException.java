package net.sf.sevenzipjbinding;

/**
 * SevenZip initialization exception. This exception can be thrown by any of initialization routines indicating problems
 * with loading native SevenZipJBinding library into JVM.
 * 
 * @author Boris Brodski
 * @since 4.65-1
 */
public class SevenZipNativeInitializationException extends Exception {

    private static final long serialVersionUID = 42L;

    /**
     * Constructs a new exception with <code>null</code> as its detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable)}.
     */
    public SevenZipNativeInitializationException() {
        super();
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this
     * exception's detail message.
     * 
     * @param message
     *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @since 1.4
     */
    public SevenZipNativeInitializationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new exception with the specified detail message. The cause is not initialized, and may subsequently
     * be initialized by a call to {@link #initCause(Throwable)}.
     * 
     * @param message
     *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
     *            method.
     */
    public SevenZipNativeInitializationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause and a detail message of
     * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail message of
     * <tt>cause</tt>). This constructor is useful for exceptions that are little more than wrappers for other
     * throwables (for example, <code>PrivilegedActionException</code>).
     * 
     * @param cause
     *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
     *            value is permitted, and indicates that the cause is nonexistent or unknown.)
     * @since 1.4
     */
    public SevenZipNativeInitializationException(Throwable cause) {
        super(cause);
    }

}
