import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class FrakkingToaster {
	private final String NAME = "Frakking Toaster";
	
	private BufferedReader input;
	private PrintWriter output;
	
	private Board initialBoard;
	
	private static int SIZE;
	private static int MOVE;
	private static final boolean DEBUG = false;
	
	public static void debug(String message){
		if(DEBUG){
			System.err.println(message);
		}
	}
	
	/**
	 * Define player pieces.
	 */
	public static final int MAX = 1;
	public static final int MIN = 2;
	public static final int EMPTY = 0;
	
	/**
	 * Search space limiting variables.
	 */
	// How many levels of the search tree to consider.
	private static final int CUTOFF = 2;
	
	/**
	 * Valid moves, represented as vectors.
	 */
	// Moves
	static final int[][] VALID_MOVES = {
		{-1,-1}, {-2,-2}, {0,-1}, {0,-2}, {1,-1},
		{2,-2}, {-1,0}, {1,0}, {-1,1},
		{-2,2},{0,1}, {0,2}, {1,1},
		{2,2}, {0,-3}, {0,3}, {-3,0}, {3,0} 
	};
	
	/**
	 * Vectors representing all the surrounding tiles that can be infected.
	 */
	// Infectable tiles
	static final int[][] INFECTABLE = {
		{-1,-1}, {0,-1}, {1,-1},
		{-1,0},{1,0},
		{-1,1},{0,1},{1,1}
	};
	
	/**
	 * Map used in the heuristic function.
	 * The values here are awarded if a player's piece occupies that position 
	 * on the map.
	 * 
	 * Edges and corners are given higher weights as they are harder to attack. 
	 */
	// Position Scores
	static final int[][] POS_SCORES = {
		{3,2,2,2,2,2,3},
		{2,1,1,1,1,1,2},
		{2,1,1,1,1,1,2},
		{2,1,1,1,1,1,2},
		{2,1,1,1,1,1,2},
		{2,1,1,1,1,1,2},
		{3,2,2,2,2,2,3}
	};
	
	/**
     * Program entry point
     **/    
    public static void main(String args[]) {             
        // We are not expecting any arguments
                
        // Start an instance of the player and start playing the game!
        FrakkingToaster colonelTigh = new FrakkingToaster();
        colonelTigh.goOnline();
    } // end main()
    
    /**
     * Wrapper class to hold actions and the boards they result in.
     * 
     * An action is represented as [x1,y1,x2,y2] where (x1,y1)
     * are the initial coordinates of a piece on the board,
     * and (x2,y2) are their final coordinates.
     * 
     * When a Move is created the heuristic value for it's board
     * is calculated and stored.
     */
    private class Move {
    	int[] action;
    	int heuristicValue = 0;
    	Board result;
    	
    	public Move(int[] action, Board result) {
    		this.action = action;
			this.heuristicValue = result.evaluate();
			this.result = result;
		}

		@Override
		public String toString() {
			if (action == null) return null;
			return action[0] + ":" + action[1] + ":" + action[2] + ":" + action[3] + ":"; 
		}
    }
    
    /**
     * Class representing the game board. 
     * 
     * The game board is stored as a matrix, with M[x][y] holding 
     * the value of the tile at (x,y) on the board.
     */
	private class Board {
		private int[][] gameboard;
		private int numEmpty = 0;
		private int numMax = 0;
		private int numMin = 0;
		
		private void updateInfo(int value){
			switch(value){
            case EMPTY:
            	numEmpty++;
            	break;
            case MAX:
            	numMax++;
            	break;
            case MIN:
            	numMin++;
            	break;
            default:
            	System.err.println("Error reading board");
            	System.exit(1);
            }
		}
		
		/**
		 * Construct a Board from the String we receive as input.
		 * Tile number information is calculated when iterating through
		 * the String.
		 */
		public Board(int size, String data) throws Exception{
			gameboard = new int[size][size];
			
			// Populate the board with what we know
            StringTokenizer st = new StringTokenizer(data, ":");            
            for (int i = 0; i < size; ++i) {
                for (int j = 0; j < size; ++j) {
                    try {
                        String Value = st.nextToken();
                        gameboard[j][i] = (new Integer(Value)).intValue();
                        updateInfo(gameboard[j][i]);
                    } catch (NoSuchElementException e) {                        
                        throw new Exception("Unable to parse received board information.");
                    } catch (NumberFormatException e) {                        
                        throw new Exception("Invalid tile information");
                    }
                }
            }
		}
		
		/**
		 * Construct a Board from an already existing game board matrix, along with pre-existing 
		 * information. 
		 */
		public Board(int[][] values, int[] info){
			gameboard = values;
			numEmpty = info[0];
			numMin = info[1];
			numMax = info[2];
		}
		
		/**
		 * Deep copy the game board matrix.
		 */
		private int[][] copyValues(){
			int[][] copy = new int[SIZE][];
			for(int col = 0; col < SIZE; col++){
				copy[col] = (int[]) gameboard[col].clone(); 
			}
			return copy;
		}
		
		/**
		 * Test whether the given x and y values specify a valid position
		 * on the game board.
		 */
		public boolean validPosition(int x, int y){
			if(x < 0 || x >= SIZE)
				return false;
			if(y < 0 || y >= SIZE)
				return false;
			return true;
		}
		
		/**
		 * Return the value of the tile at (x,y), or -1 if (x,y) is not a valid position. 		 
		 */
		public int get(int x, int y) {
			if(validPosition(x,y))
				return gameboard[x][y];
			return -1;
		}
		
		/**
		 * Insert value into the Board if (x,y) is a valid position, and value is one of
		 * EMPTY, MAX or MIN. Returns whether this operation was successful.
		 */
		public boolean put(int x, int y, int value) {
			if(validPosition(x,y)){
				if (value == EMPTY || value == MAX || value == MIN){
					// Update info
					switch(gameboard[x][y]){
					case MAX:
						numMax--;
						break;
					case MIN:
						numMin--;
						break;
					case EMPTY:
						numEmpty--;
						break;
					}
					
					// Insert new piece.
					gameboard[x][y] = value;
					updateInfo(value);
					return true;
				}
			}
			return false;
		}
		
		/**
		 * Return a deep copy of this Board.
		 */
		public Board clone(){
			return new Board(this.copyValues(),
					new int[] {this.numEmpty, this.numMin, this.numMax});
		}
		
		/**
		 * A Board is terminal if one of the following conditions is met:
		 * 	- We have reached the 100th move of the game.
		 * 	- There are no more empty tiles on the board.
		 *	- One of the players has no more pieces left.
		 */
		public boolean isTerminal(){
	    	if(MOVE == 100) return true;
	    	else if(numEmpty == 0) return true;
	    	else if(numMax == 0) return true;
	    	else if(numMin == 0) return true;
	    	else return false;
	    }
		
		/**
		 * Goes through the Board tile by tile and calculates score.
		 * 
		 * Each MAX tile adds a positive integer to the score, the weight of
		 * which is determined by the heuristic map. This map assigns more
		 * importance to edges than non-edges, and even more to corners.
		 * 
		 * Each MIN tile on the board reduces the score by the amount defined
		 * in the map. This means that it is better for MAX to occupy edges 
		 * and corners, as well as to prevent MIN from doing so.
		 * 
		 * A grouping score (see next) for each MAX tile is also added to the score,
		 * and a score for each MIN tile is subtracted. This means that it is better
		 * for MAX to have his own tiles next to each other than apart. It is
		 * also better for MAX to have MIN's tiles more spread out.
		 */
		public int evaluate(){
			int score = 0;
			for(int i = 0; i < SIZE; i++){
				for(int j = 0; j < SIZE; j++){
					switch(gameboard[i][j]){
					case MAX:
						score += POS_SCORES[i][j];
						score += calculateGroupScore(i,j);
						break;
					case MIN:
						score -= POS_SCORES[i][j];
						score -= calculateGroupScore(i,j);
						break;
					case EMPTY:
						break;
					}
				}
			}
			return score;
	    }
		
		/**
		 * Calculates the grouping score for a tile.
		 * 
		 * This score is the number of adjacent tiles to this one 
		 * that are occupied by the player.
		 */
		private int calculateGroupScore(int x, int y){
			int res = 0;
			int player = gameboard[x][y];
			
			for (int[] tile : INFECTABLE) {
				int tx = tile[0] + x;
				int ty = tile[1] + y;

				if (validPosition(tx, ty)) {
					if(gameboard[tx][ty] == player)
						res++;
				}
			}
			return res;
		}
		
		/**
		 * Returns a priority queue holding all the possible moves that can
		 * be made by player from this Board. The queue is a max-heap for MAX
		 * and a min-heap for MIN, ordered by the heuristic value of
		 * the boards they result in.
		 * 
		 * These queues should help when alpha-beta pruning, as more valuable
		 * branches should be considered first, making it easier to prune "worthless"
		 * ones.
		 */
		public PriorityQueue<Move> getSuccessors(int player){
			// Set up priority queue
			PriorityQueue<Move> successors = null;
			if(player == MAX){
				successors = new PriorityQueue<Move>(20*numMax,
						new Comparator<Move>() {
							public int compare(Move m1, Move m2) {
								return m1.heuristicValue - m2.heuristicValue;
							}
						});
			} else {
				successors = new PriorityQueue<Move>(20*numMin,
						new Comparator<Move>() {
							public int compare(Move m1, Move m2) {
								return m2.heuristicValue - m1.heuristicValue;
							}
						});
			}
			
			// Carry out valid moves
			for(int i = 0; i < SIZE; i++){
				for(int j = 0; j < SIZE; j++){
					int value = gameboard[i][j];
					if(value == player){
						for(int[] move : VALID_MOVES){
							Board result = this.clone();
							
							int newX = move[0] + i;
							int newY = move[1] + j;
							
							if(validPosition(newX,newY) && get(newX,newY) == EMPTY){
								result.put(newX, newY, player);
								
								// If this was a jump move, delete our old position
								if(Math.abs(newX - i) > 1 || Math.abs(newY - j) > 1){
									result.put(i, j, EMPTY);
								}
								
								// Infect
								for(int[] around : INFECTABLE){
									int infX = newX + around[0];
									int infY = newY + around[1];
									
									if(validPosition(infX,infY)){
										int otherPlayer = (player % 2) + 1;
										if(result.get(infX,infY) == otherPlayer){
											result.put(infX, infY, player);
										}
									}
								}
								
								int[] action = {i,j,newX,newY};
								Move newMove = new Move(action,result);
								successors.add(newMove);
							}
						}
					}
				}
			}
			return successors;
		}

		/**
		 * Pretty-print this Board.
		 */
		@Override
		public String toString(){
			StringBuilder sb = new StringBuilder();
			for(int i = 0; i < SIZE; i++){
				for(int j = 0; j < SIZE; j++){
					sb.append(gameboard[j][i] + " ");
				}
				sb.append("\n");
			}
			sb.append("\n");
			sb.append("Empty tiles: " + numEmpty + "\n");
			sb.append("Max tiles: " + numMax + "\n");
			sb.append("Min tiles: " + numMin + "\n");
			return sb.toString();
		}
	}
	
	/**
	 * Establishes I/O
	 */
	public FrakkingToaster(){
		try {
			input = new BufferedReader(new InputStreamReader(System.in));
			output = new PrintWriter(
						new BufferedWriter(
							new OutputStreamWriter(System.out)),true);
		} catch (Exception e) {
			System.err.println("Failed to open streams.");
			System.exit(1);
		}
	}
	
	private String readLine() {
		try {
			// Listen on stdin
			String l_InLine = input.readLine();
			if (l_InLine == null) {
				// The input stream seems to have closed.
				return null;
			} else {
				return l_InLine;
			}

		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Parse commands and act on them.
	 */
	private void goOnline(){
		// Wait for command
        String l_Command = readLine();
        
        if (l_Command.compareTo("identify") == 0) {
            // We are asked for our name
            output.println(NAME);
        } else if (l_Command.compareTo("move") == 0) {
            // We need to put on our thinking cap and decide on a move to make
            // 1. Read in details about the game environment first
            try {
                MOVE = Integer.valueOf(readLine());
                SIZE = Integer.valueOf(readLine());
                
                StringBuilder l_GameBoardStr = new StringBuilder();
                for (int i = 0; i < SIZE; ++i) {
                    l_GameBoardStr.append(readLine());
                }
                
                initialBoard = new Board(SIZE, l_GameBoardStr.toString());
                
                makeMove(initialBoard);
            } catch (NumberFormatException e) {
                // Error receiving input from server.
                // Hard to recover from, so we quit.
            } catch (Exception e) {
                // Probably an error receiving inputs from the server.
                // We have to quit.
            }           
            
        } else {
            
            // Unknown command.            
            // We'd terminate then.
            
        }
        
        try {
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
        output.close();
        System.exit(0);
    }

	/**
	 * Pick the best move to make from the given Board,
	 * and print it.
	 */
	private void makeMove(Board board) {
		Move m = alphaBetaSearch(new Move(null,board));
		if (m == null) {
			output.println("pass");
		} else {
			output.println(m);
			debug("-------- FINAL DECISION ----------");
			debug(m.result + "\n");
		}
	}
	
	/**
	 * Returns the best move to be made according to the Alpha-Beta Search algorithm.
	 */
	private static Move alphaBetaSearch(Move m){
		debug("Starting alpha beta search...");
		Move best = playMax(m,Integer.MIN_VALUE,Integer.MAX_VALUE,0);
		return best;
	}
	
	/**
	 * Returns the best move for MAX to make given this initial move.
	 */
	private static Move playMax(Move m, int alpha, int beta, int depth){
		if(depth == CUTOFF || m.result.isTerminal()){
			return m;
		}
		
		int v = Integer.MIN_VALUE;
		Move best = null;
		
		PriorityQueue<Move> successors = m.result.getSuccessors(MAX);
		while(!successors.isEmpty()){
			Move s = successors.poll();
			int min = playMin(s,alpha,beta,depth+1).heuristicValue;
			
			if(min > v){
				v = min;
				best = s;
			}
			if(v >= beta) return best;
			alpha = (alpha > v) ? alpha : v;
		}
	
		return best;
	}
	
	/**
	 * Returns the best move for MIN to make given this initial move.
	 */
	private static Move playMin(Move m, int alpha, int beta, int depth){
		if(depth == CUTOFF || m.result.isTerminal()){
			return m;
		}
		
		int v = Integer.MAX_VALUE;
		Move best = null;
		
		PriorityQueue<Move> successors = m.result.getSuccessors(MIN);
		while(!successors.isEmpty()){
			Move s = successors.poll();
			int max = playMax(s,alpha,beta,depth+1).heuristicValue;
			if(max < v){
				v = max;
				best = s;
			}
			if(v <= alpha) return best;
			beta = (beta < v) ? beta : v;
		}
		
		return best;
	}
}
