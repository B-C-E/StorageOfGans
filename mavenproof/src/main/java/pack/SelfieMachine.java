package pack;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.github.sarxos.webcam.Webcam;


/**
 * Example of how to take 500 selfies after 10 seconds
 *
 * @author B-C-E
 */
public class SelfieMachine {

    public static void main(String[] args) throws IOException, InterruptedException
    {

        // get default webcam and open it
        Webcam webcam = Webcam.getDefault();
        webcam.open();
Thread.sleep(10000);
        for (int i = 0; i < 250; i++)
        {
            // get image
            BufferedImage image = webcam.getImage();

            // save image to PNG file
            ImageIO.write(image, "PNG", new File(System.getProperty("user.home") + "/Downloads/photophoto/" + i + "_face.png"));
            Thread.sleep(7);
        }
        webcam.close();
    }
}