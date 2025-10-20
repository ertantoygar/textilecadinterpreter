package tr.com.logidex.cad.model;


import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point2D;
import tr.com.logidex.cad.processor.FileProcessor;
import tr.com.logidex.cad.Unit;
import tr.com.logidex.cad.Util;

import java.text.DecimalFormat;


public class Lbl  {

	private String text;
	private double angle, origin, width, height;

	private SimpleObjectProperty<Point2D> position = new SimpleObjectProperty<>(new Point2D(0,0));

	//private String printablePosition = "";

	private Point2D originalXY;// Incoming from the file.Don't create a setter.

	private ClosedShape shape;



	public Lbl(String text, Point2D pos, double angle, double origin, double width, double height) {
		this.text = text;
		position.set(pos);
		this.angle = angle;
		this.origin = origin;
		this.width = width;
		this.height = height;
		originalXY = new Point2D(pos.getX(), pos.getY());
	}

	public SimpleObjectProperty<Point2D> positionProperty() {
		return position;
	}


	public void setText(String text) {
		this.text = text;
	}


	public String getText() {
		return text;
	}


	public Point2D getPosition() {
		return position.get();
	}

	public void setPosition(Point2D position) {
		this.position.set(position);
	}





	public double getAngle() {
		return angle;
	}



	public double getOrigin() {
		return origin;
	}



	public double getWidth() {
		return width;
	}



	public double getHeight() {
		return height;
	}



	public Point2D getOriginalXY() {
		return originalXY;
	}


	public String getPrintablePosition() {
		DecimalFormat df = new DecimalFormat("0.00");
		StringBuilder sb = new StringBuilder();
		sb.append("x=");

		sb.append(df.format( FileProcessor.unit== Unit.IN? Util.mmToInch(position.get().getX()):position.get().getX()));
		sb.append(" ");
		sb.append("y=");
		sb.append(df.format( FileProcessor.unit==Unit.IN? Util.mmToInch(position.get().getY()):position.get().getY()));
		return sb.toString();
	}

	public void offsetLabelPosition(double xOffset, double yOffset) {

		position.set(new Point2D(position.get().getX() + xOffset, position.get().getY() + yOffset));


	}



	public void changeLabelPosition(Point2D newPoint) {

		position.set(newPoint);

	}



	/**
	 * etiket pozisyonu dosyadan okunandan farkli mi? Bizim tarafimizdan
	 * degistirilmis mi?
	 * 
	 * @return
	 */
	public boolean isLabelPositionChanged() {
		return position.get().distance(originalXY) != 0 ? true : false;
	}



	@Override
	public String toString() {

		return text + "\n" + "[x=" + String.format("%,.2f", position.get().getX())+" , y="+String.format("%,.2f", position.get().getY()) + "]";
	}


	public void setShape(ClosedShape shape) {
		this.shape = shape;
	}


	public ClosedShape getShape() {
		return shape;
	}
}
