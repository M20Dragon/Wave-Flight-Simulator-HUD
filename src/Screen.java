import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class Screen extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener
{

    //ArrayList of all the 3D polygons - each 3D polygon has a 2D 'PolygonObject' inside called 'DrawablePolygon'
    static ArrayList<DPolygon> DPolygons = new ArrayList<DPolygon>();

    //The polygon that the mouse is currently over
    static PolygonObject PolygonOver = null;

    //Used for keeping mouse in center
    Robot r;


    static double[] ViewFrom = new double[] {10, 10, 10},/**Starting position(x,y,z)..... for z(add 2 0's to get altitude, ex(10 = 1000 and 15 = 1500))**/
            ViewTo = new double[] {0, 0, 0},
            LightDir = new double[] {1, 1, 1};

    static boolean DeveloperMode = false;

    //The smaller the zoom the more zoomed out you are and visa versa, although altering too far from 1000 will make it look pretty weird
    static double zoom = 1000, MinZoom = 500, MaxZoom = 2500, MouseX = 0, MouseY = 0, MovementSpeed = 0.010; /** Movement Speed will be .020 at max throttle, .0 at 50% throttle and .001 at 10 % throttle and so on**/

    //FPS is a bit primitive, you can set the MaxFPS as high as u want
    double drawFPS = 0, MaxFPS = 140, SleepTime = 1000.0/MaxFPS, LastRefresh = 0, StartTime = System.currentTimeMillis(), LastFPSCheck = 0, Checks = 0;
    //VertLook goes from 0.999 to -0.999, minus being looking down and + looking up, HorLook takes any number and goes round in radians
    //aimSight changes the size of the center-cross. The lower HorRotSpeed or VertRotSpeed, the faster the camera will rotate in those directions
    double VertLook = 0, HorLook = 0, RollLook = 0, aimSight = 7, HorRotSpeed = 72000, VertRotSpeed = 176000, RollRotSpeed = 176000, SunPos = 0;

    //will hold the order that the polygons in the ArrayList DPolygon should be drawn meaning DPolygon.get(NewOrder[0]) gets drawn first
    int[] NewOrder;

    //for program wide use of mouse vars
    public double difX,difY;

    public boolean pitchUp = false, pitchDown = false, yawLeft = false, yawRight = false;

    //for adjustment of z cord in altitude
    public boolean underCall = false;

    static boolean OutLines = false;
    boolean[] Keys = new boolean[4];
    public JLabel devModeLabel;
    public JLabel cordLabel;
    double lastMovementSpeed;

    long repaintTime = 0;

    public Screen()
    {
        this.addKeyListener(this);
        setFocusable(true);

        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);

        invisibleMouse();
        new GenerateTerrain();

        //Label to show that developer mode is on
        devModeLabel = new JLabel();
        devModeLabel.setForeground(Color.RED);
        devModeLabel.setSize(220,40);
        add(devModeLabel);
        devModeLabel.setVisible(false);

        //Label to show the x,y and z coordinates
        cordLabel = new JLabel();
        cordLabel.setForeground(Color.green);
        cordLabel.setSize(200,40);
        add(cordLabel);
        cordLabel.setVisible(true);
    }

    public void paintComponent(Graphics g)
    {
        //Clear screen and draw background color
        g.setColor(new Color(108, 174, 180)); /** Sets the color for the background. Set to Sky Blue for blue Sky **/
        g.fillRect(0, 0, (int)DDDTutorial.ScreenSize.getWidth(), (int)DDDTutorial.ScreenSize.getHeight());

        //calls the camera movement method
        CameraMovement();

        //Calculated all that is general for this camera position
        Calculator.SetPrederterminedInfo();

        ControlSunAndLight();

        //Updates each polygon for this camera position
        for(int i = 0; i < DPolygons.size(); i++)
            DPolygons.get(i).updatePolygon();

        //Set drawing order so closest polygons gets drawn last
        setOrder();

        //Set the polygon that the mouse is currently over
        setPolygonOver();

        //draw polygons in the Order that is set by the 'setOrder' function
        for(int i = 0; i < NewOrder.length; i++)
            DPolygons.get(NewOrder[i]).DrawablePolygon.drawPolygon(g);

        //draw the cross in the center of the screen
        drawMouseAim(g);

        //draw the marker in the center
        drawCenterMarker(g);

        //REFRESH HUD

        /////////////

        //FPS display
        g.drawString("FPS: " + (int)drawFPS, 40, 40);

        //Prints out Developer mode if activated
        if(DeveloperMode)
        {
            devModeLabel.setLocation((int) DDDTutorial.ScreenSize.getWidth() - 200, 10);
            devModeLabel.setText("Developer Mode");
        }

        //Sets the location of the cord label as well as showing to cord label
        cordLabel.setLocation((int)DDDTutorial.ScreenSize.getWidth()-220,(int)DDDTutorial.ScreenSize.getHeight()-40);
        if(!DeveloperMode)
        {
            if(underCall)
            {
                if(VertLook < 0)
                cordLabel.setText("X: " + String.format("%.3f", ViewFrom[0]) + "  " + "Y: " + String.format("%.3f", ViewFrom[1]) + "  " + "Z: " + String.format("%.3f", Math.abs(ViewFrom[2]) - 4));
            }
                else
                cordLabel.setText("X: " + String.format("%.3f", ViewFrom[0]) + "  " + "Y: " + String.format("%.3f", ViewFrom[1]) + "  " + "Z: " + String.format("%.3f", Math.abs(ViewFrom[2])));
        }
        SleepAndRefresh();
    }
    void setOrder()
    {
        double[] k = new double[DPolygons.size()];
        NewOrder = new int[DPolygons.size()];

        for(int i=0; i<DPolygons.size(); i++)
        {
            k[i] = DPolygons.get(i).AvgDist;
            NewOrder[i] = i;
        }

        double temp;
        int tempr;
        for (int a = 0; a < k.length-1; a++)
            for (int b = 0; b < k.length-1; b++)
                if(k[b] < k[b + 1])
                {
                    temp = k[b];
                    tempr = NewOrder[b];
                    NewOrder[b] = NewOrder[b + 1];
                    k[b] = k[b + 1];

                    NewOrder[b + 1] = tempr;
                    k[b + 1] = temp;
                }
    }

    void invisibleMouse()
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT);
        Cursor invisibleCursor = toolkit.createCustomCursor(cursorImage, new Point(0,0), "InvisibleCursor");
        setCursor(invisibleCursor);
    }

    void drawMouseAim(Graphics g)
    {
        if(!DeveloperMode)
        {
            g.setColor(Color.green);
            g.drawLine((int)MouseX-6,(int)MouseY,(int)MouseX+6,(int)MouseY);
            g.drawLine((int)MouseX,(int)MouseY-6,(int)MouseX,(int)MouseY+6);
        }
        if(DeveloperMode)
        {
            g.setColor(Color.green);
            g.drawLine((int) (DDDTutorial.ScreenSize.getWidth() / 2 - aimSight), (int) (DDDTutorial.ScreenSize.getHeight() / 2), (int) (DDDTutorial.ScreenSize.getWidth() / 2 + aimSight), (int) (DDDTutorial.ScreenSize.getHeight() / 2));
            g.drawLine((int) (DDDTutorial.ScreenSize.getWidth() / 2), (int) (DDDTutorial.ScreenSize.getHeight() / 2 - aimSight), (int) (DDDTutorial.ScreenSize.getWidth() / 2), (int) (DDDTutorial.ScreenSize.getHeight() / 2 + aimSight));
        }
    }

    void drawCenterMarker(Graphics g)
    {
        if(!DeveloperMode)
        {
            g.drawOval((int)DDDTutorial.ScreenSize.getWidth()/2-10,(int)DDDTutorial.ScreenSize.getHeight()/2,20,20);
            g.drawLine((int)DDDTutorial.ScreenSize.getWidth()/2+10,(int)DDDTutorial.ScreenSize.getHeight()/2+10,(int)DDDTutorial.ScreenSize.getWidth()/2+20,(int)DDDTutorial.ScreenSize.getHeight()/2+20);
            g.drawLine((int)DDDTutorial.ScreenSize.getWidth()/2-10,(int)DDDTutorial.ScreenSize.getHeight()/2+10,(int)DDDTutorial.ScreenSize.getWidth()/2-20,(int)DDDTutorial.ScreenSize.getHeight()/2+20);
            g.drawLine((int)DDDTutorial.ScreenSize.getWidth()/2+20,(int)DDDTutorial.ScreenSize.getHeight()/2+20,(int)DDDTutorial.ScreenSize.getWidth()/2+35,(int)DDDTutorial.ScreenSize.getHeight()/2+20);
            g.drawLine((int)DDDTutorial.ScreenSize.getWidth()/2-20,(int)DDDTutorial.ScreenSize.getHeight()/2+20,(int)DDDTutorial.ScreenSize.getWidth()/2-35,(int)DDDTutorial.ScreenSize.getHeight()/2+20);
        }
    }

    void SleepAndRefresh()
    {
        //System.out.println("vertical: " + VertLook + "       horizontal: " + HorLook);
        //makes plane go forward
        if(!DeveloperMode)
            Keys[0] = true;
        if(DeveloperMode)
            Keys[0] = false;
        ////////////////////////

        //Keeps the current value of turning current
        if(!DeveloperMode)
            {
                if(difX>0)
                {
                    HorLook += difX / HorRotSpeed;
                }
                if(difX<0)
                {
                    HorLook += difX / HorRotSpeed;
                }
                if(difY<0)
                {
                    VertLook += difY / VertRotSpeed;
                }
                if(difY>0)
                {
                    VertLook += difY / VertRotSpeed;
                }
            }
        ////////////////////////////////////////////

        //Handler if .999 < VertLook < -.999
        if(!DeveloperMode)
        {
            if(VertLook > .999)
            {
                ViewFrom[2] = ViewFrom[2] - ((ViewFrom[2]*2)-4);
                VertLook = -.999;
                underCall = false;
            }
            if(VertLook < -.999)
            {
                ViewFrom[2] = ViewFrom[2] - ((ViewFrom[2]*2)-4);
                VertLook = .999;
                underCall = true;
            }
        }
        ////////////////////////////////////

        long timeSLU = (long) (System.currentTimeMillis() - LastRefresh);

        Checks ++;
        if(Checks >= 15)
        {
            drawFPS = Checks/((System.currentTimeMillis() - LastFPSCheck)/1000.0);
            LastFPSCheck = System.currentTimeMillis();
            Checks = 0;
        }

        if(timeSLU < 1000.0/MaxFPS)
        {
            try
            {
                Thread.sleep((long) (1000.0/MaxFPS - timeSLU));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LastRefresh = System.currentTimeMillis();

        repaint();
    }

    void ControlSunAndLight()
    {
        SunPos += 0.005;
        double mapSize = GenerateTerrain.mapSize * GenerateTerrain.Size;
        LightDir[0] = mapSize/2 - (mapSize/2 + Math.cos(SunPos) * mapSize * 10);
        LightDir[1] = mapSize/2 - (mapSize/2 + Math.sin(SunPos) * mapSize * 10);
        LightDir[2] = -200;
    }

    void CameraMovement()
    {
        Vector ViewVector = new Vector(ViewTo[0] - ViewFrom[0], ViewTo[1] - ViewFrom[1], ViewTo[2] - ViewFrom[2]);
        double xMove = 0, yMove = 0, zMove = 0;
        Vector VerticalVector = new Vector (0, 0, 1);
        Vector SideViewVector = ViewVector.CrossProduct(VerticalVector);

        if(Keys[0])
        {
            xMove += ViewVector.x ;
            yMove += ViewVector.y ;
            zMove += ViewVector.z ;
        }

        if(Keys[2])
        {
            xMove -= ViewVector.x ;
            yMove -= ViewVector.y ;
            zMove -= ViewVector.z ;
        }

        if(Keys[1])
        {
            xMove += SideViewVector.x ;
            yMove += SideViewVector.y ;
            zMove += SideViewVector.z ;
        }

        if(Keys[3])
        {
            xMove -= SideViewVector.x ;
            yMove -= SideViewVector.y ;
            zMove -= SideViewVector.z ;
        }
        Vector MoveVector = new Vector(xMove, yMove, zMove);
        MoveTo(ViewFrom[0] + MoveVector.x * MovementSpeed, ViewFrom[1] + MoveVector.y * MovementSpeed, ViewFrom[2] + MoveVector.z * MovementSpeed);
    }

    void MoveTo(double x, double y, double z)
    {
        ViewFrom[0] = x;
        ViewFrom[1] = y;
        ViewFrom[2] = z;
        updateView();
    }

    void setPolygonOver()
    {
        if(DeveloperMode)
        {
            PolygonOver = null;
            for (int i = NewOrder.length - 1; i >= 0; i--)
                if (DPolygons.get(NewOrder[i]).DrawablePolygon.MouseOver() && DPolygons.get(NewOrder[i]).draw
                        && DPolygons.get(NewOrder[i]).DrawablePolygon.visible)
                {
                    PolygonOver = DPolygons.get(NewOrder[i]).DrawablePolygon;
                    break;
                }
        }
    }

    void MouseMovement(double NewMouseX, double NewMouseY)
    {
        difX = (NewMouseX - DDDTutorial.ScreenSize.getWidth()/2);
        difY = (NewMouseY - DDDTutorial.ScreenSize.getHeight()/2);
        //difY *= 6 - Math.abs(VertLook) * 5; //makes the camera faster;
       if(!DeveloperMode)
        VertLook += difY  / VertRotSpeed; //put -= to be inverted;
        HorLook += difX / HorRotSpeed;
       if(DeveloperMode)
            VertLook -= difY  /  VertRotSpeed;
            HorLook += Math.sin(difX) / HorRotSpeed;

        if(!DeveloperMode)
        {
            if(VertLook > 1.993)
            {
                VertLook = -1.992;
            }

            if(VertLook < -1.993)
            {
                VertLook = 1.992;
            }
        }
        else if(DeveloperMode)
        {
            if (VertLook > 0.999)
                VertLook = 0.999;

            if (VertLook < -0.999)
                VertLook = -0.999;
        }

        updateView();
    }

    void updateView()
    {
        double r = Math.sqrt(1 - (VertLook * VertLook));
        ViewTo[0] = ViewFrom[0] + r * Math.cos(HorLook);
        ViewTo[1] = ViewFrom[1] + r * Math.sin(HorLook);
        ViewTo[2] = ViewFrom[2] + VertLook;
    }

    void CenterMouse()
    {
        if(DeveloperMode)
        {
            try
            {
                r = new Robot();
                r.mouseMove((int) DDDTutorial.ScreenSize.getWidth() / 2, (int) DDDTutorial.ScreenSize.getHeight() / 2);
            } catch (AWTException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void keyPressed(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_W)
            if(DeveloperMode)
            {
                Keys[0] = true;
            }
        if(e.getKeyCode() == KeyEvent.VK_A)
            if(DeveloperMode)
            {
                Keys[1] = true;
            }
        if(e.getKeyCode() == KeyEvent.VK_S)
            if(DeveloperMode)
            {
                Keys[2] = true;
            }
        if(e.getKeyCode() == KeyEvent.VK_D)
            if(DeveloperMode)
            {
                Keys[3] = true;
            }
        if(e.getKeyCode() == KeyEvent.VK_O)
        {
            if(!DeveloperMode)
            {
                DeveloperMode = true;
                devModeLabel.setVisible(true);
                lastMovementSpeed = MovementSpeed;
                HorRotSpeed = 900;
                VertRotSpeed = 2200;
                MovementSpeed = 0.5;
            }
            else if(DeveloperMode)
            {
                DeveloperMode = false;
                devModeLabel.setVisible(false);
                MovementSpeed = lastMovementSpeed;
                HorRotSpeed = 72000;
                VertRotSpeed = 176000;
            }
            OutLines = !OutLines;
        }
        if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
            System.exit(0);
        if(e.getKeyCode() == KeyEvent.VK_R)
        {
            Keys[0] = false;
            Keys[1] = false;
            Keys[2] = false;
            Keys[3] = false;
        }
    }

    public void keyReleased(KeyEvent e)
    {
        if(e.getKeyCode() == KeyEvent.VK_W)
            Keys[0] = false;
        if(e.getKeyCode() == KeyEvent.VK_A)
            Keys[1] = false;
        if(e.getKeyCode() == KeyEvent.VK_S)
            Keys[2] = false;
        if(e.getKeyCode() == KeyEvent.VK_D)
            Keys[3] = false;
    }

    public void keyTyped(KeyEvent e)
    {
    }

    public void mouseDragged(MouseEvent arg0)
    {
        MouseMovement(arg0.getX(), arg0.getY());
        MouseX = arg0.getX();
        MouseY = arg0.getY();
        CenterMouse();
    }

    public void mouseMoved(MouseEvent arg0)
    {
        MouseMovement(arg0.getX(), arg0.getY());
        MouseX = arg0.getX();
        MouseY = arg0.getY();
        CenterMouse();
    }

    public void mouseClicked(MouseEvent arg0)
    {
    }

    public void mouseEntered(MouseEvent arg0)
    {
    }

    public void mouseExited(MouseEvent arg0)
    {
    }

    public void mousePressed(MouseEvent arg0)
    {
        if(arg0.getButton() == MouseEvent.BUTTON1)
            if(PolygonOver != null)
                PolygonOver.seeThrough = false;

        if(arg0.getButton() == MouseEvent.BUTTON3)
            if(PolygonOver != null)
                PolygonOver.seeThrough = true;
    }

    public void mouseReleased(MouseEvent arg0)
    {

    }

    public void mouseWheelMoved(MouseWheelEvent arg0)
    {
       if(DeveloperMode)
       {
           if (arg0.getUnitsToScroll() > 0)
           {
               if (zoom > MinZoom)
                   zoom -= 25 * arg0.getUnitsToScroll();
           } else
           {
               if (zoom < MaxZoom)
                   zoom -= 25 * arg0.getUnitsToScroll();
           }
       }
    }
}