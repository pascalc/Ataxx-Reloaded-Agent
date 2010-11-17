# Ataxx Reloaded #

## Initial Setup ##

Each player -- black and white -- starts off with 2 pieces. The black pieces occupy the top-left and bottom-right corners and the white ones occupy the top-right and bottom-left corners.

## Objective ##

The objective of this game is to end the game with more pieces on the board. This is achieved by filling up more spaces on the board than your opponent, once the game has ended. The game ends if 1) the board is filled; 2) you or your opponent do not have any pieces left; or 3) the game reaches its 100th turn (a turn is defined as a pair of moves from both players). If a player has no possible valid moves or makes an invalid move, the move is considered forfeited. For example, in the board below, white wins as black has fewer pieces when the game is finished.

## Moves ##

In the original Ataxx game, players alternate moves, with black playing as the first player. On each turn, a player moves a piece of his color either horizontally, vertically or diagonally 1 or 2 squares away, to a blank square. If the piece is moved one square, the piece replicates, leaving a piece new piece of its own color behind. If moved two squares, the piece jumps, vacating its original location. When a piece moves to its destination, it converts all neighboring pieces to its own color (horizontally, vertically as well as diagonally).

In our Ataxx reloaded, the possible jump moves have been modified. You can only jump horizonally, vertically or diagonally two squares (L-shaped Western chess knight moves are disallowed). Furthermore, jumps of three squares vertically and horizontally are allowed.

# The Agent: Frakking Toaster #

## Terminal Test ##

A Board is terminal if one of the following conditions is met:
	- We have reached the 100th move of the game.
	- There are no more empty tiles on the board.
	- One of the players has no more pieces left.

## Evaluation Function ##

Goes through the Board tile by tile and calculates score.

Each MAX tile adds a positive integer to the score, the weight of
which is determined by the heuristic map. This map assigns more
importance to edges than non-edges, and even more to corners.
 
Each MIN tile on the board reduces the score by the amount defined
in the map. This means that it is better for MAX to occupy edges 
and corners, as well as to prevent MIN from doing so.
 
A grouping score (the number of friendly adjacent tiles) for each MAX 
tile is also added to the score, and a score for each MIN tile is 
subtracted. This means that it is better for MAX to have his own tiles 
next to each other than apart. It is also better for MAX to have MIN's 
tiles more spread out.

## Successor Function ##

Returns a priority queue holding all the possible moves that can
be made by player from this Board. The queue is a max-heap for MAX
and a min-heap for MIN, ordered by the heuristic value of
the boards they result in.
 
These queues should help when alpha-beta pruning, as more valuable
branches should be considered first, making it easier to prune "worthless"
ones.
