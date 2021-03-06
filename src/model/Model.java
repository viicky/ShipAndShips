package model;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Observable;

import exception.NotInFieldException;
import exception.NotPlaceableException;
import exception.ShipException;
import model.ship.Ship;
import model.ship.factory.ModernShipFactory;
import model.ship.factory.ShipFactory;
import model.strategy.ComputerStrategy;
import model.strategy.CrossComputerStrategy;
import model.strategy.PlacementRandomStrategy;
import model.strategy.PlacementStrategy;
import model.strategy.RandomComputerStrategy;

/**
 * Base class, interface to wich communicate to play battleship
 * 
 * @author PUBC
 *
 */

public class Model extends Observable implements Serializable {
	
	public enum GameState{PLACEMENT, IN_GAME};
	
	public final transient static int PLAYER = 0, PC = 1;
	
	private int currentPlayer;
	private GameState gameState = GameState.PLACEMENT;
	
	transient private ModelDAO dao;
	transient private ShipFactory shipFactory;
	
	private int sizeBattleField;
	private ComputerStrategy strat;
	private PlacementStrategy placement;
	

	private BattleField ally, opponent;
	
	//private Ship currentShip;
	private List<Ship> shipsNoPlaced;
	private boolean shipsPlacedComputer;

	public Model() {
		// defaultvalues
		sizeBattleField = 10;

		ally = new BattleField(sizeBattleField);
		opponent = new BattleField(sizeBattleField);

		shipFactory = ModernShipFactory.getInstance();
		strat = RandomComputerStrategy.getInstance();
		placement = PlacementRandomStrategy.getInstance();
		
		dao = ModelDAO.getInstance();

		shipsNoPlaced = getShipFactory().getShips();
		shipsPlacedComputer = false;

	}

	public Model(ShipFactory age, ComputerStrategy strategy, PlacementStrategy placementStrat) {
		newGame(age, strategy, placementStrat);
	}
	
	/**
	 * to begin a new game
	 * @param age age of the Ships
	 * @param strategy attack strategy of the computer
	 * @param placementStrat placement strategy of the computer
	 */
	public void newGame(ShipFactory age, ComputerStrategy strategy, PlacementStrategy placementStrat) {
		sizeBattleField = 10;
		gameState = GameState.PLACEMENT;
		ally = new BattleField(sizeBattleField);
		opponent = new BattleField(sizeBattleField);

		shipFactory = age;
		strat = strategy;
		placement = placementStrat;
		shipsNoPlaced = getShipFactory().getShips();
		shipsPlacedComputer = false;
		update();
	}
	

	public List<Ship> getShipsNoPlaced() {
		return shipsNoPlaced;
	}
	

	/**
	 * @return true if the player or the computer won
	 */
	public boolean won() {
		if ((ally.won() || opponent.won()) && gameState == GameState.IN_GAME) {
			return true;
		}
		return false;
	}

	/**
	 * set the age of the game
	 * @param sf
	 *            ShipFactory
	 */
	public void setPeriod(ShipFactory sf) {
		shipFactory = sf;

	}

	/**
	 * set the strategy chosen by the player
	 * @param e
	 */
	public void setStrategy(String s) {
		switch(s) {
		case "Cross" : 
			strat = CrossComputerStrategy.getInstance();
			break;
		case "Random":
			strat = RandomComputerStrategy.getInstance();
		}
	}

	/**
	 * Notify Observers
	 */
	private void update() {
		setChanged();
		notifyObservers();
	}

	/**
	 * attack the opponent battlefield at the position (x,y)
	 * @param x abscissa of the shot
	 * @param y ordinate of the shot
	 * @return
	 */
	public boolean shot(int x, int y) {
		boolean success = false;
		try {
			// Shot on the current battlefield
			switch (currentPlayer) {
			case PLAYER:
				success = opponent.receiveShot(x, y);
				break;
			case PC:
				success = ally.receiveShot(x, y);
				break;
			}
			if(!won()) {
				endTurn();
			}
			update();

			return success;
		} catch (NotInFieldException e) {
			System.err.println("Shooting out of battlefield");
		}

		return success;
	}
	
	
	/**
	 * check if an ally case is touched
	 * @param x absissa
	 * @param y ordinate
	 * @return whether this case is touched
	 */
	public boolean allyTouched(int x, int y) {
		try {
			return ally.touched(x, y);
		} catch (NotInFieldException e) {
			System.err.println("Checking if ally touched out of field");
		}
		return false;
	}
	
	public void changeShipOrientation(Ship s) {
		s.changeOrientation();
		update();
	}
	
	/**
	 * check if an opponent case is touched
	 * @param x absissa
	 * @param y ordinate
	 * @return whether this case is touched
	 */
	public boolean opponentTouched(int x, int y) {
		try {
			return opponent.touched(x, y);
		} catch (NotInFieldException e) {
			System.err.println("Checking if opponent touched out of field");
		}
		return false;
	}
	
	/** 
	 * Get the ally ship at a given position
	 * @param x absissa
	 * @param y ordinate
	 * @return the ally ship at the given position, or null
	 */
	public Ship getAllyShip(int x, int y) {
		try {
			return ally.getShip(x, y);
		} catch (NotInFieldException e) {
			System.err.println("Searching ally ship out of field");
		}
		return null;
	}
	
	/** 
	 * Get the opponent ship at a given position
	 * @param x absissa
	 * @param y ordinate
	 * @return the opponent ship at the given position, or null
	 */
	public Ship getOpponentShip(int x, int y) {
		try {
			return opponent.getShip(x, y);
		} catch (NotInFieldException e) {
			System.err.println("Searching opponent ship out of field");
		}
		return null;
	}
	

	/**
	 * Execute the computer placement strategy of the ships
	 */
	public void placeShipComputer() {
		try {
			if (shipsPlacedComputer == false) {
				List<Ship> listShips = shipFactory.getShips();
				placement.placeShips(opponent, listShips);
				shipsPlacedComputer = true;
			} 

		} catch (NotPlaceableException e) {
			System.err.println("The computer can no longer place ships");
		} catch (ShipException e) {
			System.err.println("Error while trying to place computer ships");
		}

	}

	/**
	 * 
	 * @param ship
	 *            that the player want to place
	 * @return true if the player can place the ship on the BattleField
	 */
	public boolean placeShip(Ship ship, int x, int y) {
		try {
			ship.setPosition(x, y);
			boolean everythingIsOk = ally.placeShip(ship);
			if (everythingIsOk) {
				shipsNoPlaced.remove(ship);
				update();
				return true;
			} else {
				return false;
			}
		} catch (NotInFieldException e) {
			System.err.println("Impossible to place the ship");
		}
		return false;
	}

	/**
	 * change the current player
	 */
	private void endTurn() {
		if (currentPlayer == PC) {
			currentPlayer = PLAYER;
		} else {
			currentPlayer = PC;
			try {
				if(!won()) {
					strat.shot(ally);
					if(!won()) {
						currentPlayer = PLAYER;
					}
				}
			} catch (NotInFieldException e) {
				System.err.println("Computer error when he wants to shot us");
			}
		}
	}

	/**
	 * save the game in the save file fn
	 * @param fn name of the save file
	 * @throws IOException
	 */
	
	public void save(String fn) throws IOException {	
		dao.save(this, fn);
		
	}
	
	/**
	 * load the Madel saved in the file fn
	 * @param fn name of the save file
	 * @throws IOException
	 */
	
	public void load(String fn) throws IOException{
		Model info = dao.load(fn);
		
		gameState = info.getGameState();
		currentPlayer = info.currentPlayer();
		ally = info.getAlly();
		opponent = info.getOpponent();
		strat = info.getStrat();
		placement = info.getPlacement();
		sizeBattleField = info.getSizeBattleField();
		shipsPlacedComputer = info.getShipsPlacedComputer();
		shipsNoPlaced = info.getShipsNoPlaced();
		
		update();
		
	}
	
	/**
	 * 
	 * @return true if the current player is the Human ; false if it's the computer
	 */
	public int currentPlayer() {
		return currentPlayer;
	}

	public BattleField getAlly() {
		return ally;
	}

	public BattleField getOpponent() {
		return opponent;
	}
	
	public ComputerStrategy getStrat(){
		return strat;
	}
	
	public PlacementStrategy getPlacement(){
		return placement;
	}
	
	public int getSizeBattleField(){
		return sizeBattleField;
	}

	public boolean getShipsPlacedComputer(){
		return shipsPlacedComputer;
	}


	/**
	 * Access state of the game
	 * @return state of the game
	 */
	public GameState getGameState() {
		return gameState;
	}
	
	
	/**
	 * Set the game state
	 * @param gs new game state
	 */
	public void setGameState(GameState gs) {
		gameState = gs;
		update();
	}
	
	
	/**
	 * Access the ship factory of this model
	 * @return ship factory of this model
	 */
	public ShipFactory getShipFactory() {
		return shipFactory;
	}


}
