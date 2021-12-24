import Model.*;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.io.*;
import java.net.Socket;
import java.net.URL;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private ObjectDecoderInputStream in;
    private ObjectEncoderOutputStream out;
    private Path baseDir = Paths.get(System.getProperty("user.home"));
    private final String HOST = "127.0.0.1";
    private final int PORT = 8081;

    public ComboBox localDisksClient;
    public TextField pathFieldClient;
    public TableView<FileInfo> filesTableClient;
    //переделать лист на таблицу чтобы одинаково было
    //    public TableView filesTableServer;
    public ListView filesTableServer;


    //    private void read() {
//        try {
//            while (true) {
//                String command = in.readUTF();
//                System.out.println("srv comand "+ command);
//                if (command.equals("list")) {
//                    addListServerFiles();
//                } else if (command.equals("download")) {
//
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
    private void read() {
        try {
            while (true) {
                AbstractMessage msg = (AbstractMessage) in.readObject();
                switch (msg.getMessageType()) {
                    case FILE:
                        FileMessage fileMessage = (FileMessage) msg;
                        Files.write(
                                Paths.get(pathFieldClient.getText()).resolve(fileMessage.getFileName()),
                                fileMessage.getBytes()
                        );
                        Platform.runLater(this::updateFilesClient);
                        break;
                    case FILES_LIST:
                        FilesList files = (FilesList) msg;
                        Platform.runLater(() -> fillServerView(files.getFiles()));
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fillServerView(List<String> list) {
        filesTableServer.getItems().clear();
        filesTableServer.getItems().addAll(list);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            Socket socket = new Socket(HOST, PORT);
            out = new ObjectEncoderOutputStream(socket.getOutputStream());
            in = new ObjectDecoderInputStream(socket.getInputStream());

            Thread thread = new Thread(this::read);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        addFilesClient();
        openFolderClient();

    }

    // загрузка файлов НА сервер
    public void upload(ActionEvent actionEvent) throws IOException {
        String file = getSelectedFileName();
        Path path = Paths.get(pathFieldClient.getText()).resolve(file);
        out.writeObject(new FileMessage(path));


    }

    // загрузка файла с сервера
    public void download(ActionEvent actionEvent) throws IOException {

        String file = filesTableServer.getSelectionModel().getSelectedItem().toString();
        out.writeObject(new FileRequest(file));


    }

    public void btnExit(ActionEvent actionEvent) {
        Platform.exit();
    }

    public void selectDisk(ActionEvent actionEvent) {
        ComboBox<String> element = (ComboBox<String>) actionEvent.getSource();
        insertFilesToTableClient(Paths.get(element.getSelectionModel().getSelectedItem()));
    }

    public void insertFilesToTableClient(Path path) {
        try {
            pathFieldClient.setText(path.normalize().toAbsolutePath().toString());
            filesTableClient.getItems().clear();

            filesTableClient.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Ошибка обновления списка файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }
    // переделать, сейчас работает на клиенте
    public void btnPathUpSrv(ActionEvent actionEvent) {
//        Path upPath = Paths.get(pathFieldClient.getText()).getParent();
//        if (upPath != null) {
//            insertFilesToTableClient(upPath);
//        }
    }

    private void selectLocalDisk() {
        localDisksClient.getItems().clear();
        for (Path p : FileSystems.getDefault().getRootDirectories()) {
            localDisksClient.getItems().add(p.toString());
        }
        localDisksClient.getSelectionModel().select(0);
    }

    private void addFilesClient() {
        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
        fileTypeColumn.setPrefWidth(24);


        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
        fileNameColumn.setPrefWidth(240);

        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
        fileSizeColumn.setCellFactory(column -> {
            return new TableCell<FileInfo, Long>() {
                @Override
                protected void updateItem(Long item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                        setStyle("");
                    } else {
                        String text = String.format("%,d bytes", item);
                        if (item == -1L) {
                            text = "Folder";
                        }
                        setText(text);
                    }
                }
            };
        });
        fileSizeColumn.setPrefWidth(120);

        filesTableClient.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn);
        filesTableClient.getSortOrder().add(fileSizeColumn);
        selectLocalDisk();


        insertFilesToTableClient(baseDir);
    }

    private void updateFilesClient() {

        insertFilesToTableClient(Paths.get(pathFieldClient.getText()));
    }


    private void openFolderClient() {
        filesTableClient.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    Path path = Paths.get(pathFieldClient.getText()).resolve(filesTableClient.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        insertFilesToTableClient(path);
                    }
                }

            }
        });
    }

    public String getSelectedFileName() {
        if (!filesTableClient.isFocused())
            return null;
        return filesTableClient.getSelectionModel().getSelectedItem().getFileName();
    }
    // удаление файла клиента или сервера
    public void deleteFile() throws IOException {
        if (filesTableClient.isFocused()) {
            String file = getSelectedFileName();
            Path path = Paths.get(pathFieldClient.getText()).resolve(file);
            try {
                Files.delete(path);
            } catch (IOException x) {
                x.printStackTrace();
            }
        } else if (filesTableServer.isFocused()) {
            String file = filesTableServer.getSelectionModel().getSelectedItem().toString();

            out.writeObject(new DeleteFile(file));
        }
        updateFilesClient();

    }


}



