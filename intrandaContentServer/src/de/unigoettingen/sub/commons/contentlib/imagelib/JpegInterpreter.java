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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.imageio.plugins.jpeg.JPEGImageReader;
import com.sun.imageio.plugins.jpeg.JPEGImageReaderSpi;
import com.sun.imageio.plugins.jpeg.JPEGImageWriter;
import com.sun.imageio.plugins.jpeg.JPEGImageWriterSpi;
import com.sun.media.jai.codec.ByteArraySeekableStream;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;

/************************************************************************************
 * JpegInterpreter handles Jpeg-Images
 * 
 * @version 06.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ************************************************************************************/
public class JpegInterpreter extends AbstractImageInterpreter implements ImageInterpreter {
	private static final Logger logger = Logger.getLogger(JpegInterpreter.class);

	int defaultXResolution = 100;
	int defaultYResolution = 100;
	int writerCompressionValue = 80;

	/************************************************************************************
	 * Constructor for {@link JpegInterpreter} to read an jpeg image from given {@link InputStream}
	 * 
	 * @param inStream
	 *            {@link InputStream}
	 * @throws ImageInterpreterException
	 ************************************************************************************/
	public JpegInterpreter(InputStream inStream) throws ImageInterpreterException {
		ImageReader imagereader = null; // ImageReader to read the class
		ImageInputStream iis = null;
		InputStream inputStream = null;

		Iterator<ImageReader> it = ImageIO.getImageReadersByFormatName("jpeg");
		if (it.hasNext()) {
			imagereader = (ImageReader) it.next();
		} else {
			imagereader = new JPEGImageReader(new JPEGImageReaderSpi());
		}
		// } else {
		// // ERROR - no ImageReader was found
		// logger.error("Imagereader for JPEG couldn't be found");
		// throw new ImageInterpreterException(
		// "Imagereader for JPEG format couldn't be found!");
		// }

		// read the stream and store it in a byte array
		this.readImageStream(inStream);
		byte imagebytes[] = this.getImageByteStream();
		//
		try {
			inputStream = new ByteArraySeekableStream(imagebytes);
		} catch (IOException e1) {
			logger.error("Can't transform the image's byte array to stream");
			ImageInterpreterException iie = new ImageInterpreterException("Can't transform the image's byte array to stream");
			throw iie;
		}

		try {

			// read the stream
			iis = ImageIO.createImageInputStream(inputStream);

			imagereader.setInput(iis, true); // set the ImageInputStream as

			ImageReadParam readParam = imagereader.getDefaultReadParam();
			this.renderedimage = imagereader.readAsRenderedImage(0, readParam); // get
			// the
			// rendered
			// image

		} catch (IOException ioe) {
			logger.error("Can't read jpeg image", ioe);
			throw new ImageInterpreterException("Can't read the input stream", ioe);
		} catch (Exception e) {
			logger.error("something went wrong during reading of jpeg image", e);
			throw new ImageInterpreterException("Something went wrong while reading the JPEG from input stream", e);
		}

		// get all metadata of the image, color depth etc...
		//
		IIOMetadata imageMetadata = null;
		try {
			imageMetadata = imagereader.getImageMetadata(0);
		} catch (IOException e) {
			logger.error(e);
		}
		String nativeFormatName = imageMetadata.getNativeMetadataFormatName();
		Node domNode = imageMetadata.getAsTree(nativeFormatName);

		// get new metadata - this is not very sophisticated parsing the DOM
		// tree - needs to be replaced by
		// XPATH expressions - see above
		String height_str = this.getNumLines(domNode);
		if (height_str != null) {
			this.height = Integer.parseInt(height_str);
		}

		String width_str = this.getSamplesPerLine(domNode);
		if (width_str != null) {
			this.width = Integer.parseInt(width_str);
		}

		String resunits = this.getResUnits(domNode);
		int resunits_int = 1; // default is DPI
		// if resunits==1 than it is dpi,
		// if resunits==2 than it is dpcm
		if (resunits != null) {
			resunits_int = Integer.parseInt(resunits);
		}

		String xres_str = this.getXdensity(domNode);
		if (xres_str != null) {
			this.xResolution = Integer.parseInt(xres_str);
			if (resunits_int == 2) {
				this.xResolution = this.xResolution / 2.54f;
			}
		}

		String yres_str = this.getYdensity(domNode);
		if (yres_str != null) {
			this.yResolution = Integer.parseInt(yres_str);
			if (resunits_int == 2) {
				this.yResolution = this.yResolution / 2.54f;
			}
		}

		String colordepth_str = this.getSamplePrecision(domNode);
		if (colordepth_str != null) {
			this.colorDepth = Integer.parseInt(colordepth_str);
		}

		String samplesperpixel_str = this.getNumFrames(domNode);
		if (samplesperpixel_str != null) {
			this.samplesPerPixel = Integer.parseInt(samplesperpixel_str);
		}

		// TODO: INTRANDA: Remove this, if it's not needed
		// somehow I don't get any result with those xpath expressions. No
		// matter
		// what XPATH I define or if I take te domNode directly or create a new
		// XML document from
		// the xml string provided by the printXmlNode method !!!
		//
		// // get new metadata
		// try
		// {
		//
		// XPath xpath = XPathFactory.newInstance().newXPath();
		// String n = (String) xpath.evaluate("/javax_imageio_jpeg_image_1.0",
		// domNode);
		// System.out.println("XPath result:"+n+"<");
		//
		// }catch (XPathExpressionException e){
		// logger.error(e);
		// }

		// JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(inStream);

	}

	/************************************************************************************
	 * Constructor for jpeg image from given {@link RenderedImage}
	 * 
	 * @param inImage
	 *            the given {@link RenderedImage}
	 ************************************************************************************/
	public JpegInterpreter(RenderedImage inImage) {
		// will not set any metadata for this image
		// needs to be done separatly
		this.renderedimage = inImage;
	}

	/************************************************************************************
	 * Write the renderedimage to an {@link OutputStream}
	 * 
	 * @param outStream
	 *            the {@link OutputStream} to write to
	 ************************************************************************************/
	public void writeToStream(FileOutputStream fos, OutputStream outStream) {
		if (this.renderedimage == null) { // no image available
			return;
		}
		try {
			// create a buffered Image, which has no Alpha channel
			// as JPEG does not support Alpha Channels and the
			// ImageIO doesn't care - but will create a corrupt JPEG
			BufferedImage noAlphaBi = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedimage);
			ImageOutputStream imageOutStream = ImageIO.createImageOutputStream(outStream);

			// Iterator<ImageWriter> writerIter = ImageIO
			// .getImageWritersByFormatName("jpg");
			// ImageWriter writer = writerIter.next(); // get writer from ImageIO
			ImageWriter writer = new JPEGImageWriter(new JPEGImageWriterSpi());

			// create metadata by creating an XML tree
			ImageWriteParam writerParam = writer.getDefaultWriteParam();
			ImageTypeSpecifier its = new ImageTypeSpecifier(noAlphaBi);

			// ImageTypeSpecifier its = new
			// ImageTypeSpecifier(image.getColorModel(),
			// image.getSampleModel());

			// IIOMetadata iomd = writer.getDefaultImageMetadata(new
			// ImageTypeSpecifier(image), writerParam);
			// Element tree =
			// (Element)iomd.getAsTree("javax_imageio_jpeg_image_1.0");
			// Element tree = (Element)iomd.getAsTree("javax_imageio_1.0");
			//
			IIOMetadata iomd = writer.getDefaultImageMetadata(its, writerParam);

			// create the XML tree and modify the appropriate DOM elements
			// to set the metadata
			setMetadata(iomd);

			// set compression
			writerParam.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
			float comprvalue = ((float) writerCompressionValue) / 100;
			writerParam.setCompressionQuality(comprvalue);

			// set output
			writer.setOutput(imageOutStream);
			writer.prepareWriteSequence(null);

			// create new image parameters to set the compression
			// Locale locale = new Locale("en");
			// JPEGImageWriteParam jpegWriteParam = new
			// JPEGImageWriteParam(locale);

			// IIOImage iioImage = new IIOImage(renderedimage, null, iomd);

			IIOImage iioImage = new IIOImage(noAlphaBi, null, iomd);
			writer.write(null, iioImage, writerParam);
			writer.endWriteSequence();
			imageOutStream.flush();

			if (fos != null) {
				ImageOutputStream imageToFile = ImageIO.createImageOutputStream(fos);
				writer.setOutput(imageToFile);
				writer.prepareWriteSequence(null);
				writer.write(null, iioImage, writerParam);
				writer.endWriteSequence();
				imageToFile.flush();
				imageToFile.close();
			}

			writer.dispose();
			imageOutStream.close();

		} catch (IOException e) {
			logger.error("IOException occured", e);
		}
	}

	/************************************************************************************
	 * set metadata to image
	 * 
	 * @param iomd
	 *            the given {@link IIOMetadata} to set
	 ************************************************************************************/
	private void setMetadata(IIOMetadata iomd) {

		Node node = iomd.getAsTree("javax_imageio_jpeg_image_1.0");
		// what are child nodes?
		NodeList nl = node.getChildNodes();
		for (int j = 0; j < nl.getLength(); j++) {
			Node n = nl.item(j);

			if (n.getNodeName().equals("JPEGvariety")) {
				NodeList childNodes = n.getChildNodes();

				for (int k = 0; k < childNodes.getLength(); k++) {
					if (childNodes.item(k).getNodeName().equals("app0JFIF")) {
						// get the attributes resUnits, Xdensity, and Ydensity
						Node resUnitsNode = getAttributeByName(childNodes.item(k), "resUnits");
						Node XdensityNode = getAttributeByName(childNodes.item(k), "Xdensity");
						Node YdensityNode = getAttributeByName(childNodes.item(k), "Ydensity");

						// overwrite values for that node
						resUnitsNode.setNodeValue("1"); // it's dpi

						int xres = (int) this.getXResolution();
						int yres = (int) this.getYResolution();
						if (xres == 0) {
							xres = defaultXResolution;
						}
						if (yres == 0) {
							yres = defaultYResolution;
						}
						XdensityNode.setNodeValue(String.valueOf(xres));
						YdensityNode.setNodeValue(String.valueOf(yres));

					} // endif
				} // end id
				break; // don't need to change the other children
			} // end id

		} // end for

		// set the XML tree for the IIOMetadata object
		try {
			iomd.setFromTree("javax_imageio_jpeg_image_1.0", node);
		} catch (IIOInvalidTreeException e) {
			logger.error(e); // To change body of catch statement
		}

	}

	/**
	 * Indicates wether the image's bytestream is directly embeddable. jpegs are always embeddable
	 * 
	 * @return true if pdf bytes are embeddable
	 */
	public boolean pdfBytestreamEmbeddable() {
		return true;
	}

	/************************************************************************************
	 * get numLines from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getNumLines(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "numLines");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get samplesPerLine from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getSamplesPerLine(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "samplesPerLine");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get Xdensity from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getXdensity(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "JPEGvariety");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "app0JFIF");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "Xdensity");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get Ydensity from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getYdensity(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "JPEGvariety");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "app0JFIF");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "Ydensity");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get resUnits from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getResUnits(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "JPEGvariety");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "app0JFIF");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "resUnits");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get sample precision from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getSamplePrecision(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "samplePrecision");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get number of frame components from {@link Node}
	 * 
	 * @param domNode
	 *            given {@link Node}
	 ************************************************************************************/
	private String getNumFrames(Node domNode) {
		Node markerSequenceNode = getFirstElementByName(domNode, "markerSequence");
		if (markerSequenceNode == null) {
			return null; // markerSequence element not available
		}

		Node sofNode = getFirstElementByName(markerSequenceNode, "sof");
		if (sofNode == null) {
			return null; // sof element not available
		}

		Node attribute = getAttributeByName(sofNode, "numFrameComponents");
		if (attribute == null) {
			return null; // attribute not available
		}

		return attribute.getNodeValue();
	}

	/************************************************************************************
	 * get Dom {@link Node} from parent {@link Node} with given name
	 * 
	 * @param inNode
	 *            the parent {@link Node}
	 * @param elementName
	 *            the name of the Node to look for
	 ************************************************************************************/
	private Node getFirstElementByName(Node inNode, String elementName) {
		NodeList list = inNode.getChildNodes();

		int i = 0;
		while (i < list.getLength()) {
			Node n = list.item(i);

			if ((n.getNodeType() == org.w3c.dom.Element.ELEMENT_NODE) && (n.getNodeName().equals(elementName))) {
				return n;
			}

			i++;
		}
		return null;
	}

	/************************************************************************************
	 * get Dom {@link Node} of attribute from parent {@link Node} with given name
	 * 
	 * @param inNode
	 *            the parent {@link Node}
	 * @param attributeName
	 *            the name of the attribute to look for
	 ************************************************************************************/
	private Node getAttributeByName(Node inNode, String attributeName) {
		NamedNodeMap nnm = inNode.getAttributes();
		return nnm.getNamedItem(attributeName);
	}

	public void setWriterCompressionValue(int inWriterCompressionValue) throws ParameterNotSupportedException {
		if ((inWriterCompressionValue < 0) || (inWriterCompressionValue > 100)) {
			ParameterNotSupportedException pnse = new ParameterNotSupportedException("Value for JPEG compression must be between 0 and 100");
			throw pnse;
		}
		writerCompressionValue = inWriterCompressionValue;
	}
}
