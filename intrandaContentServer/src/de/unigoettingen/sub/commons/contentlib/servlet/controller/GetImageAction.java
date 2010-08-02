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
package de.unigoettingen.sub.commons.contentlib.servlet.controller;

import java.awt.Color;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.log4j.Logger;
import org.goobi.presentation.contentServlet.controller.ContentCache;
import org.goobi.presentation.contentServlet.controller.GoobiContentServer;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ContentLibException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ContentLibUtil;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/************************************************************************************
 * Image action for all kinds of image handlings first of all validate all request parameters, and than interprete all request parameters for correct
 * image handling
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class GetImageAction extends GetAction {
	private static final Logger logger = Logger.getLogger(GetImageAction.class);

	/************************************************************************************
	 * exectute all image actions (rotation, scaling etc.) and send image back to output stream of the servlet, after setting correct mime type
	 * 
	 * @param request
	 *            {@link HttpServletRequest} of ServletRequest
	 * @param response
	 *            {@link HttpServletResponse} for writing to response output stream
	 * @throws IOException
	 * @throws ServletException
	 * @throws ContentLibException
	 * @throws URISyntaxException
	 ************************************************************************************/
	public void run(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException,
			ContentLibException, URISyntaxException {
		super.run(servletContext, request, response);

		/*
		 * -------------------------------- get central configuration --------------------------------
		 */
		ContentServerConfiguration config = ContentServerConfiguration.getInstance();
		URI sourceImageUrl = new URI(config.getRepositoryPathImages() + request.getParameter("sourcepath"));
		ServletOutputStream output = response.getOutputStream();
		ContentCache cc = GoobiContentServer.getContentCache();
		String myUniqueID = getContentCacheIdForRequest(request, config);
		String targetExtension = request.getParameter("format");
		try {
			boolean ignoreCache = false;
			/* check if cache should be ignored */
			if (request.getParameter("ignoreCache") != null) {
				String ignore = request.getParameter("ignoreCache").trim();
				ignoreCache = Boolean.parseBoolean(ignore);
			}
			if (cc == null || !config.getContentCacheUse()) {
				ignoreCache = true;
				cc = null;
				logger.debug("cache deactivated via configuration");
			}
			if (!ignoreCache && cc.cacheContains(myUniqueID, targetExtension)) {
				logger.debug("get file from cache: " + myUniqueID);
				cc.writeToStream(output, myUniqueID, targetExtension);
				return;
			} else if (ignoreCache == false) {
				logger.debug("file not found in cache: " + myUniqueID);
			}

			/*
			 * -------------------------------- if there is an internal request from goobiContentServer, you have to overwrite the sourcepath with
			 * given attribute for image url --------------------------------
			 */
			if (request.getAttribute("sourcepath") != null) {
				sourceImageUrl = new URI((String) request.getAttribute("sourcepath"));
			}
			logger.debug("source image:" + sourceImageUrl);

			/*
			 * -------------------------------- retrieve source image from url --------------------------------
			 */
			ImageManager sourcemanager = new ImageManager(sourceImageUrl.toURL());
			logger.debug("imageManager initialized");

			/*
			 * -------------------------------- set the defaults --------------------------------
			 */
			int angle = 0;
			int scaleX = 100;
			int scaleY = 100;
			int scaleType = ImageManager.SCALE_BY_PERCENT;
			LinkedList<String> highlightCoordinateList = null;
			Color highlightColor = null;
			Watermark myWatermark = null;
			logger.debug("Variables set");

			/*
			 * -------------------------------- rotate --------------------------------
			 */
			if (request.getParameterMap().containsKey("rotate")) {
				angle = Integer.parseInt(request.getParameter("rotate"));
				logger.debug("rotate image:" + angle);
			}

			/*
			 * -------------------------------- scale: scale the image to some percent value --------------------------------
			 */
			if (request.getParameterMap().containsKey("scale")) {
				scaleX = Integer.parseInt(request.getParameter("scale"));
				scaleY = scaleX;
				scaleType = ImageManager.SCALE_BY_PERCENT;
				logger.debug("scale image to percent:" + scaleX);
			}

			/*
			 * -------------------------------- width: scale image to fixed width --------------------------------
			 */
			if (request.getParameterMap().containsKey("width")) {
				scaleX = Integer.parseInt(request.getParameter("width"));
				scaleY = 0;
				scaleType = ImageManager.SCALE_BY_WIDTH;
				logger.debug("scale image to width:" + scaleX);
			}

			/*
			 * -------------------------------- height: scale image to fixed height --------------------------------
			 */
			if (request.getParameterMap().containsKey("height")) {
				scaleY = Integer.parseInt(request.getParameter("height"));
				scaleX = 0;
				scaleType = ImageManager.SCALE_BY_HEIGHT;
				logger.debug("scale image to height:" + scaleY);
			}

			/*
			 * -------------------------------- highlight --------------------------------
			 */
			if (request.getParameterMap().containsKey("highlight")) {
				highlightCoordinateList = new LinkedList<String>();
				String highlight = request.getParameter("highlight");
				StrTokenizer areas = new StrTokenizer(highlight, "$");
				for (String area : areas.getTokenArray()) {
					StrTokenizer coordinates = new StrTokenizer(area, ",");
					highlightCoordinateList.add(coordinates.getContent());
				}
				highlightColor = config.getDefaultHighlightColor();
			}

			/*
			 * -------------------------------- insert watermark, if it should be used --------------------------------
			 */
			if (config.getWatermarkUse()) {
				File watermarkfile = new File(new URI(config.getWatermarkConfigFilePath()));
				myWatermark = new Watermark(watermarkfile);
			}

			/*
			 * -------------------------------- prepare target --------------------------------
			 */
			RenderedImage targetImage = sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor,
					myWatermark, true, ImageManager.BOTTOM);

			ImageFileFormat targetFormat = ImageFileFormat.getImageFileFormatFromFileExtension(targetExtension);
			ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
																			// file

			/*
			 * -------------------------------- set file name and attachment header from parameter or from configuration
			 * --------------------------------
			 */
			StringBuilder targetFileName = new StringBuilder();
			if (config.getSendImageAsAttachment()) {
				targetFileName.append("attachment; ");
			}
			targetFileName.append("filename=");

			if (request.getParameter("targetFileName") != null) {
				targetFileName.append(request.getParameter("targetFileName"));
			} else {
				String filename = ContentLibUtil.getCustomizedFileName(config.getDefaultFileNameImages(), "." + targetFormat.getFileExtension());
				targetFileName.append(filename);
			}
			response.setHeader("Content-Disposition", targetFileName.toString());
			response.setContentType(targetFormat.getMimeType());

			/*
			 * -------------------------------- resolution --------------------------------
			 */
			if (request.getParameter("resolution") != null) {
				wi.setXResolution(Float.parseFloat(request.getParameter("resolution")));
				wi.setYResolution(Float.parseFloat(request.getParameter("resolution")));
			} else {
				wi.setXResolution(config.getDefaultResolution());
				wi.setYResolution(config.getDefaultResolution());
			}

			/*
			 * -------------------------------- write target image to stream --------------------------------
			 */
			if (cc != null && !cc.isCacheSizeExceeded()) {
				logger.info("write file to cache and servlet response: " + cc.getFileForId(myUniqueID, targetExtension));
//				new CacheOutputStream(wi, cc.getFileForId( myUniqueID, targetExtension), output);
				FileOutputStream fos = new FileOutputStream(cc.getFileForId(myUniqueID, targetExtension));
				wi.writeToStream(fos, output);
			} else {
				wi.writeToStream(null, output);
			}
		} catch (CacheException e) {
			logger.warn("CacheException", e);
		}

	}

	// /************************************************************************************
	// * exectute all image actions (rotation, scaling etc.) and send image back
	// * to output stream of the servlet, after setting correct mime type
	// *
	// * @param request
	// * {@link HttpServletRequest} of ServletRequest
	// * @param response
	// * {@link HttpServletResponse} for writing to response output
	// * stream
	// * @throws IOException
	// * @throws ServletException
	// * @throws ContentLibException
	// * @throws URISyntaxException
	// ************************************************************************************/
	// public void run(ServletContext servletContext, HttpServletRequest
	// request, HttpServletResponse response)
	// throws ServletException, IOException, ContentLibException,
	// URISyntaxException {
	// super.run(servletContext, request, response);
	//
	// /* --------------------------------
	// * get central configuration and retrieve source image from url
	// * --------------------------------*/
	// ContentServerConfiguration config =
	// ContentServerConfiguration.getInstance();
	// URI sourceImageUrl = new URI(config.getRepositoryPath() +
	// request.getParameter("sourcepath"));
	// String sourceImageExtension =
	// Helper.getFileExtensionFromFileName(request.getParameter("sourcepath"));
	// FileInputStream inputFileStream = new FileInputStream(new
	// File(sourceImageUrl));
	//
	// try {
	//
	// ImageManager sourcemanager = new ImageManager(sourceImageUrl.toURL());
	// ImageInterpreter ii = sourcemanager.getMyInterpreter();
	//
	// /* --------------------------------
	// * read image
	// * --------------------------------*/
	// // ImageFileFormat sourceFormat =
	// ImageFileFormat.getImageFileFormatFromFileExtension(sourceImageExtension);
	// // ImageInterpreter ii = sourceFormat.getInterpreter(inputFileStream); //
	// read file
	// RenderedImage ri = ii.getRenderedImage(); // get the image
	//
	// ImageManipulator im = ImageManipulator.instance();
	// /* --------------------------------
	// * rotate
	// * --------------------------------*/
	// if (request.getParameterMap().containsKey("rotate")) {
	// double angle = Double.parseDouble(request.getParameter("rotate"));
	// logger.debug("rotate image:" + angle);
	// ri = im.rotate(ri, angle, "bilinear");
	// }
	//
	// /* --------------------------------
	// * scale
	// * --------------------------------*/
	// if (request.getParameterMap().containsKey("scale")) {
	// float scale = Float.parseFloat(request.getParameter("scale"));
	// logger.debug("scale image:" + scale);
	// ri = im.scaleInterpolationBilinear(ri, scale, scale);
	// }
	//
	// /* --------------------------------
	// * scalewidth
	// * --------------------------------*/
	// if (request.getParameterMap().containsKey("scalewidth")) {
	// float scalewidth = Float.parseFloat(request.getParameter("scalewidth"));
	// logger.debug("scale image width:" + scalewidth);
	//
	// }
	//
	// /* --------------------------------
	// * highlight
	// * --------------------------------*/
	// if (request.getParameterMap().containsKey("highlight")) {
	// LinkedList<String> coordinateList = new LinkedList<String>();
	// String highlight = request.getParameter("highlight");
	// StrTokenizer areas = new StrTokenizer(highlight, "$");
	// for (String area : areas.getTokenArray()) {
	// StrTokenizer coordinates = new StrTokenizer(area, ",");
	// coordinateList.add(coordinates.getContent());
	// }
	// ri = im.drawBoxes(ri, coordinateList, config.getDefaultHighlightColor());
	// }
	//
	// /* --------------------------------
	// * prepare target
	// * --------------------------------*/
	// String targetExtension = request.getParameter("format");
	// ImageFileFormat targetFormat =
	// ImageFileFormat.getImageFileFormatFromFileExtension(targetExtension);
	// ImageInterpreter wi = targetFormat.getInterpreter(ri); // read file
	//
	// /* --------------------------------
	// * insert watermark, if it should be used
	// * --------------------------------*/
	// if (config.getWatermarkUse()) {
	// Watermark wm = new Watermark(wi.getWidth(), wi.getHeight());
	// wm.readConfiguration(new File(new
	// URI(config.getWatermarkConfigFilePath())));
	// // get rendered image of watermark
	// RenderedImage wmRi = wm.getRenderedImage();
	// // merge rendered image of watermark with image
	// ri = im.mergeImages(ri, wmRi, ImageManipulator.VERTICALLY);
	// }
	//
	// /* --------------------------------
	// * set file name and attachment header from parameter or from
	// configuration
	// * --------------------------------*/
	// StringBuilder targetFileName = new StringBuilder();
	// if (config.getSendImageAsAttachment()) {
	// targetFileName.append("attachment; ");
	// }
	// targetFileName.append("filename=");
	//
	// if (request.getParameter("targetFileName") != null) {
	// targetFileName.append(request.getParameter("targetFileName"));
	// } else {
	// String filename =
	// Helper.getCustomizedFileName(config.getDefaultFileNameImages(), "."
	// + targetFormat.getFileExtension());
	// targetFileName.append(filename);
	// }
	// response.setHeader("Content-Disposition", targetFileName.toString());
	// response.setContentType(targetFormat.getMimeType());
	//
	// /* --------------------------------
	// * resolution
	// * --------------------------------*/
	// if (request.getParameter("resolution") != null) {
	// wi.setXResolution(Float.parseFloat(request.getParameter("resolution")));
	// wi.setYResolution(Float.parseFloat(request.getParameter("resolution")));
	// } else {
	// wi.setXResolution(config.getDefaultResolution());
	// wi.setYResolution(config.getDefaultResolution());
	// }
	//
	// /* --------------------------------
	// * write target image to stream
	// * --------------------------------*/
	// wi.writeToStream(response.getOutputStream());
	//
	// } finally {
	// /* close open stream */
	// inputFileStream.close();
	// }
	// }

	/************************************************************************************
	 * validate all parameters of request for image handling, throws IllegalArgumentException if one request parameter is not valid
	 * 
	 * @param request
	 *            {@link HttpServletRequest} of ServletRequest
	 * @throws IllegalArgumentException
	 ************************************************************************************/
	public void validateParameters(HttpServletRequest request) throws IllegalArgumentException {

		/* call super.validation for main parameters of image and pdf actions */
		super.validateParameters(request);

		/* validate path of source image */
		if (request.getParameter("sourcepath") == null && request.getAttribute("sourcepath") == null) {
			throw new IllegalArgumentException("no source path defined (sourcepath)");
		}

		/* validate rotation agle is a number */
		if (request.getParameterMap().containsKey("rotate")) {
			if (!StringUtils.isNumeric(request.getParameter("rotate"))) {
				throw new IllegalArgumentException("rotation angle is not numeric");
			}
		}

		/* validate percent scaling is a number */
		if (request.getParameterMap().containsKey("scale")) {
			if (!StringUtils.isNumeric(request.getParameter("scale"))) {
				throw new IllegalArgumentException("scale is not numeric");
			}
		}

		/* validate width scaling is a number */
		if (request.getParameterMap().containsKey("width")) {
			if (!StringUtils.isNumeric(request.getParameter("width"))) {
				throw new IllegalArgumentException("width is not numeric");
			}
		}

		/* validate height scaling is a number */
		if (request.getParameterMap().containsKey("height")) {
			if (!StringUtils.isNumeric(request.getParameter("height"))) {
				throw new IllegalArgumentException("height is not numeric");
			}
		}

		/* validate resolution */
		if (request.getParameter("resolution") != null) {
			if (!StringUtils.isNumeric(request.getParameter("resolution"))) {
				throw new IllegalArgumentException("resolution is not numeric");
			}
		}

		/* validate highlighting coordinates */
		if (request.getParameter("highlight") != null) {
			String highlight = request.getParameter("highlight");
			StrTokenizer areas = new StrTokenizer(highlight, "$");
			for (String area : areas.getTokenArray()) {
				/* currently only 4 coordinates are supported per area */
				StrTokenizer coordinates = new StrTokenizer(area, ",");
				if (coordinates.getTokenArray().length != 4) {
					throw new IllegalArgumentException("highlight area does not have 4 coordinates: " + coordinates.getContent());
				}

				/* only numbers are permitted */
				for (String coordinate : coordinates.getTokenArray()) {
					if (!StringUtils.isNumeric(coordinate)) {
						throw new IllegalArgumentException("highlight coordinate is not numeric: " + coordinates.getContent());
					}
				}

			} // end for (String area ...
		} // end if (request.getParameter("highlight") ...

	} // end method

	/*************************************************************************************
	 * generate an ID for a pdf file, to cache it under an unique name
	 * 
	 * @param request
	 *            the current {@link HttpServletRequest}
	 * @param inConfig
	 *            current internal {@link ContentServerConfiguration} objekt
	 ************************************************************************************/
	private String getContentCacheIdForRequest(HttpServletRequest request, ContentServerConfiguration inConfig) {
		String myId = "";
		Map<String, String[]> myIdMap = request.getParameterMap();
		for (String s : myIdMap.keySet()) {
			myId += s;
			String[] v = myIdMap.get(s);
			for (String val : v) {
				myId += val;
			}
		}
		

		

		try {
			byte[] defaultBytes = myId.getBytes("UTF-8");
			MessageDigest algorithm = MessageDigest.getInstance("MD5");
			algorithm.reset();
			algorithm.update(defaultBytes);
			byte messageDigest[] = algorithm.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			}
			
			System.out.println("sessionid " + myId + " md5 version is " + hexString.toString());
			myId = hexString + "";

		} catch (NoSuchAlgorithmException nsae) {

		} catch (UnsupportedEncodingException e) {
			
		}
		return myId;

	}
}
