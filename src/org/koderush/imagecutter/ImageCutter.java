
package org.koderush.imagecutter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import javax.imageio.ImageIO;

import org.imgscalr.Scalr;


/**
 * Use "Kvisoft PDF to Image" to convert PDF to PNG
 */
public class ImageCutter
{
    static final String ROOT_PATH = "res/test_book";
    static final String OUTPUT_DIR = "output";
    static final String OUTPUT_FORMAT = "png";

    /**
     * @param args
     * @throws Exception 
     */
    public static void main( String[] args )
        throws Exception
    {
        String outputPath = ROOT_PATH + File.separatorChar + OUTPUT_DIR;
        
        File root = new File( ROOT_PATH );
        File outputDir = new File( outputPath );

        if( !outputDir.exists() )
        {
            if( !outputDir.mkdirs() )
            {
                throw new Exception( "Not able to create output dir: " + outputPath );
            }
        }
            
        File[] files = root.listFiles();
        
        for( File f : files )
        {
            if( f.isFile() )
            {
                try
                {
                    BufferedImage img = ImageIO.read( f );

                    BufferedImage newImg = cutEdge( img );

                    ImageIO.write( newImg, OUTPUT_FORMAT, new File( outputPath + File.separatorChar + f.getName() ) );
                }
                catch( Exception e )
                {
                    System.out.println( "[File:" + f.getAbsolutePath() + "] " + e.getMessage() );
                }
            }
        }
    }


    private static BufferedImage cutEdge( BufferedImage image ) throws Exception
    {
        final double threshHold = 0.02;
        
        int border = 0;
        final Color borderColor = new Color( 255, 127, 255 );
        
        final int marginTop = 15;
        final int marginBottom = 15;
        final int marginLeft = 15;
        final int marginRight = 15;
        
        final boolean greyMode = true;
        
        int w = image.getWidth();
        int h = image.getHeight();
        

        int[][] imageData = convertTo2D( image );

        double[] rowDiff = calcDiffForRow( imageData, w, h, greyMode );
        double[] colDiff = calcDiffForCol( imageData, w, h, greyMode );

        int cutTop = 0;
        int cutBottom = 0;
        int cutLeft = 0;
        int cutRight = 0;

        for( int i = 0; i < rowDiff.length; i++ )
        {
            if( rowDiff[ i ] > threshHold )
            {
                cutTop = i;

                break;
            }
        }
        if( cutTop > marginTop )
        {
            cutTop -= marginTop;
        }

        for( int i = rowDiff.length - 1; i >= 0; i-- )
        {
            if( rowDiff[ i ] > threshHold )
            {
                cutBottom = rowDiff.length - 1 - i;

                break;
            }
        }
        if( cutBottom > marginBottom )
        {
            cutBottom -= marginBottom;
        }
        
        for( int i = 0; i < colDiff.length; i++ )
        {
            if( colDiff[ i ] > threshHold )
            {
                cutLeft = i;

                break;
            }
        }
        if( cutLeft > marginLeft )
        {
            cutLeft -= marginLeft;
        }
        
        for( int i = colDiff.length - 1; i >= 0; i-- )
        {
            if( colDiff[ i ] > threshHold )
            {
                cutRight = colDiff.length - 1 - i;

                break;
            }
        }
        if( cutRight > marginRight )
        {
            cutRight -= marginRight;
        }

        BufferedImage croppedImage =
                Scalr.crop( image, cutLeft, cutTop, w - cutLeft - cutRight, h - cutTop - cutBottom );

        if( border > 0 )
        {
            return Scalr.pad( croppedImage, border, borderColor );
        }
        else
        {
            return croppedImage;
        }
    }


    private static double[] calcDiffForCol( int[][] data, int width, int height, boolean greyMode )
    {
        double[] result = new double[width];
        
        double max = 0.0001;

        for( int x = 0; x < width; x++ )
        {
            int lastPixel = data[ x ][ 0 ];

            int lastR = (int) ( lastPixel & 0x000000ff );
            int lastG = (int) ( lastPixel & 0x0000ff00 ) >> 8;
            int lastB = (int) ( lastPixel & 0x00ff0000 ) >> 16;
            int lastGrey = toGrey( lastR, lastG, lastB );

            int diffR = 0;
            int diffG = 0;
            int diffB = 0;
            int diffGrey = 0;

            for( int y = 1; y < height; y++ )
            {
                int pixel = data[ x ][ y ];

                int r = (int) ( pixel & 0x000000ff );
                int g = (int) ( pixel & 0x0000ff00 ) >> 8;
                int b = (int) ( pixel & 0x00ff0000 ) >> 16;
                int grey = toGrey( r, g, b );
                
                diffR += Math.abs( r - lastR );
                diffG += Math.abs( g - lastG );
                diffB += Math.abs( b - lastB );
                diffGrey += Math.abs( grey - lastGrey );

                lastR = r;
                lastG = g;
                lastB = b;
                lastGrey = toGrey( lastR, lastG, lastB );
            }

            if( greyMode )
            {
                result[ x ] = diffGrey;
            }
            else
            {
                result[ x ] = diffR  + diffG + diffB;
            }
            
            if( result[ x ] > max )
            {
                max = result[ x ];
            }
        }


        for( int x = 0; x < width; x++ )
        {
            result[ x ] /= max; 
        }
                
        return result;
    }

    
    private static double[] calcDiffForRow( int[][] data, int width, int height, boolean greyMode )
    {
        double[] result = new double[height];
        
        double max = 0.0001;

        for( int y = 0; y < height; y++ )
        {
            int lastPixel = data[ 0 ][ y ];

            int lastR = (int) ( lastPixel & 0x000000ff );
            int lastG = (int) ( lastPixel & 0x0000ff00 ) >> 8;
            int lastB = (int) ( lastPixel & 0x00ff0000 ) >> 16;
            int lastGrey = toGrey( lastR, lastG, lastB );

            int diffR = 0;
            int diffG = 0;
            int diffB = 0;
            int diffGrey = 0;

            for( int x = 1; x < width; x++ )
            {
                int pixel = data[ x ][ y ];

                int r = (int) ( pixel & 0x000000ff );
                int g = (int) ( pixel & 0x0000ff00 ) >> 8;
                int b = (int) ( pixel & 0x00ff0000 ) >> 16;
                int grey = toGrey( r, g, b );
                
                diffR += Math.abs( r - lastR );
                diffG += Math.abs( g - lastG );
                diffB += Math.abs( b - lastB );
                diffGrey += Math.abs( grey - lastGrey );

                lastR = r;
                lastG = g;
                lastB = b;
                lastGrey = toGrey( lastR, lastG, lastB );
            }

            if( greyMode )
            {
                result[ y ] = diffGrey;
            }
            else
            {
                result[ y ] = diffR  + diffG + diffB;
            }
            
            if( result[ y ] > max )
            {
                max = result[ y ];
            }
        }


        for( int y = 0; y < height; y++ )
        {
            result[ y ] /= max; 
        }
                
        return result;
    }


    private static int toGrey( int r, int g, int b )
    {
        return (int) ( 0.212671 * r + 0.715160 * g + 0.072169 * b );
    }


    /**
     * pixel(int) : | alpha  |  blue  |  green |  red   |
     *              |========|========|========|========|
     * 
     * @param image
     * @return int[x][y]
     * @throws Exception 
     */
    private static int[][] convertTo2D( BufferedImage image ) throws Exception
    {
        final byte[] pixels = ( (DataBufferByte) image.getRaster().getDataBuffer() ).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();

        final int pixelLength =  pixels.length / ( width * height );

        int[][] result = new int[width][height];
        
        switch( pixelLength )
        {
            case 4:
            {
                for( int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength )

                {
                    int argb = 0;
                    argb += ( (int) pixels[ pixel ] & 0xff ) << 24; // alpha
                    argb += ( (int) pixels[ pixel + 3 ] & 0xff ) << 16; // red
                    argb += ( (int) pixels[ pixel + 2 ] & 0xff ) << 8; // green
                    argb += ( (int) pixels[ pixel + 1 ] & 0xff ); // blue

                    result[ col ][ row ] = argb;
                    col++;
                    if( col == width )
                    {
                        col = 0;
                        row++;
                    }
                }
                
                break;
            }
            case 3:
            {
                for( int pixel = 0, row = 0, col = 0; pixel < pixels.length; pixel += pixelLength )
                {
                    int argb = 0xff000000;
                    argb += ( (int) pixels[ pixel + 2 ] & 0xff ) << 16; // red
                    argb += ( (int) pixels[ pixel + 1 ] & 0xff ) << 8; // green
                    argb += ( (int) pixels[ pixel ] & 0xff ); // blue

                    result[ col ][ row ] = argb;
                    col++;
                    if( col == width )
                    {
                        col = 0;
                        row++;
                    }
                }
                
                break;
            }
            default:
            {
                throw new Exception( "Not support pixel length: " + pixelLength );
            }
        }

        return result;
    }
}
