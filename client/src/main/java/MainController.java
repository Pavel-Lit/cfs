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
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable {
    private final int BUFFER_SIZE = 8192;
    private final String HOST = "127.0.0.1";
    private final int PORT = 8081;
    private final String SERVER_DIR = "C:\\Cloud_Storage";
    public ComboBox localDisksClient;
    public TextField pathFieldClient;
    public TableView<FileInfo> filesTableClient;
    //    public TableView filesTableServer;
    public ListView filesTableServer;


    private Socket socket;


    private DataInputStream in;
    private DataOutputStream out;


    private void read() {
        try {
            while (true) {
                String command = in.readUTF();
                System.out.println("srv comand "+ command);
                if (command.equals("list")) {
                    addListServerFiles();
                } else if (command.equals("download")) {

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addListServerFiles() {
        try {
            int fileCount = in.readInt();
            Platform.runLater(() -> filesTableServer.getItems().clear());
            for (int i = 0; i < fileCount; i++) {
                String name = in.readUTF();
                Platform.runLater(() -> filesTableServer.getItems().add(name));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            socket = new Socket(HOST, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            Thread thread = new Thread(this::read);
            thread.setDaemon(true);
            thread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out.writeUTF("getFile");
        } catch (IOException e) {
            e.printStackTrace();
        }
        addFilesClient();
        openFolderClient();
//        addFilesServer();


    }


    public void upload(ActionEvent actionEvent) throws IOException {
        String file = getSelectedFileName();
        Path path = Paths.get(pathFieldClient.getText()).resolve(file);
        System.out.println(path);
        out.writeUTF("putFile");
        out.writeUTF(file);
        out.writeLong(Files.size(path));
        out.write(Files.readAllBytes(path));
        out.flush();


    }
    public void download(ActionEvent actionEvent) throws IOException {
//        System.out.println(filesTableServer.getSelectionModel().getSelectedItem().toString());
        byte[] buffer = new byte[BUFFER_SIZE];

        String file = filesTableServer.getSelectionModel().getSelectedItem().toString();
        Path path = Paths.get(pathFieldClient.getText()).resolve(file);

        out.writeUTF("download");
        out.writeUTF(file);

        long size = in.readLong();


        try (FileOutputStream fos = new FileOutputStream(path.toFile())){
            for (int i = 0; i < (size + BUFFER_SIZE - 1) / BUFFER_SIZE; i++) {
                int read = in.read(buffer);
                fos.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        addFilesClient();

    }

    public void getFile(ActionEvent actionEvent) {

    }


    public void btnExit(ActionEvent actionEvent) {
        Platform.exit();
    }


    public void uploadFile(ActionEvent actionEvent) throws IOException {

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

//    public void insertFilesToTableServer(Path path) {
//        try {
//            filesTableServer.getItems().addAll(Files.list(path).map(FileInfo::new).collect(Collectors.toList()));
//        } catch (IOException e) {
//            Alert alert = new Alert(Alert.AlertType.WARNING, "Ошибка обновления списка файлов", ButtonType.OK);
//            alert.showAndWait();
//        }
//    }

    public void btnPathUpSrv(ActionEvent actionEvent) {
        Path upPath = Paths.get(pathFieldClient.getText()).getParent();
        if (upPath != null) {
            insertFilesToTableClient(upPath);
        }
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


        // переделать для серверной части директорию старта начала постороения списка...


        insertFilesToTableClient(Paths.get("C:\\testCS"));
    }

//    private void addFilesServer() {
//        TableColumn<FileInfo, String> fileTypeColumn = new TableColumn<>();
//        fileTypeColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getType().getName()));
//        fileTypeColumn.setPrefWidth(24);
//
//
//        TableColumn<FileInfo, String> fileNameColumn = new TableColumn<>("Name");
//        fileNameColumn.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFileName()));
//        fileNameColumn.setPrefWidth(240);
//
//        TableColumn<FileInfo, Long> fileSizeColumn = new TableColumn<>("Size");
//        fileSizeColumn.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getSize()));
//        fileSizeColumn.setCellFactory(column -> {
//            return new TableCell<FileInfo, Long>() {
//                @Override
//                protected void updateItem(Long item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (item == null || empty) {
//                        setText(null);
//                        setStyle("");
//                    } else {
//                        String text = String.format("%,d bytes", item);
//                        if (item == -1L) {
//                            text = "Folder";
//                        }
//                        setText(text);
//                    }
//                }
//            };
//        });
//        fileSizeColumn.setPrefWidth(120);
//
//        filesTableServer.getColumns().addAll(fileTypeColumn, fileNameColumn, fileSizeColumn);
//        filesTableServer.getSortOrder().add(fileSizeColumn);
//
//
//        // переделать для серверной части директорию старта начала постороения списка...
//
//
//        insertFilesToTableServer(Paths.get(SERVER_DIR));
//    }

    private void openFolderClient() {
        filesTableClient.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    Path path = Paths.get(pathFieldClient.getText()).resolve(filesTableClient.getSelectionModel().getSelectedItem().getFileName());
                    if (Files.isDirectory(path)) {
                        insertFilesToTableClient(path);
//                        fullPathFile(path);
                    }
                }
//                else {
//
//                    Path path = Paths.get(fullFilePath.getText()).resolve(filesTable.getSelectionModel().getSelectedItem().getFileName());
//                    fullPathFile();
//                }
            }
        });
    }

    public String getSelectedFileName() {
        if (!filesTableClient.isFocused())
            return null;
        return filesTableClient.getSelectionModel().getSelectedItem().getFileName();
    }

    public String getCurrentPath() {
        return pathFieldClient.getText();


    }

    public void btnRefresh(ActionEvent actionEvent) {
        try {
            out.writeUTF("getFile");
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "неудалось получить список файлов", ButtonType.OK);
            alert.showAndWait();
        }
    }


    }


