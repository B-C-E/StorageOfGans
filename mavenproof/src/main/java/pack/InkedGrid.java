package pack;

//imports here
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.BitSet;

//used to store an image of either purely black or white pixels.
public class InkedGrid
{

    public BitSet data;
    public int width;
    public int height;

    public InkedGrid(BitSet input,int width, int height)
    {
        data = input;
        this.width = width;
        this.height = height;
    }

    //input is the image to process. InkAllowance is how close to black a pixel must be to be considered ink
    //(0 is pure black only,  255 means even pure white is considered ink)
    //scale is the desired scale. Use 1 to not change the scale
    public InkedGrid(BufferedImage input, int inkAllowance, double scale)
    {
        //scale it!
        if (scale != 1)
        {
            input = scaleBufferedImage(input,scale);
        }

        //set up width and height
        width = input.getWidth();
        height = input.getHeight();

        //setup BitSet of Data
        data = new BitSet();

        //go through each pixel, and determine if it is black enough to be considered ink, or not
        int spotInBitset = 0;//where we are in the bitset (used to remember where we should be adding blacks or whites

        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {
                //get the color of a given pixel
                Color colorAtSpot = new Color(input.getRGB(j,i));

                //if it the average lightness level of a pixel is black enough, add black to the BitSet
                if ((colorAtSpot.getRed()+colorAtSpot.getBlue()+colorAtSpot.getGreen())/3 < inkAllowance)
                {
                    data.set(spotInBitset, true);
                }
                else//elsewise, add white
                {
                    data.set(spotInBitset,false);
                }

                spotInBitset++;
            }
        }

    }

    //creates a png with name name.png, puts it into the downloads folder
    //Why have a name parameter? That way you can iteratively turn an ImageBook into a bunch of PNGs without trying to
    //claim the same filename more than once
    public void drawToPNG(String name)
    {
        //create a new Red Green Blue image of the proper width and height
        BufferedImage toPrint = new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);


        //propagate image with the proper blacks and whites

        int spotInBitSet = 0;

        for (int i = 0; i < height; i++)//for each pixel horizontally...
        {
            for (int j = 0; j < width; j++)//for each pixel vertically...
            {
                //draw the proper color in the proper spot
                if (data.get(spotInBitSet) == true)//true means black
                {
                    toPrint.setRGB(j,i, Color.black.getRGB());
                }
                else //false means white
                {
                    toPrint.setRGB(j,i,Color.white.getRGB());
                }
                spotInBitSet++;
            }//end of each pixel vertically.
        }//end of each pixel horizontally.
        //end of image propagation


        //print (write) to the downloads!
        try
        {
            ImageIO.write(toPrint,
                    "PNG",
                    new File(System.getProperty("user.home") + "/Downloads/" + name + ".PNG"));
        }
        catch (IOException ie)
        {
            ie.printStackTrace();
        }
    }//end of drawToPNG


    //PRIVATE UTILITY METHODS

    //Scales a buffered image by factor scale
    private BufferedImage scaleBufferedImage(BufferedImage input, double scale)
    {
        int newWidth = new Double(input.getWidth() * scale).intValue();
        int newHeight = new Double(input.getHeight() * scale).intValue();

        BufferedImage resized = new BufferedImage(newWidth, newHeight, input.getType());
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(input, 0, 0, newWidth, newHeight, 0, 0, input.getWidth(),
                input.getHeight(), null);
        g.dispose();
        return resized;
    }//end of scaleBufferedImage

}//end of InkedGrid


//subcredits
//
//scaleBufferedImage
// - written by user charisis on stackExchange
// - https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage