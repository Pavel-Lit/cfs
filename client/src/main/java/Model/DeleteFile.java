package Model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Path;

public class DeleteFile implements AbstractMessage{

    private String fileName;

    public DeleteFile(String fileName) {
        this.fileName = fileName;
    }


    @Override
    public MessageType getMessageType() {

        return MessageType.FILE_DELETE;
    }

}
