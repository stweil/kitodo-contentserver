/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 * 		- http://gdz.sub.uni-goettingen.de 
 * 		- http://www.intranda.com 
 * 		- http://www.digiverso.com
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software
 * 
 * This is the extended version updated by intranda
 * Copyright 2012, intranda GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the “License�?);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an “AS IS�? BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.intranda.testICS;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.PdfCopyFields;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.TiffInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;

/**
 * The Class ImageHelper.
 */
public class ImageHelper {

	/** Logger for this class. */
	private static final Logger logger = Logger.getLogger(ImageHelper.class);

	/**
	 * Open image.
	 * 
	 * @param sourcePath
	 *            the source path
	 * @return the image manager
	 */
	public static ImageManager openImage(String sourcePath) {

		ImageManager sourcemanager = null;

		try {
			sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		return sourcemanager;
	}

	public static Rectangle getBounds(String sourcePath) {

		ImageManager sourcemanager = null;

		try {
			sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());
			int width = sourcemanager.getMyInterpreter().getWidth();
			int height = sourcemanager.getMyInterpreter().getHeight();

			return new Rectangle(width, height);

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

		// logger.info("WIDTH:" + sourcemanager.getMyInterpreter().getWidth());
		// logger.info("HEIGHT:" +
		// sourcemanager.getMyInterpreter().getHeight());

		return null;
	}

	public static void covertImageToTIFF(String sourcePath, String destPath, int compression) throws ParameterNotSupportedException, MalformedURLException,
			ImageManagerException {
		ImageManager sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());

		RenderedImage renderedImage = getRenderedImage(sourcemanager);
		BufferedImage bImage = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedImage);

		try {
			writeTIFFImage(bImage, destPath, compression, sourcemanager.getMyInterpreter().getXResolution(), sourcemanager.getMyInterpreter().getYResolution());
		} finally {
			bImage.flush();
		}
	}

	public static void createImageForOCR(String sourcePath, String destPath, Rectangle selection) {
		ImageManager sourcemanager = null;
		BufferedImage sourceImage = null;
		BufferedImage destImage = null;
		try {
			sourcemanager = new ImageManager(new File(sourcePath).toURI().toURL());
			RenderedImage renderedImage = getRenderedImage(sourcemanager);

			sourceImage = ImageManipulator.fromRenderedToBufferedNoAlpha(renderedImage);
			destImage = new BufferedImage(selection.width, selection.height, sourceImage.getType());

			if (!destImage.createGraphics().drawImage(sourceImage.getSubimage(selection.x, selection.y, selection.width, selection.height), 0, 0, null)) {
				logger.error("Could not convert File."); //$NON-NLS-1$
			}

			writeTIFFImageForOCR(destImage, destPath);
		} catch (Exception e) {
			logger.error(e.getMessage());
		} finally {
			if (sourceImage != null) {
				sourceImage.flush();
			}
			if (destImage != null) {
				destImage.flush();
			}
		}

		// logger.info("WIDTH:" + sourcemanager.getMyInterpreter().getWidth());
		// logger.info("HEIGHT:" +
		// sourcemanager.getMyInterpreter().getHeight());

	}

	/**
	 * Scale image.
	 * 
	 * @param sourcemanager
	 *            the sourcemanager
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 * @return the rendered image
	 */
	public static RenderedImage getRenderedImage(ImageManager sourcemanager) {
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

		RenderedImage targetImage = null;

		try {
			targetImage = sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, false,
					ImageManager.BOTTOM);
		} catch (ImageManipulatorException e) {
			logger.error(e.getMessage(), e);
		}

		return targetImage;
	}

	public static RenderedImage scaleImage(ImageManager sourcemanager, int width, int height) {
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

		/*
		 * -------------------------------- rotate --------------------------------
		 */angle = 0;

		/*
		 * -------------------------------- width: scale image to fixed width --------------------------------
		 */

		scaleX = width;
		scaleY = height;

		int sourceImageWidth = sourcemanager.getMyInterpreter().getWidth();
		int sourceImageHeight = sourcemanager.getMyInterpreter().getHeight();

		if (width / (double) height < sourceImageWidth / (double) sourceImageHeight) {
			scaleType = ImageManager.SCALE_BY_WIDTH;
			scaleX = width;
			scaleY = 0;
			// logger.info("scale image to width:" + scaleX);
		} else {
			scaleType = ImageManager.SCALE_BY_HEIGHT;
			scaleX = 0;
			scaleY = height;
			// logger.info("scale image to height:" + scaleY);
		}

		/*
		 * -------------------------------- prepare target --------------------------------
		 */
		// change to true if watermark should scale

		RenderedImage targetImage = null;

		try {
			targetImage = sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, false,
					ImageManager.BOTTOM);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return targetImage;
	}

	public static RenderedImage rotateImage(ImageManager sourcemanager, int angle) {
		/*
		 * -------------------------------- set the defaults --------------------------------
		 */
		int scaleX = 100;
		int scaleY = 100;
		int scaleType = ImageManager.SCALE_BY_PERCENT;
		LinkedList<String> highlightCoordinateList = null;
		Color highlightColor = null;
		Watermark myWatermark = null;

		/*
		 * -------------------------------- prepare target --------------------------------
		 */
		// change to true if watermark should scale

		RenderedImage targetImage = null;

		try {
			targetImage = sourcemanager.scaleImageByPixel(scaleX, scaleY, scaleType, angle, highlightCoordinateList, highlightColor, myWatermark, false,
					ImageManager.BOTTOM);

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return targetImage;
	}

	/**
	 * Write image.
	 * 
	 * @param targetImage
	 *            the target image
	 * @param targetPath
	 *            the target path
	 */
	public static void writeImage(RenderedImage targetImage, String targetPath) {
		int dotPos = targetPath.lastIndexOf("."); //$NON-NLS-1$
		String extension = targetPath.substring(dotPos);

		ImageFileFormat targetFormat = ImageFileFormat.getImageFileFormatFromFileExtension(extension);
		ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
																		// file

		/*
		 * -------------------------------- set file name and attachment header from parameter or from configuration
		 * --------------------------------
		 */

		wi.setXResolution(100);
		wi.setYResolution(100);

		logger.debug("start writing"); //$NON-NLS-1$

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetPath);
			wi.writeToStream(null, fos);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		logger.debug("finished"); //$NON-NLS-1$
	}

	public static void writeTIFFImage(RenderedImage targetImage, String targetPath, int compression, float xResolution, float yResolution)
			throws ParameterNotSupportedException {
		ImageFileFormat targetFormat = ImageFileFormat.TIFF;
		ImageInterpreter wi = targetFormat.getInterpreter(targetImage);
		wi.setWriterCompressionType(compression);
		wi.setXResolution(xResolution);
		wi.setYResolution(yResolution);

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetPath);
			wi.writeToStream(null, fos);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	public static void writeTIFFImageForOCR(RenderedImage targetImage, String targetPath) {
		//int dotPos = targetPath.lastIndexOf("."); //$NON-NLS-1$

		ImageFileFormat targetFormat = ImageFileFormat.TIFF;
		ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
																		// file

		/*
		 * -------------------------------- set file name and attachment header from parameter or from configuration
		 * --------------------------------
		 */

		wi.setXResolution(100);
		wi.setYResolution(100);

		try {
			wi.setWriterCompressionType(TiffInterpreter.COMPRESSION_NONE);
		} catch (ParameterNotSupportedException e1) {
			logger.error(e1.getMessage());
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetPath);
			wi.writeToStream(null, fos);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	public static void writeTIFFImageTest(RenderedImage targetImage, String targetPath) {
		//int dotPos = targetPath.lastIndexOf("."); //$NON-NLS-1$

		ImageFileFormat targetFormat = ImageFileFormat.TIFF;
		ImageInterpreter wi = targetFormat.getInterpreter(targetImage); // read
																		// file

		/*
		 * -------------------------------- set file name and attachment header from parameter or from configuration
		 * --------------------------------
		 */

		wi.setXResolution(100);
		wi.setYResolution(100);

		try {
			wi.setWriterCompressionType(TiffInterpreter.COMPRESSION_NONE);
		} catch (ParameterNotSupportedException e1) {
			logger.error(e1.getMessage());
		}

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetPath);
			wi.writeToStream(null, fos);
		} catch (FileNotFoundException e) {
			logger.error(e.getMessage());
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * Exchange file extension.
	 * 
	 * @param fileName
	 *            the file name
	 * @param newExtension
	 *            the new extension
	 * @return the string
	 */
	public static String exchangeFileExtension(String fileName, String newExtension) {
		int dotPos = fileName.lastIndexOf("."); //$NON-NLS-1$
		if (dotPos >= 0) {
			String name = fileName.substring(0, dotPos);
			return name + "." + newExtension; //$NON-NLS-1$
		} else {
			return fileName + "." + newExtension; //$NON-NLS-1$
		}
	}

	/*
	 * public static String createThumbsForDir(String dirName, String thumbDirName, int width, int height, boolean force) {
	 * 
	 * 
	 * File dir = new File(dirName); File tempdir = new File(dirName + File.separator + thumbDirName);
	 * 
	 * if (tempdir.exists()) { if (!force) return tempdir.getAbsolutePath(); else if (!FileUtils.deleteDir(tempdir)) {
	 * logger.error("FEHLER: Thumbnail-Directory konnte nicht gel�scht werden." ); return null; } }
	 * 
	 * if (!tempdir.mkdirs()) { logger.error("Thumbnail-Directory konnte nicht geschrieben werden."); return null; }
	 * 
	 * ArrayList<String> filterList = new ArrayList<String>();
	 * 
	 * filterList.add("tif"); filterList.add("tiff"); filterList.add("jpg"); filterList.add("jpeg");
	 * 
	 * FileExtensionsFilter filter = new FileExtensionsFilter(filterList);
	 * 
	 * File[] files = dir.listFiles(filter);
	 * 
	 * for (int i = 0; i < files.length; i++) { File origFile = files[i];
	 * 
	 * String extension = "jpg";
	 * 
	 * String targetFileName = tempdir.getAbsolutePath() + File.separator + exchangeFileExtension(origFile.getName(), extension);
	 * 
	 * logger.info("Target Filename: " + targetFileName);
	 * 
	 * writeImage( scaleImage(openImage(origFile.getAbsolutePath()), width, height), targetFileName);
	 * 
	 * }
	 * 
	 * return tempdir.getAbsolutePath(); }
	 */
	// public static void main(String[] args) {
	// long startMillis = System.currentTimeMillis();
	//
	//		System.out.println("START"); //$NON-NLS-1$
	//
	//		String path = "C:\\Users\\Karsten\\Desktop\\testbilder\\00000006.tif"; //$NON-NLS-1$
	//		//String targetpath = "C:\\Users\\Karsten\\Desktop\\testbilder\\00000006_contentserver.tif"; //$NON-NLS-1$
	//
	// ImageManager imageManager = openImage(path);
	//
	// long stopMillis = System.currentTimeMillis();
	//		System.out.println("OPEN:" + (stopMillis - startMillis)); //$NON-NLS-1$
	// startMillis = System.currentTimeMillis();
	//
	// RenderedImage renderedImage = rotateImage(imageManager, 90);
	//
	// // stopMillis = System.currentTimeMillis();
	// // System.out.println("ROTATED:" + (stopMillis-startMillis));
	// // startMillis = System.currentTimeMillis();
	//
	// // RenderedImage renderedImage = scaleImage(imageManager, 100, 100);
	//
	// System.out.println(renderedImage.getColorModel());
	// System.out.println(renderedImage.getSampleModel());
	//
	// stopMillis = System.currentTimeMillis();
	//		System.out.println("SCALED:" + (stopMillis - startMillis)); //$NON-NLS-1$
	// startMillis = System.currentTimeMillis();
	//
	// //BufferedImage bum =
	// ImageManipulator.fromRenderedToBufferedNoAlpha(renderedImage);
	//
	// //ImageData imageData = GalleryItemRenderer.convertToSWTFast(bum);
	// //writeTIFFImage(renderedImage, targetpath);
	//
	// stopMillis = System.currentTimeMillis();
	//		System.out.println("WRITTEN:" + (stopMillis - startMillis)); //$NON-NLS-1$
	// }

	public static void main(String[] args) {

		System.out.println("START"); //$NON-NLS-1$

		String path = "/opt/digiverso/viewer/tiff/"; //$NON-NLS-1$
		String targetpath = "/opt/digiverso/viewer/cache/"; //$NON-NLS-1$

		String[] fileNameList = { "gehtnicht00000002.jpg" };
//		File[] imageFileList = new File(path, "PPN654596379").listFiles(ImageFilter);
		File[] imageFileList = new File[fileNameList.length];
		for (int i = 0; i < fileNameList.length; i++) {
			imageFileList[i] = new File(path, fileNameList[i]);
			// SystemHelper.dumpMemory();
			//
			//
			// long startMillis = System.currentTimeMillis();
			//
			//			System.out.println("Bild: " + fileNameList[i]); //$NON-NLS-1$
			//			String path = "C:\\Users\\Karsten\\Desktop\\testbilder\\0000000" + i + ".jpg"; //$NON-NLS-1$
			//			String path = "C:\\Users\\Karsten\\Desktop\\testbilder\\00000000.jpg"; //$NON-NLS-1$
			////			String targetpath = "C:\\Users\\Karsten\\Desktop\\testbilder\\res" + i + ".tiff"; //$NON-NLS-1$
			//
			//
			// // ImageManager imageManager = openImage(path);
			// // System.out.println("Time0: " + (System.currentTimeMillis()-startMillis));
			// // RenderedImage renderedImage = imageManager.getMyInterpreter().getRenderedImage();
			// // writeTIFFImageTest(renderedImage, targetpath);
			//
			//
			// System.out.println("--------------");

		}

		ImageHelper helper = new ImageHelper();
		try {
			helper.doGeneration(imageFileList, new File(targetpath, "gehtnicht.pdf"));
		} catch (OutOfMemoryError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("STOP"); //$NON-NLS-1$

	}

	/**
	 * Delete file extension.
	 * 
	 * @param fileName
	 *            the file name
	 * @return the string
	 */
	public static String deleteFileExtension(String fileName) {
		int dotPos = fileName.lastIndexOf("."); //$NON-NLS-1$
		String name = fileName;
		if (dotPos > 0)
			name = fileName.substring(0, dotPos);
		return name;
	}

	/**
	 * Returns the color depth of the image at the given path.
	 * 
	 * @param imagePath
	 *            The path of the image to check.
	 * @return Color depth in bits.
	 */
	public static int getColorDepth(String imagePath) {
		ImageManager imageManager = openImage(imagePath);
		return imageManager.getMyInterpreter().getColordepth();
	}

	public static String[] getImageFileExtensions() {
		String[] extensions = { "*.jpg", "*.jpeg", "*.tiff", "*.tif" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		return extensions;
	}

	public void doGeneration(File[] imageFiles, File pdfFile) throws IOException, DocumentException, OutOfMemoryError {

		if (imageFiles.length > 0) {

			// allImages = reverseFileList(allImages);
			Document pdfDocument = null;
			@SuppressWarnings("unused")
			int pageCount = 1;
			PdfWriter pdfWriter = null;

			pdfDocument = new Document();
			FileOutputStream outputPdfFile = new FileOutputStream(pdfFile);
			pdfWriter = PdfWriter.getInstance(pdfDocument, outputPdfFile);
			pdfDocument.open();

			for (File imageFile : imageFiles) {
				addPage(imageFile, pdfWriter, pdfDocument, 1, 0);
				pageCount++;
			}

			pdfDocument.close();
			pdfWriter.close();
			try {
				if (outputPdfFile != null)
					outputPdfFile.close();
			} catch (IOException e) {
				logger.warn("Error on closing fileoutputstream");
			}
		}
	}

	private void addFrontPage(File frontPage, File pdfFile) throws IOException, DocumentException {
		File tempFile = new File(pdfFile.getParent(), System.currentTimeMillis() + ".pdf");
		pdfFile.renameTo(tempFile);
		PdfReader reader1 = new PdfReader(frontPage.getAbsolutePath());
		PdfReader reader2 = new PdfReader(tempFile.getAbsolutePath());
		PdfCopyFields copy = new PdfCopyFields(new FileOutputStream(pdfFile));
		copy.addDocument(reader1);
		copy.addDocument(reader2);
		copy.close();
		if (tempFile != null && tempFile.isFile())
			tempFile.delete();
	}

	private void addPage(File imageFile, PdfWriter pdfWriter, Document pdfDocument, float shrinkRatio, float rotationDegree) throws DocumentException,
			IOException {

		float pointsPerInch = 200.0f;
		Image pageImage = null;
		float pageImageHeight = 0, pageImageWidth = 0;
		boolean lowMemory = (shrinkRatio == 1 ? false : true);

		URL inputImage = imageFile.toURI().toURL();

		pdfWriter.setFullCompression();
		pdfWriter.setStrictImageSequence(true);
		pdfWriter.setLinearPageMode();

		logger.debug("Out of memory on loading image for pdf generation");
//		 ByteArrayOutputStream stream = new ByteArrayOutputStream();
		 BufferedImage bitmap = ImageIO.read(imageFile);
		// logger.debug( "Size of temporary image bitmap: Width = " + bitmap.getWidth() + "; Height = " + bitmap.getHeight());
		System.out.println("Reading file " + imageFile.getAbsolutePath());
		pageImage = Image.getInstance(bitmap, null, false);
		bitmap.flush();
//		 stream.close();
		
		pageImage.setRotationDegrees(-rotationDegree);
		logger.debug("Image dimensions: Width = " + pageImage.getWidth() + "; Height = " + pageImage.getHeight());
		pageImageHeight = pageImage.getHeight();
		pageImageWidth = pageImage.getWidth();
		pageImage.setAbsolutePosition(0, 0);
		// Rectangle pageRect = new Rectangle(pageImageWidth/shrinkRatio, pageImageHeight/shrinkRatio);
		com.lowagie.text.Rectangle pageRect = new com.lowagie.text.Rectangle(pageImageWidth, pageImageHeight);
		logger.debug("Creating rectangle: Width = " + pageRect.getWidth() + "; Height = " + pageRect.getHeight());
		pdfDocument.setPageSize(pageRect);

		if (pdfDocument.isOpen()) {
			pdfDocument.newPage();
			pdfWriter.getDirectContent().addImage(pageImage);

		} else {
			pdfDocument.open();
			pdfWriter.getDirectContent().addImage(pageImage);
		}
		pdfWriter.flush();
		System.gc();
	}
	
	public static FilenameFilter ImageFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String name) {
			boolean validImage = false;
			// jpeg
			if (name.endsWith("jpg") || name.endsWith("JPG") || name.endsWith("jpeg") || name.endsWith("JPEG")) {
				validImage = true;
			}
			if (name.endsWith(".tif") || name.endsWith(".TIF")) {
				validImage = true;
			}
			// png
			if (name.endsWith(".png") || name.endsWith(".PNG")) {
				validImage = true;
			}
			// gif
			if (name.endsWith(".gif") || name.endsWith(".GIF")) {
				validImage = true;
			}
			// jpeg2000
			if (name.endsWith(".jp2") || name.endsWith(".JP2")) {
				validImage = true;
			}

			return validImage;
		}
	};
}
