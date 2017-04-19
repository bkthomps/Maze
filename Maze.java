/*
 * Bailey Thompson
 * Maze (1.3.2)
 * 18 April 2017
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.Integer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

/**
 * The user is first introduced to a default grid, in which a file is created using file io. The user has an option of
 * three buttons and two sliders on the bottom. When the clear button is pressed, the board is reset to only walls and
 * paths, when the generate button is pressed, a new board of specified size is created, and when the exit button is
 * pressed, the program exits. The size slider is a value between and including 2 to 30, when the generate button is
 * pressed, the size is thus reflected, if the user hovers over the slider, important information is displayed to the
 * user. The time slider is between and including 0 to 1000 — the time is in milliseconds; 0 is instant — the time
 * slider is reflected immediately after it is changed, as with the other slider this one also displays important
 * information if hovered over. The first click on the board is the start position and is in red. The second click is
 * the end position in blue. A green cell will go from start to end and change cell once per turn as specified by the
 * time slider. Once the green cell reached the end, it will display the path taken. When generate is clicked, the
 * progress in percentage is displayed next to the title of the program.
 */
class Maze {

    enum Tile {WHITE, BLACK, RED, BLUE, GREEN, NEXT, WALL}

    enum Direction {UP, DOWN, RIGHT, LEFT}

    private static final Path FILE = Paths.get("Maze.txt");

    private JFrame frame;
    private JSlider size, timing;

    private boolean firstTime;
    private boolean[][] visitedArray;
    private int xOffset, yOffset, colourMode, currentX, currentY, endX, endY, startX, startY;
    private int guiDisplay, sizeValue, time, positionCounter;
    private int[][] positionArray;
    private long tries;
    private double percentage;
    private List<Rectangle> cells;
    private Point selectedCell;
    private String saveFile;
    private String[] split;
    private Direction direction;
    private Tile[][] mazeArray;

    private boolean flag;

    public static void main(String[] args) {
        Maze maze = new Maze();
        maze.mazeCompute();
    }

    private void mazeCompute() {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.getWidth() < screenSize.getHeight()) {
            guiDisplay = (int) (screenSize.getWidth() * 0.8);
        } else {
            guiDisplay = (int) (screenSize.getHeight() * 0.8);
        }
        load();
        mazeArray = new Tile[sizeValue][sizeValue];
        positionArray = new int[sizeValue][sizeValue];
        visitedArray = new boolean[sizeValue][sizeValue];
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                final int VALUE = Integer.parseInt(split[vertical * sizeValue + horizontal + 2], 10);
                mazeArray[vertical][horizontal] = intToTile(VALUE);
            }
        }
        prepareGUI();
    }

    private void prepareGUI() {
        frame = new JFrame("Maze");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());
        frame.add(new GridPane());
        frame.pack();

        frame.setLocationRelativeTo(null);

        JPanel panel = new JPanel();

        JButton clear = new JButton("Clear");
        JButton generate = new JButton("Generate");
        JButton exit = new JButton("Exit");

        size = new JSlider(JSlider.HORIZONTAL, 2, 30, sizeValue);
        size.setPaintLabels(true);
        size.setMajorTickSpacing(4);
        size.setPreferredSize(new Dimension(150, 40));
        size.setToolTipText("Size Of The Grid");

        timing = new JSlider(JSlider.HORIZONTAL, 0, 1000, time);
        timing.setPaintLabels(true);
        timing.setMajorTickSpacing(250);
        timing.setPreferredSize(new Dimension(150, 40));
        timing.setToolTipText("Milli-seconds Between Turns");

        panel.setLayout(new FlowLayout(FlowLayout.CENTER));

        panel.add(size);
        panel.add(clear);
        panel.add(generate);
        panel.add(exit);
        panel.add(timing);

        frame.add(panel, BorderLayout.SOUTH);

        frame.setVisible(true);

        clear.addActionListener((ActionEvent e) -> {
            for (int vertical = 0; vertical < sizeValue; vertical++) {
                for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                    if (mazeArray[vertical][horizontal] != Tile.BLACK) {
                        mazeArray[vertical][horizontal] = Tile.WHITE;
                        positionArray[vertical][horizontal] = 0;
                    }
                }
            }
            firstTime = false;
            colourMode = 0;
            positionCounter = 0;
        });

        generate.addActionListener((ActionEvent e) -> {
            firstTime = false;
            percentage = 0;
            colourMode = 0;
            positionCounter = 0;
            tries = 0;
            sizeValue = size.getValue();
            mazeArray = new Tile[sizeValue][sizeValue];
            positionArray = new int[sizeValue][sizeValue];
            visitedArray = new boolean[sizeValue][sizeValue];
            cells = new ArrayList<>(sizeValue * sizeValue);
            randomize();
            save();
        });

        exit.addActionListener((ActionEvent e) -> System.exit(0));
    }

    private void randomize() {
        int white, black;
        do {
            initializeRandomize();
            white = 0;
            black = 0;
            for (int vertical = 0; vertical < sizeValue; vertical++) {
                for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                    if (mazeArray[vertical][horizontal] == Tile.WHITE) {
                        white++;
                    } else if (mazeArray[vertical][horizontal] == Tile.BLACK) {
                        black++;
                    }
                }
            }
        } while (black / white > 0.5);
        frame.setTitle("Maze");
    }

    private void initializeRandomize() {
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                mazeArray[vertical][horizontal] = Tile.BLACK;
                visitedArray[vertical][horizontal] = false;
            }
        }
        final int randomOne = (int) (Math.random() * sizeValue);
        final int randomTwo = (int) (Math.random() * sizeValue);
        mazeArray[randomOne][randomTwo] = Tile.WHITE;
        visitedArray[randomOne][randomTwo] = true;
        if (randomOne > 0) {
            mazeArray[randomOne - 1][randomTwo] = Tile.WALL;
        }
        if (randomOne < sizeValue - 1) {
            mazeArray[randomOne + 1][randomTwo] = Tile.WALL;
        }
        if (randomTwo > 0) {
            mazeArray[randomOne][randomTwo - 1] = Tile.WALL;
        }
        if (randomTwo < sizeValue - 1) {
            mazeArray[randomOne][randomTwo + 1] = Tile.WALL;
        }
        randomGenerator();
    }

    private void randomGenerator() {
        boolean skip = false, allDone = true;
        int wallCells = 0;
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                if (mazeArray[vertical][horizontal] == Tile.WALL) {
                    wallCells++;
                }
            }
        }
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                int randomPass = (int) (Math.random() * (wallCells + 1));
                if (!skip && mazeArray[vertical][horizontal] == Tile.WALL && randomPass == 0) {
                    int neighbours = 0;
                    if (vertical > 0 && mazeArray[vertical - 1][horizontal] == Tile.WHITE) {
                        neighbours++;
                    }
                    if (vertical < sizeValue - 1 && mazeArray[vertical + 1][horizontal] == Tile.WHITE) {
                        neighbours++;
                    }
                    if (horizontal > 0 && mazeArray[vertical][horizontal - 1] == Tile.WHITE) {
                        neighbours++;
                    }
                    if (horizontal < sizeValue - 1 && mazeArray[vertical][horizontal + 1] == Tile.WHITE) {
                        neighbours++;
                    }
                    mazeArray[vertical][horizontal] = (neighbours == 1) ? (Tile.WHITE) : (Tile.BLACK);
                    if (vertical > 0 && !visitedArray[vertical - 1][horizontal]) {
                        mazeArray[vertical - 1][horizontal] = Tile.WALL;
                    }
                    if (vertical < sizeValue - 1 && !visitedArray[vertical + 1][horizontal]) {
                        mazeArray[vertical + 1][horizontal] = Tile.WALL;
                    }
                    if (horizontal > 0 && !visitedArray[vertical][horizontal - 1]) {
                        mazeArray[vertical][horizontal - 1] = Tile.WALL;
                    }
                    if (horizontal < sizeValue - 1 && !visitedArray[vertical][horizontal + 1]) {
                        mazeArray[vertical][horizontal + 1] = Tile.WALL;
                    }
                    skip = true;
                    visitedArray[vertical][horizontal] = true;
                }
            }
        }
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                if (!visitedArray[vertical][horizontal]) {
                    allDone = false;
                }
            }
        }
        /*
         * Setting the loading percentage using an algorithm.
         * From 0% to 80% it is normal speed.
         * From 80% to 90% it is half speed.
         * From 90% to 95% it is a quarter speed.
         * From 95% to 99% it is one eight speed.
         * It never goes above 99%.
         */
        if (!allDone) {
            tries++;
            if (percentage != 99) {
                percentage = (100 * tries) / (Math.pow(48, (2 * sizeValue - 10) * 0.05 + 1));
                if (percentage > 80) {
                    percentage = (percentage - 80) / 2 + 80;
                    if (percentage > 90) {
                        percentage = (percentage - 90) / 4 + 90;
                        if (percentage > 95) {
                            percentage = (percentage - 95) / 8 + 95;
                            if (percentage > 99) {
                                percentage = 99;
                            }
                        }
                    }
                }
                frame.setTitle("Maze (" + (int) percentage + "% Done loading)");
            }
            randomGenerator();
        }
    }

    private void startSolver() {
        while (Math.abs(endX - currentX) + Math.abs(endY - currentY) != 1) {
            flag = false;
            solvingLogic();
            mazeArray[currentY][currentX] = Tile.NEXT;
            if (mazeArray[startY][startX] != Tile.RED) {
                mazeArray[startY][startX] = Tile.RED;
            }
        }
        if (!firstTime) {
            switch (direction) {
                case UP:
                    direction = Direction.DOWN;
                    break;
                case DOWN:
                    direction = Direction.UP;
                    break;
                case LEFT:
                    direction = Direction.RIGHT;
                    break;
                case RIGHT:
                    direction = Direction.LEFT;
                    break;
            }
            currentX = endX;
            currentY = endY;
            makePath();
            firstTime = true;
        }
    }

    private void makePath() {
        if (Math.abs(startX - currentX) + Math.abs(startY - currentY) != 1) {
            flag = true;
            solvingLogic();
            mazeArray[currentY][currentX] = Tile.GREEN;
            positionCounter++;
            positionArray[currentY][currentX] = positionCounter;
            makePath();
        }
    }

    private void solvingLogic() {
        if (currentX > 0 && currentX < sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                    computeDirection(Direction.DOWN, Direction.RIGHT, Direction.UP, Direction.LEFT);
                    break;
                case LEFT:
                    computeDirection(Direction.UP, Direction.LEFT, Direction.DOWN, Direction.RIGHT);
                    break;
                case DOWN:
                    computeDirection(Direction.LEFT, Direction.DOWN, Direction.RIGHT, Direction.UP);
                    break;
                case UP:
                    computeDirection(Direction.RIGHT, Direction.UP, Direction.LEFT, Direction.DOWN);
                    break;
            }
        } else if (currentX == 0 && currentY > 0 && currentY < sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                case DOWN:
                    computeDirection(Direction.DOWN, Direction.RIGHT, Direction.UP, null);
                    break;
                case LEFT:
                    computeDirection(Direction.UP, Direction.DOWN, Direction.RIGHT, null);
                    break;
                case UP:
                    computeDirection(Direction.RIGHT, Direction.UP, Direction.DOWN, null);
                    break;
            }
        } else if (currentX == sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                    computeDirection(Direction.DOWN, Direction.UP, Direction.LEFT, null);
                    break;
                case LEFT:
                case UP:
                    computeDirection(Direction.UP, Direction.LEFT, Direction.DOWN, null);
                    break;
                case DOWN:
                    computeDirection(Direction.LEFT, Direction.DOWN, Direction.UP, null);
                    break;
            }
        } else if (currentY == 0 && currentX > 0 && currentX < sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                    computeDirection(Direction.DOWN, Direction.RIGHT, Direction.LEFT, null);
                    break;
                case LEFT:
                case DOWN:
                    computeDirection(Direction.LEFT, Direction.DOWN, Direction.RIGHT, null);
                    break;
                case UP:
                    computeDirection(Direction.RIGHT, Direction.LEFT, Direction.DOWN, null);
                    break;
            }
        } else if (currentY == sizeValue - 1 && currentX > 0 && currentX < sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                case UP:
                    computeDirection(Direction.RIGHT, Direction.UP, Direction.LEFT, null);
                    break;
                case LEFT:
                    computeDirection(Direction.UP, Direction.LEFT, Direction.RIGHT, null);
                    break;
                case DOWN:
                    computeDirection(Direction.LEFT, Direction.RIGHT, Direction.UP, null);
                    break;
            }
        } else if (currentX == 0 && currentY == 0) {
            switch (direction) {
                case RIGHT:
                case LEFT:
                case DOWN:
                    computeDirection(Direction.DOWN, Direction.RIGHT, null, null);
                    break;
                case UP:
                    computeDirection(Direction.RIGHT, Direction.DOWN, null, null);
                    break;
            }
        } else if (currentX == 0 && currentY == sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                case DOWN:
                case UP:
                    computeDirection(Direction.RIGHT, Direction.UP, null, null);
                    break;
                case LEFT:
                    computeDirection(Direction.UP, Direction.RIGHT, null, null);
                    break;
            }
        } else if (currentX == sizeValue - 1 && currentY == 0) {
            switch (direction) {
                case RIGHT:
                    computeDirection(Direction.DOWN, Direction.LEFT, null, null);
                    break;
                case LEFT:
                case DOWN:
                case UP:
                    computeDirection(Direction.LEFT, Direction.DOWN, null, null);
                    break;
            }
        } else if (currentX == sizeValue - 1 && currentY == sizeValue - 1) {
            switch (direction) {
                case RIGHT:
                case LEFT:
                case UP:
                    computeDirection(Direction.UP, Direction.LEFT, null, null);
                    break;
                case DOWN:
                    computeDirection(Direction.LEFT, Direction.UP, null, null);
                    break;
            }
        }
    }

    private void computeDirection(Direction one, Direction two, Direction three, Direction four) {
        if (flag) {
            if (mazeAtDirection(one) == Tile.NEXT) {
                computeCurrentDirection(one);
            } else if (mazeAtDirection(two) == Tile.NEXT) {
                computeCurrentDirection(two);
            } else if (three != null && mazeAtDirection(three) == Tile.NEXT) {
                computeCurrentDirection(three);
            } else if (four != null && mazeAtDirection(four) == Tile.NEXT) {
                computeCurrentDirection(four);
            }
        } else {
            if (mazeAtDirection(one) != Tile.BLACK) {
                computeCurrentDirection(one);
            } else if (mazeAtDirection(two) != Tile.BLACK) {
                computeCurrentDirection(two);
            } else if (three != null && mazeAtDirection(three) != Tile.BLACK) {
                computeCurrentDirection(three);
            } else if (four != null && mazeAtDirection(four) != Tile.BLACK) {
                computeCurrentDirection(four);
            }
        }
    }

    private Tile mazeAtDirection(Direction input) {
        switch (input) {
            case UP:
                return mazeArray[currentY - 1][currentX];
            case DOWN:
                return mazeArray[currentY + 1][currentX];
            case RIGHT:
                return mazeArray[currentY][currentX + 1];
            case LEFT:
                return mazeArray[currentY][currentX - 1];
            default:
                System.err.println("Error in Maze.mazeAtDirection: invalid direction.");
                return mazeArray[currentY][currentX - 1];
        }
    }

    private void computeCurrentDirection(Direction input) {
        if (input == Direction.UP) {
            currentY--;
            direction = Direction.UP;
        } else if (input == Direction.DOWN) {
            currentY++;
            direction = Direction.DOWN;
        } else if (input == Direction.RIGHT) {
            currentX++;
            direction = Direction.RIGHT;
        } else if (input == Direction.LEFT) {
            currentX--;
            direction = Direction.LEFT;
        }
    }

    private class GridPane extends JPanel {

        GridPane() {
            cells = new ArrayList<>(sizeValue * sizeValue);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (colourMode == 0 || colourMode == 1) {
                        final int horizontalClickPosition = (e.getX() - xOffset) / (getWidth() / sizeValue);
                        final int verticalClickPosition = (e.getY() - yOffset) / (getHeight() / sizeValue);
                        if (verticalClickPosition >= 0 && verticalClickPosition <= sizeValue - 1
                                && horizontalClickPosition >= 0 && horizontalClickPosition <= sizeValue - 1) {
                            if (mazeArray[verticalClickPosition][horizontalClickPosition] == Tile.WHITE) {
                                if (colourMode == 0) {
                                    startY = verticalClickPosition;
                                    currentY = verticalClickPosition;
                                    startX = horizontalClickPosition;
                                    currentX = horizontalClickPosition;
                                    if (currentX > 0 && currentX < sizeValue - 1 && currentY > 0
                                            && currentY < sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == Tile.WHITE) {
                                            direction = Direction.RIGHT;
                                        } else if (mazeArray[currentY + 1][currentX] == Tile.WHITE) {
                                            direction = Direction.DOWN;
                                        } else if (mazeArray[currentY][currentX - 1] == Tile.WHITE) {
                                            direction = Direction.LEFT;
                                        } else if (mazeArray[currentY - 1][currentX] == Tile.WHITE) {
                                            direction = Direction.UP;
                                        }
                                    } else if (currentX == 0 && currentY > 0 && currentY < sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == Tile.WHITE) {
                                            direction = Direction.RIGHT;
                                        } else if (mazeArray[currentY + 1][currentX] == Tile.WHITE) {
                                            direction = Direction.DOWN;
                                        } else if (mazeArray[currentY - 1][currentX] == Tile.WHITE) {
                                            direction = Direction.UP;
                                        }
                                    } else if (currentX == sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
                                        if (mazeArray[currentY + 1][currentX] == Tile.WHITE) {
                                            direction = Direction.DOWN;
                                        } else if (mazeArray[currentY][currentX - 1] == Tile.WHITE) {
                                            direction = Direction.LEFT;
                                        } else if (mazeArray[currentY - 1][currentX] == Tile.WHITE) {
                                            direction = Direction.UP;
                                        }
                                    } else if (currentX > 0 && currentX < sizeValue - 1 && currentY == 0) {
                                        if (mazeArray[currentY][currentX + 1] == Tile.WHITE) {
                                            direction = Direction.RIGHT;
                                        } else if (mazeArray[currentY + 1][currentX] == Tile.WHITE) {
                                            direction = Direction.DOWN;
                                        } else if (mazeArray[currentY][currentX - 1] == Tile.WHITE) {
                                            direction = Direction.LEFT;
                                        }
                                    } else if (currentX > 0 && currentX < sizeValue - 1 && currentY == sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == Tile.WHITE) {
                                            direction = Direction.RIGHT;
                                        } else if (mazeArray[currentY][currentX - 1] == Tile.WHITE) {
                                            direction = Direction.LEFT;
                                        } else if (mazeArray[currentY - 1][currentX] == Tile.WHITE) {
                                            direction = Direction.UP;
                                        }
                                    } else if (currentX == 0 && currentY == 0) {
                                        if (mazeArray[currentY][currentX + 1] == Tile.WHITE) {
                                            direction = Direction.RIGHT;
                                        } else if (mazeArray[currentY + 1][currentX] == Tile.WHITE) {
                                            direction = Direction.DOWN;
                                        }
                                    } else if (currentX == 0 && currentY == sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == Tile.WHITE) {
                                            direction = Direction.RIGHT;
                                        } else if (mazeArray[currentY - 1][currentX] == Tile.WHITE) {
                                            direction = Direction.UP;
                                        }
                                    } else if (currentX == sizeValue - 1 && currentY == 0) {
                                        if (mazeArray[currentY + 1][currentX] == Tile.WHITE) {
                                            direction = Direction.DOWN;
                                        } else if (mazeArray[currentY][currentX - 1] == Tile.WHITE) {
                                            direction = Direction.LEFT;
                                        }
                                    } else if (currentX == sizeValue - 1 && currentY == sizeValue - 1) {
                                        if (mazeArray[currentY][currentX - 1] == Tile.WHITE) {
                                            direction = Direction.LEFT;
                                        } else if (mazeArray[currentY - 1][currentX] == Tile.WHITE) {
                                            direction = Direction.UP;
                                        }
                                    }
                                    mazeArray[verticalClickPosition][horizontalClickPosition] = Tile.RED;
                                    colourMode++;
                                } else if (colourMode == 1) {
                                    endY = verticalClickPosition;
                                    endX = horizontalClickPosition;
                                    mazeArray[verticalClickPosition][horizontalClickPosition] = Tile.BLUE;
                                    colourMode++;
                                    startSolver();
                                }
                            }
                        }
                    }
                }
            });
            MouseAdapter mouseHandler;
            mouseHandler = new MouseAdapter() {

                @Override
                public void mouseMoved(MouseEvent e) {
                    int width = getWidth();
                    int height = getHeight();
                    int cellWidth = width / sizeValue;
                    int cellHeight = height / sizeValue;
                    selectedCell = null;
                    if (e.getX() >= xOffset && e.getY() >= yOffset) {
                        int column = (e.getX() - xOffset) / cellWidth;
                        int row = (e.getY() - yOffset) / cellHeight;
                        if (column >= 0 && row >= 0 && column < sizeValue && row < sizeValue) {
                            selectedCell = new Point(column, row);
                        }
                    }
                    repaint();
                }
            };
            addMouseMotionListener(mouseHandler);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(guiDisplay, guiDisplay + 49);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2d = (Graphics2D) g.create();
            final int WIDTH = getWidth();
            final int HEIGHT = getHeight();
            final int cellWidth = WIDTH / sizeValue;
            final int cellHeight = HEIGHT / sizeValue;
            xOffset = (WIDTH - (sizeValue * cellWidth)) / 2;
            yOffset = (HEIGHT - (sizeValue * cellHeight)) / 2;
            if (cells.isEmpty()) {
                for (int row = 0; row < sizeValue; row++) {
                    for (int col = 0; col < sizeValue; col++) {
                        Rectangle cell = new Rectangle(
                                xOffset + (col * cellWidth),
                                yOffset + (row * cellHeight),
                                cellWidth,
                                cellHeight);
                        cells.add(cell);
                    }
                }
            }

            if (selectedCell != null && (colourMode == 0 || colourMode == 1)) {
                if (selectedCell.x + (selectedCell.y * sizeValue) <= sizeValue * sizeValue) {
                    int index = selectedCell.x + (selectedCell.y * sizeValue);
                    Rectangle cell = cells.get(index);
                    if (colourMode == 0) {
                        g2d.setColor(Color.RED);
                    } else if (colourMode == 1) {
                        g2d.setColor(Color.BLUE);
                    }
                    g2d.fill(cell);
                }
            }

            g2d.setColor(Color.GRAY);
            cells.forEach(g2d::draw);

            int tempTime = time;
            time = timing.getValue();
            if (tempTime != time) {
                save();
            }

            boolean cancel = false;
            for (int vertical = 0; vertical < sizeValue; vertical++) {
                for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                    Rectangle cell = cells.get(horizontal + vertical * sizeValue);
                    switch (mazeArray[vertical][horizontal]) {
                        case BLACK:
                            g2d.setColor(Color.BLACK);
                            g2d.fill(cell);
                            break;
                        case RED:
                            g2d.setColor(Color.RED);
                            g2d.fill(cell);
                            break;
                        case BLUE:
                            g2d.setColor(Color.BLUE);
                            g2d.fill(cell);
                            break;
                        case GREEN:
                            g2d.setColor(Color.GREEN);
                            if (positionArray[vertical][horizontal] == positionCounter && !cancel) {
                                for (int vertical2 = 0; vertical2 < sizeValue; vertical2++) {
                                    for (int horizontal2 = 0; horizontal2 < sizeValue; horizontal2++) {
                                        if (positionArray[vertical2][horizontal2] >= positionCounter) {
                                            g2d.fill(cells.get(horizontal2 + vertical2 * sizeValue));
                                        }
                                    }
                                }
                                try {
                                    Thread.sleep(time);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(Maze.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                positionCounter--;
                                cancel = true;
                            } else if (positionCounter == 0) {
                                g2d.fill(cell);
                            }
                            break;
                    }
                    repaint();
                }
            }
        }
    }

    private void load() {
        try {
            Files.createFile(FILE);
        } catch (FileAlreadyExistsException x) {
            try (InputStream in = Files.newInputStream(FILE);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    saveFile = line;
                }
            } catch (IOException y) {
                System.err.println("Error in Maze.load: could not open file.");
            }
        } catch (IOException x) {
            System.err.println("Error in Maze.load: could not create file.");
        }
        if (saveFile == null) {
            saveFile = "10 100 0 0 0 0 0 1 0 0 0 0 1 1 0 1 0 1 0 1 1 0 1 1 0 1 0 1 0 1 1 0 1 0 1 0 0 0 1 1 0 0 0 0 0 "
                    + "1 1 0 0 1 1 0 0 1 0 1 1 0 1 1 0 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1 0 1 0 0 0 1 0 0 0 1 1 1 1 1 0 "
                    + "0 0 1 0 0 0 1 0 0 0";
        }
        split = saveFile.split("\\s+");
        sizeValue = Integer.parseInt(split[0], 10);
        time = Integer.parseInt(split[1], 10);
    }

    private void save() {
        saveFile = sizeValue + " " + time;
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                saveFile += " ";
                if (mazeArray[vertical][horizontal] == Tile.WHITE || mazeArray[vertical][horizontal] == Tile.BLACK) {
                    saveFile += tileToInt(mazeArray[vertical][horizontal]);
                } else {
                    saveFile += 0;
                }
            }
        }
        byte data[] = saveFile.getBytes();
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(FILE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))) {
            out.write(data, 0, data.length);
        } catch (IOException x) {
            System.err.println("Error in Maze.save: could not open or write to file.");
        }
    }

    private int tileToInt(Tile input) {
        switch (input) {
            case WHITE:
                return 0;
            case BLACK:
                return 1;
            case RED:
                return 2;
            case BLUE:
                return 3;
            case GREEN:
                return 4;
            case NEXT:
                return 5;
            case WALL:
                return 10;
            default:
                System.err.println("Error in Maze.tileToInt: hit default.");
                return 10;
        }
    }

    private Tile intToTile(int input) {
        switch (input) {
            case 0:
                return Tile.WHITE;
            case 1:
                return Tile.BLACK;
            case 2:
                return Tile.RED;
            case 3:
                return Tile.BLUE;
            case 4:
                return Tile.GREEN;
            case 5:
                return Tile.NEXT;
            case 10:
                return Tile.WALL;
            default:
                System.err.println("Error in Maze.intToTile: hit default.");
                return Tile.WALL;
        }
    }
}
