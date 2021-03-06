package model.ship.modern;

import model.ship.Ship;


/**
 * A cruiser from the modern period
 * @author Maureen
 *
 */
public class Cruiser extends Ship {

	public Cruiser() {
		height = 8;
		width = 2;
		nbCases = height*width;
		nbCasesTouched = 0;
		isPlaced = false;
	}
	

	public String toString() {
		return "Cruiser " + getHeight() + "x" + getWidth();
	}
	
	@Override
	public String letter() {
		return "C";
	}

}
