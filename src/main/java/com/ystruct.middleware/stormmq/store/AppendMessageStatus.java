package store;

/**
 * Created by yang on 16-11-30.
 */
public enum AppendMessageStatus {
    PUT_OK,
    END_OF_FILE,
    MESSAGE_SIZE_EXCEEDED,
    UNKNOWN_ERROR,
}
