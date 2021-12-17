import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Handler implements Runnable {
    private static final String SERVER_DIR = "C:\\Cloud_Storage";
    private static final int BUFFER_SIZE = 8192;
    private final DataInputStream is;
    private final DataOutputStream os;
    private Path path;
    private byte[] buffer;

    public Handler(Socket socket) throws IOException {
        is = new DataInputStream(socket.getInputStream());
        os = new DataOutputStream(socket.getOutputStream());
        createDir();
        path = Paths.get(SERVER_DIR);
        System.out.println("Client accepted...");
        buffer = new byte[BUFFER_SIZE];
    }

    private void createDir() {
        File srvDir = new File(SERVER_DIR);
        if (!srvDir.exists())
            srvDir.mkdir();
    }

    @Override
    public void run() {
        try {
            while (true) {
                String command = is.readUTF().trim();
                System.out.println("received command: " + command);
                switch (command) {
                    case "download": {
                        downloadFile();
                    }

                    case "getFile":

                        sendServerFiles();
                        break;
                    case "putFile": {
                        writeFile();
                        sendServerFiles();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadFile() throws IOException {
        String fileName = is.readUTF();
        Path path = Path.of(getPath(fileName, SERVER_DIR));

//        os.writeUTF(fileName);
        os.writeLong(Files.size(path));
        System.out.println("size"+Files.size(path));
        os.write(Files.readAllBytes(path));
        os.flush();

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

    private void writeFile() throws IOException {
        String fileName = is.readUTF();
        long size = is.readLong();
        try (FileOutputStream fos = new FileOutputStream(path.resolve(fileName).toFile())) {
            for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                int read = is.read(buffer);
                fos.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void sendServerFiles() {
        try {
            os.writeUTF("list");
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<String> names = getFileName();
        try {
            os.writeInt(names.size());
            for (String name : names) {
                os.writeUTF(name);
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> getFileName() {
        try {
            return Files.list(path)
                    .map(p -> p.getFileName().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}