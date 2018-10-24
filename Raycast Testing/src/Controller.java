

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class Controller {
	
	private static class ControllerListener implements MouseListener, MouseMotionListener, MouseWheelListener, KeyListener {
		@Override
		public void keyTyped(KeyEvent e) {}
		
		@Override
		public void keyPressed(KeyEvent e) {
			int code = e.getKeyCode();
			if(!keysDown.contains(code)) {
				newKeysDown.add(code);
			}
		}
		@Override
		public void keyReleased(KeyEvent e) {
			int code = e.getKeyCode();
			keysReleased.add(code);
		}
		@Override
		public void mouseDragged(MouseEvent e) {
			dragDX += mousePos.getX()-e.getX();
			dragDY += mousePos.getY()-e.getY();
			mousePos = e.getPoint();
		}
		@Override
		public void mouseMoved(MouseEvent e) {
			mousePos = e.getPoint();
		}
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {
			mousePos = e.getPoint();
			if(!mouseButtonsDown.contains(e.getButton())) {
				newMouseButtonsDown.add(e.getButton());
			}
		}
		@Override
		public void mouseReleased(MouseEvent e) {
			mousePos = e.getPoint();
			mouseButtonsReleased.add(e.getButton());
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			mousePos = e.getPoint();
			
		}
		@Override
		public void mouseExited(MouseEvent e) {
			mousePos = e.getPoint();
			
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			mousePos = e.getPoint();
			dScroll += e.getPreciseWheelRotation();
		}
	}

	public static ControllerListener listener = new ControllerListener();
	
	private static HashMap<Integer,Set<String>> controls = new HashMap<Integer,Set<String>>();
	static {
		addKeyBind("UP",KeyEvent.VK_UP);
		addKeyBind("DOWN",KeyEvent.VK_DOWN);
		addKeyBind("LEFT",KeyEvent.VK_LEFT);
		addKeyBind("RIGHT",KeyEvent.VK_RIGHT);
		addKeyBind("UP",KeyEvent.VK_W);
		addKeyBind("DOWN",KeyEvent.VK_S);
		addKeyBind("LEFT",KeyEvent.VK_A);
		addKeyBind("RIGHT",KeyEvent.VK_D);
		addKeyBind("SPACE",KeyEvent.VK_SPACE);
	}
	
	private static Set<String> 
			controlsDown = new HashSet<String>(),
			controlsPressed = new HashSet<String>();
	private static Set<Integer>
			keysDown = new HashSet<Integer>(),
			keysPressed = new HashSet<Integer>(),
			newKeysDown = new HashSet<Integer>(),
			keysReleased = new HashSet<Integer>(),
			mouseButtonsDown = new HashSet<Integer>(),
			mouseButtonsPressed = new HashSet<Integer>(),
			newMouseButtonsDown = new HashSet<Integer>(),
			mouseButtonsReleased = new HashSet<Integer>();
	
	private static Point
			mousePos = new Point(0,0),
			prevMouseState = new Point(0,0),
			currMouseState = new Point(0,0);
	private static double scroll,dScroll,dragX,dragY,dragDX,dragDY;
	private static Robot robot;
	
	private static Component mouseLockTarget;
	
	public static void addListeners(Component comp) {
		comp.setFocusable(true);
		comp.addKeyListener(listener);
		comp.addMouseListener(listener);
		comp.addMouseMotionListener(listener);
		comp.addMouseWheelListener(listener);
	}
	
	/**
	 * Updates the lists of keys storing new presses, held keys, and released keys.
	 */
	static void refresh() {
		keysDown.addAll(newKeysDown);
		keysPressed = newKeysDown;
		newKeysDown = new HashSet<Integer>();
		keysDown.removeAll(keysReleased);
		keysReleased.clear();
		
		controlsDown.clear();
		for(int key : keysDown) {
			if(controls.containsKey(key)) controlsDown.addAll(controls.get(key));
		}
		controlsPressed.clear();
		for(int key : keysPressed) {
			if(controls.containsKey(key)) controlsPressed.addAll(controls.get(key));
		}
		
		if(mouseLockTarget != null) {
			if(robot == null) {
				try {
					robot = new Robot();
				} catch (AWTException e) {
					System.err.println("Operating System does not allow constraining mouse");
					e.printStackTrace();
				}
			}
			Point onScreen = mouseLockTarget.getLocationOnScreen();
			Dimension size = mouseLockTarget.getSize();
			prevMouseState = new Point(size.width/2,size.height/2);
			robot.mouseMove(onScreen.x+size.width/2, onScreen.y+size.height/2);
		} else {
			prevMouseState = currMouseState;
		}
		currMouseState = mousePos;
		
		scroll = dScroll;
		dScroll = 0;
		dragX = dragDX;
		dragDX = 0;
		dragY = dragDY;
		dragDY = 0;
		mouseButtonsDown.addAll(newMouseButtonsDown);
		mouseButtonsPressed = newMouseButtonsDown;
		newMouseButtonsDown = new HashSet<Integer>();
		keysDown.removeAll(keysReleased);
		keysReleased.clear();
	}
	
	/****************************************************************************/
	/****************************************************************************/
	/**************************** BEGIN MOUSE STUFF *****************************/
	/****************************************************************************/
	/****************************************************************************/
	
	private static Cursor noCursor = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB), new Point(0,0), "blank");
	private static FocusListener focus = new FocusListener() {
		
		@Override
		public void focusGained(FocusEvent e) {
			mouseLockTarget = e.getComponent();
			e.getComponent().setCursor(noCursor);
		}

		@Override
		public void focusLost(FocusEvent e) {
			mouseLockTarget = null;
			e.getComponent().setCursor(null);
		}
		
	};
	public static void constrainMouseTo(Component target) {
		if(mouseLockTarget != null) {
			mouseLockTarget.removeFocusListener(focus);
			mouseLockTarget.setCursor(null);
		}
		mouseLockTarget = target;
		if(target != null) {
			target.addFocusListener(focus);
			target.setCursor(noCursor);
		}
	}
	
	public static Point mousePos() {
		return currMouseState;
	}
	
	public static double mouseMoveX() {
		return currMouseState.getX() - prevMouseState.getX();
	}
	
	public static double mouseMoveY() {
		return currMouseState.getY() - prevMouseState.getY();
	}
	
	public static boolean mouseButtonDown(int button) {
		return mouseButtonsDown.contains(button);
	}
	
	public static boolean mouseButtonPressed(int button) {
		return mouseButtonsPressed.contains(button);
	}
	
	public static double mouseScroll() {
		return scroll;
	}
	
	public static double mouseDragX() {
		return dragX;
	}
	
	public static double mouseDragY() {
		return dragY;
	}
	
	/****************************************************************************/
	/****************************************************************************/
	/*************************** BEGIN KEYBOARD STUFF ***************************/
	/****************************************************************************/
	/****************************************************************************/
	
	/**
	 * Binds the specified key to the named control
	 * </br></br>
	 * More accurately, makes it so that if the specified key is pressed, the named control will be added to the lists
	 * for {@linkplain Controller#controlDown(String)} and {@linkplain Controller#controlPressed(String)}
	 * @param control The control to add to the the key
	 * @param keyCode The key code of the key to bind
	 */
	public static void addKeyBind(String control, int keyCode) {
		if(!controls.containsKey(keyCode)) {
			controls.put(keyCode, new HashSet<String>());
		}
		controls.get(keyCode).add(control.toUpperCase());
	}
	
	/**
	 * Removes the named control from the specified key
	 * @param control The control to remove from the key
	 * @param keyCode The key code of the key to unbind
	 */
	public static void removeKeyBind(String control, int keyCode) {
		if(controls.containsKey(keyCode)) {
			Set<String> keyBinds = controls.get(keyCode);
			keyBinds.remove(control.toUpperCase());
			if(keyBinds.size() <= 0) {
				controls.remove(keyCode);
			}
		}
	}
	
	/**
	 * Removes all key bindings (including defaults)
	 */
	public static void clearKeyBindings() {
		controls.clear();
	}
	
	/**
	 * Returns whether any key has more than one binding
	 * @return whether any key has more than one binding
	 */
	private static boolean hasMultiBinding() {
		for(Set<String> set : controls.values()) {
			if(set.size() > 1) {
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Returns whether any binding has more than one key
	 * @return whether any binding has more than one key
	 */
	private static boolean hasMultiKeys() {
		Set<String> binds = new HashSet<String>();
		for(Set<String> set : controls.values()) {
			for(String str : set) {
				if(binds.contains(str)) {
					return true;
				} else {
					binds.add(str);
				}
			}
		}
		return false;
	}
	
	/**
	 * Displays all current key bindings
	 * @see Controller#printKeyBinds(boolean)
	 */
	public static void printKeyBinds() {
		printKeyBinds(!hasMultiBinding() && hasMultiKeys());
	}
	
	/**
	 * Displays all current key bindings, sorted by bindings if specified, else sorted by keys
	 * @param sortByBind whether to sort by bindings
	 */
	public static void printKeyBinds(boolean sortByBind) {
		class Binding implements Comparable<Binding>{
			String key,bind;
			Binding(String key, String bind){
				this.key = key;
				this.bind = bind;
			}
			@Override
			public int compareTo(Binding other) {
				if(sortByBind) {
					return this.bind.compareTo(other.bind);
				} else {
					return this.key.compareTo(other.key);
				}
			}
		}
		List<Binding> toPrint = new ArrayList<Binding>();
		for(Map.Entry<Integer, Set<String>> entry : controls.entrySet()) {
			String key = entry.getKey()+" ("+KeyEvent.getKeyText(entry.getKey())+")";
			for(String bind : entry.getValue()) {
				toPrint.add(new Binding(key,bind));
			}
		}
		int width = 0;
		for(Binding s : toPrint) {
			width = Math.max(width, Math.max(s.key.length(), s.bind.length())+1);
		}
		Collections.sort(toPrint);
		System.out.println(String.format("|%"+width+"s|%-"+width+"s|","Key","Bind"));
		for(Binding b : toPrint) {
			System.out.println(String.format("|%-"+width+"s|%"+width+"s|", b.key, b.bind));
		}
	}
	
	/**
	 * Returns whether the key corresponding to the specified key code is currently down. This returns
	 * {@code true} as long as the key is down.
	 * </br></br><b>Note:</b> This checks all keys, not just those with {@linkplain Controller#addKeyBind(String, int) assigned} controls
	 * @param keyCode The key code to check
	 * @return Whether the specified key is down
	 * @see Controller#keyPressed(int)
	 * @see Controller#controlDown(String)
	 */
	public static boolean keyDown(int keyCode) {
		return keysDown.contains(keyCode);
	}
	
	/**
	 * Returns whether the key corresponding to the specified key code was pressed since the last refresh. This returns
	 * {@code true} as long as the key is down.
	 * </br></br><b>Note:</b> This checks all keys, not just those with {@linkplain Controller#addKeyBind(String, int) assigned} controls
	 * @param keyCode The key code to check
	 * @return Whether the specified key has been pressed
	 * @see Controller#keyDown(int)
	 * @see Controller#controlPressed(String)
	 */
	public static boolean keyPressed(int keyCode) {
		return keysPressed.contains(keyCode);
	}
	
	/**
	 * Returns whether the specified control is currently being held. This returns {@code true} as long as the key is down.
	 * @param control The name of the control to check
	 * @return Whether the named control is being held
	 * @see {@linkplain Controller#registerKeyInput(String, int)} to register controls
	 * @see Controller#controlPressed(String)
	 * @see Controller#keyDown(int)
	 */
	public static boolean controlDown(String control) {
		return controlsDown.contains(control.toUpperCase());
	}
	
	/**
	 * Returns whether the specified control was pressed since the last refresh. This returns {@code true} only once per press of a given key.
	 * @param control The name of the control to check
	 * @return Whether the named control has been pressed
	 * @see {@linkplain Controller#registerKeyInput(String, int)} to register controls
	 * @see Controller#controlDown(String)
	 * @see Controller#keyPressed(int)
	 */
	public static boolean controlPressed(String control) {
		return controlsPressed.contains(control.toUpperCase());
	}

}
