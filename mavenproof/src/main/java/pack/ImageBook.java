package pack;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

//a collection of InkedGrids. All of them should have the same width and height.
public class ImageBook
{
    public ArrayList<InkedGrid> gridList;
    public Integer width;
    public Integer height;

    public ImageBook(Integer desiredWidth, Integer desiredHeight)
    {
        gridList = new ArrayList<>();
        width = desiredWidth;
        height = desiredHeight;
    }


    //loads some Images from the storage folder
    // - downloads/ImportImages
    public void loadImages(int scale, int inkAllowance)
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
                gridList.add(new InkedGrid(ImageIO.read(file), inkAllowance, scale));
            }
            catch(IOException e)
            {
                System.out.println(e.getStackTrace());
            }
        }//end of going through all of the files
    }//end of loadImages

    //returns a random image from our book
    public InkedGrid getRand()
    {
        int spot = (int)((gridList.size()-1) * Math.random());
        return gridList.get(spot);
    }//end of getRand

    //saves to a txt file
    //String name is the file name
    public void saveToTxt(String name)
    {
        try {
            System.out.println("Saving...\nFinding folder...");
            String home = System.getProperty("user.home");
            String fileName = name;

            Path path = Paths.get(home+"/Downloads/" + fileName + ".txt");

            //Generating things to write
            String toWrite = "";

            System.out.println("Figuring out what to write...");

            //first two lines are width and height
            toWrite+=width + System.lineSeparator() + height + System.lineSeparator();

            //for each of the InkedGrids
            for(InkedGrid grid: gridList)
            {
                int spotInBitSet = 0;
                //draw each pixel as a 1 or a 0
                for (int i = 0; i < height; i++)
                {
                    for (int j = 0; j < width; j++)
                    {
                        //if black, draw 1, else draw 0
                        if (grid.data.get(spotInBitSet))//binary
                        {
                            toWrite+=1;
                        }
                        else
                        {
                            toWrite+=0;
                        }
                        spotInBitSet++;
                    }
                    toWrite += System.lineSeparator();
                }
            }//end of for each InkedGrid


            //Writing
            System.out.println("Writing...");
            Files.write(path, toWrite.getBytes(), StandardOpenOption.CREATE);
            System.out.println("Successfully exported your images to " + name +".txt!");
        } catch (IOException e) {
            System.out.println("A mysterious error occurred... : (");
            System.out.print(e.getStackTrace());
        }
    }//end of saveToTxt

    //reads from a .txt
    // (name = .txt name)
    public void readFromTxt(String name)
    {
        try {
            String home = System.getProperty("user.home");
            String fileName = name;
            Path path = Paths.get(home+"/Downloads/" + fileName + ".txt");

            //read it all!
            List<String> list = Files.readAllLines(path, Charset.defaultCharset());

            //get width and height
            width = Integer.parseInt(list.get(0));
            height = Integer.parseInt(list.get(1));

            //make a bunch of new InkedGrids
            for (int lineAt = 2; lineAt < list.size(); lineAt+=height)//read all the file
            {

                BitSet newBits = new BitSet();//this will be used in the new InkedGrid we will add.
                int spotInNewBits = 0;       //keeps track of where we are in newBits

                for (int i = 0; i < height; i++)//read a full height's length of lines
                {
                    String line = list.get(lineAt + i);    //get the line as a string
                    for (char charAt : line.toCharArray())//read each char. if 1, add a true, else add a 0
                    {
                        if (charAt == '1')
                        {
                            newBits.set(spotInNewBits,true);
                        }
                        else
                        {
                            newBits.set(spotInNewBits,false);
                        }

                        spotInNewBits++;
                    }
                }//end of reading a full heights length of lines

                gridList.add(new InkedGrid(newBits, width,height));
            }//end of while there's file left to read

        } catch (Exception e) {
            // exception handling
            System.out.println("An error occurred. Sorry.");
            System.out.print(e.getStackTrace());

        }

    }//end of readFromTxt

}