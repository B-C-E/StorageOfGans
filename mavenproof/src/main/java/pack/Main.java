package pack;

import pack.ImageBook;

public class Main {

    public static void main(String[] args) {
        System.out.println("");
        ImageProcessor pro = new ImageProcessor();

        pro.loadImages();
        int count = 0;
        pro.currentID = 500;
        while(!pro.inputImages.isEmpty())
        {
            pro.exportImages.push(pro.inputImages.pop());
            System.out.print(".");
            if (++count > 99)
            {
                System.out.println();
                count =0;
            }
        }

        System.out.println();

        pro.saveImages();

    }
}
