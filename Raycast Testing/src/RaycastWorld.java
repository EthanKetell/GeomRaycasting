import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;


public class RaycastWorld {
	
	@SuppressWarnings("serial")
	private class RaycastPanel2D extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D baseG,worldG;
			baseG = (Graphics2D)g.create();
			baseG.setStroke(new BasicStroke(4));
			baseG.setColor(Color.BLACK);
			baseG.drawRect(0, 0, getWidth(), getHeight());
			baseG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			baseG.translate(this.getWidth()/2, this.getHeight()/2);
			baseG.scale(0.2, 0.2);
			baseG.rotate(-Math.PI/2);
			
			worldG = (Graphics2D)baseG.create();
			worldG.rotate(-player.direction);
			worldG.translate(-player.x, -player.y);
			worldG.setColor(Color.GRAY);
			worldG.fill(shape);
			worldG.setColor(Color.BLACK);
			worldG.draw(shape);
			worldG.setColor(Color.WHITE);
			worldG.setStroke(new BasicStroke(2));
			
			if(player.leftCast != null) {
				worldG.draw(new Line2D.Double(player.x,player.y,player.leftCast.point.getX(),player.leftCast.point.getY()));
			} else {
				worldG.draw(new Line2D.Double(player.x, player.y, player.x+1000*Math.cos(player.direction-player.FOV/2), player.y+1000*Math.sin(player.direction-player.FOV/2)));
			}
			if(player.rightCast != null) {
				worldG.draw(new Line2D.Double(player.x,player.y,player.rightCast.point.getX(),player.rightCast.point.getY()));
			} else {
				worldG.draw(new Line2D.Double(player.x, player.y, player.x+1000*Math.cos(player.direction+player.FOV/2), player.y+1000*Math.sin(player.direction+player.FOV/2)));
			}
			player.paint(baseG);
			
			
		}
	}
	
	@SuppressWarnings("serial")
	private class RaycastPanel3D extends JPanel {
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.setColor(new Color(0xaaddff));
			g.fillRect(0, 0, getWidth(), getHeight()/2);
			Graphics2D g2 = (Graphics2D)g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.translate(getWidth()/2, getHeight()/2);
			AffineTransform playerPos = new AffineTransform();
			playerPos.rotate(-player.direction);
			playerPos.translate(-player.x, -player.y);
			
			double ratio = (getWidth()/2)/Math.tan(player.FOV/2);			
			for(RaycastShape.Intersection wall : player.visibleWalls) {
				Point2D one,two;
				one = playerPos.transform(wall.side.getP1(), null);
				two = playerPos.transform(wall.side.getP2(), null);
				RaycastShape.Side relativeWall = new RaycastShape.Side(one,two);
				relativeWall.constrainToFOV(player.FOV);
				
				int x1 = (int)(ratio*relativeWall.getY1()/relativeWall.getX1());
				int y1 = (int)(20*getHeight()/relativeWall.getP1().distance(0, 0));
				int x2 = (int)(ratio*relativeWall.getY2()/relativeWall.getX2());
				int y2 = (int)(20*getHeight()/relativeWall.getP2().distance(0, 0));
				
				Polygon wallShape = new Polygon();
				wallShape.addPoint(x1, y1);
				wallShape.addPoint(x1, -y1);
				wallShape.addPoint(x2, -y2);
				wallShape.addPoint(x2, y2);
				
				g2.setColor(Color.LIGHT_GRAY);
				g2.fill(wallShape);
				g2.setColor(Color.BLACK);
				g2.draw(wallShape);
			}
		}
	}
	
	Timer clock;
	Player player;
	RaycastShape shape;
	RaycastPanel2D panel2D;
	RaycastPanel3D panel3D;
	
	public static void main(String[] args) {
		new RaycastWorld().start();
	}
	
	public RaycastWorld() {
		panel2D = new RaycastPanel2D();
		panel2D.setPreferredSize(new Dimension(128,96));
		panel2D.setBackground(Color.DARK_GRAY);
		panel3D = new RaycastPanel3D();
		panel3D.setPreferredSize(new Dimension(640,480));
		panel3D.setBackground(Color.DARK_GRAY);
		panel3D.add(panel2D);
		this.createWorld();
		player = new Player(this);
		Controller.removeKeyBind("left", KeyEvent.VK_LEFT);
		Controller.removeKeyBind("right", KeyEvent.VK_RIGHT);
		Controller.removeKeyBind("up", KeyEvent.VK_UP);
		Controller.removeKeyBind("down", KeyEvent.VK_DOWN);
		Controller.addKeyBind("cam_right", KeyEvent.VK_RIGHT);
		Controller.addKeyBind("cam_left", KeyEvent.VK_LEFT);
		Controller.addKeyBind("esc", KeyEvent.VK_ESCAPE);
	}
	
	private void createWorld() {
		shape = new RaycastShape();
		shape.addPoint(-100, -100);
		shape.addPoint(-100, 100);
		shape.addPoint(100, 100);
		shape.addPoint(100, -100);
		shape.addPoint(50, -100);
		shape.addPoint(50, -50);
		shape.addPoint(-50, -50);
		shape.addPoint(-50, -100);
	}
	
	public void start() {
		Controller.addListeners(panel2D);
		Controller.addListeners(panel3D);
		Controller.constrainMouseTo(panel3D);
		
		JFrame frame3D = new JFrame();
		frame3D.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame3D.add(panel3D);
		frame3D.pack();
		frame3D.setLocationRelativeTo(null);
		clock = new Timer(25, e-> {
			Controller.refresh();
			this.update();
			panel3D.repaint();
		});
		frame3D.setVisible(true);
		clock.start();
	}
	
	public void setTimerRunning(boolean running) {
		if(running != clock.isRunning()) {
			if(running) {
				clock.start();
			} else {
				clock.stop();
			}
		}
	}
	boolean locked = true;
	public void update() {
		player.update();
		if(Controller.controlPressed("esc")) {
			locked = !locked;
			if(locked) {
				Controller.constrainMouseTo(panel3D);
			} else {
				Controller.constrainMouseTo(null);
			}
		}
	}
	
}
