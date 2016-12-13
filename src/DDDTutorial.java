import javax.swing.*;
import java.awt.*;

public class DDDTutorial
{
    static Dimension ScreenSize = Toolkit.getDefaultToolkit().getScreenSize();

    public static void main(String[] args)
    {
        JFrame F = new JFrame();
        Screen ScreenObject = new Screen();
        F.add(ScreenObject);
        F.setUndecorated(true);
        F.setSize(ScreenSize);
        F.setVisible(true);
        F.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
