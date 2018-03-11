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
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.ClassLoader;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class Remologue extends Application implements Runnable
{


    private Button butApply;
    private Button butClear;
    private ComboBox <String> levelCombo;
    private SearchBox search              = new SearchBox();
    private TableView <SyslogItem> table  = new TableView<SyslogItem>();
    private ObservableList<SyslogItem> data = FXCollections.observableArrayList();
    private ObservableList<SyslogItem> dataFiltered = FXCollections.observableArrayList();

    private MenuItem menuCBind      = new MenuItem("Bind");
    private static BorderPane root  = new BorderPane();

    private final static float WIDTH        = 700;
    private final static float HEIGHT       = 500;
    private final static int   PACKETSIZE   = 4096;
    private Thread thread;

    private static boolean running;
    private static boolean filter;
    private String  filterText;
    private String  filterLevelString;
    private int     filterLevel;
    private final static int SOCKET_TIMEOUT = 10000;

    private double  MenuHeight   = 5.0;
    private boolean filterchange = false;

    private ByteBuffer          buffer      = null;
    private Selector            selector    = null;
    private DatagramChannel     udpchannel  = null;
    private ServerSocketChannel tcpchannel  = null;

    public static void main(String[] args)
    {
        if (Settings.getInstance().getStatus() != ErrorStatus.NONE)
        {
            System.out.println("fatal error occurred.");
            return;
        }

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

        TableColumn <SyslogItem, String> serverCol = new TableColumn <SyslogItem, String> ("Server");
        serverCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("server"));

        TableColumn <SyslogItem, String> facilityCol = new TableColumn <SyslogItem, String> ("Facility");
        facilityCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("facility"));

        TableColumn <SyslogItem, String> levelCol = new TableColumn <SyslogItem, String> ("Level");
        levelCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("level"));

        TableColumn <SyslogItem, String> messageCol = new TableColumn <SyslogItem, String> ("Message");
        messageCol.setCellValueFactory(new PropertyValueFactory<SyslogItem,String>("message"));
        messageCol.prefWidthProperty().bind(
                table.widthProperty()
                .subtract(timeCol.widthProperty())
                .subtract(serverCol.widthProperty())
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
        table.getColumns().add(serverCol);
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

        stage.getIcons().add(new Image("image/icon.png"));
        Scene scene = new Scene(root, WIDTH, HEIGHT);

        URL css = this.getClass().getClassLoader().getResource("css/style.css");
        if (css != null)
        {
            scene.getStylesheets().addAll(css.toExternalForm());
        }


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

    private void digestPacket(DatagramPacket packet)
    {

        String      message = new String();
        SyslogItem  item;

        List<String> ignoreList = Settings.getInstance().getIgnoreList();
        message = new String(packet.getData(), packet.getOffset(), packet.getLength());

        for (String repl : ignoreList)
            message = message.replaceAll(repl, "");

        item = new SyslogItem(packet.getAddress().getHostAddress(), message);

        data.addAll(item);

        if (filter && filterText.isEmpty() && item.getLevelInt() <= filterLevel)
        {
            dataFiltered.addAll(item);
        }
        else if (filter && filterCheck(item.getMessage()) && item.getLevelInt() <= filterLevel)
        {
            dataFiltered.addAll(item);
        }
    }

    // TODO: 'upnp' and 'nat-pmp'
    @Override
    public void run()
    {
        try
        {

            InetSocketAddress sockaddr = new InetSocketAddress(
                    Settings.getInstance().getInterface(),
                    Settings.getInstance().getPort());

            buffer  = ByteBuffer.allocate(PACKETSIZE);

            selector = Selector.open();

            if (Settings.getInstance().getProtocol().equals("udp"))
            {
                udpchannel = DatagramChannel.open();
                udpchannel.socket().bind(sockaddr);
                udpchannel.configureBlocking(false);
                udpchannel.register(selector, SelectionKey.OP_READ , null);
            }
            else if (Settings.getInstance().getProtocol().equals("tcp"))
            {
                tcpchannel = ServerSocketChannel.open();
                tcpchannel.register(selector, SelectionKey.OP_ACCEPT, null);
            }
            else
            {
                running = false;
                addInternalLog("unknown protocol (" + Settings.getInstance().getProtocol() + ").");
                return;
            }

            DatagramPacket packet = new DatagramPacket(new byte[PACKETSIZE], PACKETSIZE);

            addInternalLog("socket has been opened.");
            menuCBind.setText("Unbind");

        }
        catch (Exception ex)
        {
            running = false;
            menuCBind.setText("Bind");
            System.out.println(ex);
            addInternalLog("socket has been closed.");
        }

        while(running)
        {

            try
            {

                int readyChannels = selector.select(SOCKET_TIMEOUT);

                if (filterchange)
                {
                    applyFilter();
                    filterchange = false;
                }

                if (readyChannels == 0) continue;

                Iterator <SelectionKey> iterKeys = selector.selectedKeys().iterator();

                while (iterKeys.hasNext())
                {
                    try
                    {
                        SelectionKey key = iterKeys.next();

                        if (!key.isValid())
                        {
                            continue;
                        }
                        if (key.isAcceptable())
                        {
                            SocketChannel client = tcpchannel.accept();
                            client.configureBlocking(false);
                            client.register(selector, SelectionKey.OP_READ, null);
                        }
                        else if (udpchannel != null && key.isReadable())
                        {
                            buffer.clear();
                            SocketAddress remoteAddr = udpchannel.receive(buffer);
                            buffer.flip();
                            int limits = buffer.limit();
                            byte bytes[] = new byte[limits];
                            buffer.get(bytes, 0, limits);
                            digestPacket(new DatagramPacket(bytes, limits, remoteAddr));
                        }
                        else if (tcpchannel != null && key.isReadable())
                        {
                            SocketChannel client = (SocketChannel) key.channel();
                            client.read(buffer);
                            buffer.flip();
                            int limits = buffer.limit();
                            byte bytes[] = new byte[limits];
                            buffer.get(bytes, 0, limits);
                            SocketAddress tcpaddress = client.getRemoteAddress();
                            digestPacket(new DatagramPacket(bytes, limits, tcpaddress));
                        }

                        iterKeys.remove();
                    }
                    catch (IOException ex)
                    {
                        addInternalLog("IOException " + ex.getMessage());
                    }
                }
            }
            catch (IOException ex)
            {
                addInternalLog("IOException " + ex.getMessage());
            }
        }

        try
        {
            if (udpchannel != null)
            {
                udpchannel.close();
                udpchannel = null;
            }
            if (tcpchannel != null)
            {
                tcpchannel.close();
                tcpchannel = null;
            }

            addInternalLog("socket has been closed.");
        }
        catch (IOException ex)
        {
            addInternalLog("socket could not be closed properly.");
        }

        menuCBind.setText("Bind");

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
        MenuItem menuIReload = new MenuItem("Reload Settings");
        MenuItem menuIStats = new MenuItem("Statistics");

        //menuIExit.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.CONTROL_DOWN));

        menuIAbout.setOnAction(event -> {displayAbout();});

        menuISave.setOnAction(event -> {saveLogs();});
        menuIOpen.setOnAction(event -> {openLogs();});
        menuIReload.setOnAction(event -> {reloadSettings();});

        menuIStats.setOnAction(event -> {displayStats();});

        menuIExit.setOnAction(event -> {exitApp();});

        menuIReset.setOnAction(event -> {resetAll();});

        menuCBind.setOnAction(event -> {
            if (running) disconnect();
            else connect();
        });

        menuHelp.getItems().addAll(menuIStats, menuIAbout);
        menuRemote.getItems().addAll(menuCBind);
        menuFile.getItems().addAll(menuISave, menuIOpen, menuIReset, menuIReload, new SeparatorMenuItem(), menuIExit);
        MenuBar menuBar = new MenuBar();
        //menuBar.prefWidthProperty().bind(root.getScene().getWindow().widthProperty());
        menuBar.getMenus().addAll(menuFile, menuRemote, menuHelp);

        MenuHeight = menuBar.getHeight() + 1;

        root.setTop(menuBar);

        butClear = new Button("Clear");

        butClear.setOnAction(event -> {clearFilter();});

        butClear.setDisable(true);

        butApply = new Button("Apply");
        butApply.setOnAction(event -> {
            filterchange = true;
            selector.wakeup();
        });

        search.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER)
        {
            filterchange = true;
            selector.wakeup();
        }
        });

        levelCombo = new ComboBox<String>();

        for (int indx = SyslogItem.LOG_EMERG; indx <= SyslogItem.LOG_INTERN; indx++)
        {
            levelCombo.getItems().add(SyslogItem.getLevelString(indx));
        }

        levelCombo.setEditable(false);
        levelCombo.getSelectionModel().selectLast();
        filterLevel = SyslogItem.LOG_DEBUG;
        filterLevelString = levelCombo.getSelectionModel().getSelectedItem().toString();
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

        if (search.getText().isEmpty() && filterLevel >= SyslogItem.LOG_INTERN)
        {
            filter = false;
            clearFilter();
            return;
        }

        filter = true;
        filterText = search.getText();
        dataFiltered.clear();

        if (filterText.isEmpty())
        {

            addInternalLog("apply filter with level (" + filterLevelString + ")");

            for (SyslogItem item : data)
            {
                if (item.getLevelInt() <= filterLevel)
                {
                    dataFiltered.add(item);
                }
            }
        }
        else
        {

            addInternalLog("apply filter with level (" + filterLevelString + ") and (" + filterText + ")");

            for (SyslogItem item : data)
            {
                if (filterCheck(item.getMessage()) && item.getLevelInt() <= filterLevel)
                {
                    dataFiltered.add(item);
                }
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
        disconnect();
        Platform.exit();
    }

    void displayStats()
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

        String msg = "Memory Usage : " + String.format("%.2f", memory) + "  " + unit;

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Remologue");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    void reloadSettings()
    {
        if (running)
        {
            disconnect();
            Settings.getInstance().reload();
            connect();
        }
        else
        {

            Settings.getInstance().reload();
        }

        addInternalLog("settings reloaded.");
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
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null)
        {
            disconnect();

            resetAll();

            SyslogItem  item;
            String line             = new String();
            List<String> ignoreList = Settings.getInstance().getIgnoreList();

            try
            {
                addInternalLog("read file : " + file.getPath());

                FileReader reader       = new FileReader(file);
                BufferedReader breader  = new BufferedReader(reader);

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
                    else if (filter && filterCheck(item.getMessage()) && item.getLevelInt() <= filterLevel)
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
        alert.setContentText("Remologue\nCopyright 2017 Behrooz Kamary Aliabadi");
        alert.showAndWait();
    }

    void connect()
    {
        thread = new Thread(this);
        running = true;
        thread.start();
    }

    void resetAll()
    {
        data.clear();
        dataFiltered.clear();
    }

    void disconnect()
    {
        running = false;
        if (selector != null) selector.wakeup();
        thread = null;
    }

    void addInternalLog(String msg)
    {
        String time = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS").format(new Date());
        SyslogItem item = new SyslogItem(time, "local", SyslogItem.LOG_REMO, SyslogItem.LOG_INTERN, msg + System.lineSeparator());
        data.addAll(item);
    }

    boolean filterCheck(String text)
    {
        Pattern pattern = Pattern.compile(filterText);
        Matcher matcher = pattern.matcher(text);
        return matcher.lookingAt();
    }
}
