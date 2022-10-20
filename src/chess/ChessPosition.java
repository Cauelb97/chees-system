package chess;

import boardgame.Position;

public class ChessPosition {

	private char column;
	private int row;
	
	public ChessPosition(char column, int row) {
		if (column < 'a' || column > 'h' || row < 1 || row > 8) {
			throw new ChessException("Erro em instanciar Chessposition. Valores validos de a1 ate h8");
		}
		this.column = column;
		this.row = row;
	}

	public char getColumn() {
		return column;
	}

	public int getRow() {
		return row;
	}
	
	//Converte ChessPosition para Position
	protected  Position toPosition() {
		return new Position(8 - row, column - 'a');
	}
	
	//Converte de Position para ChessPosition
	protected static ChessPosition fromPosition(Position position) {
		return new ChessPosition((char)('a' + position.getColumn()), 8 - position.getRow());
	}
 
	//colocamos as aspas para forçar a entender que é uma concatenação de strigs
	@Override
	public String toString() {
		return "" + column + row;
	}
}
