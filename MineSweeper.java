import java.util.*;

class MineSweeper {

    private Board board;
    private AI ai;

    public MineSweeper(final String board, final int nMines) {
        this.board = new Board(board, nMines);
        this.ai = new AI();
    }

    public String solve() {

        while (board.getUnknowns() > 0) {

            int[] nextMove = ai.getNextMove(board);

            if (nextMove[0] == -1) {
                return "?";
            }

            board.makeMove(nextMove);
        }

        return board.toString();
    }
}

class Board {

    // -1 represents the "?"
    // -2 represents the "x"
    int[][] cells;
    int unknowns;
    int leftMines;
    int nRows;
    int nCols;

    Board() {
        ;
    }

    Board (String board, int nMines) {
        // builds board from string input.
        String[] rows = board.split("\n");
        nCols = (rows[0].length() + 1) / 2;
        nRows = rows.length;
        unknowns = 0;
        leftMines = nMines;

        // cells initialization
        cells = new int[nRows][nCols];

        // cells filling
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                String element = Character.toString(rows[i].charAt(2 * j));
                if (element.equals("?")) {
                    cells[i][j] = -1;
                    unknowns++;
                }
                else {
                    cells[i][j] = Integer.parseInt(element);
                }
            }
        }
    }

    @Override
    public String toString() {
        // builds string from self
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < cells.length; i++) {
            builder.append(translate(cells[i][0]));
            for (int j = 1; j < cells[0].length; j++) {
                builder.append(" ");
                builder.append(translate(cells[i][j]));
            }
            if (i < cells.length - 1) builder.append("\n");
        }
        return builder.toString();
    }

    private String translate(int number) {
        if (number == -2) {
            return "x";
        }
        else if (number == -1) {
            return "?";
        }
        return Integer.toString(number);
    }

    void makeMove(int[] move) {
        // opens a cell
        if (move[0] == 0) {
            cells[move[1]][move[2]] = Game.open(move[1], move[2]);
        }

        // xs a cell
        else {
            cells[move[1]][move[2]] = -2;
            leftMines--;
        }

        unknowns--;
    }

    int getUnknowns() {
        // returns the number of unknown cells
        return unknowns;
    }

    int getLeftMines() {
        // returns the number nMines - "x"s
        return leftMines;
    }

    int getNumber(int i, int j) {
        return cells[i][j];
    }

    int[] getNSurroundings(int row, int col) {
        int nUnknown = 0;
        int nMines = 0;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (isAValidSurrounder(row, col, i, j)) {
                    if (cells[row + i][col + j] == -1) {
                        nUnknown++;
                    }
                    else if (cells[row + i][col + j] == -2) {
                        nMines++;
                    }

                }
            }
        }
        return new int[] {nUnknown, nMines};
    }

    boolean isAValidSurrounder(int refRow, int refCol, int rowIncrement, int colIncrement) {
        // -3 is a fake wall (needed for optimization)
        return (refRow + rowIncrement >= 0 &&
                refRow + rowIncrement < nRows &&
                refCol + colIncrement >= 0 &&
                refCol + colIncrement < nCols &&
                (rowIncrement != 0 || colIncrement != 0));
    }

    int[][] cloneCells() {
        // deep copy needed
        int[][] clonedCells = new int[nRows][nCols];
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                clonedCells[i][j] = getNumber(i, j);
            }
        }
        return clonedCells;
    }

}

class AI {

    // this keeps in mind moves if more than 1 are feasible in a single check
    // it is present just for speed purposes
    private Queue<int[]> moveBuffer;

    AI() {
        this.moveBuffer = new LinkedList<>();
    }

    int[] getNextMove(Board board) {

        // if I have no moves I recharge the buffer
        if (moveBuffer.size() == 0) {
            rechargeBuffer(board);

            // still empty? Impossible
            if (moveBuffer.size() == 0) {
                return new int[] {-1};
            }
        }

        // then I return a move
        return moveBuffer.remove();
    }

    private void rechargeBuffer(Board board) {
        for (int i = 0; i < board.nRows; i++) {
            for (int j = 0; j < board.nCols; j++) {
                int number = board.getNumber(i, j);
                int[] nSurroundings = board.getNSurroundings(i, j);
                int nUnknownSurroundings = nSurroundings[0];
                int nMineSurroundings = nSurroundings[1];

                // obvious win conditions
                if (board.getUnknowns() == board.getLeftMines() && number == -1) {
                    moveBuffer.add(new int[] {1, i, j});
                    return;
                }
                else if (board.getLeftMines() == 0 && number == -1) {
                    moveBuffer.add(new int[] {0, i, j});
                    return;
                }
                // if the surroundings are already opened, no need to check anything
                // also, if the number is negative, it is not a number!
                if (nUnknownSurroundings == 0 || number < 0) {
                    continue;
                }
                // win condition 1
                if (number == nMineSurroundings) {
                    addUnknownSurroundings(board, i, j, 0);
                    return;
                }
                // win condition 2
                if (nUnknownSurroundings == number - nMineSurroundings) {
                    addUnknownSurroundings(board, i, j, 1);
                    return;
                }
            }
        }
        // if we are not able to add any move, a tree search is NEEDED
        TreeSearch search = new TreeSearch();
        int[] move = search.launch(new TempBoard(board));
        if (move[0] != -1) {
            moveBuffer.add(move);
        }

    }

    private void addUnknownSurroundings(Board board, int row, int col, int openClose) {
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (board.isAValidSurrounder(row, col, i, j) &&
                        board.getNumber(row + i, col + j) == -1) {
                    moveBuffer.add(new int[]{openClose, row + i, col + j});
                }
            }
        }
    }
}

// --------------------------------------------------- //
// -------------------- HARD PART -------------------- //
// --------------------------------------------------- //

class TempBoard extends Board {

    final int[][] boundary;

    TempBoard(Board board) {
        this.cells = board.cloneCells();
        this.leftMines = board.leftMines;
        this.unknowns = board.unknowns;
        this.nCols = board.nCols;
        this.nRows = board.nRows;
        this.boundary = getBoundary();
    }

    TempBoard(TempBoard board) {
        this.cells = board.cloneCells();
        this.leftMines = board.leftMines;
        this.unknowns = board.unknowns;
        this.nCols = board.nCols;
        this.nRows = board.nRows;
        this.boundary = board.boundary;
    }

    void makeMove(int[] move) {
        // the temp board does not know the number to put, so the move code first element is now:
        // -2: bomb, another number: the actual number
        cells[move[1]][move[2]] = move[0];
        unknowns--;
        if (move[0] == -2) {
            leftMines--;
        }
    }

    private boolean hasSomethingAround(int row, int col) {
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {
                if (isAValidSurrounder(row, col, i, j) && getNumber(row + i, col + j) != -1) {
                    return true;
                }
            }
        }
        return false;
    }

    private int[][] getBoundary() {

        List<int[]> result = new ArrayList<>();
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                if (getNumber(i, j) == -1 && hasSomethingAround(i, j)) {
                    result.add(new int[] {i, j});
                }
            }
        }
        int[][] primitive = new int[result.size()][2];
        return result.toArray(primitive);
    }

    int getBoundaryUnknowns() {
        int result = 0;
        for (int[] cell : boundary) {
            if (getNumber(cell[0], cell[1]) == -1) {
                result++;
            }
        }
        return result;
    }
}

class TreeSearch {

    int[] launch(TempBoard board) {

        int[][] orderedUnknown = getOrderedUnknowns(board);
        for (int[] cell : orderedUnknown) {
            int[] possibleNumbers = findPossibleNumbers(board, cell);

            // if the bomb is NOT a possibility (it is always the first element), opening that cell is a valid move
            if (possibleNumbers[0] != -2) {
                return new int[] {0, cell[0], cell[1]};
            }

            List<Integer> confirmedPossibleNumbers = new ArrayList<>();
            for (int number : possibleNumbers) {

                if (recursiveSearch(new TempBoard(board), cell, number)) {
                    confirmedPossibleNumbers.add(number);
                }
                // if the bomb IS NOT confirmed, then I am in the pre-for loop case
                else if (number == -2) {
                    return new int[] {0, cell[0], cell[1]};
                }
                // if I have the bomb PLUS another possibility, it means that cell's content cannot be determined
                if (confirmedPossibleNumbers.size() > 1) {
                    break;
                }
            }

            // if I have ONLY the bomb as confirmed possibility, then it's a win
            if (confirmedPossibleNumbers.size() == 1) {
                return new int[] {1, cell[0], cell[1]};
            }
        }
        // if the whole search failed, then the problem cannot be solved
        return new int[] {-1};
    }

    private boolean recursiveSearch(TempBoard board, int[] cell, int numberToPut) {
        // when I receive the move (number into cell) I'm sure that is feasible since I checked the iteration before
        board.makeMove(new int[] {numberToPut, cell[0], cell[1]});

        if (board.getBoundaryUnknowns() == 0) {
            return true;
        }

        // now I don't need to cycle on the unknowns, because the move before is not confirmed yet
        // I just complete all the board so the order I fill every cell is indifferent
        int[] bestUnknown = findBestUnknown(board);
        int[] possibleNumbers = findPossibleNumbers(board, bestUnknown);

        // if anyone of the possible number lead to a complete acceptable board, then I return True
        for (int number : possibleNumbers) {
            if (recursiveSearch(new TempBoard(board), bestUnknown, number)) {
                return true;
            }
        }

        // if not, I return false
        return false;
    }

    private int[] findPossibleNumbers(TempBoard board, int[] cell) {
        List<Integer> result = new ArrayList<>();

        // bombs
        if (board.getLeftMines() > 0) {
            boolean feasible = true;
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {

                    int row = cell[0] + i;
                    int col = cell[1] + j;

                    // for each number around I check the number of bombs does not break the limit
                    if (board.isAValidSurrounder(cell[0], cell[1], i, j) && board.getNumber(row, col) >= 0) {

                        int nMines = board.getNSurroundings(row, col)[1];
                        if (nMines == board.getNumber(row, col)) {
                            feasible = false;
                            break;
                        }

                    }
                }
                if (!feasible) break;
            }
            if (feasible) result.add(-2);
        }

        // maybe a number is not possible there
        boolean checkNumbers = true;
        for (int i = -1; i < 2; i++) {
            for (int j = -1; j < 2; j++) {

                int row = cell[0] + i;
                int col = cell[1] + j;

                // for each number around I check the number of bombs does not go under the limit
                if (board.isAValidSurrounder(cell[0], cell[1], i, j) && board.getNumber(row, col) >= 0) {

                    int[] surroundings = board.getNSurroundings(row, col);
                    int nUnknowns = surroundings[0];
                    int nMines = surroundings[1];

                    // since I currently want to put a number into an unknown, I need one more space
                    if (board.getNumber(row, col) >= nUnknowns + nMines) {
                        checkNumbers = false;
                        break;
                    }

                }
            }
            if (!checkNumbers) break;
        }

        // numbers
        if (checkNumbers && board.getLeftMines() < board.getUnknowns()) {
            for (int n = 0; n < 9; n++) {
                int[] surroundings = board.getNSurroundings(cell[0], cell[1]);
                int nUnknowns = surroundings[0];
                int nMines = surroundings[1];

                // the number must be at least the current nMines and at most the sum nMines + nUnknowns
                if (n < nMines) {
                    continue;
                }

                if (n > nMines + nUnknowns) {
                    break;
                }

                result.add(n);
            }
        }

        return result.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] findBestUnknown(TempBoard board) {

        int bestNSurroundings = 9;
        int[] bestUnknown = {};

        for (int[] cell : board.boundary) {
            int nUnknown = board.getNSurroundings(cell[0], cell[1])[0];
            // checks that it is actually unknown
            if (board.getNumber(cell[0], cell[1]) == -1 && nUnknown < bestNSurroundings) {
                bestNSurroundings = nUnknown;
                bestUnknown = cell;
            }
        }

        return bestUnknown;
    }

    private int[][] getOrderedUnknowns(TempBoard board) {

        int[][] orderedUnknowns = new int[board.boundary.length][2];
        TempBoard currentBoard = new TempBoard(board);

        for (int c = 0; c < orderedUnknowns.length; c++) {

            int[] cell = findBestUnknown(currentBoard);
            orderedUnknowns[c] = cell;

            // findBestUnknown looks only for -1s so it is SAFE to do this
            currentBoard.makeMove(new int[] {-3, cell[0], cell[1]});
        }

        return orderedUnknowns;
    }
}
