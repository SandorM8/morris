package morris.service;

import java.io.IOException;

/** The file is not a valid save file. */
public class GameFormatException extends IOException {
    public GameFormatException(String message) {
        super(message);
    }

    public GameFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
