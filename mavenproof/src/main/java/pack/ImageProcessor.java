package pack;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Stack;



public class ImageProcessor
{
    public Stack<BufferedImage> inputImages;//a list of all our images that are unprocessed
    public Stack<BufferedImage> exportImages;//a list of all our images that are processed
    public Integer currentID;//used for file naming

    //constructor
    public ImageProcessor()
    {
        inputImages = new Stack<>();
        exportImages = new Stack<>();
        currentID = 0;
    }


    //loads all images from:
    // - downloads/ImportImages
    //and adds them to the stack
    public void loadImages()
    {
        //get the folder
        File folder = new File(System.getProperty("user.home") + "/Downloads/ImportImages");
        //get all the image files
        File[] allImageFiles = folder.listFiles();

        //for all the files
        for(File file: allImageFiles)
        {
            try
            {
                inputImages.push(ImageIO.read(file));
            }
            catch(IOException e)
            {
                System.out.println(e.getStackTrace());
            }
        }//end of going through all of the files
    }//end of loadImages

    //exports all the images to downloads/ExportImages
    public void saveImages()
    {
        while(!exportImages.isEmpty())
        {
            try
            {
                ImageIO.write(exportImages.pop(),
                        "PNG",
                        new File(System.getProperty("user.home") + "/Downloads/ExportImages/" + currentID++ + ".PNG"));//notice the incrementing
            } catch (IOException ie)
            {
                ie.printStackTrace();
            }
        }
    }//end of saveImages

    //returns an image that is horizontally flipped
    public BufferedImage flipHorizontal(BufferedImage input)
    {

        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-input.getWidth(null), 0);
        AffineTransformOp op = new AffineTransformOp(tx,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        input = op.filter(input, null);
        return input;
    }

    public BufferedImage rotateXDegrees(BufferedImage input, int degrees)
    {
        final double rads = Math.toRadians(degrees);
        final double sin = Math.abs(Math.sin(rads));
        final double cos = Math.abs(Math.cos(rads));
        final int w = (int) Math.floor(input.getWidth() * cos + input.getHeight() * sin);
        final int h = (int) Math.floor(input.getHeight() * cos + input.getWidth() * sin);
        final BufferedImage rotatedImage = new BufferedImage(w, h, input.getType());
        final AffineTransform at = new AffineTransform();
        at.translate(w / 2, h / 2);
        at.rotate(rads,0, 0);
        at.translate(-input.getWidth() / 2, -input.getHeight() / 2);
        final AffineTransformOp rotateOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        rotateOp.filter(input,rotatedImage);
        return rotatedImage;
    }


}
