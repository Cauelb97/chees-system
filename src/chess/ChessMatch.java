package chess;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


import boardgame.Board;
import boardgame.Piece;
import boardgame.Position;
import chess.pieces.Bishop;
import chess.pieces.Horse;
import chess.pieces.King;
import chess.pieces.Pawn;
import chess.pieces.Queen;
import chess.pieces.Rook;

public class ChessMatch {
	
	private int turn;
	private Color currentPlayer;
	private Board board;
	private boolean check;
	private boolean checkMate;
	private ChessPiece enPassanVulnerable;
	private ChessPiece promoted;
	
	private List<Piece> piecesOnTheBoard = new ArrayList<>();
	private List<Piece> capturedPieces = new ArrayList<>();
	
	public ChessMatch() {
		board = new Board(8, 8);
		turn = 1;
		currentPlayer = Color.WHITE;
		initialSetup();
	}
	
	public int getTurn() {
		return turn;
	}
	
	public Color getCurrentPlayer() {
		return currentPlayer;
	}
	
	public boolean getCheck() {
		return check;
	}
	
	public boolean getCheckMate() {
		return checkMate;
	}
	
	public ChessPiece getEnPassanVulnerable() {
		return enPassanVulnerable;
	}

	public ChessPiece getPromoted() {
		return promoted;
	}

	
	//retorna a matriz de pe?as
	public ChessPiece[][] getPieces(){
		ChessPiece[][] mat = new ChessPiece[board.getRows()][board.getColumns()];
		for(int i=0; i<board.getRows(); i++) {
			for(int j=0; j<board.getColumns(); j++) {
				mat[i][j] = (ChessPiece) board.piece(i, j);
			}
		}
		return mat;
	}
	
	public boolean[][] possibleMoves(ChessPosition startPosition){
		Position position = startPosition.toPosition();
		validateStartPosition(position);
		return board.piece(position).possibleMoves();
	}
	   
	//Retira a pe?a da posi??o de origem e leva para a posi??o de destino
	public ChessPiece performChessPiece(ChessPosition startPosition, ChessPosition endPosition) {
		Position start = startPosition.toPosition();
		Position end = endPosition.toPosition();
		validateStartPosition(start);
		validateEndPosition(start, end);
		Piece capturedPiece = makeMove(start, end);
		
		if(testCheck(currentPlayer)) {
			undoMove(start, end, capturedPiece);
			throw new ChessException("Voce nao pode se colocar em check");
		}
		
		ChessPiece movedPiece = (ChessPiece)board.piece(end);
		
		// #specialmove promotion
		promoted = null;
		if (movedPiece instanceof Pawn) {
			if ((movedPiece.getColor() == Color.WHITE && end.getRow() == 0) || movedPiece.getColor() == Color.BLACK && end.getRow() == 7) {
				promoted = (ChessPiece)board.piece(end);
				promoted = replacePromotedPiece("Q");
				
			}
		}
		
		
		check = (testCheck(opponent(currentPlayer))) ? true : false;
		
		if (testCheckMate(opponent(currentPlayer))) {
			checkMate = true;
		}
		else {
		nextTurn();
		}
		
		// #specialmove en passant
		if(movedPiece instanceof Pawn && (end.getRow() == start.getRow() - 2 || end.getRow() == start.getRow() + 2)) {
			enPassanVulnerable = movedPiece;
		}
		else {
			enPassanVulnerable = null;
		}
		
		return (ChessPiece)capturedPiece;
	}
	
	public ChessPiece replacePromotedPiece(String type) {
		if(promoted == null) {
			throw new IllegalStateException("Nao ha nenhuma peca promovida");
		}
		if(!type.equals("B") && !type.equals("N") && !type.equals("R") && !type.equals("Q")) {
			return promoted;
		}
		Position pos = promoted.getChessPosition().toPosition();
		Piece p = board.removePiece(pos);
		piecesOnTheBoard.remove(p);
		
		ChessPiece newPeca = newPiece(type, promoted.getColor());
		board.placePiece(newPeca, pos);
		piecesOnTheBoard.add(newPeca);
		 
		return newPeca;
		
	}
	
	private ChessPiece newPiece(String type, Color color) {
		if (type.equals("B")) {
			return new Bishop(board, color);
		}
		if (type.equals("H")) {
			return new Horse(board, color);
		}
		if (type.equals("Q")) {
			return new Queen(board, color);
		}
		else {
			return new Rook(board, color);
		}
	}
	
	private void validateStartPosition(Position position) {
		if(!board.thereIsAPiece(position)) {
			throw new ChessException("Nao existe peca nessa posicao");
		}
		if (currentPlayer != ((ChessPiece)board.piece(position)).getColor()) {
			throw new ChessException("A peca escolhida nao e sua");
		}
		if(!board.piece(position).isThereAnyPossibleMove()) {
			throw new ChessException("Nao tem nenhum movimento possivel para a peca");
		}
	}
	
	private void validateEndPosition(Position start, Position end) {
		if (!board.piece(start).possibleMoves(end)) {
			throw new ChessException("A peca escolhido nao pode se movimentar para a posicao de destino");
		}
	}
	
	private void nextTurn() {
		turn++;
		//Se o currentPlayer for WHITE, ent?o agora ? o BLACK, sen?o ? o WHITE 
		currentPlayer = (currentPlayer == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}
	
	private Piece makeMove(Position start , Position end) {
		ChessPiece p = (ChessPiece)board.removePiece(start);
		p.increaseMoveCount();
		Piece capturedPiece = board.removePiece(end);
		board.placePiece(p, end);
		
		
		if (capturedPiece != null) {
			piecesOnTheBoard.remove(capturedPiece);
			capturedPieces.add(capturedPiece);
		}
		
		// #specialmove castling small kingside rook
		if(p instanceof King && end.getColumn() == start.getColumn() + 2) {
			Position startT = new Position(start.getRow(), start.getColumn() + 3);
			Position endT = new Position(start.getRow(), start.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(startT);
			board.placePiece(rook, endT);
			rook.increaseMoveCount();
		}
		
		// #specialmove castling big kingside rook
		if(p instanceof King && end.getColumn() == start.getColumn() - 2) {
			Position startT = new Position(start.getRow(), start.getColumn() - 4);
			Position endT = new Position(start.getRow(), start.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(startT);
			board.placePiece(rook, endT);
			rook.increaseMoveCount();
		}
		
		// #specialmove en passant
		if (p instanceof Pawn) {
			if(start.getColumn() != end.getColumn() && capturedPiece == null) {
				Position pawnPosition;
				if(p.getColor() == Color.WHITE) {
					pawnPosition = new Position(end.getRow() + 1, end.getColumn());
				}
				else {
					pawnPosition = new Position(end.getRow() - 1, end.getColumn());
				}
				capturedPiece = board.removePiece(pawnPosition);
				piecesOnTheBoard.remove(capturedPiece);
				capturedPieces.add(capturedPiece);
			}
		}
		
		return capturedPiece;
	}
	
	private void undoMove(Position start, Position end, Piece capturedPiece) {
		ChessPiece p = (ChessPiece)board.removePiece(end);
		p.decreaseMoveCount();
		board.placePiece(p, start);
		
		if (capturedPiece != null) {
			board.placePiece(capturedPiece, end);
			capturedPieces.remove(capturedPiece);
			piecesOnTheBoard.add(capturedPiece);
		}
		
		// #specialmove castling small kingside rook
		if(p instanceof King && end.getColumn() == start.getColumn() + 2) {
			Position startT = new Position(start.getRow(), start.getColumn() + 3);
			Position endT = new Position(start.getRow(), start.getColumn() + 1);
			ChessPiece rook = (ChessPiece)board.removePiece(endT);
			board.placePiece(rook, startT);
			rook.decreaseMoveCount();
		}
		
		// #specialmove castling big kingside rook
		if(p instanceof King && end.getColumn() == start.getColumn() - 2) {
			Position startT = new Position(start.getRow(), start.getColumn() - 4);
			Position endT = new Position(start.getRow(), start.getColumn() - 1);
			ChessPiece rook = (ChessPiece)board.removePiece(endT);
			board.placePiece(rook, startT);
			rook.decreaseMoveCount();
		}
		
		// #specialmove en passant
		if (p instanceof Pawn) {
			if(start.getColumn() != end.getColumn() && capturedPiece == enPassanVulnerable) {
				ChessPiece pawn = (ChessPiece)board.removePiece(end);
				Position pawnPosition;
				if(p.getColor() == Color.WHITE) {
					pawnPosition = new Position(3, end.getColumn());
				}
				else {
					pawnPosition = new Position(4, end.getColumn());
				}
				board.placePiece(pawn, pawnPosition);
			}
		}
	}
	
	private Color opponent(Color color) {
		return (color == Color.WHITE) ? Color.BLACK : Color.WHITE;
	}
	
	private ChessPiece king(Color color) {
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		for (Piece p : list) {
			if (p instanceof King) {
				return (ChessPiece)p;
			}
		}
		throw new IllegalStateException("Nao ha King da cor " + color);
	}
	
	private boolean testCheck(Color color) {
		Position kingPosition= king(color).getChessPosition().toPosition();
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == opponent(color)).collect(Collectors.toList());
		for(Piece p : list) {
			boolean [][] mat = p.possibleMoves();
			if (mat[kingPosition.getRow()][kingPosition.getColumn()]) {
				return true;
			}
		}
		return false;
	}
	
	private boolean testCheckMate(Color color) {
		if (!testCheck(color)) {
			return false;
		}
		List<Piece> list = piecesOnTheBoard.stream().filter(x -> ((ChessPiece)x).getColor() == color).collect(Collectors.toList());
		for(Piece p : list) {
			boolean[][] mat = p.possibleMoves();
			for(int i=0; i<board.getRows(); i++) {
				for(int j=0; j<board.getColumns(); j++) {
					if (mat[i][j]) {
						Position start = ((ChessPiece)p).getChessPosition().toPosition();
						Position end = new Position(i, j);
						Piece capturedPiece = makeMove(start, end);
						boolean testeCheck = testCheck(color);
						undoMove(start, end, capturedPiece);
						if (!testeCheck) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	
	private void placeNewPiece(char column, int row, ChessPiece piece) {
		board.placePiece(piece, new ChessPosition(column, row).toPosition());
		piecesOnTheBoard.add(piece);
	}
	 
	private void initialSetup() { 
		placeNewPiece('b', 1, new Horse(board, Color.WHITE));
		placeNewPiece('g', 1, new Horse(board, Color.WHITE));
		placeNewPiece('c', 1, new Bishop(board, Color.WHITE));
		placeNewPiece('f', 1, new Bishop(board, Color.WHITE));
        placeNewPiece('a', 1, new Rook(board, Color.WHITE));
        placeNewPiece('h', 1, new Rook(board, Color.WHITE));
        placeNewPiece('d', 1, new Queen(board, Color.WHITE));
        placeNewPiece('e', 1, new King(board, Color.WHITE, this));
        placeNewPiece('a', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('b', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('c', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('d', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('e', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('f', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('g', 2, new Pawn(board, Color.WHITE, this));
        placeNewPiece('h', 2, new Pawn(board, Color.WHITE, this));
        

        placeNewPiece('b', 8, new Horse(board, Color.BLACK));
		placeNewPiece('g', 8, new Horse(board, Color.BLACK));
        placeNewPiece('c', 8, new Bishop(board, Color.BLACK));
		placeNewPiece('f', 8, new Bishop(board, Color.BLACK));
        placeNewPiece('a', 8, new Rook(board, Color.BLACK));
        placeNewPiece('h', 8, new Rook(board, Color.BLACK));
        placeNewPiece('d', 8, new Queen(board, Color.BLACK));
        placeNewPiece('e', 8, new King(board, Color.BLACK, this));
        placeNewPiece('a', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('b', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('c', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('d', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('e', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('f', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('g', 7, new Pawn(board, Color.BLACK, this));
        placeNewPiece('h', 7, new Pawn(board, Color.BLACK, this));
        
	} 
}
