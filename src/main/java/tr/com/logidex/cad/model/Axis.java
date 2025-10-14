package tr.com.logidex.cad.model;

import javafx.geometry.Point2D;

     enum Axis {
    X {
        @Override
        double getCoordinate(Point2D point) {
            return point.getX();
        }

        @Override
        Point2D setCoordinate(Point2D point, double value) {
            return new Point2D(value, point.getY());
        }

        @Override
        Point2D move(Point2D point, int delta) {
            return new Point2D(point.getX() + delta, point.getY());
        }
    },
    Y {
        @Override
        double getCoordinate(Point2D point) {
            return point.getY();
        }

        @Override
        Point2D setCoordinate(Point2D point, double value) {
            return new Point2D(point.getX(), value);
        }

        @Override
        Point2D move(Point2D point, int delta) {
            return new Point2D(point.getX(), point.getY() + delta);
        }
    };

    abstract double getCoordinate(Point2D point);
    abstract Point2D setCoordinate(Point2D point, double value);
    abstract Point2D move(Point2D point, int delta);
}