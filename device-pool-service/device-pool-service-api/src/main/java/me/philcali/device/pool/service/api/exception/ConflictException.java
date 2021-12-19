package me.philcali.device.pool.service.api.exception;

public class ConflictException extends ClientException {
    private static final long serialVersionUID = -4171506922223538482L;

    public ConflictException(String message) {
        super(message);
    }
}
