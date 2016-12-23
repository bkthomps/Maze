/**
 ***********************************************************************************************************************
 * Bailey Thompson
 * Maze (1.1.3)
 * 22 December 2016
 * Info: The  user  is  first  introduced  to  a default grid, in which a file is created using file io. The user has an
 * Info: option  of three buttons and two sliders on the bottom. When the clear button is pressed, the board is reset to
 * Info: only  walls  and paths, when the generate button is pressed, a new board of specified size is created, and when
 * Info: the  exit  button is pressed, the program exits. The size slider is a value between and including 2 to 30, when
 * Info: the  generate  button  is  pressed,  the  size is thus reflected, if the user hovers over the slider, important
 * Info: information  is  displayed  to  the  user. The time slider is between and including 0 to 1000 -- the time is in
 * Info: milliseconds;  0  is instant -- the time slider is reflected immediately after it is changed, as with the other
 * Info: slider  this one also displays important information if hovered over. The first click on the board is the start
 * Info: position  and  is  in red. The second click is the end position in blue. A green cell will go from start to end
 * Info: and  change  cell  once  per turn as specified by the time slider. Once the green cell reached the end, it will
 * Info: display  the path taken. When generate is clicked, the progress in percentage is displayed next to the title of
 * Info: the program.
 ***********************************************************************************************************************
 */
package maze;

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
import static java.lang.Integer.parseInt;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class Maze {

    private static final Path FILE = Paths.get("RecursiveMazeSolver.txt");
    private JFrame frame;
    private JSlider sizeSlider, timingSlider;
    private JPanel panel;
    private JButton btnClear, btnGenerate, btnExit;
    private List<Rectangle> cells;
    private Point selectedCell;
    boolean firstTime;
    boolean[][] visitedArray;
    int xOffset, yOffset, colourMode, currentX, currentY, endX, endY, startX, startY;
    int guiDisplay, sizeValue, time, positionCounter;
    int[][] mazeArray, positionArray;
    long tries;
    double percentage;
    char direction;
    String saveFile;
    String[] split;

    public static void main(String[] args) {
        Maze Maze = new Maze();
        Maze.Maze();
    }

    private void Maze() {
        //checking the monitor dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        //setting the gui size
        if (screenSize.getWidth() < screenSize.getHeight()) {
            guiDisplay = (int) (screenSize.getWidth() * 0.8);
        } else {
            guiDisplay = (int) (screenSize.getHeight() * 0.8);
        }
        load();
        mazeArray = new int[sizeValue][sizeValue];
        positionArray = new int[sizeValue][sizeValue];
        visitedArray = new boolean[sizeValue][sizeValue];
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                mazeArray[vertical][horizontal] = parseInt(split[vertical * sizeValue + horizontal + 2], 10);
            }
        }
        prepareGUI();
    }

    private void prepareGUI() {
        frame = new JFrame("Maze");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLayout(new BorderLayout());
        frame.add(new GridPane());
        frame.pack();

        frame.setLocationRelativeTo(null);

        panel = new JPanel();

        btnClear = new JButton("Clear");
        btnGenerate = new JButton("Generate");
        btnExit = new JButton("Exit");

        sizeSlider = new JSlider(JSlider.HORIZONTAL, 2, 30, sizeValue);
        sizeSlider.setPaintLabels(true);
        sizeSlider.setMajorTickSpacing(4);
        sizeSlider.setPreferredSize(new Dimension(150, 40));
        sizeSlider.setToolTipText("Size Of The Grid");

        timingSlider = new JSlider(JSlider.HORIZONTAL, 0, 1000, time);
        timingSlider.setPaintLabels(true);
        timingSlider.setMajorTickSpacing(250);
        timingSlider.setPreferredSize(new Dimension(150, 40));
        timingSlider.setToolTipText("Miliseconds Between Turns");

        //setting the layout of both rows of buttons
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));

        //setting upper row of buttons to variable panel
        panel.add(sizeSlider);
        panel.add(btnClear);
        panel.add(btnGenerate);
        panel.add(btnExit);
        panel.add(timingSlider);

        frame.add(panel, BorderLayout.SOUTH);

        frame.setVisible(true);

        btnClear.addActionListener((ActionEvent e) -> {
            for (int vertical = 0; vertical < sizeValue; vertical++) {
                for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                    if (mazeArray[vertical][horizontal] != 1) {
                        mazeArray[vertical][horizontal] = 0;
                        positionArray[vertical][horizontal] = 0;
                    }
                }
            }
            firstTime = false;
            colourMode = 0;
            positionCounter = 0;
        });

        btnGenerate.addActionListener((ActionEvent e) -> {
            firstTime = false;
            percentage = 0;
            colourMode = 0;
            positionCounter = 0;
            tries = 0;
            sizeValue = sizeSlider.getValue();
            mazeArray = new int[sizeValue][sizeValue];
            positionArray = new int[sizeValue][sizeValue];
            visitedArray = new boolean[sizeValue][sizeValue];
            cells = new ArrayList<>(sizeValue * sizeValue);
            randomize();
            save();
        });

        btnExit.addActionListener((ActionEvent e) -> {
            System.exit(0);
        });
    }

    private void randomize() {
        int white, black;
        //loop executed then executed again if too many black tiles
        do {
            initializerandomize();
            white = 0;
            black = 0;
            //using 2d array to check every single cells
            for (int vertical = 0; vertical < sizeValue; vertical++) {
                for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                    //adding one to white if there is a white cell present
                    if (mazeArray[vertical][horizontal] == 0) {
                        white++;
                        //adding one to black if there is a black cell present
                    } else if (mazeArray[vertical][horizontal] == 1) {
                        black++;
                    }
                }
            }
        } while (black / white > 0.5);
        //displaying the title of the program
        frame.setTitle("Maze");
    }

    private void initializerandomize() {
        //setting every cell to wall and unvisited
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                mazeArray[vertical][horizontal] = 1;
                visitedArray[vertical][horizontal] = false;
            }
        }
        int randomOne = (int) (Math.random() * sizeValue);
        int randomTwo = (int) (Math.random() * sizeValue);
        //setting the start tile to path and visited
        mazeArray[randomOne][randomTwo] = 0;
        visitedArray[randomOne][randomTwo] = true;
        //creating various temporary tiles around the seed tile
        if (randomOne > 0) {
            mazeArray[randomOne - 1][randomTwo] = 10;
        }
        if (randomOne < sizeValue - 1) {
            mazeArray[randomOne + 1][randomTwo] = 10;
        }
        if (randomTwo > 0) {
            mazeArray[randomOne][randomTwo - 1] = 10;
        }
        if (randomTwo < sizeValue - 1) {
            mazeArray[randomOne][randomTwo + 1] = 10;
        }
        randomGenerator();
    }

    private void randomGenerator() {
        boolean skip = false, allDone = true;
        int wallCells = 0;
        //2d array used for setting the amount of wall cells to a variable
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                if (mazeArray[vertical][horizontal] == 10) {
                    wallCells++;
                }
            }
        }
        //2d array used for creating path cells
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                //random generation used for detemining if cell should be picked
                int randomPass = (int) (Math.random() * (wallCells + 1));
                //only uses cell if random generation picks cell and if cells this turn has not 
                //already been picked and if the cell is actually a temp cell
                if (!skip && mazeArray[vertical][horizontal] == 10 && randomPass == 0) {
                    //declaring and setting variable to zero
                    int neighbours = 0;
                    //checking all four sides of cell and reporting amount fo neighbours
                    if (vertical > 0) {
                        if (mazeArray[vertical - 1][horizontal] == 0) {
                            neighbours++;
                        }
                    }
                    if (vertical < sizeValue - 1) {
                        if (mazeArray[vertical + 1][horizontal] == 0) {
                            neighbours++;
                        }
                    }
                    if (horizontal > 0) {
                        if (mazeArray[vertical][horizontal - 1] == 0) {
                            neighbours++;
                        }
                    }
                    if (horizontal < sizeValue - 1) {
                        if (mazeArray[vertical][horizontal + 1] == 0) {
                            neighbours++;
                        }
                    }
                    //setting if cells is full or if it empty depending on amount of neighbours
                    if (neighbours == 1) {
                        mazeArray[vertical][horizontal] = 0;
                    } else {
                        mazeArray[vertical][horizontal] = 1;
                    }
                    //setting the temp cells around the cell
                    if (vertical > 0) {
                        if (!visitedArray[vertical - 1][horizontal]) {
                            mazeArray[vertical - 1][horizontal] = 10;
                        }
                    }
                    if (vertical < sizeValue - 1) {
                        if (!visitedArray[vertical + 1][horizontal]) {
                            mazeArray[vertical + 1][horizontal] = 10;
                        }
                    }
                    if (horizontal > 0) {
                        if (!visitedArray[vertical][horizontal - 1]) {
                            mazeArray[vertical][horizontal - 1] = 10;
                        }
                    }
                    if (horizontal < sizeValue - 1) {
                        if (!visitedArray[vertical][horizontal + 1]) {
                            mazeArray[vertical][horizontal + 1] = 10;
                        }
                    }
                    //setting skip to true so that this round only one cell is created
                    skip = true;
                    //setting the cell to visited
                    visitedArray[vertical][horizontal] = true;
                }
            }
        }
        //determining if the maze is completed
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                if (!visitedArray[vertical][horizontal]) {
                    allDone = false;
                }
            }
        }
        //setting the percentage to the user using an algorithm
        //from 0% to 80% it is normal speed
        //from 80% to 90% it is half speed
        //from 90% to 95% it is a quarter speed
        //from 95% to 99% it is one eight speed
        //it never goes above 99%
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
                //displaying for the user to wait and showing percentage
                frame.setTitle("Maze (" + (int) percentage + "% Done loading)");
            }
            randomGenerator();
        }
    }

    private void startSolver() {
        //only do such if the maze is not solved
        if (Math.abs(endX - currentX) + Math.abs(endY - currentY) != 1) {
            //case for if in the middle of the board and not corners or sides
            if (currentX > 0 && currentX < sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //case for if on side of board
            } else if (currentX == 0 && currentY > 0 && currentY < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //case for if on side of board
            } else if (currentX == sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //case for if on side of board
            } else if (currentY == 0 && currentX > 0 && currentX < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //case for if on side of board
            } else if (currentY == sizeValue - 1 && currentX > 0 && currentX < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                }
                //case for if in corner of board
            } else if (currentX == 0 && currentY == 0) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //case for if in corner of board
            } else if (currentX == 0 && currentY == sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] != 1) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                }
                //case for if in corner of board
            } else if (currentX == sizeValue - 1 && currentY == 0) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] != 1) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //case for if in corner of board
            } else if (currentX == sizeValue - 1 && currentY == sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY - 1][currentX] != 1) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] != 1) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                }
            }
            mazeArray[currentY][currentX] = 5;
            //if the start point is no longer marked, re-mark it
            if (mazeArray[startY][startX] != 2) {
                mazeArray[startY][startX] = 2;
            }
            startSolver();
        }
        //only execute once
        if (!firstTime) {
            //changes the direction so that it can find most direct path
            switch (direction) {
                case 'u':
                    direction = 'd';
                    break;
                case 'd':
                    direction = 'u';
                    break;
                case 'l':
                    direction = 'r';
                    break;
                case 'r':
                    direction = 'l';
                    break;
            }
            currentX = endX;
            currentY = endY;
            makePath();
            //making it so this is not executed again
            firstTime = true;
        }
    }

    private void makePath() {
        //only do such if shortest path is not yet found
        if (Math.abs(startX - currentX) + Math.abs(startY - currentY) != 1) {
            //situation for when cell is in middle of board and not corner or side
            if (currentX > 0 && currentX < sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //situation for when cell is on side of board
            } else if (currentX == 0 && currentY > 0 && currentY < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //situation for when cell is on side of board
            } else if (currentX == sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //situation for when cell is on side of board
            } else if (currentY == 0 && currentX > 0 && currentX < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //situation for when cell is on side of board
            } else if (currentY == sizeValue - 1 && currentX > 0 && currentX < sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                }
                //situation for when cell is on corner of board
            } else if (currentX == 0 && currentY == 0) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //situation for when cell is on corner of board
            } else if (currentX == 0 && currentY == sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX + 1] == 5) { //going right
                            currentX++;
                            direction = 'r';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                }
                //situation for when cell is on corner of board
            } else if (currentX == sizeValue - 1 && currentY == 0) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY + 1][currentX] == 5) { //going down
                            currentY++;
                            direction = 'd';
                        }
                        break;
                }
                //situation for when cell is on corner of board
            } else if (currentX == sizeValue - 1 && currentY == sizeValue - 1) {
                switch (direction) {
                    case 'r':
                        //know they were going right
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'l':
                        //know they were going left
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                    case 'd':
                        //know they were going down
                        if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        } else if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        }
                        break;
                    case 'u':
                        //know they were going up
                        if (mazeArray[currentY - 1][currentX] == 5) { //going up
                            currentY--;
                            direction = 'u';
                        } else if (mazeArray[currentY][currentX - 1] == 5) { //going left
                            currentX--;
                            direction = 'l';
                        }
                        break;
                }
            }
            mazeArray[currentY][currentX] = 4;
            positionCounter++;
            positionArray[currentY][currentX] = positionCounter;
            makePath();
        }
    }

    //declaring class used for the grid gui
    public class GridPane extends JPanel {

        //declaring public used for mouse events
        public GridPane() {
            //declaring an array list used for grid gui
            cells = new ArrayList<>(sizeValue * sizeValue);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (colourMode == 0 || colourMode == 1) {
                        //declaring and setting x and y variables
                        int horizontalClickPosition = (e.getX() - xOffset) / (getWidth() / sizeValue);
                        int verticalClickPosition = (e.getY() - yOffset) / (getHeight() / sizeValue);
                        //do only if cell in area is a path tile
                        if (verticalClickPosition >= 0 && verticalClickPosition <= sizeValue - 1
                                && horizontalClickPosition >= 0 && horizontalClickPosition <= sizeValue - 1) {
                            if (mazeArray[verticalClickPosition][horizontalClickPosition] == 0) {
                                if (colourMode == 0) {
                                    //setting the start y coordinate
                                    startY = verticalClickPosition;
                                    currentY = verticalClickPosition;
                                    //setting the start x coordinate
                                    startX = horizontalClickPosition;
                                    currentX = horizontalClickPosition;
                                    //setting starting direction in maze
                                    if (currentX > 0 && currentX < sizeValue - 1 && currentY > 0
                                            && currentY < sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == 0) {
                                            direction = 'r';
                                        } else if (mazeArray[currentY + 1][currentX] == 0) {
                                            direction = 'd';
                                        } else if (mazeArray[currentY][currentX - 1] == 0) {
                                            direction = 'l';
                                        } else if (mazeArray[currentY - 1][currentX] == 0) {
                                            direction = 'u';
                                        }
                                    } else if (currentX == 0 && currentY > 0 && currentY < sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == 0) {
                                            direction = 'r';
                                        } else if (mazeArray[currentY + 1][currentX] == 0) {
                                            direction = 'd';
                                        } else if (mazeArray[currentY - 1][currentX] == 0) {
                                            direction = 'u';
                                        }
                                    } else if (currentX == sizeValue - 1 && currentY > 0 && currentY < sizeValue - 1) {
                                        if (mazeArray[currentY + 1][currentX] == 0) {
                                            direction = 'd';
                                        } else if (mazeArray[currentY][currentX - 1] == 0) {
                                            direction = 'l';
                                        } else if (mazeArray[currentY - 1][currentX] == 0) {
                                            direction = 'u';
                                        }
                                    } else if (currentX > 0 && currentX < sizeValue - 1 && currentY == 0) {
                                        if (mazeArray[currentY][currentX + 1] == 0) {
                                            direction = 'r';
                                        } else if (mazeArray[currentY + 1][currentX] == 0) {
                                            direction = 'd';
                                        } else if (mazeArray[currentY][currentX - 1] == 0) {
                                            direction = 'l';
                                        }
                                    } else if (currentX > 0 && currentX < sizeValue - 1 && currentY == sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == 0) {
                                            direction = 'r';
                                        } else if (mazeArray[currentY][currentX - 1] == 0) {
                                            direction = 'l';
                                        } else if (mazeArray[currentY - 1][currentX] == 0) {
                                            direction = 'u';
                                        }
                                    } else if (currentX == 0 && currentY == 0) {
                                        if (mazeArray[currentY][currentX + 1] == 0) {
                                            direction = 'r';
                                        } else if (mazeArray[currentY + 1][currentX] == 0) {
                                            direction = 'd';
                                        }
                                    } else if (currentX == 0 && currentY == sizeValue - 1) {
                                        if (mazeArray[currentY][currentX + 1] == 0) {
                                            direction = 'r';
                                        } else if (mazeArray[currentY - 1][currentX] == 0) {
                                            direction = 'u';
                                        }
                                    } else if (currentX == sizeValue - 1 && currentY == 0) {
                                        if (mazeArray[currentY + 1][currentX] == 0) {
                                            direction = 'd';
                                        } else if (mazeArray[currentY][currentX - 1] == 0) {
                                            direction = 'l';
                                        }
                                    } else if (currentX == sizeValue - 1 && currentY == sizeValue - 1) {
                                        if (mazeArray[currentY][currentX - 1] == 0) {
                                            direction = 'l';
                                        } else if (mazeArray[currentY - 1][currentX] == 0) {
                                            direction = 'u';
                                        }
                                    }
                                    //setting start tile
                                    mazeArray[verticalClickPosition][horizontalClickPosition] = 2;
                                    colourMode++;
                                } else if (colourMode == 1) {
                                    endY = verticalClickPosition;
                                    endX = horizontalClickPosition;
                                    mazeArray[verticalClickPosition][horizontalClickPosition] = 3;
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

                //if user moves mouse execute following line of code in order to show 
                //temporary colour where a tile would be if user mouse clicked
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

        //setting size of the grid gui
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(guiDisplay, guiDisplay + 49);
        }

        //protected void used for setting cell colour
        @Override
        protected void paintComponent(Graphics g) {
            //following lines used to determine x and y coordinates
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            int width = getWidth();
            int height = getHeight();
            int cellWidth = width / sizeValue;
            int cellHeight = height / sizeValue;
            xOffset = (width - (sizeValue * cellWidth)) / 2;
            yOffset = (height - (sizeValue * cellHeight)) / 2;
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

            //used for showing temporary cell colour where cursor is 
            //hovering and when if clicked would become permanent colour
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

            //drawing grey outlines of the cells
            g2d.setColor(Color.GRAY);
            cells.stream().forEach((cell) -> {
                g2d.draw(cell);
            });

            int tempTime = time;
            time = timingSlider.getValue();
            if (tempTime != time) {
                save();
            }

            boolean cancel = false;
            for (int vertical = 0; vertical < sizeValue; vertical++) {
                for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                    Rectangle cell = cells.get(horizontal + vertical * sizeValue);
                    switch (mazeArray[vertical][horizontal]) {
                        case 1:
                            g2d.setColor(Color.BLACK);
                            g2d.fill(cell);
                            break;
                        case 2:
                            g2d.setColor(Color.RED);
                            g2d.fill(cell);
                            break;
                        case 3:
                            g2d.setColor(Color.BLUE);
                            g2d.fill(cell);
                            break;
                        case 4:
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
            //file is read from and saved to variable saveFile if file already exists
            try (InputStream in = Files.newInputStream(FILE);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line = reader.readLine();
                while (line != null) {
                    //content of file is saved to saveFile
                    saveFile = line;
                }
            } catch (IOException y) {
                System.err.println(y);
            }
        } catch (IOException x) {
            System.err.println(x);
        }
        //if the file does not contain anything since it was just created, default variables are used for save file
        if (saveFile == null) {
            saveFile = "10 100 0 0 0 0 0 1 0 0 0 0 1 1 0 1 0 1 0 1 1 0 1 1 0 1 0 1 0 1 1 0 1 0 1 0 0 0 1 1 0 0 0 0 0 "
                    + "1 1 0 0 1 1 0 0 1 0 1 1 0 1 1 0 0 1 0 0 0 0 0 0 0 1 0 1 1 1 0 1 0 1 0 0 0 1 0 0 0 1 1 1 1 1 0 "
                    + "0 0 1 0 0 0 1 0 0 0";
        }
        //a String array is created and each part of the array is saved to from saveFile seperated by spaces
        split = saveFile.split("\\s+");
        //variable size is the first number
        sizeValue = parseInt(split[0], 10);
        //variable time is the second number
        time = parseInt(split[1], 10);
    }

    private void save() {
        //saveFile is created using the main variables seperated by spaces
        saveFile = sizeValue + " " + time;
        for (int vertical = 0; vertical < sizeValue; vertical++) {
            for (int horizontal = 0; horizontal < sizeValue; horizontal++) {
                saveFile += " ";
                if (mazeArray[vertical][horizontal] == 0 || mazeArray[vertical][horizontal] == 1) {
                    saveFile += mazeArray[vertical][horizontal];
                } else {
                    saveFile += 0;
                }
            }
        }
        byte data[] = saveFile.getBytes();
        try (OutputStream out = new BufferedOutputStream(
                Files.newOutputStream(FILE, WRITE, TRUNCATE_EXISTING))) {
            out.write(data, 0, data.length);
        } catch (IOException x) {
            System.err.println(x);
        }
    }
}
