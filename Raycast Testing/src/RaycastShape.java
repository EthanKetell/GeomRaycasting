import java.awt.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("serial")
public class RaycastShape extends Polygon {
	
	private class PointIterator implements Iterable<Point2D> {
		@Override
		public Iterator<Point2D> iterator() {
			return new Iterator<Point2D>() {
				int i = 0;
				int[] x = xpoints,y = ypoints;

				public boolean hasNext() {
					return i < x.length;
				}
				
				public Point2D next() {
					return new Point2D.Double(x[i],y[i++]);
				}
			};
		}
	}
	
	public static class Side extends Line2D.Double {

		public Side(double x1, double y1, double x2, double y2) {
			super(x1,y1,x2,y2);
		}
		
		public Side(Point2D p1, Point2D p2) {
			super(p1.getX(),p1.getY(),p2.getX(),p2.getY());
		}
		
		public double length() {
			return getP1().distance(getP2());
		}
		
		public double slope() {
			return (this.getY2()-this.getY1())/(this.getX2()-this.getX1());
		}
		
		private Point2D getIntersection(double direction) {
			double m1 = this.slope();
			double m2 = Math.tan(direction);
			if(m1 == m2) {
				return null;
			} else if(java.lang.Double.isInfinite(m1)){
				double x = this.getX1();
				double y = m2*x;
				return new Point2D.Double(x, y);
			} else {
				double b1 = this.getY1() - m1*this.getX1();
				double x = b1/(m2-m1);
				double y = m2*x;
				return new Point2D.Double(x, y);
			}
		}
		
		public void constrainToFOV(double radians) {
			Point2D p1 = getP1();
			Point2D p2 = getP2();
			double theta;
			
			if(p1.getX() < 0) {
				if(getIntersection(Math.PI/2).getY() < 0) {
					p1 = getIntersection(-radians/2);
				} else {
					p1 = getIntersection(radians/2);
				}
			} else {
				theta = Math.atan2(p1.getY(), p1.getX());
				if(theta < -radians/2) {
					p1 = getIntersection(-radians/2);
				} else if(theta > radians/2) {
					p1 = getIntersection(radians/2);
				}
			}
			
			if(p2.getX() < 0) {
				if(getIntersection(Math.PI/2).getY() < 0) {
					p2 = getIntersection(-radians/2);
				} else {
					p2 = getIntersection(radians/2);
				}
			} else {
				theta = Math.atan2(p2.getY(), p2.getX());
				if(theta < -radians/2) {
					p2 = getIntersection(-radians/2);
				} else if(theta > radians/2) {
					p2 = getIntersection(radians/2);
				}
			}
			this.setLine(p1, p2);
		}
		
	}
	
	public class Intersection {
		public Point2D point;
		public Side side;
		Intersection(Point2D point, Side side) {
			this.point = point;
			this.side = side;
		}
	}
	
	public List<Side> sides = new ArrayList<Side>();
	public PointIterator points = new PointIterator();
	
	@Override
	public void addPoint(int x, int y) {
		super.addPoint(x, y);
		if(sides.size() > 0) {
			sides.get(sides.size()-1).x2 = x;
			sides.get(sides.size()-1).y2 = y;
		}
		if(this.npoints >= 1) {
			sides.add(new Side(x,y,this.xpoints[0],this.ypoints[0]));
		}
		
	}
	
	public Intersection cast(Point2D start, Point2D towards) {
		double direction = Math.atan2(towards.getY()-start.getY(), towards.getX()-start.getX());
		return this.cast(start, direction);
	}
	
	public Intersection cast(Point2D start, double direction, Side line) {
		Line2D ray = new Line2D.Double(
				start.getX(),
				start.getY(),
				start.getX()+1000*Math.cos(direction),
				start.getY()+1000*Math.sin(direction));
		Point2D intersect = this.getIntersection(ray, line);
		if(intersect != null) {
			return new Intersection(intersect,line);
		} else {
			return null;
		}
	}
	
	public Intersection cast(Point2D start, double direction) {
		Line2D ray = new Line2D.Double(
				start.getX(),
				start.getY(),
				start.getX()+1000*Math.cos(direction),
				start.getY()+1000*Math.sin(direction));
		double closestDist = Double.MAX_VALUE;
		Point2D closestPoint = null;
		Side closestSide = null;
		for(Side side : sides) {
			if(ray.intersectsLine(side)) {
				Point2D intersection = getIntersection(ray,side);
				double dist = start.distanceSq(intersection);
				if(dist < closestDist) {
					closestDist = dist;
					closestSide = side;
					closestPoint = intersection;
				}
			}
		}
		if(closestSide != null) {
			return new Intersection(closestPoint,closestSide);
		} else {
			return null;
		}
	}
	
	public Point2D getIntersection(Line2D line1, Line2D line2) {
		if(!line1.intersectsLine(line2)) {
			return null;
		}
		double m1 = (line1.getY2()-line1.getY1())/(line1.getX2()-line1.getX1());
		double m2 = (line2.getY2()-line2.getY1())/(line2.getX2()-line2.getX1());
		if(Double.isInfinite(m1)) {
			return new Point2D.Double(line1.getX1(), line2.getY1()+m2*(line1.getX1()-line2.getX1()));
		} else if(Double.isInfinite(m2)) {
			return new Point2D.Double(line2.getX1(), line1.getY1()+m1*(line2.getX1()-line1.getX1()));
		} else {
			double b1 = line1.getY1()-line1.getX1()*m1;
			double b2 = line2.getY1()-line2.getX1()*m2;
			// m1x+b1=m2x+b2
			// m1x-m2x=b2-b1
			// x = (b2-b1)/(m1-m2)
			double x = (b2-b1)/(m1-m2);
			double y = m1*x+b1;
			return new Point2D.Double(x, y);
		}
	}
	
}
