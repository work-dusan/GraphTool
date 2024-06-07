package main.graphtool;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;

public class GraphTool extends Application {

    private final List<Vertex> vertices = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private Vertex dragStartVertex = null;
    private final ObservableList<Step> stepsData = FXCollections.observableArrayList();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        HBox canvasBox = new HBox();

        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        canvasBox.getChildren().add(canvas);
        canvasBox.setAlignment(Pos.CENTER);
        canvasBox.setMaxWidth(800);
        canvasBox.setMaxHeight(600);
        canvasBox.setStyle("-fx-background-color: #fff");

        VBox instructions = new VBox(10);
        instructions.getChildren().addAll(
                new Label("INSTRUCTIONS"),
                new Label("Add: Left Click"),
                new Label("Move: Ctrl Drag"),
                new Label("Connect: Drag"),
                new Label("Remove: Right Click")
        );
        instructions.setPadding(new Insets(20));

        HBox controls = new HBox(10);

        Button bfsButton = new Button("BFS Tree");
        Button dfsButton = new Button("DFS Tree");
        Button spButton = new Button("Shortest Path");
        TextField startVertexField = new TextField();
        startVertexField.setPromptText("Starting vertex");
        TextField endVertexField = new TextField();
        endVertexField.setPromptText("Ending vertex");

        controls.getChildren().addAll(new Label("Start Vertex: "),
                startVertexField,
                bfsButton,
                dfsButton,
                spButton,
                new Label("End Vertex: "),
                endVertexField);

        controls.setMaxWidth(root.getMaxWidth());
        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(0,0,20,0));

        TableView<Step> tableView = new TableView<>();

        TableColumn<Step, String> iterationColumn = new TableColumn<>("Iteration");
        TableColumn<Step, String> currentNodeColumn = new TableColumn<>("Current Node");
        TableColumn<Step, String> queueOrStackColumn = new TableColumn<>("Queue/Stack");

        iterationColumn.setCellValueFactory(data -> data.getValue().iterationProperty());
        currentNodeColumn.setCellValueFactory(data -> data.getValue().currentNodeProperty());
        queueOrStackColumn.setCellValueFactory(data -> data.getValue().queueOrStackProperty());

        tableView.getColumns().addAll(iterationColumn, currentNodeColumn, queueOrStackColumn);
        tableView.setItems(stepsData);

        VBox rightPane = new VBox(10);
        VBox.setMargin(tableView, new Insets(0,0,0,20));
        rightPane.getChildren().addAll(instructions, tableView);

        root.setLeft(rightPane);
        root.setCenter(canvasBox);
        root.setBottom(controls);
        BorderPane.setAlignment(controls, Pos.BOTTOM_CENTER);

        Scene scene = new Scene(root, 1000, 600);

        primaryStage.setTitle("Graph Learning Tool");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setFullScreen(true);
        primaryStage.show();

        bfsButton.setOnAction(e -> {
            stepsData.clear();
            try {
                int startId = Integer.parseInt(startVertexField.getText());
                bfs(vertices.get(startId), gc);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        dfsButton.setOnAction(e -> {
            stepsData.clear();
            try {
                int startId = Integer.parseInt(startVertexField.getText());
                dfs(vertices.get(startId), gc);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        spButton.setOnAction(e -> {
            stepsData.clear();
            try {
                int startId = Integer.parseInt(startVertexField.getText());
                int endId = Integer.parseInt(endVertexField.getText());
                shortestPath(vertices.get(startId), vertices.get(endId), gc);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        final Vertex[] selectedVertex = new Vertex[1];

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                addVertex(e.getX(), e.getY());
            } else if (e.getButton() == MouseButton.SECONDARY) {
                removeVertex(e.getX(), e.getY());
            }
            drawGraph(gc);
        });

        canvas.setOnMouseDragged(e -> {
            if (e.isControlDown()) {
                moveVertex(e.getX(), e.getY(), selectedVertex, gc);
            }
        });

        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                startDrag(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                endDrag(e.getX(), e.getY());
            }
            drawGraph(gc);
        });
    }

    private void addVertex(double x, double y) {
        if (isValidVertexPosition(x, y)) {
            vertices.add(new Vertex(vertices.size(), x, y));
        }
    }

    private void removeVertex(double x, double y) {
        Vertex vertexToRemove = vertices.stream().filter(v -> v.contains(x, y)).findFirst().orElse(null);

        if (vertexToRemove != null) {
            vertices.remove(vertexToRemove);
            edges.removeIf(edge -> edge.start() == vertexToRemove || edge.end() == vertexToRemove);
            renumberVertices();
        }
    }

    private boolean isValidVertexPosition(double x, double y) {
        for (Vertex v : vertices) {
            if (v.contains(x, y)) {
                return false;
            }
        }
        return true;
    }

    private void renumberVertices() {
        for (int i = 0; i < vertices.size(); i++) {
            vertices.get(i).setId(i);
        }
    }

    private void drawGraph(GraphicsContext gc) {
        gc.clearRect(0, 0, 800, 600);
        for (Edge edge : edges) {
            edge.draw(gc);
        }
        for (Vertex vertex : vertices) {
            vertex.draw(gc);
        }
    }

    private void startDrag(double x, double y) {
        for (Vertex v : vertices) {
            if (v.contains(x, y)) {
                dragStartVertex = v;
                break;
            }
        }
    }

    private void endDrag(double x, double y) {
        if (dragStartVertex != null) {
            for (Vertex v : vertices) {
                if (v.contains(x, y) && v != dragStartVertex) {
                    edges.add(new Edge(dragStartVertex, v));
                    break;
                }
            }
        }
        dragStartVertex = null;
    }

    private void moveVertex(double x, double y, Vertex[] selectedVertex, GraphicsContext gc) {
        if (selectedVertex[0] == null) {
            for (Vertex v : vertices) {
                if (v.contains(x, y)) {
                    selectedVertex[0] = v;
                    break;
                }
            }
        } else {
            selectedVertex[0].setX(x);
            selectedVertex[0].setY(y);
            selectedVertex[0] = null;
            drawGraph(gc);
        }
    }

    private void bfs(Vertex start, GraphicsContext gc) {
        System.out.println("Starting BFS from vertex: " + start.getId());

        Map<Vertex, Vertex> parentMap = new HashMap<>();
        Set<Vertex> visited = new HashSet<>();
        Queue<Vertex> queue = new LinkedList<>();

        int iteration = 1;

        visited.add(start);
        queue.add(start);
        parentMap.put(start, null); // Root has no parent

        while (!queue.isEmpty()) {
            int queueSize = queue.size();
            for (int i = 0; i < queueSize; i++) {
                Vertex current = queue.poll();
                assert current != null;
                System.out.println("Visiting vertex: " + current.getId());

                for (Edge edge : edges) {
                    Vertex neighbor = null;
                    if (edge.start() == current) {
                        neighbor = edge.end();
                    } else if (edge.end() == current) {
                        neighbor = edge.start();
                    }

                    if (neighbor != null && !visited.contains(neighbor)) {
                        System.out.println("Adding vertex to queue: " + neighbor.getId());
                        visited.add(neighbor);
                        queue.add(neighbor);
                        parentMap.put(neighbor, current);
                    }
                }

                stepsData.add(new Step(iteration, current, queue.toString()));
            }
            iteration++;
        }

        gc.clearRect(0, 0, 800, 600);

        for (Map.Entry<Vertex, Vertex> entry : parentMap.entrySet()) {
            Vertex child = entry.getKey();
            Vertex parent = entry.getValue();
            if (parent != null) {
                System.out.println("Drawing edge from " + parent.getId() + " to " + child.getId());
                gc.strokeLine(parent.getX(), parent.getY(), child.getX(), child.getY());
            }
            gc.strokeOval(child.getX() - Vertex.getRadius(),
                    child.getY() - Vertex.getRadius(),
                    Vertex.getRadius() * 2,
                    Vertex.getRadius() * 2);
            gc.strokeText(String.valueOf(child.getId()), child.getX() - 5, child.getY() + 5);
        }

        System.out.println("BFS complete");
        stepsData.add(new Step(iteration, null, "BFS complete"));
    }

    private void dfs(Vertex start, GraphicsContext gc) {
        System.out.println("Starting DFS from vertex: " + start.getId());

        Map<Vertex, Vertex> parentMap = new HashMap<>();
        Set<Vertex> visited = new HashSet<>();
        Stack<Vertex> stack = new Stack<>();
        int iteration = 1;

        visited.add(start);
        stack.push(start);
        parentMap.put(start, null); // Root has no parent

        while (!stack.isEmpty()) {
            Vertex current = stack.pop();
            System.out.println("Visiting vertex: " + current.getId());

            for (Edge edge : edges) {
                Vertex neighbor = null;
                if (edge.start() == current) {
                    neighbor = edge.end();
                } else if (edge.end() == current) {
                    neighbor = edge.start();
                }

                if (neighbor != null && !visited.contains(neighbor)) {
                    System.out.println("Adding vertex to stack: " + neighbor.getId());
                    visited.add(neighbor);
                    stack.push(neighbor);
                    parentMap.put(neighbor, current);
                }
            }

            stepsData.add(new Step(iteration, current, stack.toString()));
            iteration++;
        }

        gc.clearRect(0, 0, 800, 600);

        for (Map.Entry<Vertex, Vertex> entry : parentMap.entrySet()) {
            Vertex child = entry.getKey();
            Vertex parent = entry.getValue();
            if (parent != null) {
                System.out.println("Drawing edge from " + parent.getId() + " to " + child.getId());
                gc.strokeLine(parent.getX(), parent.getY(), child.getX(), child.getY());
            }
            gc.strokeOval(child.getX() - Vertex.getRadius(),
                    child.getY() - Vertex.getRadius(),
                    Vertex.getRadius() * 2,
                    Vertex.getRadius() * 2);
            gc.strokeText(String.valueOf(child.getId()), child.getX() - 5, child.getY() + 5);
        }

        System.out.println("DFS complete");
        stepsData.add(new Step(iteration, null, "DFS complete"));
    }

    private void shortestPath(Vertex start, Vertex end, GraphicsContext gc) {
        System.out.println("Finding shortest path from vertex: " + start.getId() + " to vertex: " + end.getId());

        Map<Vertex, Vertex> parentMap = new HashMap<>();
        Set<Vertex> visited = new HashSet<>();
        Queue<Vertex> queue = new LinkedList<>();
        int iteration = 1;

        visited.add(start);
        queue.add(start);
        parentMap.put(start, null); // Root has no parent

        boolean found = false;
        while (!queue.isEmpty() && !found) {
            Vertex current = queue.poll();
            System.out.println("Visiting vertex: " + current.getId());

            for (Edge edge : edges) {
                Vertex neighbor = null;
                if (edge.start() == current) {
                    neighbor = edge.end();
                } else if (edge.end() == current) {
                    neighbor = edge.start();
                }

                if (neighbor != null && !visited.contains(neighbor)) {
                    System.out.println("Adding vertex to queue: " + neighbor.getId());
                    visited.add(neighbor);
                    queue.add(neighbor);
                    parentMap.put(neighbor, current);
                    if (neighbor == end) {
                        found = true;
                        break;
                    }
                }
            }

            stepsData.add(new Step(iteration, current, queue.toString()));
            iteration++;
        }

        gc.clearRect(0, 0, 800, 600);

        if (found) {
            Vertex current = end;
            while (current != null) {
                Vertex parent = parentMap.get(current);
                if (parent != null) {
                    System.out.println("Drawing edge from " + parent.getId() + " to " + current.getId());
                    gc.strokeLine(parent.getX(), parent.getY(), current.getX(), current.getY());
                }
                gc.strokeOval(current.getX() - Vertex.getRadius(),
                        current.getY() - Vertex.getRadius(),
                        Vertex.getRadius() * 2,
                        Vertex.getRadius() * 2);
                gc.strokeText(String.valueOf(current.getId()), current.getX() - 5, current.getY() + 5);
                current = parent;
            }
            stepsData.add(new Step(iteration, null, "Shortest path found"));
        } else {
            System.out.println("No path found");
            stepsData.add(new Step(iteration, null, "No path found"));
        }

        System.out.println("Shortest path complete");
        stepsData.add(new Step(iteration, null, "Shortest path complete"));
    }
}
class Vertex {
    private int id;
    private double x, y;
    private static final double RADIUS = 20;
    public Vertex(int id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    public boolean contains(double x, double y) {
        return Math.sqrt(Math.pow(this.x - x, 2) + Math.pow(this.y - y, 2)) <= RADIUS;
    }

    public void draw(GraphicsContext gc) {
        gc.strokeOval(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
        gc.strokeText(String.valueOf(id), x - 5, y + 5);
    }

    public static double getRadius() {
        return RADIUS;
    }
}

record Edge(Vertex start, Vertex end) {

    public void draw(GraphicsContext gc) {
        gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
    }
}

class Step {
    private final SimpleStringProperty iteration;
    private final SimpleStringProperty currentNode;
    private final SimpleStringProperty queueOrStack;

    public Step(int iteration, Vertex currentNode, String queueOrStack) {
        this.iteration = new SimpleStringProperty(Integer.toString(iteration));
        this.currentNode = new SimpleStringProperty(currentNode != null ? Integer.toString(currentNode.getId()) : "");
        this.queueOrStack = new SimpleStringProperty(queueOrStack);
    }

    public SimpleStringProperty iterationProperty() {
        return iteration;
    }

    public SimpleStringProperty currentNodeProperty() {
        return currentNode;
    }

    public SimpleStringProperty queueOrStackProperty() {
        return queueOrStack;
    }
}