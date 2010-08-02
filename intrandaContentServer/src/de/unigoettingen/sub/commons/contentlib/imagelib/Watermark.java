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
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.unigoettingen.sub.commons.contentlib.exceptions.WatermarkException;

/*******************************************************************************
 * Watermark class
 * 
 * @version 26.01.2009
 * @author Steffen Hankiewicz
 * @author Markus Enders
 ******************************************************************************/
public class Watermark {
	protected static final Logger logger = Logger.getLogger(Watermark.class);

	protected BufferedImage watermarkImage; // BUfferedImage to draw components
	// into
	protected int width = 0;
	protected int height = 0;

	protected Color backgroundColor = new Color(1f, 1f, 1f);

	protected List<WatermarkComponent> allWatermarkComponents = new LinkedList<WatermarkComponent>(); // contains

	// all
	// text
	// components

	/***************************************************************************
	 * Constructor for Watermark with given height and width
	 * 
	 * @param width
	 *            The width to use
	 * @param heigth
	 *            The height to use
	 **************************************************************************/
	public Watermark(int width, int heigth) {
		this.width = width;
		this.height = heigth;

		watermarkImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		// set white background
		WatermarkBox wb = new WatermarkBox(width, height, backgroundColor);
		this.addWatermarkComponent(wb);
	}

	public Watermark(File inFile) throws WatermarkException {
		this.readConfiguration(inFile);

		watermarkImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);

	}
	
	public Watermark(InputStream is) throws WatermarkException  {
		this.readConfiguration(is);

		watermarkImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);

	}

	/***************************************************************************
	 * add some {@link WatermarkComponent} to the Watermark
	 * 
	 * @param inComponent
	 *            The WatermarkComponent to add
	 **************************************************************************/
	public void addWatermarkComponent(WatermarkComponent inComponent) {
		allWatermarkComponents.add(inComponent);
		inComponent.setParent_watermark(this);
		inComponent.setTargetImage(watermarkImage); // set the canvas for the
		// component
	}

	/***************************************************************************
	 * render the Watermark
	 **************************************************************************/
	private void render() {
		// iterate over all images
		Iterator<WatermarkComponent> it1 = allWatermarkComponents.iterator();
		while (it1.hasNext()) {
			WatermarkComponent wc = (WatermarkComponent) it1.next();
			boolean rendered = false;

			try {
				WatermarkImage wi = (WatermarkImage) wc;
				wi.render();
				rendered = true;
			} catch (Exception e) {
			}

			try {
				WatermarkText wi = (WatermarkText) wc;
				wi.render();
				rendered = true;
			} catch (Exception e) {
			}

			try {
				WatermarkBox wi = (WatermarkBox) wc;
				wi.render();
				rendered = true;
			} catch (Exception e) {
			}

			if (rendered == false) {
				logger.warn("A Watermark Component could not be rendered.");
			}

		}
	}

	/**
	 * @return the width
	 */
	protected int getWidth() {
		return width;
	}

	/**
	 * @param width
	 *            the width to set
	 */
	protected void setWidth(int width) {
		this.width = width;
	}

	/**
	 * @return the height
	 */
	protected int getHeight() {
		return height;
	}

	/**
	 * @param height
	 *            the height to set
	 */
	protected void setHeight(int height) {
		this.height = height;
	}

	/***************************************************************************
	 * returns the rendered result of the {@link Watermark} as a
	 * {@link RenderedImage}
	 * 
	 * @return the Watermark as RenderedImage
	 **************************************************************************/
	public RenderedImage getRenderedImage() {
		this.render();
		return (RenderedImage) watermarkImage;
	}

	/***************************************************************************
	 * write Watermark to {@link OutputStream}
	 * 
	 * @param outputFileStream
	 *            The {@link OutputStream} to use for writing
	 **************************************************************************/
	public void writeToFile(OutputStream outputFileStream) {
		RenderedImage ri = this.getRenderedImage();

		logger.debug("size of watermark:" + ri.getWidth() + " / " + ri.getHeight());

		JpegInterpreter ti = new JpegInterpreter(ri);
		ti.setXResolution(72f);
		ti.setYResolution(72f);
		ti.setColordepth(8);
		ti.setSamplesperpixel(3);
		ti.writeToStream(outputFileStream);
	}

	/***************************************************************************
	 * Setter for all WatermarkCompotents
	 * 
	 * @param allWatermarkComponents
	 *            the allWatermarkComponents to set as List
	 **************************************************************************/
	public void setAllWatermarkComponents(List<WatermarkComponent> allWatermarkComponents) {
		this.allWatermarkComponents = allWatermarkComponents;
	}

	public void readConfiguration(File inFile) throws WatermarkException {
		try {
			InputStream is = new FileInputStream(inFile);
			this.readConfiguration(is);
			
		} catch (IOException ioe) {
			logger.error("Can't read XML configuration for Watermark:" + inFile.getAbsolutePath() + " due to " + ioe.getMessage());
			throw new WatermarkException("Can't read XML configuration for Watermark:" + inFile.getAbsolutePath(), ioe);
		}
	}
	
	
	public void readConfiguration(InputStream is) throws WatermarkException {

		Document xmldoc = null;

		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			docBuilderFactory.setValidating(false);
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();


			xmldoc = docBuilder.parse(is);

			is.close();

			// iterate over all nodes and read nodes
			Node topmostelement = xmldoc.getDocumentElement(); // get uppermost
			if (!topmostelement.getNodeName().equals("watermark")) {
				logger.error("Don't get correct xml response - topelement is NOT <watermark>");
				throw new WatermarkException("Don't get correct xml response - topelement is NOT <watermark>");
			}

			// iterate over attributes
			NamedNodeMap nnm = topmostelement.getAttributes();
			if (nnm != null) {
				Node colornode = nnm.getNamedItem("color"); // read background
				// color
				readBackgroundColor(colornode);

				Node widthnode = nnm.getNamedItem("width"); // read width
				Node heightnode = nnm.getNamedItem("height"); // read heigth

				if (widthnode != null) {
					String value = widthnode.getNodeValue();
					try {
						this.width = Integer.parseInt(value);
					} catch (Exception e) {
						logger.error("Invalid value for y-coordinate for Watermark Text");
						throw new WatermarkException("Invalid value for y-coordinate for Watermark Text", e);
					}
				}

				if (heightnode != null) {
					String value = heightnode.getNodeValue();
					try {
						this.height = Integer.parseInt(value);
					} catch (Exception e) {
						logger.error("Invalid value for watermark's height.");
						throw new WatermarkException("Invalid value for watermark's height", e);
					}
				}

				if ((width > 0) && (height > 0)) {

					// set background
					WatermarkBox wb = new WatermarkBox(width, height, backgroundColor);
					this.addWatermarkComponent(wb);
				}
			}

			// iterate over all child elements
			NodeList children = topmostelement.getChildNodes();

			for (int i = 0; i < children.getLength(); i++) {
				Node singlenode = children.item(i);

				if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().startsWith("image"))) {
					// read configuration
					WatermarkImage watermarkImage = new WatermarkImage(singlenode);
					allWatermarkComponents.add(watermarkImage);
					watermarkImage.setParent_watermark(this);
				} else if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().startsWith("text"))) {
					WatermarkText watermarkText = new WatermarkText(singlenode);
					allWatermarkComponents.add(watermarkText);
					watermarkText.setParent_watermark(this);
				} else if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().startsWith("box"))) {
					WatermarkBox watermarkBox = new WatermarkBox(singlenode);
					allWatermarkComponents.add(watermarkBox);
					watermarkBox.setParent_watermark(this);
				}

			}
		} catch (ParserConfigurationException pce) {

		} catch (WatermarkException we) {
			logger.warn("Error occured while reading Watermark configuration");
		} catch (SAXException sae) {
			logger.error("XML configuration for Watermark is invalid; non wellformed XML?\n" + sae);
			throw new WatermarkException("XML configuration for Watermark is invalid; non wellformed XML?", sae);
		} catch (IOException ioe) {
			logger.error("Can't read XML configuration for Watermark stream due to " + ioe.getMessage());
			throw new WatermarkException("Can't read XML configuration for Watermark stream.", ioe);
		}

	}

	private void readBackgroundColor(Node colornode) throws WatermarkException {
		if (colornode != null) {
			String colorstring = colornode.getNodeValue();
			// split string into compontents
			if (colorstring.length() < 6) {
				// invalid string
				logger.error("Invalid value for background color of watermark: color=" + colorstring);
				throw new WatermarkException("Invalid value for background color of watermark");
			}
			String r = colorstring.substring(0, 2);
			String g = colorstring.substring(2, 4);
			String b = colorstring.substring(4, 6);

			// convert hexadecimals into decimals

			try {
				int r_int = Integer.parseInt(r, 16);
				int g_int = Integer.parseInt(g, 16);
				int b_int = Integer.parseInt(b, 16);

				this.backgroundColor = new Color(r_int, g_int, b_int);
			} catch (Exception e) {
				logger.error("Invalid value for background color of watermark");
				throw new WatermarkException("Invalid value for background color of watermark", e);
			}
		}
	}

	/**
	 * @return the backgroundColor
	 */
	protected Color getBackgroundColor() {
		return backgroundColor;
	}

	/**
	 * @param backgroundColor
	 *            the backgroundColor to set
	 */
	protected void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}

	/**
	 * @return the watermarkImage
	 */
	protected BufferedImage getWatermarkImage() {
		return watermarkImage;
	}



}