
import org.apache.commons.lang3.ArrayUtils;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.distribution.Distribution;
import org.deeplearning4j.nn.conf.distribution.NormalDistribution;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.PerformanceListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationLReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;

public class App {
    private static final double LEARNING_RATE = 0.0002;
    //private static final double L2 = 0.005;
    private static final double GRADIENT_THRESHOLD = 100.0;
    private static final IUpdater UPDATER = Adam.builder().learningRate(LEARNING_RATE).beta1(0.5).build();
    private static final IUpdater UPDATER_ZERO = Sgd.builder().learningRate(0.0).build();

    private static JFrame frame;
    private static JPanel panel;


    //0.3 - 50
    //0.4 - 26
    //0.5 - 31
    private static WeightInit distr = WeightInit.RELU;
    private static int seed = 1235643;

    //@TODO DATASTUFFS
    private static boolean loading = false;
    private static int gensBetweenSaves = 2800;
    private static int gensBetweenPhotos = 5;
    private static double percentSize = 0.6;
    private static int width = (int) (120*percentSize);
    private static int height = (int) (160*percentSize);
    private static int totalGens = 0;

    //batch array holds the number to include in a batch, and the generation at which we switch to the next batch, or -1 for the final batch size
    private static int[]batchArray = new int[]{12,500,16,1000,32,1500,64,2000,128,3000,256,-1};
    private static int batchSpot = 0; //where are we at in the array right now?
    private static int batchSize  = 12;

    private static int genGracePeriod = 0;//how long does the generator get to work things out before it is attack by the discriminator?
    private static boolean bonusFittingDis = true;

    private static int sampleNumber = 4;

    private static Layer[] genLayers() {
        return new Layer[] {
                new DenseLayer.Builder().nIn(64).nOut(256).weightInit(WeightInit.NORMAL).build(),
                new ActivationLayer.Builder(new ActivationLReLU(0.2)).build(),
                new DenseLayer.Builder().nIn(256).nOut(512).build(),
                new ActivationLayer.Builder(new ActivationLReLU(0.2)).build(),
                new DenseLayer.Builder().nIn(512).nOut(1024).build(),
                new ActivationLayer.Builder(new ActivationLReLU(0.2)).build(),
                new DenseLayer.Builder().nIn(1024).nOut(width*height*3).activation(Activation.TANH).build()
        };
    }

    /**
     * Returns a network config that takes in a 8x8 random number and produces a 24x32 color image.
     *
     * @return config
     */
    private static MultiLayerConfiguration generator() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(UPDATER)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .gradientNormalizationThreshold(GRADIENT_THRESHOLD)
                //.l2(L2)
                .weightInit( distr)
                .activation(Activation.IDENTITY)
                .list(genLayers())
                .build();

        return conf;
    }

    private static Layer[] disLayers(IUpdater updater) {
        return new Layer[] {
                new DenseLayer.Builder().nIn(width*height*3).nOut(1024).updater(updater).build(),
                new ActivationLayer.Builder(new ActivationLReLU(0.2)).build(),
                new DenseLayer.Builder().nIn(1024).nOut(512).updater(updater).build(),
                new ActivationLayer.Builder(new ActivationLReLU(0.2)).build(),
             //   new DropoutLayer.Builder(1 - 0.5).build(),
                new DenseLayer.Builder().nIn(512).nOut(256).updater(updater).build(),
                new ActivationLayer.Builder(new ActivationLReLU(0.2)).build(),
                new OutputLayer.Builder(LossFunctions.LossFunction.XENT).nIn(256).nOut(1).activation(Activation.SIGMOID).updater(updater).build()
        };
    }

    private static MultiLayerConfiguration discriminator() {
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(UPDATER)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .gradientNormalizationThreshold(GRADIENT_THRESHOLD)
                //.l2(L2)
                .weightInit(distr)
                .activation(Activation.IDENTITY)
                .list(disLayers(UPDATER))
                .build();

        return conf;
    }

    private static MultiLayerConfiguration gan() {
        Layer[] genLayers = genLayers();
        Layer[] disLayers = disLayers(UPDATER_ZERO); // Freeze discriminator layers in combined network.
        Layer[] layers = ArrayUtils.addAll(genLayers, disLayers);

        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .updater(UPDATER)
                .gradientNormalization(GradientNormalization.RenormalizeL2PerLayer)
                .gradientNormalizationThreshold(GRADIENT_THRESHOLD)
                //.l2(L2)
                .weightInit(distr)
                .activation(Activation.IDENTITY)
                .list(layers)
                .build();

        return conf;
    }

    public static void main(String... args) throws Exception {
        Nd4j.getMemoryManager().setAutoGcWindow(15 * 1000);



        //@TODO set up data thingie here
        System.out.println("Getting myData");
        float[][] myData = CustomDataLoader.getData(width,height);

        //loading nets
        MultiLayerNetwork gen = null;
        MultiLayerNetwork dis = null;
        MultiLayerNetwork gan = null;

        //this code displays random images from the given dataset, for testing purposes
        /*
        int m = 10;
        while (m > 0)
        {
            m++;
            INDArray[] samples = new INDArray[9];
            for (int i = 0; i < 9; i++)
            {
                samples[i] = new NDArray(myData[(int)(Math.random()*1000)]);
            }
            visualize(samples,width,height);
        }

         */


        if (loading)
        {
            System.out.println("Loading == true. Lets load some networks!");
            gen = ModelSerializer.restoreMultiLayerNetwork(System.getProperty("user.home") + "/Downloads/genNumb.zip");
            dis = ModelSerializer.restoreMultiLayerNetwork(System.getProperty("user.home") + "/Downloads/disNumb.zip");
            gan = ModelSerializer.restoreMultiLayerNetwork(System.getProperty("user.home") + "/Downloads/ganNumb.zip");
        } else
        {
            System.out.println("!Loading == true. Lets create some networks from scratch!");
            gen = new MultiLayerNetwork(generator());
            dis = new MultiLayerNetwork(discriminator());
            gan = new MultiLayerNetwork(gan());
        }
        //end of loading nets

        gen.init();
        dis.init();
        gan.init();

        copyParams(gen, dis, gan);

       // gen.setListeners(new PerformanceListener(10, true));
        dis.setListeners(new PerformanceListener(100, true));
        //gan.setListeners(new PerformanceListener(10, true));



        while (true) {
                totalGens++;

                //saving
                if (totalGens % gensBetweenSaves == 0 )
                {
                    System.out.println("\n\n\nSAVING...\n\n\n");
                    System.out.println("Generation " + totalGens + ".");
                    try
                    {
                        String downloads = System.getProperty("user.home") + "/Downloads";
                      //  downloads = "D:\\AI RECORDS";
                        ModelSerializer.writeModel(gan, new File(downloads + "/ganBig"+totalGens+ ".zip"), true);
                        ModelSerializer.writeModel(dis, new File(downloads + "/disBig"+totalGens+ ".zip"), true);
                        ModelSerializer.writeModel(gen, new File(downloads + "/genBig"+totalGens+ ".zip"), true);
                    } catch (Exception e)
                    {
                    }
                }

                // generate data
                //batchsize manager
                if (batchArray[batchSpot+1] != -1 && totalGens > batchArray[batchSpot+1])
                {
                    batchSpot+=2;
                    batchSize = batchArray[batchSpot];
                }

                //real is a random subset of 128 random images (duplicates possible) from the original good dataset
                float[][] newData = new float[batchSize][width*height];
                for (int i = 0; i < batchSize;i++)
                {
                newData[i]=myData[(int)(Math.random()*1000)];
                }
                INDArray real = new NDArray(newData);



                INDArray fakeIn = Nd4j.rand(new int[]{batchSize,  64});
                INDArray fake = gan.activateSelectedLayers(0, gen.getLayers().length - 1, fakeIn);

                DataSet realSet = new DataSet(real, Nd4j.zeros(batchSize, 1));
                DataSet fakeSet = new DataSet(fake, Nd4j.ones(batchSize, 1));

                DataSet data = DataSet.merge(Arrays.asList(realSet, fakeSet));

                if (dis.score() > 0.09 || (totalGens < genGracePeriod ) || Math.random() > 0.94)
                {
                    System.out.print("fitting dis");
                    dis.fit(data);


                    if (totalGens > genGracePeriod)
                    {
                        dis.fit(data);

                    }

               }
          //  if (dis.score() < 0.005)
          //  {
           //     System.out.println("Giving Mr. Discriminator a hand. : ) " + dis.score() + " dis score");
          //      dis.fit(data);
          //  }



            //dis.fit(realSet);
                //dis.fit(fakeSet);

                // Update the discriminator in the GAN network
                updateGan(gen, dis, gan);

                gan.fit(new DataSet(Nd4j.rand(new int[] { batchSize, 64}), Nd4j.zeros(batchSize, 1)));

                if (totalGens < 250)
                {
                    gan.fit(new DataSet(Nd4j.rand(new int[] { batchSize, 64}), Nd4j.zeros(batchSize, 1)));
                }

              //  if (dis.score() > 4.0)
               // {
               //     System.out.println("Giving gen a hand (" + dis.score() +" discriminator score, " + gan.score() + " gan score, " + gen.score() + " gen score)");
            //        gan.fit(new DataSet(Nd4j.rand(new int[] { batchSize, 64}), Nd4j.zeros(batchSize, 1)));
//
               // }

         //       if (dis.score() > 5.0)
          //  {
          //      System.out.println("Giving gen lots of hands");
          //      gan.fit(new DataSet(Nd4j.rand(new int[] { batchSize, 64}), Nd4j.zeros(batchSize, 1)));
          //  }

                //gan.fit(fakeSet2);



                // Copy the GANs generator to gen.
                //updateGen(gen, gan);

                if (totalGens % gensBetweenPhotos == 1) {
                    System.out.println("Iteration " + totalGens + " Visualizing... " + dis.score());
                    INDArray[] samples = new INDArray[sampleNumber];
                    DataSet fakeSet2 = new DataSet(fakeIn, Nd4j.ones(batchSize, 1));

                    for (int k = 0; k < sampleNumber; k++) {
                        INDArray input = fakeSet2.get(k).getFeatures();
                        //samples[k] = gen.output(input, false);
                        samples[k] = gan.activateSelectedLayers(0, gen.getLayers().length - 1, input);

                    }
                    visualize(samples,width,height);
                }

        }
    }

    private static void copyParams(MultiLayerNetwork gen, MultiLayerNetwork dis, MultiLayerNetwork gan) {
        int genLayerCount = gen.getLayers().length;
        for (int i = 0; i < gan.getLayers().length; i++) {
            if (i < genLayerCount) {
                gen.getLayer(i).setParams(gan.getLayer(i).params());
            } else {
                dis.getLayer(i - genLayerCount).setParams(gan.getLayer(i).params());
            }
        }
    }


    private static void updateGan(MultiLayerNetwork gen, MultiLayerNetwork dis, MultiLayerNetwork gan) {
        int genLayerCount = gen.getLayers().length;
        for (int i = genLayerCount; i < gan.getLayers().length; i++) {
            gan.getLayer(i).setParams(dis.getLayer(i - genLayerCount).params());
        }
    }

    private static void visualize(INDArray[] samples,int width, int height) {
        if (frame == null) {
            frame = new JFrame();
            frame.setTitle("Viz");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            panel = new JPanel();

            panel.setLayout(new GridLayout(samples.length / 3, 1, 8, 8));
            frame.add(panel, BorderLayout.CENTER);
            frame.setVisible(true);
        }

        panel.removeAll();

        for (int i = 0; i < samples.length; i++) {
            panel.add(getImage(samples[i], width, height));
        }

        frame.revalidate();
        frame.pack();
    }

    private static JLabel getImage(INDArray tensor,int width,int height) {

        BufferedImage bi = indToBuffered(tensor, width, height);


        ImageIcon orig = new ImageIcon(bi);
        Image imageScaled = orig.getImage().getScaledInstance((3 * width), (3 * height), Image.SCALE_REPLICATE);

        ImageIcon scaled = new ImageIcon(imageScaled);

        return new JLabel(scaled);
    }

    //the red and blue channels are swapped, as it looks nicer
    //you're going to want to shift that later, probably
    private static BufferedImage indToBuffered(INDArray tensor,int width,int height)
    {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int count = 0;

        for (int i = 0; i < height; i++)
        {
            for (int j = 0; j < width; j++)
            {

                double red = clamp(tensor.getDouble(count),0,1);
                double green = clamp(tensor.getDouble(count+width*height),0,1);
                double blue = clamp(tensor.getDouble(count + (width*height*2)),0,1);

                Color newColor = new Color((int)(red*255),(int)(green*255),(int)(blue*255));
                bi.setRGB(j,i,newColor.getRGB());
                count++;
            }
        }
        return bi;
    }


    private static double clamp(double value, double min, double max)
    {
        return Math.max(min,Math.min(max,value));
    }

}