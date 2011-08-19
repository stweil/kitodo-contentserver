/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 * 		- http://gdz.sub.uni-goettingen.de 
 * 		- http://www.intranda.com 
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.unigoettingen.sub.commons.contentlib.imagelib;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator.MergingMode;

/************************************************************************************
 * central Image Manager for all kinds of image handlings, wraps all functionalities of the {@link ImageManipulator}
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class ImageManager {
	private static final Logger logger = Logger.getLogger(ImageManager.class);

	ImageInterpreter myInterpreter = null;
	OutputStream outputStream = null;

	public static final Integer SCALE_BY_WIDTH = 1;
	public static final Integer SCALE_BY_HEIGHT = 2;
	public static final Integer SCALE_BY_PERCENT = 3;
	public static final Integer SCALE_BY_RESOLUTION = 4;
	public static final Integer SCALE_TO_BOX = 5;

	public static final Integer DPI_DEFAULT = 100;
	public static final Integer DPI_DISPLAY = 72;
	public static final Integer DPI_PRINT = 600;

	public static final Integer IMAGE_ROTATION_0 = 0;
	public static final Integer IMAGE_ROTATION_90 = 90;
	public static final Integer IMAGE_ROTATION_180 = 180;
	public static final Integer IMAGE_ROTATION_270 = 270;

	HashMap<Integer, Integer> parameters = null;

	// possible values for scaling parameters
	public static final Integer SUBSAMPLETOGREY = 1;
	public static final Integer BILINEAR = 2;
	public static final Integer NEXTNEIGHBOR = 3;

	// possible parameters "BITONALSCALEMETHOD", "GREYSCALESCALEMETHOD",
	// "COLORSCALEMETHOD"
	public static final Integer BITONALSCALEMETHOD = 1;
	public static final Integer GREYSCALESCALEMETHOD = 2;
	public static final Integer COLORSCALEMETHOD = 3;

	public static final Integer TOP = 1;
	public static final Integer BOTTOM = 2;
	public static final Integer RIGHT = 3;
	public static final Integer LEFT = 4;

	private String httpproxyhost = null;
	private String httpproxyport = null;
	private String httpproxyuser = null;
	private String httpproxypassword = null;

	/************************************************************************************
	 * simple Constructor for given {@link URL}
	 * 
	 * @param url the URL to use
	 ************************************************************************************/
	public ImageManager(URL url) throws ImageManagerException {
		init(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
	}

	/**
	 * @param url
	 * @param httpproxyhost
	 * @param httpproxyport
	 * @param httpproxyuser
	 * @param httpproxypassword
	 * @throws ImageManagerException 
	 */
	public ImageManager(URL url, String httpproxyhost, String httpproxyport, String httpproxyuser, String httpproxypassword) throws ImageManagerException {
		init(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
	}

	/**
	 * @param url
	 * @param httpproxyhost
	 * @param httpproxyport
	 * @param httpproxyuser
	 * @param httpproxypassword
	 * @throws ImageManagerException 
	 */
	public void init(URL url, String httpproxyhost, String httpproxyport, String httpproxyuser, String httpproxypassword) throws ImageManagerException {
		try {
			myInterpreter = ImageFileFormat.getInterpreter(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
			logger.debug("url: " + url);
		} catch (Exception e) {
			logger.error("Error while getting ImageInterpreter", e);
			throw new ImageManagerException(e);
		}

		parameters = new HashMap<Integer, Integer>();
		parameters.put(BITONALSCALEMETHOD, SUBSAMPLETOGREY);
		parameters.put(GREYSCALESCALEMETHOD, BILINEAR);
		parameters.put(COLORSCALEMETHOD, NEXTNEIGHBOR);
	}

	/************************************************************************************
	 * Constructor for given {@link URL} and initial parameters
	 * 
	 * @param url
	 * @param inParameters
	 ************************************************************************************/
	public ImageManager(URL url, HashMap<Integer, Integer> inParameters) {
		// get the right Interpreter and read the image using the url
		init2(url, inParameters, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
	}

	/**
	 * @param url
	 * @param inParameters
	 * @param httpproxyhost
	 * @param httpproxyport
	 * @param httpproxyuser
	 * @param httpproxypassword
	 */
	public ImageManager(URL url, HashMap<Integer, Integer> inParameters, String httpproxyhost, String httpproxyport, String httpproxyuser,
			String httpproxypassword) {
		// get the right Interpreter and read the image using the url
		init2(url, inParameters, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
	}

	/**
	 * @param url
	 * @param inParameters
	 * @param httpproxyhost
	 * @param httpproxyport
	 * @param httpproxyuser
	 * @param httpproxypassword
	 */
	public void init2(URL url, HashMap<Integer, Integer> inParameters, String httpproxyhost, String httpproxyport, String httpproxyuser,
			String httpproxypassword) {
		parameters = inParameters;
		try {
			myInterpreter = ImageFileFormat.getInterpreter(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);
		} catch (Exception e) {
			logger.error("Error while getting ImageInterpreter", e);
		}
	}

	/*************************************************************************************
	 * @return the myInterpreter
	 ************************************************************************************/
	public ImageInterpreter getMyInterpreter() {
		return this.myInterpreter;
	}

	/*************************************************************************************
	 * @param myInterpreter the myInterpreter to set
	 ************************************************************************************/
	public void setMyInterpreter(ImageInterpreter myInterpreter) {
		this.myInterpreter = myInterpreter;
	}

	/*************************************************************************************
	 * @return the outputStream
	 ************************************************************************************/
	public OutputStream getOutputStream() {
		return this.outputStream;
	}

	/************************************************************************************
	 * @param outputStream the outputStream to set
	 ***********************************************************************************/
	public void setOutputStream(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	/**
	 * Scales and rotates an image. Rotation is optional. If no rotation is needed, the angle must be set to 0 <br/>
	 * <br/>
	 * 
	 * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param scaleby defines the scaling method
	 * @param angle angle for rotation; value will be betwetween 0 and 360
	 * 
	 * @return the generated {@link RenderedImage}
	 * @throws ImageManipulatorException
	 */
	public RenderedImage scaleImageByPixel(int pixelx, int pixely, int scaleby, int angle) throws ImageManipulatorException {
		return scaleImageByPixel(pixelx, pixely, scaleby, angle, null, null);
	}

	/**
	 * Scales an image. Several different modes are possible. It may also draw small little boxes for highlighting words into the image. <br/>
	 * <br/>
	 * If no boxes should be drawn, the coordinates must be set to null. An empty List ist NOT sufficient and will cause errors. <br/>
	 * 
	 * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param scaleby defines the scaling method
	 * @param coordinates a LinkedList containing String. Those strings are representing coordinates: x1,y1,x2,y2 for boxes
	 * @param inColor color for drawing those boxes
	 * 
	 * @return the generated {@link RenderedImage}
	 * @throws ImageManipulatorException
	 */
	public RenderedImage scaleImageByPixel(int pixelx, int pixely, int scaleby, LinkedList<String> coordinates, Color inColor)
			throws ImageManipulatorException {
		return scaleImageByPixel(pixelx, pixely, scaleby, 0, coordinates, inColor);
	}

	/**
	 * Scales and rotates an image. Several different modes are possible. It may also draw small little boxes for highlighting words into the image. <br/>
	 * Rotation is optional. If no rotation is needed, the angle must be set to 0 <br/>
	 * If no boxes should be drawn, the coordinates must be set to null. An empty List ist NOT sufficient and will cause errors. <br/>
	 * 
	 * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param scaleby defines the scaling method
	 * @param angle angle for rotation; value will be betwetween 0 and 360
	 * @param coordinates a LinkedList containing String. Those strings are representing coordinates: x1,y1,x2,y2 for boxes
	 * @param inColor color for drawing those boxes
	 * 
	 * @return the generated {@link RenderedImage}
	 * @throws ImageManipulatorException
	 */
	public RenderedImage scaleImageByPixel(int pixelx, int pixely, int scaleby, int angle, LinkedList<String> coordinates, Color inColor)
			throws ImageManipulatorException {

		return scaleImageByPixel(pixelx, pixely, scaleby, angle, coordinates, inColor, null, false, 0);
	}

	/**
	 * Scales and rotates an image. Several different modes are possible. It may also draw small little boxes for highlighting words into the image.
	 * If a watermark is given, the watermark can also be added. <br/>
	 * Rotation is optional. If no rotation is needed, the angle must be set to 0 <br/>
	 * If no boxes should be drawn, the coordinates must be set to null. An empty List ist NOT sufficient and will cause errors. <br/>
	 * If a watermark is set, all watermark related parameters must be set as well. However setting the watermark is optional. If the watermark is set
	 * to null, no watermark will be added to the image.
	 * 
	 * @param pixelx horizontal size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param pixely vertical size of the new image in pixel; or the scale factor in percent, depending on the scale method
	 * @param externalscalemethod defines the scaling method
	 * @param angle angle for rotation; value will be betwetween 0 and 360
	 * @param coordinates a LinkedList containing String. Those strings are representing coordinates: x1,y1,x2,y2 for boxes
	 * @param inColor color for drawing those boxes
	 * @param inWatermark Watermark to be added to the scaled and rotated image
	 * @param watermarkscale method for adjusting the size of the watermark
	 * @param watermarkposition tells if watermark should be added to TOP,BOTTOM,RIGHT or LEFT of the image
	 * @return the generated {@link RenderedImage}
	 * @throws ImageManipulatorException
	 */
	public RenderedImage scaleImageByPixel(int pixelx, int pixely, int externalscalemethod, int angle, LinkedList<String> coordinates, Color inColor,
			Watermark inWatermark, boolean watermarkscale, int watermarkposition) throws ImageManipulatorException {
		RenderedImage inImage = null;
		RenderedImage outImage = null;
		List<String> draw_coordinates = null;
		float internalScaling_y = 0;
		float internalScaling_x = 0;
		int scalemethod; // method for scaling: next neighbor, subsample

		// check parameters
		if ((angle < 0) || (angle > 360)) {
			throw new ImageManipulatorException("Invalid value for angle - must be between 0 and 360 degrees");
		}

		// if ((angle > 0) && (coordinates != null) && (coordinates.size() > 0)) {
		// throw new ImageManipulatorException("Can't rotate angle, when box cordinates are used!");
		// }

		// get image
		if (myInterpreter != null) {
			inImage = myInterpreter.getRenderedImage();
		}

		if (inImage == null) {
			throw new ImageManipulatorException("Can't get RenderedImage from ImageInterpreter");
		}

		// ----------------------------------------------------------------------------------------------------
		if (externalscalemethod == SCALE_TO_BOX) {
			// pixely = neue höhe
			// pixelx = neue breite
			internalScaling_x = (float) pixelx / (float) myInterpreter.getWidth();
			internalScaling_y = (float) pixely / (float) myInterpreter.getHeight();

			logger.debug("x: " + internalScaling_x + " y: " + internalScaling_y);
			if (internalScaling_x > internalScaling_y) {

				internalScaling_x = internalScaling_y;
			} else {
				internalScaling_y = internalScaling_x;
			}
			logger.debug("new values: " + internalScaling_x);
			logger.debug("new x " + +myInterpreter.getWidth() * internalScaling_x);
			logger.debug("new y " + myInterpreter.getHeight() * internalScaling_y);

		}

		// ----------------------------------------------------------------------------------------------------
		// calculate internal scaling factor
		else if (externalscalemethod == SCALE_BY_WIDTH) {
			internalScaling_x = (float) pixelx / (float) myInterpreter.getWidth();
			if (pixely == 0) {
				// scale proportionally
				internalScaling_y = internalScaling_x;
			} else {
				internalScaling_y = (float) pixely / (float) myInterpreter.getHeight();
			}
			// ----------------------------------------------------------------------------------------------------
		} else if (externalscalemethod == SCALE_BY_HEIGHT) {
			internalScaling_y = (float) pixely / (float) myInterpreter.getHeight();
			if (pixelx == 0) {
				// scale proportionally
				internalScaling_x = internalScaling_y;
			} else {
				internalScaling_x = (float) pixelx / (float) myInterpreter.getWidth();
			}
			// ----------------------------------------------------------------------------------------------------
		} else if (externalscalemethod == SCALE_BY_PERCENT) {
			float xres = myInterpreter.getXResolution(); // get x resolution
			float yres = myInterpreter.getYResolution(); // get y resolution

			float intpercentx = ((float) pixelx / 100);
			float intpercenty = ((float) pixely / 100);

			internalScaling_x = (myInterpreter.getXResolution() / xres);
			// internalScaling_x = (DPI_DEFAULT / xres);
			internalScaling_x = internalScaling_x * intpercentx;
			internalScaling_y = (myInterpreter.getXResolution() / yres);
			// internalScaling_y = (DPI_DEFAULT / yres);
			internalScaling_y = internalScaling_y * intpercenty;

		} else {
			// no known scale mechanism
			throw new ImageManipulatorException("no known scale mechanism");
		}

		// ----------------------------------------------------------------------------------------------------
		// calculate the new coordinates
		// ----------------------------------------------------------------------------------------------------
		if (coordinates != null) {
			draw_coordinates = ImageManipulator.scaleCoordinates(coordinates, internalScaling_x, internalScaling_y);
		}

		// ----------------------------------------------------------------------------------------------------
		// scale image depending on mechanism
		// mechanism depends on color depth
		// ----------------------------------------------------------------------------------------------------
		if ((myInterpreter.getSamplesperpixel() == 1) && (myInterpreter.getColordepth() == 1)) {
			// it's bitonal
			scalemethod = parameters.get(BITONALSCALEMETHOD);
		} else if ((myInterpreter.getSamplesperpixel() == 1) && (myInterpreter.getColordepth() > 1)) {
			// it's greyscale
			scalemethod = parameters.get(GREYSCALESCALEMETHOD);
		} else {
			// it's color
			scalemethod = parameters.get(COLORSCALEMETHOD);
		}
		// check value of scale method and scale
		if (scalemethod == SUBSAMPLETOGREY) {
			outImage = ImageManipulator.scaleSubsampleBinaryToGrey(inImage, internalScaling_x, internalScaling_y);
		} else if (scalemethod == BILINEAR) {
			outImage = ImageManipulator.scaleInterpolationBilinear(inImage, internalScaling_x, internalScaling_y);
		} else if (scalemethod == NEXTNEIGHBOR) {
			outImage = ImageManipulator.scaleNextNeighbor(inImage, internalScaling_x, internalScaling_y);
		} else {
			throw new ImageManipulatorException("wrong scalemethod");
		}

		// ----------------------------------------------------------------------------------------------------
		// draw boxes, but only if we don't rotate the image
		// ----------------------------------------------------------------------------------------------------
		if ((coordinates != null)) {
			if (inColor == null) {
				inColor = Color.GREEN;
			}
			outImage = ImageManipulator.drawBoxes(outImage, draw_coordinates, inColor);
		}

		// ----------------------------------------------------------------------------------------------------
		// rotate image
		// ----------------------------------------------------------------------------------------------------
		if (angle > 0) {
			outImage = ImageManipulator.rotate(outImage, angle, "bicubic");
		}

		// ----------------------------------------------------------------------------------------------------
		// deal with the watermarks
		RenderedImage watermarkRi = null;
		if (inWatermark != null) {
			// we have to scale watermark
			// ------------------------------------------------------------------------------------------------
			if (watermarkscale) {
				// watermark will be rendered and then scaled
				watermarkRi = inWatermark.getRenderedImage();
				// calculate internal scaling factor
				if ((watermarkposition == ImageManager.TOP) || (watermarkposition == ImageManager.BOTTOM)) {
					internalScaling_x = (float) outImage.getWidth() / (float) watermarkRi.getWidth();
					internalScaling_y = internalScaling_x;
				} else {
					internalScaling_y = (float) outImage.getHeight() / (float) watermarkRi.getHeight();
					internalScaling_x = internalScaling_y;
				}
				logger.debug("Scaling watermark: image size:" + outImage.getWidth() + " / " + outImage.getHeight() + "   watermark size:"
						+ watermarkRi.getWidth() + " / " + watermarkRi.getHeight() + "\n scalefactor:" + internalScaling_x);
				// scale watermark
				watermarkRi = ImageManipulator.scaleNextNeighbor(watermarkRi, internalScaling_x, internalScaling_y);
			}
			// we don't have to scale watermark
			// ------------------------------------------------------------------------------------------------
			else {
				if ((watermarkposition == ImageManager.TOP) || (watermarkposition == ImageManager.BOTTOM)) {
					inWatermark.overrideWidth(outImage.getWidth());
				} else {
					inWatermark.overrideHeight(outImage.getHeight());
				}
				watermarkRi = inWatermark.getRenderedImage();
			}
			logger.debug("Watermark size is: " + watermarkRi.getWidth() + " / " + watermarkRi.getHeight());

			// ------------------------------------------------------------------------------------------------
			// add renderedImage of Watermark to outImage
			// ------------------------------------------------------------------------------------------------
			if (watermarkposition == ImageManager.RIGHT) {
				outImage = ImageManipulator.mergeImages(outImage, watermarkRi, MergingMode.HORIZONTALLY);
			} else if (watermarkposition == ImageManager.LEFT) {
				outImage = ImageManipulator.mergeImages(watermarkRi, outImage, MergingMode.HORIZONTALLY);
			} else if (watermarkposition == ImageManager.TOP) {
				outImage = ImageManipulator.mergeImages(watermarkRi, outImage, MergingMode.VERTICALLY);
			} else if (watermarkposition == ImageManager.BOTTOM) {
				outImage = ImageManipulator.mergeImages(outImage, watermarkRi, MergingMode.VERTICALLY);
			}
		}

		if (outImage == null) {
			throw new ImageManipulatorException("Can't create a RenderedImage object, outImage is null");
		}
		return outImage;
	}

	/**
	 * @return the httpproxyhost
	 */
	public String getHttpproxyhost() {
		return httpproxyhost;
	}

	/**
	 * @param httpproxyhost the httpproxyhost to set
	 */
	public void setHttpproxyhost(String httpproxyhost) {
		this.httpproxyhost = httpproxyhost;
	}

	/**
	 * @return the httpproxyport
	 */
	public String getHttpproxyport() {
		return httpproxyport;
	}

	/**
	 * @param httpproxyport the httpproxyport to set
	 */
	public void setHttpproxyport(String httpproxyport) {
		this.httpproxyport = httpproxyport;
	}

	/**
	 * @return the httpproxyuser
	 */
	public String getHttpproxyuser() {
		return httpproxyuser;
	}

	/**
	 * @param httpproxyuser the httpproxyuser to set
	 */
	public void setHttpproxyuser(String httpproxyuser) {
		this.httpproxyuser = httpproxyuser;
	}

	/**
	 * @return the httpproxypassword
	 */
	public String getHttpproxypassword() {
		return httpproxypassword;
	}

	/**
	 * @param httpproxypassword the httpproxypassword to set
	 */
	public void setHttpproxypassword(String httpproxypassword) {
		this.httpproxypassword = httpproxypassword;
	}

}
