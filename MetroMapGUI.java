import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import java.util.*;
import java.util.Map.Entry; 
import java.util.stream.Collectors;

public class MetroMapGUI extends Application {
    private MetroMap metroMap;
    private DijkstraAlgo dijkstraAlgo;
    private ListView<String> stationListView;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {     
        metroMap = new MetroMap();
        dijkstraAlgo = new DijkstraAlgo();
        addDefaultStationsAndEdges();         
        primaryStage.setTitle("Metro Map GUI");
        
        // Green ----> class   blue ----> object  new ----> keyword  yellow ----> Constructor
        Image metroMapImage = new Image("MinorPresentation.jpg");    // object 
        ImageView imageView = new ImageView(metroMapImage);    // actual show on GUI

        stationListView = new ListView<>();
        stationListView.setPrefSize(300, 400);

        BorderPane borderPane = new BorderPane();

        // Create a StackPane to hold the image
        StackPane imagePane = new StackPane();
        imagePane.getChildren().add(imageView);
        borderPane.setLeft(stationListView);
        // Set the StackPane as the center of the BorderPane
        borderPane.setCenter(imagePane);

        MenuBar menuBar = new MenuBar();
        Menu actionsMenu = new Menu("Actions");
        MenuItem shortestPathItem = new MenuItem("Shortest path by Dijkstra's algorithm");
        shortestPathItem.setOnAction(e -> showShortestPathDialog());
        MenuItem displayStationsItem = new MenuItem("Display stations");
        displayStationsItem.setOnAction(e -> displayStations());
        MenuItem addStationItem = new MenuItem("Add Station");
        addStationItem.setOnAction(e -> showAddStationDialog());
        MenuItem addConnectionItem = new MenuItem("Add Connection");
        addConnectionItem.setOnAction(e -> showAddConnectionDialog());
        MenuItem displayMapItem = new MenuItem("Display Metro Map");
        displayMapItem.setOnAction(e -> displayMetroMap());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> System.exit(0));
        actionsMenu.getItems().addAll(shortestPathItem, displayStationsItem, addStationItem, addConnectionItem, displayMapItem, exitItem);
        menuBar.getMenus().add(actionsMenu);
        borderPane.setTop(menuBar);
        Scene scene = new Scene(borderPane, 600, 400);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void displayResultInTable(String title, TableView<ShortestPathEntry> resultTable) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(title);
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButton);
        // Set the minimum and maximum sizes for the dialog content
        dialog.getDialogPane().setMinWidth(420);
        dialog.getDialogPane().setMaxWidth(300);
        dialog.getDialogPane().setMinHeight(400);
        dialog.getDialogPane().setMaxHeight(600);
        dialog.getDialogPane().setContent(resultTable);
        dialog.showAndWait();
    }
    
    private void showShortestPathDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Shortest Path");
        dialog.setHeaderText("Select the source station:");
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        ComboBox<String> sourceStationCombo = new ComboBox<>(FXCollections.observableArrayList(metroMap.getAdj().keySet()));
        sourceStationCombo.setPromptText("Select Source Station");
        VBox content = new VBox(sourceStationCombo);
        content.setSpacing(10);
        content.setPadding(new Insets(20, 150, 10, 10));
        dialog.getDialogPane().setContent(content);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                String source = sourceStationCombo.getValue();
                if (source != null && !source.isEmpty()) {
                    return source;
                }
            }
            return null;
        });
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(source -> {
            Map<String, Integer> distances = dijkstraAlgo.dijkstra(metroMap.getAdj(), source);
            List<ShortestPathEntry> resultEntries = new ArrayList<>();
            for (Entry<String, Integer> entry : distances.entrySet()) {
                if (!entry.getKey().equals(source)) {
                    resultEntries.add(new ShortestPathEntry(source, entry.getKey(), entry.getValue() / 10.0));
                }
            }
            TableView<ShortestPathEntry> resultTable = new TableView<>();
            TableColumn<ShortestPathEntry, String> sourceColumn = new TableColumn<>("Source");
            sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
            TableColumn<ShortestPathEntry, String> destinationColumn = new TableColumn<>("Destination");
            destinationColumn.setCellValueFactory(new PropertyValueFactory<>("destination"));
            TableColumn<ShortestPathEntry, Double> distanceColumn = new TableColumn<>("Distance (KM)");
            distanceColumn.setCellValueFactory(new PropertyValueFactory<>("distance"));
            resultTable.getColumns().addAll(sourceColumn, destinationColumn, distanceColumn);
            resultTable.setItems(FXCollections.observableArrayList(resultEntries));
            displayResultInTable("Shortest Path", resultTable);
        });
    }
    
    public class ShortestPathEntry {
        private String source;
        private String destination;
        private double distance;
        public ShortestPathEntry(String source, String destination, double distance) {
            this.source = source;
            this.destination = destination;
            this.distance = distance;
        }
        public String getSource() {
            return source;
        }
        public String getDestination() {
            return destination;
        }
        public double getDistance() {
            return distance;
        }
    }
    
    private void displayStations() {
        stationListView.getItems().clear();
        stationListView.getItems().addAll(metroMap.getAdj().keySet());
    }

    private void showAddStationDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Station");
        dialog.setHeaderText("Enter the name of the station to add:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(stationName -> {
            metroMap.addNode(stationName);
            displayStations();
        });
    }

    private void showAddConnectionDialog() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add Connection");
        dialog.setHeaderText("Enter the details for the connection:");
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        ComboBox<String> station1Combo = new ComboBox<>(FXCollections.observableArrayList(metroMap.getAdj().keySet()));
        ComboBox<String> station2Combo = new ComboBox<>(FXCollections.observableArrayList(metroMap.getAdj().keySet()));
        TextField distanceField = new TextField();
        distanceField.setPromptText("Distance");
        grid.add(new Label("Station 1:"), 0, 0);
        grid.add(station1Combo, 1, 0);
        grid.add(new Label("Station 2:"), 0, 1);
        grid.add(station2Combo, 1, 1);
        grid.add(new Label("Distance:"), 0, 2);
        grid.add(distanceField, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                String station1 = station1Combo.getValue();
                String station2 = station2Combo.getValue();
                String distanceStr = distanceField.getText();
                if (station1 != null && station2 != null && !distanceStr.isEmpty()) {
                    return new Pair<>(station1, station2);
                }
            }
            return null;
        });
        Optional<Pair<String, String>> result = dialog.showAndWait();
        result.ifPresent(pair -> {
            try {
                int distance = Integer.parseInt(distanceField.getText());
                metroMap.addEdge(pair.getFirst(), pair.getSecond(), distance);
                displayStations();
            } catch (NumberFormatException e) {
                displayResult("Invalid Input", "Please enter a valid distance.");
            }
        });
    }

    private void displayMetroMap() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Metro Map");
        dialog.setHeaderText("Metro Map Stations and Connections");
        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButton);
        TableView<ConnectionInfo> connectionTable = new TableView<>();
        TableColumn<ConnectionInfo, String> stationCol = new TableColumn<>("Station");
        stationCol.setCellValueFactory(new PropertyValueFactory<>("station"));
        TableColumn<ConnectionInfo, String> connectionsCol = new TableColumn<>("Connections");
        connectionsCol.setCellValueFactory(new PropertyValueFactory<>("connections"));
        connectionTable.getColumns().addAll(stationCol, connectionsCol);
        int stationCount = 1;
        for (Entry<String, List<Pair<String, Integer>>> entry : metroMap.getAdj().entrySet()) {
            String stationName = stationCount + ". " + entry.getKey();
            String connections = entry.getValue().stream()
                    .map(p -> p.getFirst() + " (" + (p.getSecond() / 10.0) + " KM)")
                    .collect(Collectors.joining(", "));
            connectionTable.getItems().add(new ConnectionInfo(stationName, connections));
            stationCount++;
        }
        connectionTable.setPrefSize(600, 400);
        connectionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        dialog.getDialogPane().setContent(connectionTable);
        dialog.showAndWait();
    }

    public static class ConnectionInfo {
        private String station;
        private String connections;
        public ConnectionInfo(String station, String connections) {
        this.station = station;
        this.connections = connections;
    }
    public String getStation() {
        return station;
    }
    public String getConnections() {
        return connections;
    }
}

    private void displayResult(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void addDefaultStationsAndEdges() {
        metroMap.addNode("CLOCK TOWER");
        metroMap.addNode("DILARAM CHOWK");
        metroMap.addNode("BB");
        metroMap.addNode("CENTERIO MALL");
        metroMap.addNode("KRISHAN NAGAR CHOWK");
        metroMap.addNode("RAJ BHAWAN");
        metroMap.addNode("ISBT");
        metroMap.addNode("BALLUPUR CHOWK");
        metroMap.addNode("ONGC");
        metroMap.addNode("VASANT VIHAR");
        metroMap.addNode("PANDITWADI");
        metroMap.addNode("IMA");
        metroMap.addNode("MB");
        metroMap.addNode("PREM NAGER");
        metroMap.addNode("PHULSANI");
        metroMap.addNode("NANDI KI CHOWKI");
        metroMap.addNode("PONDHA");
        metroMap.addNode("KANDOLI");
        metroMap.addNode("UPES");

        metroMap.addEdge("CLOCK TOWER", "BB", 10);
        metroMap.addEdge("CLOCK TOWER", "DILARAM CHOWK", 19);
        metroMap.addEdge("BB", "KRISHAN NAGAR CHOWK", 13);
        metroMap.addEdge("KRISHAN NAGAR CHOWK", "BALLUPUR CHOWK", 19);
        metroMap.addEdge("BALLUPUR CHOWK", "ISBT", 75);
        metroMap.addEdge("BALLUPUR CHOWK", "VASANT VIHAR", 25);
        metroMap.addEdge("VASANT VIHAR", "PANDITWADI", 22);
        metroMap.addEdge("PANDITWADI", "IMA", 18);
        metroMap.addEdge("IMA", "MB", 10);
        metroMap.addEdge("MB", "PREM NAGER", 5);
        metroMap.addEdge("IMA", "PREM NAGER", 23);
        metroMap.addEdge("PREM NAGER", "NANDI KI CHOWKI", 24);
        metroMap.addEdge("NANDI KI CHOWKI", "PHULSANI", 50);
        metroMap.addEdge("DILARAM CHOWK", "CENTERIO MALL", 28);
        metroMap.addEdge("CENTERIO MALL", "RAJ BHAWAN", 13);
        metroMap.addEdge("RAJ BHAWAN", "ONGC", 42);
        metroMap.addEdge("ONGC", "BALLUPUR CHOWK", 25);
        metroMap.addEdge("ONGC", "PHULSANI", 40);
        metroMap.addEdge("PHULSANI", "PONDHA", 20);
        metroMap.addEdge("NANDI KI CHOWKI", "PONDHA", 36);
        metroMap.addEdge("PONDHA", "KANDOLI", 30);
        metroMap.addEdge("KANDOLI", "UPES", 30);
    }
}

class Pair<K, V> {    // <1 , 2>
    private K first;
    private V second;
    Pair(K first, V second) {
        this.first = first;
        this.second = second;
    }
    public K getFirst() {
        return first;
    }
    public V getSecond() {
        return second;
    }
}

class MetroMap {
    private Map<String, List<Pair<String, Integer>>> adj = new HashMap<>();
    public void addNode(String stationName) {
        adj.put(stationName, new ArrayList<>());
    }
    public void addEdge(String station1, String station2, int distance) {
        adj.get(station1).add(new Pair<>(station2, distance * 10));
        adj.get(station2).add(new Pair<>(station1, distance * 10));
    }
    public Map<String, List<Pair<String, Integer>>> getAdj() {
        return adj;
    }
}

class DijkstraAlgo {
    public Map<String, Integer> dijkstra(Map<String, List<Pair<String, Integer>>> adj, String source) {
        Map<String, Integer> dist = new HashMap<>();
        for (String station : adj.keySet()) {
            dist.put(station, Integer.MAX_VALUE);
        }
        dist.put(source, 0);
        PriorityQueue<Pair<Integer, String> > pq = new PriorityQueue<>(Comparator.comparing(Pair::getFirst));
        pq.add(new Pair<>(0, source));
        while (!pq.isEmpty()) {
            int dis = pq.peek().getFirst();
            String node = pq.poll().getSecond();
            for (Pair<String, Integer> it : adj.get(node)) {
                String adjNode = it.getFirst();
                int edgeWeight = it.getSecond();
                if (dis + edgeWeight < dist.get(adjNode)) {
                    dist.put(adjNode, dis + edgeWeight);
                    pq.add(new Pair<>(dist.get(adjNode), adjNode));
                }
            }
        }
        return dist;
    }
}
