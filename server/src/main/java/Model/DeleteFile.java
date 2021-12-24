package Model;

import lombok.Data;

import java.io.IOException;
import java.nio.file.Path;

@Data
public class DeleteFile implements AbstractMessage{



    private  String fileName;


    public void DeleteMessage(String s) throws IOException {

        this.fileName = fileName;
    }
    public String getFileName() {
        return fileName;
    }

    @Override
    public MessageType getMessageType() {

        return MessageType.FILE_DELETE;
    }

}
