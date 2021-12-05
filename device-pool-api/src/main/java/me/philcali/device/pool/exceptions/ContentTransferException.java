package me.philcali.device.pool.exceptions;

public class ContentTransferException extends RuntimeException {
    private static final long serialVersionUID = 7149765931329187911L;

    public ContentTransferException(Throwable ex) {
        super(ex);
    }
}
