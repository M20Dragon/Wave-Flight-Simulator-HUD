import java.awt.Color;
import java.util.Random;

public class GenerateTerrain
{
    /** Here is where the editing with the terrain can take place **/
    Random r;
    static double roughness = 4; /** Sets the ruggedness for the terrain. Set to lower value for flatter terrain. Ex(5 = mountainous terrain, 1 = flat/hilly land) **/
    static int mapSize = 40; /** Sets how large the map is. Set to higher value for larger map **/
    static double Size = 2; /** Sets the Size of the terrain **/
    static Color G = new Color(116, 103, 77); /** Sets the color of the terrain. **/

    public GenerateTerrain()
    {
        r = new Random();
        double[] values1 = new double[mapSize];
        double[] values2 = new double[values1.length];

        for (int y = 0; y < values1.length/2; y+=2)
        {

            for(int i = 0; i < values1.length; i++)
            {
                values1[i] = values2[i];
                values2[i] = r.nextDouble()*roughness;
            }

            if(y != 0)
            {
                for (int x = 0; x < values1.length/2; x++)
                {
                    Screen.DPolygons.add(new DPolygon(new double[]{(Size * x), (Size * x), Size + (Size * x)}, new double[]{(Size * y), Size + (Size * y), Size + (Size * y)}, new double[]{values1[x], values2[x], values2[x+1]}, G, false));
                    Screen.DPolygons.add(new DPolygon(new double[]{(Size * x), Size + (Size * x), Size + (Size * x)}, new double[]{(Size * y), Size + (Size * y), (Size * y)}, new double[]{values1[x], values2[x+1], values1[x+1]}, G, false));
                }
            }

            for(int i = 0; i < values1.length; i++)
            {
                values1[i] = values2[i]; /** Evens out terrain **/
                values2[i] = r.nextDouble()*roughness; /** Evens out terrain **/
            }

            if(y != 0)
            {
                for (int x = 0; x < values1.length/2; x++)
                {
                    Screen.DPolygons.add(new DPolygon(new double[]{(Size * x), (Size * x), Size + (Size * x)}, new double[]{(Size * (y+1)), Size + (Size * (y+1)), Size + (Size * (y+1))}, new double[]{values1[x], values2[x],  values2[x+1]}, G, false));
                    Screen.DPolygons.add(new DPolygon(new double[]{(Size * x), Size + (Size * x), Size + (Size * x)}, new double[]{(Size * (y+1)), Size + (Size * (y+1)), (Size * (y+1))}, new double[]{values1[x], values2[x+1],  values1[x+1]}, G, false));
                }
            }
        }
    }
}