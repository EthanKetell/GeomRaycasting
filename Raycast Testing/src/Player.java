import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class Player {
	
	public final double FOV = Math.PI/3;
	
	private RaycastWorld world;
	public double x, y, direction;
	public Shape shape;
	
	SortedSet<RaycastShape.Intersection> visibleWalls;
	RaycastShape.Intersection leftCast,rightCast;
	
	@SuppressWarnings("serial")
	public Player(RaycastWorld world) {
		this.shape = new Ellipse2D.Double(-5,-5,10,10);
		this.world = world;
		visibleWalls = new TreeSet<RaycastShape.Intersection>(new Comparator<RaycastShape.Intersection>() {
			@Override
			public int compare(RaycastShape.Intersection o1, RaycastShape.Intersection o2) {
				Point2D pos = new Point2D.Double(x, y);
				return Double.compare(o2.point.distanceSq(pos), o1.point.distanceSq(pos));
			}
			
		}) {
			Set<RaycastShape.Side> lines = new HashSet<RaycastShape.Side>();
			@Override
			public boolean add(RaycastShape.Intersection toAdd) {
				if(toAdd != null && lines.add(toAdd.side)) {
					return super.add(toAdd);
				} else {
					return false;
				}
			}
			@Override
			public void clear() {
				super.clear();
				lines.clear();
			}
		};
	}
	
	public void paint(Graphics2D g) {
		g.setColor(Color.WHITE);
		g.fill(shape);
	}
	
	public void update() {
		if(Controller.controlDown("up")) {
			this.x += 2*Math.cos(direction);
			this.y += 2*Math.sin(direction);
		}
		if(Controller.controlDown("down")) {
			this.x -= 2*Math.cos(direction);
			this.y -= 2*Math.sin(direction);
		}
		if(Controller.controlDown("left")) {
			this.x += 2*Math.cos(direction-Math.PI/2);
			this.y += 2*Math.sin(direction-Math.PI/2);
		}
		if(Controller.controlDown("right")) {
			this.x += 2*Math.cos(direction+Math.PI/2);
			this.y += 2*Math.sin(direction+Math.PI/2);
		}
		
		if(Controller.controlDown("cam_left")) {
			this.direction -= 0.05;
		}
		if(Controller.controlDown("cam_right")) {
			this.direction += 0.05;
		}
		this.direction += Controller.mouseMoveX()/250;
		this.raycast();
	}
	
	private class SegmentedLine implements Iterable<Point2D> {
		
		private int segments;
		private double startX, startY, dx, dy;
		
		SegmentedLine(Point2D p1, Point2D p2, int segments) {
			startX = p1.getX();
			startY = p1.getY();
			dx = p2.getX()-startX;
			dy = p2.getY()-startY;
			this.segments = segments;
		}
		public Iterator<Point2D> iterator() {
			return new Iterator<Point2D>() {
				int i = 0;
				public boolean hasNext() {
					return i < segments;
				}

				@Override
				public Point2D next() {
					return new Point2D.Double(startX+dx*i/segments, startY+dy*(i++)/segments);
				}
				
			};
		}
	}
	
	private void raycast() {
		visibleWalls.clear();
		Area vision = new Area();
		double leftTheta = direction-Math.PI/6;
		double rightTheta = direction+Math.PI/6;
		Point2D pos = new Point2D.Double(x,y);
		leftCast = world.shape.cast(pos, leftTheta);
		rightCast = world.shape.cast(pos, rightTheta);
		
		Point2D leftBound = new Point2D.Double(x+20*Math.cos(leftTheta), y+20*Math.sin(leftTheta));
		Point2D rightBound = new Point2D.Double(x+20*Math.cos(rightTheta), y+20*Math.sin(rightTheta));
		for(Point2D point : new SegmentedLine(leftBound,rightBound,100)) {
			RaycastShape.Intersection toAdd = world.shape.cast(pos, point);
			if(toAdd != null) {
				Polygon shapeAdd = new Polygon();
				shapeAdd.addPoint((int)x, (int)y);
				shapeAdd.addPoint((int)toAdd.side.getP1().getX(), (int)toAdd.side.getP1().getY());
				shapeAdd.addPoint((int)toAdd.side.getP2().getX(), (int)toAdd.side.getP2().getY());
				vision.add(new Area(shapeAdd));
				visibleWalls.add(toAdd);
			}
		}
	}

}
