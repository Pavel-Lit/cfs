package Netty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Model.*;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AbstractMessageHandler extends SimpleChannelInboundHandler<AbstractMessage> {

    private Path currentPath;

    public AbstractMessageHandler() {
        currentPath = Paths.get("storage");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new FilesList(currentPath));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                AbstractMessage message) throws Exception {
        log.debug("received: {}", message);
        switch (message.getMessageType()) {
            case FILE_REQUEST:
                FileRequest req = (FileRequest) message;
                ctx.writeAndFlush(
                        new FileMessage(currentPath.resolve(req.getFileName()))
                );
                break;
            case FILE:
                FileMessage fileMessage = (FileMessage) message;
                Files.write(
                        currentPath.resolve(fileMessage.getFileName()),
                        fileMessage.getBytes()
                );
                ctx.writeAndFlush(new FilesList(currentPath));
                break;
            case FILE_DELETE:
                DeleteFile deleteMessage = (DeleteFile) message;
                Path path = Path.of(getPath(deleteMessage.getFileName(), "storage"));
                Files.delete(path);
                ctx.writeAndFlush(new FilesList(currentPath));
        }
    }
    private void downloadFile() throws IOException {




    }
    private String getPath(String fileName, String folder) {
        File directory = new File(folder);

        File[] fileList = directory.listFiles();
        for (File file : fileList) {

            if(fileName.equals(file.getName())) {
                return file.getAbsolutePath();
            }


        }

        return "";
    }
}
