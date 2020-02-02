

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;

import org.nd4j.linalg.factory.Nd4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;

public class CustomDataLoader
{
    public static float[][] getData(int width, int height)
    {
        try
        {



            int numberFiles = 1000;

            File location = new File("D:\\1000 sean selfies");//just images here please

            float[][] listOfINDs = new float[numberFiles][width*height*3];

            int uberCount = 0;
            //get each one, turn to INDarray, add together return
            for (String local : location.list())
            {
                uberCount++;
                if (uberCount > numberFiles)
                    break;

                File imageFile = new File (location + "\\" +  local);
                BufferedImage img = ImageIO.read(imageFile);

                //scale it!
                BufferedImage resized = new BufferedImage(width, height, img.getType());
                Graphics2D g = resized.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.drawImage(img, 0, 0, width, height, 0, 0, img.getWidth(),
                        img.getHeight(), null);
                g.dispose();

                img = resized;

                float[]colors = new float[width*height*3];

                int spotNumb = 0;

                for (int i = 0; i < height; i++)
                {
                    for (int j = 0; j < width; j++)
                    {
                        spotNumb++;

                        Color color = new Color(img.getRGB(j,i));

                        colors[spotNumb-1] = color.getRed()/255f;
                        colors[spotNumb+(width*height)-1] = color.getGreen()/255f;
                        colors[spotNumb+(width*height*2)-1] = color.getBlue()/255f;
                    }
                }


                listOfINDs[uberCount-1] = colors;



            }//end of for each file

            System.out.println();
            System.out.println("compilingIndarray... ");

            INDArray big = new NDArray(listOfINDs);




            org.nd4j.linalg.dataset.DataSet myData = new DataSet(big,Nd4j.ones(big.length()));



            return listOfINDs;

        }catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }



}
