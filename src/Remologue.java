/*
 *   Copyright 2017 Behrooz Kamary Aliabadi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableCell;
import javafx.scene.control.ComboBox;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;
import javafx.event.ActionEvent;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TextField;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.StageStyle;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;

import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import java.io.BufferedReader;
import java.io.*;
import java.net.*;
import java.util.regex.*;
import java.util.ArrayList;
import java.util.List;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Remologue extends Application implements Runnable
{

    private SearchBox search = new SearchBox();
    private Button butApply;
    private Button butClear;
    private ComboBox <String> levelCombo;
    private TableView <SyslogItem> table = new TableView<SyslogItem>();
    private ObservableList<SyslogItem> data = FXCollections.observableArrayList();
    private ObservableList<SyslogItem> dataFiltered = FXCollections.observableArrayList();


    private MenuItem menuCBind = new MenuItem("Bind");
    private DatagramSocket socket;
    private static BorderPane root = new BorderPane();

    private final static float WIDTH = 700;
    private final static float HEIGHT = 500;
    private final static int PACKETSIZE = 2048;
    private Thread thread;

    private static boolean running;
    private static boolean filter;
    private String filterText;
    private String filterLevelString;
    private int filterLevel;

    private double MenuHeight = 5.0;

    public static void main(String[] args)
    {
        launch(args);
    }

    @Override
    public void start(Stage stage)
    {
        setupMenu();

        table.setEditable(true);

        table.setFixedCellSize(Region.USE_COMPUTED_SIZE);
        //table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn <SyslogItem, String> timeCol = new TableColumn <SyslogItem, String> ("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("time"));

        TableColumn <SyslogItem, String> facilityCol = new TableColumn <SyslogItem, String> ("Facility");
        facilityCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("facility"));

        TableColumn <SyslogItem, String> levelCol = new TableColumn <SyslogItem, String> ("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("level"));

        TableColumn <SyslogItem, String> messageCol = new TableColumn <SyslogItem, String> ("Message");
        messageCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("message"));
        messageCol.prefWidthProperty().bind(
                table.widthProperty()
                .subtract(timeCol.widthProperty())
                .subtract(facilityCol.widthProperty())
                .subtract(levelCol.widthProperty())
                .subtract(0));

        //timeCol.setCellFactory(column -> {return new GenericCell();});
        //facilityCol.setCellFactory(column -> {return new GenericCell();});
        //messageCol.setCellFactory(column -> {return new GenericCell();});
        levelCol.setCellFactory(column -> {return new LevelCell();});

        table.setItems(data);

        // we wanna avoid addAll() warning
        table.getColumns().add(timeCol);
        table.getColumns().add(facilityCol);
        table.getColumns().add(levelCol);
        table.getColumns().add(messageCol);

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setPrefSize(WIDTH, HEIGHT);    
        anchorPane.getChildren().addAll(table);

        AnchorPane.setTopAnchor(table, MenuHeight);
        AnchorPane.setLeftAnchor(table, 1.0);
        AnchorPane.setRightAnchor(table, 1.0);
        AnchorPane.setBottomAnchor(table, 1.0);

        root.setCenter(anchorPane);

        stage.getIcons().add(new Image("/resources/image/icon.png"));
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().addAll(this.getClass().getResource("/resources/css/style.css").toExternalForm());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.END)
            {
                Platform.runLater( () -> table.scrollTo(table.getItems().size() - 1));
                return;
            }
            if (event.getCode() == KeyCode.HOME)
            {
                Platform.runLater( () -> table.scrollTo(1));
                return;
            }
        });

        stage.setOnCloseRequest(event -> {handleOnClose(event);});
        stage.setScene(scene);
        stage.setTitle("Remologue");
        stage.setWidth(WIDTH);
        stage.setHeight(HEIGHT);
        stage.setMinWidth(WIDTH);
        stage.setMinHeight(HEIGHT);
        stage.show();

        Settings.getInstance();

    }

    // TODO: 'upnp' and 'nat-pmp'
    @Override
    public void run()
    {
        try
        {
            SocketAddress sockaddr = new InetSocketAddress(
                    Settings.getInstance().getIPAddress(),
                    Settings.getInstance().getPort());
            socket = new DatagramSocket(sockaddr);
            socket.setSoTimeout(5000);
            DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);

            addInternalLog("socket has been opened.");

            String      message = new String();
            SyslogItem  item;

            List<String> ignoreList = Settings.getInstance().getIgnoreList();

            while(running)
            {
                try
                {
                    socket.receive(packet);
                }
                catch (SocketTimeoutException ex)
                {
                    if (!message.isEmpty()) addInternalLog("socket timeout.");
                    message = "";
                    continue;
                }

                message = new String(packet.getData(), packet.getOffset(), packet.getLength());

                for (String repl : ignoreList)
                    message = message.replaceAll(repl, "");

                item = new SyslogItem(message);

                data.addAll(item);

                if (filter && filterText.isEmpty() && item.getLevelInt() <= filterLevel)
                {
                    dataFiltered.addAll(item);
                }
                else if (filter && item.getMessage().contains(filterText) &&
                        item.getLevelInt() <= filterLevel)
                {
                    dataFiltered.addAll(item);
                }
            }
        }
        catch (SocketException ex)
        {
            addInternalLog("socket has been closed.");
        }
        catch (Exception ex)
        {
            running = false;
            System.out.println(ex);
        }
        if (socket != null)
        {
            socket.close();
            addInternalLog("socket has been closed.");
        }

    }

    void handleOnClose(WindowEvent event)
    {
        Platform.runLater(new Runnable() {
            @Override
            public void run()
            {
                addInternalLog("Handling event " + event.getEventType());
                exitApp();
            }
        });
    }

    void setupMenu()
    {

        final Menu menuFile = new Menu("File");
        final Menu menuRemote = new Menu("Remote");
        final Menu menuHelp = new Menu("Help");
        MenuItem menuIAbout = new MenuItem("About");
        MenuItem menuIExit = new MenuItem("Exit");
        MenuItem menuISave = new MenuItem("Save RAW");
        MenuItem menuIOpen = new MenuItem("Open RAW");
        MenuItem menuIReset = new MenuItem("Reset");
        MenuItem menuIMemory = new MenuItem("Memory");

        //menuIExit.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));

        menuIAbout.setOnAction(event -> {displayAbout();});

        menuISave.setOnAction(event -> {saveLogs();});
        menuIOpen.setOnAction(event -> {openLogs();});

        menuIMemory.setOnAction(event -> {displayMemory();});

        menuIExit.setOnAction(event -> {exitApp();});

        menuIReset.setOnAction(event -> {resetAll();});

        menuCBind.setOnAction(event -> {connect();});

        menuHelp.getItems().addAll(menuIMemory, menuIAbout);
        menuRemote.getItems().addAll(menuCBind);
        menuFile.getItems().addAll(menuISave, menuIOpen, menuIReset, new SeparatorMenuItem(), menuIExit);
        MenuBar menuBar = new MenuBar();
        //menuBar.prefWidthProperty().bind(root.getScene().getWindow().widthProperty());
        menuBar.getMenus().addAll(menuFile, menuRemote, menuHelp);

        MenuHeight = menuBar.getHeight() + 1;

        root.setTop(menuBar);

        butClear = new Button("Clear");

        butClear.setOnAction(event -> {clearFilter();});

        butClear.setDisable(true);

        butApply = new Button("Apply");
        butApply.setOnAction(event -> {applyFilter();});

        search.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) applyFilter();
        });

        levelCombo = new ComboBox<String>();

        for (int indx = SyslogItem.LOG_EMERG; indx <= SyslogItem.LOG_INTERN; indx++)
        {
            levelCombo.getItems().add(SyslogItem.getLevelString(indx));
        }

        levelCombo.setEditable(false);
        levelCombo.getSelectionModel().selectLast();
        filterLevel = SyslogItem.LOG_DEBUG;
        levelCombo.setOnAction(event -> {
            filterLevelString = levelCombo.getSelectionModel().getSelectedItem().toString();
            filterLevel = levelCombo.getSelectionModel().getSelectedIndex();
        });

        HBox toolBar = new HBox();
        toolBar.getChildren().addAll(search, levelCombo, butApply, butClear);
        toolBar.setAlignment(Pos.CENTER_LEFT);
        toolBar.setPadding(new Insets(2, 2, 2, 2));

        VBox vBox = new VBox();
        vBox.getChildren().addAll(menuBar, toolBar);
        HBox.setHgrow(search, Priority.SOMETIMES);
        root.setTop(vBox);
    }

    void applyFilter()
    {

        butApply.setDisable(true);
        levelCombo.setDisable(true);
        butClear.setDisable(false);

        if (search.getText().isEmpty() && filterLevel >= SyslogItem.LOG_DEBUG)
        {
            filter = false;
            clearFilter();
            return;
        }

        filter = true;
        filterText = search.getText();
        dataFiltered.clear();

        addInternalLog("apply filter with level (" + filterLevelString + ") and (" + filterText + ")");

        for (SyslogItem item : data)
        {
            if (filterText.isEmpty() && item.getLevelInt() <= filterLevel)
            {
                dataFiltered.add(item);
            }
            else if (item.getMessage().contains(filterText) && item.getLevelInt() <= filterLevel)
            {
                dataFiltered.add(item);
            }
        }

        table.setItems(dataFiltered);

    }

    void clearFilter()
    {
        filter = false;
        table.setItems(data);
        dataFiltered.clear();
        butClear.setDisable(true);
        butApply.setDisable(false);
        levelCombo.setDisable(false);
    }

    void exitApp()
    {
        running = false;
        if (socket != null) socket.close();
        Platform.exit();
    }

    void displayMemory()
    {
        double memory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        String unit = "[Bytes]";

        if (memory > 1024)
        {
            memory /= 1024;
            unit = "[KBytes]";
        }

        if (memory > 1024)
        {
            memory /= 1024;
            unit = "[MBytes]";
        }

        if (memory > 1024)
        {
            memory /= 1024;
            unit = "[GBytes]";
        }

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Remologue");
        alert.setHeaderText(null);
        alert.setContentText("Memory Usage : " + String.format("%.2f", memory) + "  " + unit);
        alert.showAndWait();
    }

    void saveLogs()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Logs");
        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null)
        {
            try
            {
                System.out.println("save file : " + file.getPath());
                String raw = new String();
                FileWriter writer = new FileWriter(file.getPath()); 
                for(SyslogItem item: data)
                {
                    raw = item.getRAW();
                    if (!raw.isEmpty())
                        writer.write(raw);
                }
                writer.close();
            }
            catch (IOException ex)
            {
                System.out.println(ex.getMessage());
            }
        }
    }

    void openLogs()
    {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Logs");
        File file = fileChooser.showSaveDialog(root.getScene().getWindow());
        if (file != null)
        {
            running = false;
            menuCBind.setText("Bind");

            resetAll();

            String line = new String();
            SyslogItem  item;
            List<String> ignoreList = Settings.getInstance().getIgnoreList();

            try
            {
                addInternalLog("read file : " + file.getPath());

                FileReader reader = new FileReader(file);
                BufferedReader breader = new BufferedReader(reader);

                while ((line = breader.readLine()) != null)
                {

                    for (String repl : ignoreList)
                        line = line.replaceAll(repl, "");

                    item = new SyslogItem(line);

                    data.addAll(item);

                    if (filter && filterText.isEmpty() && item.getLevelInt() <= filterLevel)
                    {
                        dataFiltered.addAll(item);
                    }
                    else if (filter && item.getMessage().contains(filterText) &&
                            item.getLevelInt() <= filterLevel)
                    {
                        dataFiltered.addAll(item);
                    }
                }

                reader.close();
            }
            catch (IOException ex)
            {
                System.out.println(ex.getMessage());
            }
        }
    }

    void displayAbout()
    {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText(null);
        alert.setContentText("Remologue Copyright 2017");
        alert.showAndWait();
    }

    void connect()
    {
        if (running)
        {
            running = false;
            menuCBind.setText("Bind");
        }
        else
        {
            thread = new Thread(this);
            running = true;
            thread.start();
            menuCBind.setText("Unbind");
        }
    }

    void resetAll()
    {
        data.clear();
        dataFiltered.clear();
    }

    void addInternalLog(String msg)
    {
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
        SyslogItem item = new SyslogItem(time, SyslogItem.LOG_REMO, SyslogItem.LOG_INTERN, msg);
        data.addAll(item);
    }
    
}
