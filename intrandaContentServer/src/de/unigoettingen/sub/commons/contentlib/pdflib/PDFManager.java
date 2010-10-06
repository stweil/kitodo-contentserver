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
package de.unigoettingen.sub.commons.contentlib.pdflib;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfBoolean;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfDestination;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfICCBased;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfIndirectObject;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfOutline;
import com.lowagie.text.pdf.PdfPage;
import com.lowagie.text.pdf.PdfPageLabels;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfString;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.xml.xmp.DublinCoreSchema;
import com.lowagie.text.xml.xmp.PdfSchema;
import com.lowagie.text.xml.xmp.XmpSchema;
import com.lowagie.text.xml.xmp.XmpWriter;

import de.unigoettingen.sub.commons.contentlib.exceptions.ImageInterpreterException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ImageManipulatorException;
import de.unigoettingen.sub.commons.contentlib.exceptions.PDFManagerException;
import de.unigoettingen.sub.commons.contentlib.exceptions.ParameterNotSupportedException;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageFileFormat;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManager;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator;
import de.unigoettingen.sub.commons.contentlib.imagelib.ImageManipulator.MergingMode;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.JpegTwoThousandInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.TiffInterpreter;
import de.unigoettingen.sub.commons.contentlib.imagelib.Watermark;
import de.unigoettingen.sub.commons.util.datasource.Structure;
import de.unigoettingen.sub.commons.util.datasource.UrlImage;

/**
 * *****************************************************************************
 * PDFManager controls the generation of pdf files from images.
 * 
 * @version 06.01.2009 
 * @author Markus Enders
 *         ********************************************************
 *         ********************
 */
// TODO: This should use the ImageSource interface
public class PDFManager {

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(PDFManager.class);

	// TODO: Better use a Enum
	/** The Constant PDF_ORIGPAGESIZE. */
	public static final int PDF_ORIGPAGESIZE = 1;

	/** The Constant PDF_A4PAGESIZE. */
	public static final int PDF_A4PAGESIZE = 2;

	/** The Constant PDF_A4PAGESIZE_BOX. */
	public static final int PDF_A4PAGESIZE_BOX = 3;

	/** The Constant PDF_ORIGPAGESIZE_NAME. */
	public static final String PDF_ORIGPAGESIZE_NAME = "original";

	/** The Constant PDF_A4PAGESIZE_NAME. */
	public static final String PDF_A4PAGESIZE_NAME = "A4";

	/** The Constant PDF_A4PAGESIZE_BOX_NAME. */
	public static final String PDF_A4PAGESIZE_BOX_NAME = "A4Box";

	/** The Constant EMBEDD_ORIGBYTESTREAM. */
	public static final int EMBEDD_ORIGBYTESTREAM = 0;

	/** The Constant EMBEDD_RENDEREDIMAGE. */
	public static final int EMBEDD_RENDEREDIMAGE = 1;

	/** The Constant EMBEDD_JPEG. */
	public static final int EMBEDD_JPEG = 2;

	/** The Constant EMBEDD_LOSSYJPEG2000. */
	public static final int EMBEDD_LOSSYJPEG2000 = 3;

	/** The Constant EMBEDD_LOSSLESSJPEG2000. */
	public static final int EMBEDD_LOSSLESSJPEG2000 = 4;

	/** The Constant EMBEDD_TIFFG4. */
	public static final int EMBEDD_TIFFG4 = 5;

	/** The creator. */
	private String creator = null;

	/** The author. */
	private String author = null;

	/** The title. */
	private String title = null;

	/** The subject. */
	private String subject = null;

	/** The keyword. */
	private String keyword = null;

	// TODO: Check if this comments are right
	/** The url watermark. */
	private String urlWatermark = null; // not used yet

	/** The url metadatafile. */
	private String urlMetadatafile = null; // not used yet

	/** The xmp header. */
	private String xmpHeader = null; // not used yet

	/** The pdftitlepage. */
	private PDFTitlePage pdftitlepage = null; // object defining the contents
	// of the PDF title page

	/** The image names. */
	private Map<Integer, String> imageNames = null; // contains all the page
	// numbers
	/** The image ur ls. */
	private Map<Integer, UrlImage> imageURLs = null; //

	/** The pdftitlepages. */
	private HashMap<Integer, PDFTitlePage> pdftitlepages = null; // thsoe
	// title pages are added before the page with the given pagenumber

	/** The structure list. */
	private List<PDFBookmark> structureList = null;

	/** The pdfa. */
	private boolean pdfa = true; // if set to true, PDF/A is created;
	// otherwise not
	/** The iccprofile. */
	private ICC_Profile iccprofile = null; // ICC color profile; needed for
	// PDFA

	/** The always use rendered image. */
	Boolean alwaysUseRenderedImage = false; // uses rendered Image and embedd
											// this into PDF

	/** The always compress to jpeg. */
	Boolean alwaysCompressToJPEG = false;

	/** The embedd bitonal image. */
	Integer embeddBitonalImage = 0;

	/** The embedd greyscale image. */
	Integer embeddGreyscaleImage = 0;

	/** The embedd color image. */
	Integer embeddColorImage = 0;

	/** The httpproxyhost. */
	String httpproxyhost = null;

	/** The httpproxyport. */
	String httpproxyport = null;

	/** The httpproxyuser. */
	String httpproxyuser = null;

	/** The httpproxypassword. */
	String httpproxypassword = null;

	/**
	 * *************************************************************************
	 * The PDFManager class organizes all pdf generation handlings depending on
	 * its parameters the images get compressed, is written as pdf/a etc.
	 * 
	 * The {@link Integer} for the images in HashMap has to start at 1 for
	 * references of image names
	 * ************************************************************************
	 */
	public PDFManager() {
	}

	/**
	 * *************************************************************************
	 * Constructor for {@link PDFManager}.
	 * 
	 * @param inPages
	 *            a {@link Map} with {@link PdfPage}
	 *            **************************************************************
	 *            **********
	 */
	public PDFManager(Map<Integer, UrlImage> inPages) {
		imageURLs = inPages;
		logger.debug("PDFManager intstantiated");
	}

	/**
	 * *************************************************************************
	 * Constructor for {@link PDFManager}.
	 * 
	 * @param inPages
	 *            a {@link Map} with {@link PdfPage}
	 * @param inPdfa
	 *            a boolean set to true, if the pdf should be written in pdf/a
	 *            mode
	 *            **********************************************************
	 *            **************
	 */
	public PDFManager(Map<Integer, UrlImage> inPages, boolean inPdfa) {
		this.pdfa = inPdfa;
		imageURLs = inPages;
		logger.debug("PDFManager intstantiated");
	}

	/**
	 * *************************************************************************
	 * Creates a PDF, which is streams to the OutputStream out. Pagesize mode
	 * may have the following values:<br/>
	 * <b>PDF_ORIGPAGESIZE</b> - every page has the size of the image
	 * <b>PDF_A4PAGESIZE</b> - every page has A4 size, the page image is
	 * horizontally centered <b>PDF_A4PAGESIZE_BOX</b> - same as PDF_A4PAGESIZE,
	 * but a small,black bounding box is drawn around the original page.
	 * 
	 * @param out
	 *            the out
	 * @param pagesizemode
	 *            the pagesizemode
	 * 
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws FileNotFoundException
	 *             the file not found exception
	 * @throws ImageManagerException
	 *             the image manager exception
	 * @throws PDFManagerException
	 *             the PDF manager exception
	 * @throws ImageInterpreterException
	 *             the image interpreter exception
	 * @throws URISyntaxException
	 *             *************************************************************
	 *             ***********
	 */
	public void createPDF(OutputStream out, int pagesizemode, Watermark myWatermark) throws ImageManagerException, FileNotFoundException,
			IOException, PDFManagerException, ImageInterpreterException, URISyntaxException {
		PdfWriter writer = null; // writer for creating the PDF
		Document pdfdoc; // the pdfdocument
		Rectangle pagesize = null; // pagesize of the first page
		PdfPageLabels pagelabels = null; // object to stora all page labels

		if ((imageURLs == null) || (imageURLs.size() == 0)) {
			throw new PDFManagerException("No URLs for images available, HashMap is null or empty");
		}
		// set the page sizes
		pdfdoc = setPDFPageSizeForFirstPage(pagesizemode, pagesize, 80);

		writer = createPDFWriter(out, writer, pdfdoc);

		// set metadata for PDF as author and title

		if (this.title != null) {
			pdfdoc.addTitle(this.title);
		}
		if (this.author != null) {
			pdfdoc.addAuthor(this.author);
		}
		if (this.keyword != null) {
			pdfdoc.addKeywords(this.keyword);
		}
		if (this.subject != null) {
			pdfdoc.addSubject(this.subject);
		}

		// add title page to PDF
		if (pdftitlepage != null) {
			// create a title page
			pdftitlepage.render(pdfdoc);
		}

		// iterate over all files, they must be ordered by the key
		// the key contains the page number (as integer), the String
		// contains the Page name
		pagelabels = addAllPages(pagesizemode, writer, pdfdoc, myWatermark);

		// add page labels
		if (pagelabels != null) {
			writer.setPageLabels(pagelabels);
		}

		// create the required xmp metadata
		// for pdfa
		if (pdfa) {
			writer.createXmpMetadata();
		}

		// close documents and writer
		pdfdoc.close();
		writer.close();

		logger.debug("PDF document and writer closed");
	}

	/**
	 * Adds the all pages.
	 * 
	 * @param pagesizemode
	 *            the pagesizemode
	 * @param writer
	 *            the writer
	 * @param pdfdoc
	 *            the pdfdoc
	 * 
	 * @return the pdf page labels
	 * 
	 * @throws ImageInterpreterException
	 *             the image interpreter exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws MalformedURLException
	 *             the malformed url exception
	 * @throws PDFManagerException
	 *             the PDF manager exception
	 */
	private PdfPageLabels addAllPages(int pagesizemode, PdfWriter writer, Document pdfdoc, Watermark myWatermark) throws ImageInterpreterException,
			IOException, MalformedURLException, PDFManagerException {

		PdfPageLabels pagelabels = new PdfPageLabels();
		int pageadded = 0;
		// sort the HashMap by the KeySet (pagenumber)
		Map<Integer, UrlImage> sortedMap = new TreeMap<Integer, UrlImage>(imageURLs);

		logger.debug("iterate over " + imageURLs.size() + " pages.");
		float scalefactor = 1; // scaling factor of the image
		int page_w = 210; // default page size for A4
		int page_h = 290; //

		for (Integer key : sortedMap.keySet()) {
			Image pdfImage = null; // PDF-Image

			logger.debug("Writing page " + key);

			// pagename (physical page
			// number)
			// check if we have to create a title page before
			// this page number

			if ((pdftitlepages != null) && (pdftitlepages.get(key) != null)) {
				// title page available
				PDFTitlePage pdftitlepage = pdftitlepages.get(key);
				// create new PDF page
				try {
					pdfdoc.setPageSize(PageSize.A4);
					pdfdoc.setMargins(36, 72, 108, 180);
					pageadded++;
					pdfdoc.newPage(); // create new page
					// set page name
					pagelabels.addPageLabel(pageadded, PdfPageLabels.EMPTY, "-");
				} catch (Exception e1) {
					throw new PDFManagerException("PDFManagerException occured while creating new page in PDF", e1);
				}
				// render title page
				pdftitlepage.render(pdfdoc);
			}

			UrlImage pdfpage = imageURLs.get(key);
			if (pdfpage.getURL() != null) {
				boolean emergencyCase = false;

				// it is an image file which must be inserted
				URL url = pdfpage.getURL();
				logger.debug("page:" + key + "  url:" + url.toString());

				ImageInterpreter myInterpreter = ImageFileFormat.getInterpreter(url, httpproxyhost, httpproxyport, httpproxyuser, httpproxypassword);

				// check if image format is directly embeddable
				try {

					// check preferred compresiion type dependent
					// on color depth
					int preferredEmbeddingType = 0;

					if (myInterpreter.getColordepth() == 1) {
						// bitonal image
						preferredEmbeddingType = embeddBitonalImage;
					} else if ((myInterpreter.getColordepth() > 1) && (myInterpreter.getSamplesperpixel() == 1)) {
						// greyscale image
						preferredEmbeddingType = embeddGreyscaleImage;
					} else {
						// color image
						preferredEmbeddingType = embeddColorImage;
					}

					// if the bytestream is directly embeddable
					if (preferredEmbeddingType == PDFManager.EMBEDD_ORIGBYTESTREAM) {
						// should be embedded, but is it the bytestream
						// embeddable?
						if (myInterpreter.pdfBytestreamEmbeddable()) {
							pdfImage = Image.getInstance(myInterpreter.getImageByteStream());
						} else {
							emergencyCase = true;
						}
					} else if (preferredEmbeddingType == PDFManager.EMBEDD_JPEG) {
						// it is NOT embeddable
						//
						RenderedImage ri = null;
						if (myInterpreter.getColordepth() == 1) {
							emergencyCase = true;
							// ImageManager sourcemanager = new
							// ImageManager(url);
							// ri = sourcemanager.scaleImageByPixel(1000, 0,
							// ImageManager.SCALE_BY_WIDTH, 0, null, null,
							// myWatermark, false, ImageManager.BOTTOM);
							// myInterpreter = sourcemanager.getMyInterpreter();
						} else {
							ri = myInterpreter.getRenderedImage();
							if (myWatermark != null) {
								ri = addwatermark(ri, myWatermark, 2);
								myInterpreter.setHeight(myInterpreter.getHeight() + myWatermark.getRenderedImage().getHeight());
							}
						}
						// TODO: scale bitonal images here for correct
						// watermarks
						// if ( myInterpreter.getColordepth() == 1) {
						// ri=ImageManipulator.scaleInterpolationBilinear(ri,
						// 0.1f, 0.1f);
						// }

						if (myInterpreter.getColordepth() > 1) {
							// compress image if greyscale or color
							JpegInterpreter jpint = new JpegInterpreter(ri);
							ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
							jpint.setXResolution(myInterpreter.getXResolution());
							jpint.setYResolution(myInterpreter.getYResolution());
							jpint.writeToStream(null, bytesoutputstream);

							byte[] returnbyteArray = bytesoutputstream.toByteArray();
							pdfImage = Image.getInstance(returnbyteArray);
						} else {
							emergencyCase = true;
						}
					} else if ((preferredEmbeddingType == PDFManager.EMBEDD_LOSSLESSJPEG2000)
							|| ((preferredEmbeddingType == PDFManager.EMBEDD_LOSSYJPEG2000))) {

						RenderedImage ri = myInterpreter.getRenderedImage();
						if (myInterpreter.getColordepth() > 1) {
							// compress image if greyscale or color
							JpegTwoThousandInterpreter jpint = new JpegTwoThousandInterpreter(ri);
							ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
							jpint.setXResolution(myInterpreter.getXResolution());
							jpint.setYResolution(myInterpreter.getYResolution());

							try {
								if (preferredEmbeddingType == PDFManager.EMBEDD_LOSSLESSJPEG2000) {
									jpint.setWriterCompressionType(JpegTwoThousandInterpreter.LOSSLESS);
								} else {
									jpint.setWriterCompressionType(JpegTwoThousandInterpreter.LOSSY);
									jpint.setWriterCompressionValue(80);
								}
							} catch (ParameterNotSupportedException e) {
								// should never happen, as the JPEG200
								// Interpreter supports this
								logger.debug("Can't create JPEG2000 stream for PDF; compression parameter not supported!");
								e.printStackTrace();
							}

							jpint.writeToStream(null, bytesoutputstream);

							byte[] returnbyteArray = bytesoutputstream.toByteArray();
							pdfImage = Image.getInstance(returnbyteArray);
						} else {
							emergencyCase = true;
						}

					} else if (preferredEmbeddingType == PDFManager.EMBEDD_RENDEREDIMAGE) {
						RenderedImage ri = myInterpreter.getRenderedImage();

						// image is NOT compressed as a JPEG, but
						// iText decides what to do with it
						BufferedImage buffImage = ImageManipulator.fromRenderedToBuffered(ri);
						pdfImage = Image.getInstance(buffImage, null, false);
					} else if (preferredEmbeddingType == PDFManager.EMBEDD_TIFFG4) {
						if (myInterpreter.getColordepth() > 1) {
							// it's not a bitonal image
							emergencyCase = true;
						} else {
							TiffInterpreter tiffint = new TiffInterpreter();
							ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
							tiffint.setXResolution(myInterpreter.getXResolution());
							tiffint.setYResolution(myInterpreter.getYResolution());
							try {
								tiffint.setWriterCompressionType(TiffInterpreter.COMPRESSION_CCITTFAX4);
							} catch (ParameterNotSupportedException e) {
								// should never happen, as the TiffInterpreter
								// supports this
								// kind of compression
								logger.warn("Can't create TIFF G4 compressed image for embedding into PDF", e);
							}

							tiffint.writeToStream(null, bytesoutputstream);

							byte[] returnbyteArray = bytesoutputstream.toByteArray();
							pdfImage = Image.getInstance(returnbyteArray);
						}
					}

					//
					// emergency case - image couldn't be embedded yet
					//

					if (emergencyCase) {

						logger.warn("Couldn't use preferred method for embedding the image. Instead had to use JPEG or RenderedImage");
						// image couldn't be embedded yet, try using the JPEG
						// or RenderedImage if bitonal
						RenderedImage ri = null;
						if (preferredEmbeddingType == embeddBitonalImage) {

							ImageManager sourcemanager = new ImageManager(url);
							ri = sourcemanager.scaleImageByPixel(3000, 0, ImageManager.SCALE_BY_WIDTH, 0, null, null, myWatermark, false,
									ImageManager.BOTTOM);
							myInterpreter = sourcemanager.getMyInterpreter();
						} else {
							ri = myInterpreter.getRenderedImage();
							if (myWatermark != null) {
								ri = addwatermark(ri, myWatermark, 2);
								myInterpreter.setHeight(myInterpreter.getHeight() + myWatermark.getRenderedImage().getHeight());
							}
						}
						if (myInterpreter.getColordepth() > 1) {
							// compress image if greyscale or color
							JpegInterpreter jpint = new JpegInterpreter(ri);
							ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
							jpint.setXResolution(myInterpreter.getXResolution());
							jpint.setYResolution(myInterpreter.getYResolution());
							jpint.writeToStream(null, bytesoutputstream);

							byte[] returnbyteArray = bytesoutputstream.toByteArray();
							pdfImage = Image.getInstance(returnbyteArray);
						} else {
							// its bitonal, but can't be embedded directly,
							// need to go via RenderedImage
							BufferedImage buffImage = ImageManipulator.fromRenderedToBuffered(ri);
							pdfImage = Image.getInstance(buffImage, null, false);

							JpegInterpreter jpint = new JpegInterpreter(myWatermark.getRenderedImage());
							ByteArrayOutputStream bytesoutputstream = new ByteArrayOutputStream();
							jpint.setXResolution(myInterpreter.getXResolution());
							jpint.setYResolution(myInterpreter.getYResolution());
							jpint.writeToStream(null, bytesoutputstream);
							byte[] returnbyteArray = bytesoutputstream.toByteArray();
							Image blaImage = Image.getInstance(returnbyteArray);
							Chunk c = new Chunk(blaImage, 200, 200);
							Phrase p = new Phrase(c);
							HeaderFooter hf = new HeaderFooter(p, false);
							pdfdoc.setFooter(hf);
							// pdfdoc.setPageSize(arg0)

						}
					}

				} catch (BadElementException e) {
					throw new PDFManagerException("Can't create a PDFImage from a Buffered Image.", e);
				} catch (ImageManipulatorException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// place the image on the page
				if (pagesizemode == PDF_ORIGPAGESIZE) {
					// calculate the image width and height in points, create
					// the rectangle in points
					float image_w_points = (myInterpreter.getWidth() / myInterpreter.getXResolution()) * 72;
					float image_h_points = ((myInterpreter.getHeight()) / myInterpreter.getYResolution()) * 72;
					Rectangle rect = new Rectangle(image_w_points, image_h_points);

					logger.debug("creating original page sized PDF page:" + image_w_points + " x " + image_h_points);

					// create the pdf page according to this rectangle
					pdfdoc.setPageSize(rect);
					try {
						pageadded++;
						pdfdoc.newPage(); // create new page to put the content
					} catch (Exception e1) {
						throw new PDFManagerException("DocumentException occured while creating new page in PDF", e1);
					}

					// scale image and place it on page; scaling the image does
					// not scale the images bytestream
					pdfImage.scalePercent((72f / myInterpreter.getXResolution() * 100), (72f / myInterpreter.getYResolution() * 100));
					pdfImage.setAbsolutePosition(0, 0); // set image to lower
					// left corner
					boolean result;
					try {
						result = pdfdoc.add(pdfImage); // add it to PDF
						if (!result) {
							throw new PDFManagerException("Image \"" + url.toString()
									+ "\" can's be added to PDF! Error during placing image on page");
						}
					} catch (DocumentException e) {
						throw new PDFManagerException("DocumentException occured while adding the image to PDF", e);
					}
				} else {
					// it is not the original page size
					// PDF will contain only A4 pages
					logger.debug("creating A4 pdf page");

					try {
						pageadded++;
						pdfdoc.setPageSize(PageSize.A4);
						pdfdoc.newPage(); // create new page
					} catch (Exception e1) {
						throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
					}

					float page_w_pixel = (float) (page_w * myInterpreter.getXResolution() / 25.4);
					float page_h_pixel = (float) (page_h * myInterpreter.getYResolution() / 25.4);

					float res_x = myInterpreter.getXResolution();
					float res_y = myInterpreter.getYResolution();

					long w = myInterpreter.getWidth(); // get height and width
					long h = myInterpreter.getHeight();

					// if the page is landscape, we have to rotate the page;
					// this is only done in PDF, the orig
					// image bytestream is NOT rotated
					if (w > h) {
						logger.debug("rotate image");

						// must be rotated
						pdfImage.setRotationDegrees(90);

						// change width and height
						long dummy = w;
						w = h;
						h = dummy;

						// change the resolutions x and y
						float dummy2 = res_x;
						res_x = res_y;
						res_y = dummy2;
					}

					// check, if the image needs to be scaled, because it's
					// bigger than A4 calculate the new scalefactor
					if ((w > page_w_pixel) || (h > page_h_pixel)) {
						logger.debug("scale image to fit the page");

						// System.out.println("DEBUG: Scale image for PDF;
						// doesn't fit on page");
						float scalefactor_w = page_w_pixel / w;
						float scalefactor_h = page_h_pixel / h;

						if (scalefactor_h < scalefactor_w) {
							scalefactor = scalefactor_h;
						} else {
							scalefactor = scalefactor_w;
						}
						w = (long) (w * scalefactor);
						h = (long) (h * scalefactor);
					}
					pdfImage.scalePercent((72f / res_x * 100) * scalefactor, (72f / res_y * 100) * scalefactor);

					// center the image on the page
					float y_offset = 0; // y - offset
					float h_cm = (float) (h / (res_x / 2.54)); // get image
					// size in cm; height
					// float w_cm = (float) (w / (res_y / 2.54)); // and width
					if ((h_cm + 2) < (page_h / 10)) {
						y_offset = 2 * 72f / 2.54f;
					}
					float freespace_x = ((page_w_pixel - w) / res_x * 72f);
					float freespace_y = ((page_h_pixel - h) / res_y * 72f) - (y_offset);

					pdfImage.setAbsolutePosition(freespace_x / 2, freespace_y); // set
					// position
					// add image
					boolean result;
					try {
						result = pdfdoc.add(pdfImage);
					} catch (DocumentException e) {
						logger.error(e);
						throw new PDFManagerException("DocumentException occured while adding the image to PDF", e);
					}
					if (!result) {
						// placing the image in the PDF was not successful
						throw new PDFManagerException("Image \"" + url.toString() + "\" can's be added to PDF! Error during placing image on page");
					}

					if (pagesizemode == PDF_A4PAGESIZE_BOX) {
						logger.debug("draw box around the image page");

						// draw a black frame around the image
						PdfContentByte pcb = writer.getDirectContent();

						// calculate upper left corner of the box (measurment is
						// in points)
						float left_x = (freespace_x / 2);
						float left_y = freespace_y;

						// calculate the lower right corner of the box
						// (measurement is in points)
						float image_w_points = (w / res_x) * 72;
						float image_h_points = (h / res_y) * 72;

						pcb.setLineWidth(1f);
						pcb.stroke();
						pcb.rectangle(left_x, left_y, image_w_points, image_h_points);

						pcb.stroke();
					}

				} // endif of origsize
			} else if (pdfpage.getClass() == PDFPage.class && ((PDFPage) pdfpage).getPdfreader() != null) {
				// it is a page from a PDF file which should be inserted
				PdfContentByte pdfcb = writer.getDirectContent();

				PdfReader pdfreader = ((PDFPage) pdfpage).getPdfreader();
				PdfImportedPage importpage = writer.getImportedPage(pdfreader, pdfpage.getPageNumber());

				if (pagesizemode == PDF_ORIGPAGESIZE) {
					logger.debug("creating orig pdf page");

					Rectangle rect = pdfreader.getPageSize(pdfpage.getPageNumber());

					try {
						pdfdoc.setPageSize(rect);
						pdfdoc.newPage(); // create new page
					} catch (Exception e1) {
						throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
					}

					// add content
					pageadded++;
					pdfcb.addTemplate(importpage, 0, 0);

				} else {
					logger.debug("creating A4 pdf page");
					try {
						pdfdoc.setPageSize(PageSize.A4);
						pdfdoc.newPage(); // create new page
					} catch (Exception e1) {
						throw new PDFManagerException("Exception occured while creating new page in PDF", e1);
					}

					// add content
					pageadded++;
					pdfcb.addTemplate(importpage, 0, 0);

					// draw box
					if (pagesizemode == PDF_A4PAGESIZE_BOX) {

					}
				}

			}
			// handle pagename
			if (imageNames != null) {
				String pagename = imageNames.get(key);

				if (pagename != null) {
					pagelabels.addPageLabel(pageadded, PdfPageLabels.EMPTY, pagename);
				} else {
					pagelabels.addPageLabel(pageadded, PdfPageLabels.EMPTY, "unnumbered");
				}
			}
			// handle bookmarks and set destinator for bookmarks
			logger.debug("handle bookmark(s) for page");

			PdfDestination destinator = new PdfDestination(PdfDestination.FIT);
			setBookmarksForPage(writer, destinator, key); // the key in the
			// mashMap is the pagenumber

		} // end of while iterator over all pages
		return pagelabels;
	}

	private RenderedImage addwatermark(RenderedImage outImage, Watermark inWatermark, Integer watermarkposition) throws ImageManipulatorException {
		RenderedImage watermarkRi = null;
		int orginalSize = outImage.getHeight();
		if (inWatermark != null) {
			// watermark is get as big as image
			if ((watermarkposition == ImageManager.TOP) || (watermarkposition == ImageManager.BOTTOM)) {
				inWatermark.overrideWidth(outImage.getWidth());
			} else {
				inWatermark.overrideHeight(outImage.getHeight());
			}

			watermarkRi = inWatermark.getRenderedImage();

			logger.debug("Watermark size is: " + watermarkRi.getWidth() + " / " + watermarkRi.getHeight());

			// add renderedImage of Watermark to outImage
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

		// ImageManipulator.scaleCoordinates(inCoordinates, scalex, scaley)

		return outImage;
	}

	/**
	 * Creates the pdf writer.
	 * 
	 * @param out
	 *            the out
	 * @param writer
	 *            the writer
	 * @param pdfdoc
	 *            the pdfdoc
	 * 
	 * @return the pdf writer
	 * 
	 * @throws PDFManagerException
	 *             the PDF manager exception
	 */
	private PdfWriter createPDFWriter(OutputStream out, PdfWriter writer, Document pdfdoc) throws PDFManagerException {
		// open the pdfwriter using the outstream
		//
		try {
			writer = PdfWriter.getInstance(pdfdoc, out);
			logger.debug("PDFWriter intstantiated");

			// register Fonts
			int numoffonts = FontFactory.registerDirectories();

			logger.debug(numoffonts + " fonts found and registered!");

			if ((pdfa) && (iccprofile != null)) {
				// we want to write PDFA, we have to set the PDFX conformance
				// before we open the writer
				writer.setPDFXConformance(PdfWriter.PDFA1B);
			}

			// open the pdf document to add pages and other content
			try {
				pdfdoc.open();
				logger.debug("PDFDocument opened");
			} catch (Exception e) {
				throw new PDFManagerException("PdfWriter was opened, but the pdf document couldn't be opened", e);
			}

			if ((pdfa) && (iccprofile != null)) {

				// set the required PDFDictionary which
				// contains the appropriate ICC profile
				PdfDictionary pdfdict_out = new PdfDictionary(PdfName.OUTPUTINTENT);

				// set identifier for ICC profile
				pdfdict_out.put(PdfName.OUTPUTCONDITIONIDENTIFIER, new PdfString("sRGBIEC61966-2.1"));
				pdfdict_out.put(PdfName.INFO, new PdfString("sRGB IEC61966-2.1"));
				pdfdict_out.put(PdfName.S, PdfName.GTS_PDFA1);

				// PdfICCBased ib = new PdfICCBased(iccprofile);
				// writer.setOutputIntents("Custom", "PDF/A sRGB", null, "PDF/A
				// sRGB ICC Profile, sRGB_IEC61966-2-1_withBPC.icc",
				// colorProfileData);

				// read icc profile
				// ICC_Profile icc = ICC_Profile.getInstance(new
				// FileInputStream("c:\\srgb.profile"));
				PdfICCBased ib = new PdfICCBased(iccprofile);
				ib.remove(PdfName.ALTERNATE);

				PdfIndirectObject pio = writer.addToBody(ib);
				pdfdict_out.put(PdfName.DESTOUTPUTPROFILE, pio.getIndirectReference());
				writer.getExtraCatalog().put(PdfName.OUTPUTINTENTS, new PdfArray(pdfdict_out));

				// create MarkInfo elements
				// not sure this is necessary; maybe just needed for tagged PDFs
				// (PDF/A 1a)
				PdfDictionary markInfo = new PdfDictionary(PdfName.MARKINFO);
				markInfo.put(PdfName.MARKED, new PdfBoolean("false"));
				writer.getExtraCatalog().put(PdfName.MARKINFO, markInfo);

				// write XMP
				this.writeXMPMetadata(writer);
			}
		} catch (Exception e) {
			logger.error("Can't open the PdfWriter object\n" + e.toString() + "\n" + e.getMessage());
			throw new PDFManagerException("Can't open the PdfWriter object", e);
		}
		return writer;
	}

	/**
	 * Sets the default size of the page and creates the pdf document
	 * (com.lowagie.text.Document) instance.
	 * 
	 * @param pagesizemode
	 *            the pagesizemode
	 * @param pagesize
	 *            the pagesize
	 * 
	 * @return the pdf-document instance
	 * 
	 * @throws ImageInterpreterException
	 *             the image interpreter exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	private Document setPDFPageSizeForFirstPage(int pagesizemode, Rectangle pagesize, int footer) throws ImageInterpreterException, IOException {

		Document pdfdoc;
		boolean isTitlePage = false;

		// set page size of the PDF
		if ((pagesizemode == PDF_ORIGPAGESIZE) && (pdftitlepage == null)) {
			logger.debug("Page size of the first page is size of first image");

			// GDZ: Check if this changes the order of the Pages
			// What if 0000002 ist intentionaly before 00000001 ?

			// page size is set to size of first page of the document
			// (first image of imageURLs)
			Map<Integer, UrlImage> sortedMap = new TreeMap<Integer, UrlImage>(imageURLs);
			for (Integer key : sortedMap.keySet()) {
				// the

				if ((pdftitlepages != null) && (pdftitlepages.get(key) != null)) {
					// title page for Document Part available; set pagesize to
					// A4
					pagesize = setA4pagesize();
					isTitlePage = true;
					break;
				}

				// no title page, so get the size of the first page
				UrlImage pdfpage = imageURLs.get(key);

				if (pdfpage.getURL() != null) {
					// it's an image file
					URL url = pdfpage.getURL();
					logger.debug("Using image" + pdfpage.getURL().toString());
					ImageInterpreter myInterpreter = ImageFileFormat.getInterpreter(url, httpproxyhost, httpproxyport, httpproxyuser,
							httpproxypassword);

					float xres = myInterpreter.getXResolution();
					float yres = myInterpreter.getYResolution();

					int height = myInterpreter.getHeight();
					int width = myInterpreter.getWidth();

					int image_w_points = (width * 72) / ((int) xres);
					int image_h_points = (height * 72) / ((int) yres);

					pagesize = new Rectangle(image_w_points, image_h_points); // set
					// a retangle in the size of the image
					break; // get out of loop
				} else if (pdfpage.getClass() == PDFPage.class && ((PDFPage) pdfpage).getPdfreader() != null) {
					// a pdf page, not an image file

					PdfReader pdfreader = ((PDFPage) pdfpage).getPdfreader();
					pagesize = pdfreader.getPageSize(pdfpage.getPageNumber());

				}
			}

		} else if (pdftitlepage != null) {
			isTitlePage = true;
			logger.debug("Page size of the first page is A4, cause it is a title page");
			pagesize = setA4pagesize();
		} else {
			// page size is set to A4, because either the whole
			// PDF is in A4 or we will have a title page which is
			// in A4
			logger.debug("Page size of the first page is A4, page size mode is " + pagesizemode);
			pagesize = setA4pagesize();
		}

		if (pagesize != null) { // pagesize is a rectangle; pagesize sets the
			// page for the first page
			pdfdoc = new Document(pagesize, 2.5f * 72f / 2.54f, 2.5f * 72f / 2.54f, 2.5f * 72f / 2.54f, 3f * 72f / 2.54f);
			if (isTitlePage) {
				pdfdoc.setMargins(36, 72, 108, 180);
			}
		} else {
			logger.warn("No pagesize available.... strange!");
			pdfdoc = new Document();
		}
		return pdfdoc;
	}

	/**
	 * *************************************************************************
	 * Sets all the bookmarks which have the same page name for this page. Te
	 * hierachical relationships between bookmarks are recognized
	 * 
	 * @param writer
	 *            the writer
	 * @param pdfdestination
	 *            The PDF destination of the page
	 * @param pagenumber
	 *            the name of the page
	 *            ******************************************
	 *            ******************************
	 */
	private void setBookmarksForPage(PdfWriter writer, PdfDestination pdfdestination, Integer pagenumber) {
		PdfContentByte cb = writer.getDirectContent();
		// PdfOutline rootoutline = cb.getRootOutline();

		// iterate through the tree and find all the bookmarks for this page
		// bookmarks for this page will have the same pagenumber

		if ((structureList == null) || (structureList.size() == 0)) {
			return; // no bookmarks available
		}

		// iterate over all parent bookmarks
		for (PDFBookmark bm : structureList) {
			if (bm.getImageNumber().intValue() == pagenumber.intValue()) {
				// add bookmark
				// rootoutline = cb.getRootOutline(); // get root outline
				PdfOutline outline = new PdfOutline(cb.getRootOutline(), pdfdestination, bm.getContent()); // create
				// a
				// new
				// outline as child
				// of rootoutline
				bm.setPdfOutline(outline);
			}

			checkChildrenBookmarks(bm, pdfdestination, pagenumber); // check for
			// bookmarks
			// children

		}
	}

	/**
	 * *************************************************************************
	 * checks all children of a bookmark and see if any of them fits to the
	 * appropriate page name/ page number.
	 * 
	 * @param parent
	 *            the parent
	 * @param pdfdestination
	 *            the pdfdestination
	 * @param pagenumber
	 *            **************************************************************
	 *            **********
	 */
	private void checkChildrenBookmarks(PDFBookmark parent, PdfDestination pdfdestination, Integer pagenumber) {
		for (PDFBookmark child : parent.getChildren()) {
			if (child == null) {
				// should not happen, but may happen, if we have a logical
				// <div> which
				// does not link to a phys div
				continue; // get next in loop
			}
			if (child.getImageNumber().intValue() == pagenumber.intValue()) {
				// must set a bookmark for this page
				PDFBookmark childsparent = findParentBookmark(child);
				if (childsparent != null) {
					// parent was found, so add this bookmark to the PDF
					PdfOutline parentOutline = childsparent.getPdfOutline();
					if (parentOutline == null) {
						// parent doesn't have an outline probably because
						// it started on a later
						// page - anyhow write something to logfile
						logger.error("Parent Bookmark \"" + childsparent.getContent() + "\"has no PdfOutline.");
					} else {
						PdfOutline outline = new PdfOutline(parentOutline, pdfdestination, child.getContent()); // create
						// a new outline as child of rootoutline
						child.setPdfOutline(outline);
						logger.debug("Bookmark \"" + childsparent.getContent() + "\"set successfully");
					}
				}
			}

			// check children of this child
			checkChildrenBookmarks(child, pdfdestination, pagenumber);
		}

	}

	/**
	 * *************************************************************************
	 * find parent {@link PDFBookmark} from given {@link PDFBookmark}.
	 * 
	 * @param inBookmark
	 *            given {@link PDFBookmark}
	 * 
	 * @return parent {@link PDFBookmark}
	 *         *****************************************************************
	 *         *******
	 */
	private PDFBookmark findParentBookmark(PDFBookmark inBookmark) {
		for (PDFBookmark rootBookmark : structureList) {
			if (rootBookmark.equals(inBookmark)) {
				// bookmark is a root bookmark and therefore has no parent
				return null;
			}

			// search for bookmark just under the current myBookmark
			PDFBookmark foundBookmark = findParentInBranch(inBookmark, rootBookmark);
			if (foundBookmark != null) {
				return foundBookmark;
			}
		}
		return null; // no parent found
	}

	/**
	 * *************************************************************************
	 * find parent {@link PDFBookmark} in branch from given {@link PDFBookmark}.
	 * 
	 * @param inBookmark
	 *            given {@link PDFBookmark}
	 * @param topBookmark
	 *            given {@link PDFBookmark}
	 * 
	 * @return parent {@link PDFBookmark}
	 *         *****************************************************************
	 *         *******
	 */
	private PDFBookmark findParentInBranch(PDFBookmark inBookmark, PDFBookmark topBookmark) {
		for (PDFBookmark checkBM : topBookmark.getChildren()) {
			if (checkBM.equals(inBookmark)) {
				// inBookmark is a child, s return topBookmark as a parent
				return topBookmark;
			}

			// check if the inBookmark is child of any children
			PDFBookmark foundBookmark = findParentInBranch(inBookmark, checkBM);
			if (foundBookmark != null) {
				return foundBookmark;
			}
		}
		return null;
	}

	/**
	 * Write xmp metadata.
	 * 
	 * @param inWriter
	 *            the in writer
	 */
	private void writeXMPMetadata(PdfWriter inWriter) {
		ByteArrayOutputStream os = new ByteArrayOutputStream();

		try {
			XmpWriter xmp = new XmpWriter(os);
			XmpSchema dc = new DublinCoreSchema();

			// set DublinCore metadata
			if (this.subject != null) {
				dc.setProperty(DublinCoreSchema.SUBJECT, this.subject);
			}
			if (this.title != null) {
				dc.setProperty(DublinCoreSchema.TITLE, this.title);
			}
			if (this.author != null) {
				dc.setProperty(DublinCoreSchema.CREATOR, this.author);
			}

			// add the DublinCore Simple Metadata to the RDF container

			xmp.addRdfDescription(dc);

			PdfSchema pdf = new PdfSchema();
			// set keywords
			pdf.setProperty(PdfSchema.KEYWORDS, "Hello World, XMP, Metadata");

			// set the version; must be 1.4 for PDF/A
			pdf.setProperty(PdfSchema.VERSION, "1.4");
			xmp.addRdfDescription(pdf);

			xmp.close();
		} catch (IOException e) {
			logger.error("error occured while writing xmp metadata", e);
		}
		inWriter.setXmpMetadata(os.toByteArray());
	}

	/**
	 * *************************************************************************
	 * create a {@link Rectangle} for DIN A4 format.
	 * 
	 * @return {@link Rectangle} with A4 size
	 *         ***********************************
	 *         *************************************
	 */
	private Rectangle setA4pagesize() {
		int page_w = 210; // dimensions of the page; A4 in mm
		int page_h = 297;

		int page_w_points = (int) ((page_w * 72) / 25.4);
		int page_h_points = (int) ((page_h * 72) / 25.4);
		// front page, it's always A4
		Rectangle pageSize = new Rectangle(page_w_points, page_h_points);
		return pageSize;
	}

	/**
	 * *************************************************************************
	 * Getter for creator.
	 * 
	 * @return the creator
	 *         ******************************************************
	 *         ******************
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * *************************************************************************
	 * Setter for creator.
	 * 
	 * @param creator
	 *            the creator to set
	 *            ********************************************
	 *            ****************************
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * *************************************************************************
	 * Getter for author.
	 * 
	 * @return the author
	 *         *******************************************************
	 *         *****************
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * *************************************************************************
	 * Setter for author.
	 * 
	 * @param author
	 *            the author to set
	 *            *********************************************
	 *            ***************************
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * *************************************************************************
	 * Getter for title.
	 * 
	 * @return the title
	 *         ********************************************************
	 *         ****************
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * *************************************************************************
	 * Setter for title.
	 * 
	 * @param title
	 *            the title to set
	 *            **********************************************
	 *            **************************
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * *************************************************************************
	 * Getter for subject.
	 * 
	 * @return the subject
	 *         ******************************************************
	 *         ******************
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * *************************************************************************
	 * Setter for subject.
	 * 
	 * @param subject
	 *            the subject to set
	 *            ********************************************
	 *            ****************************
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * *************************************************************************
	 * Getter for keyword.
	 * 
	 * @return the keyword
	 *         ******************************************************
	 *         ******************
	 */
	public String getKeyword() {
		return keyword;
	}

	/**
	 * *************************************************************************
	 * Setter for keyword.
	 * 
	 * @param keyword
	 *            the keyword to set
	 *            ********************************************
	 *            ****************************
	 */
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	/**
	 * *************************************************************************
	 * Getter for imageNames.
	 * 
	 * @return the imageNames
	 *         ***************************************************
	 *         *********************
	 */
	public Map<Integer, String> getImageNames() {
		return imageNames;
	}

	/**
	 * *************************************************************************
	 * Sets Image names. The HashMap contains an integer number as a string for
	 * identifying the page number and the name of the page. The {@link Integer}
	 * for the image has to start at 1.
	 * 
	 * @param imageNames
	 *            the imageNames to set, first page Integer must start with 1
	 *            ***
	 *            ************************************************************
	 *            *********
	 */
	public void setImageNames(Map<Integer, String> imageNames) {
		this.imageNames = imageNames;
	}

	/**
	 * *************************************************************************
	 * Getter for imageURLs.
	 * 
	 * @return the imageURLs
	 *         ****************************************************
	 *         ********************
	 */
	public Map<Integer, UrlImage> getImageURLs() {
		return this.imageURLs;
	}

	/**
	 * *************************************************************************
	 * Getter for urlWatermark.
	 * 
	 * @return the urlWatermark
	 *         *************************************************
	 *         ***********************
	 */
	public String getUrlWatermark() {
		return urlWatermark;
	}

	/**
	 * Sets the iccprofile.
	 * 
	 * @param iccprofile
	 *            the iccprofile to set
	 */
	public void setIccprofile(ICC_Profile iccprofile) {
		this.iccprofile = iccprofile;
	}

	/**
	 * *************************************************************************
	 * Setter for urlWatermark.
	 * 
	 * @param urlWatermark
	 *            the urlWatermark to set
	 *            ***************************************
	 *            *********************************
	 */
	public void setUrlWatermark(String urlWatermark) {
		this.urlWatermark = urlWatermark;
	}

	/**
	 * *************************************************************************
	 * Getter for url_metadatafile.
	 * 
	 * @return the url_metadatafile
	 *         *********************************************
	 *         ***************************
	 */
	public String getUrlMetadatafile() {
		return urlMetadatafile;
	}

	/**
	 * *************************************************************************
	 * Setter for urlMetadatafile.
	 * 
	 * @param urlMetadatafile
	 *            the urlMetadatafile to set
	 *            ************************************
	 *            ************************************
	 */
	public void setUrlMetadatafile(String urlMetadatafile) {
		this.urlMetadatafile = urlMetadatafile;
	}

	/**
	 * *************************************************************************
	 * Getter for xmpHeader.
	 * 
	 * @return the xmpHeader
	 *         ****************************************************
	 *         ********************
	 */
	public String getXmpHeader() {
		return xmpHeader;
	}

	/**
	 * *************************************************************************
	 * Setter for xmpHeader.
	 * 
	 * @param xmpHeader
	 *            the xmp header
	 */
	public void setXmpHeader(String xmpHeader) {
		this.xmpHeader = xmpHeader;
	}

	/**
	 * *************************************************************************
	 * Getter for rootBookmarkList.
	 * 
	 * @return the rootBookmarkList
	 *         *********************************************
	 *         ***************************
	 */
	public List<? extends Structure> getStructureList() {
		return structureList;
	}

	/**
	 * *************************************************************************
	 * Setter for rootBookmarkList.
	 * 
	 * @param structureList
	 *            the structure list
	 */
	public void setStructureList(List<? extends Structure> structureList) {
		this.structureList = PDFBookmark.convertList(structureList);
	}

	/**
	 * This is the only mandatory method which must be called before createPDF
	 * is called and the PDF is created. The HashMap must contain a String for
	 * order (an integer number as a string) and the URL
	 * 
	 * @param imageURLs
	 *            the imageURLs to set
	 */
	public void setImageURLs(HashMap<Integer, UrlImage> imageURLs) {
		this.imageURLs = imageURLs;
	}

	/**
	 * A PDF file may consists of several parts. These parts may have their own
	 * title page. The integer contains the pagenumber before the appropriate
	 * title page is added to the PDF.
	 * 
	 * @return the pdftitlepages
	 */
	public HashMap<Integer, PDFTitlePage> getPdftitlepages() {
		return pdftitlepages;
	}

	/**
	 * Sets the pdftitlepages.
	 * 
	 * @param pdftitlepages
	 *            the pdftitlepages to set
	 */
	public void setPdftitlepages(HashMap<Integer, PDFTitlePage> pdftitlepages) {
		this.pdftitlepages = pdftitlepages;
	}

	/**
	 * *************************************************************************
	 * Getter for pdftitlepage.
	 * 
	 * @return the pdftitlepage
	 *         *************************************************
	 *         ***********************
	 */
	public PDFTitlePage getPdftitlepage() {
		return pdftitlepage;
	}

	/**
	 * *************************************************************************
	 * Setter for pdftitlepage.
	 * 
	 * @param pdftitlepage
	 *            the pdftitlepage to set
	 *            ***************************************
	 *            *********************************
	 */
	public void setPdftitlepage(PDFTitlePage pdftitlepage) {
		this.pdftitlepage = pdftitlepage;
	}

	// /**
	// * Checks if is always use rendered image.
	// *
	// * @return the alwaysUseRenderedImage
	// */
	// private boolean isAlwaysUseRenderedImage() {
	// return alwaysUseRenderedImage;
	// }

	/**
	 * Checks if is always compress to jpeg.
	 * 
	 * @return the alwaysCompressToJPEG
	 */
	public boolean isAlwaysCompressToJPEG() {
		return alwaysCompressToJPEG;
	}

	/**
	 * Sets the always use rendered image.
	 * 
	 * @param alwaysUseRenderedImage
	 *            the alwaysUseRenderedImage to set
	 */
	// TODO: there is a bug in here, since it only works correctly if this
	// method is called before setAlwaysCompressToJPEG
	public void setAlwaysUseRenderedImage(boolean alwaysUseRenderedImage) {
		this.alwaysUseRenderedImage = alwaysUseRenderedImage;

		// set everything to rendered image
		this.embeddBitonalImage = PDFManager.EMBEDD_RENDEREDIMAGE;
		this.embeddGreyscaleImage = PDFManager.EMBEDD_RENDEREDIMAGE;
		this.embeddColorImage = PDFManager.EMBEDD_RENDEREDIMAGE;
	}

	/**
	 * Sets the always compress to jpeg.
	 * 
	 * @param alwaysCompressToJPEG
	 *            the alwaysCompressToJPEG to set
	 */
	public void setAlwaysCompressToJPEG(boolean alwaysCompressToJPEG) {
		this.alwaysCompressToJPEG = alwaysCompressToJPEG;

		// set everything to jpeg
		this.embeddBitonalImage = PDFManager.EMBEDD_JPEG;
		this.embeddGreyscaleImage = PDFManager.EMBEDD_JPEG;
		this.embeddColorImage = PDFManager.EMBEDD_JPEG;
	}

	/**
	 * Gets the embedd bitonal image.
	 * 
	 * @return the embedd bitonal image
	 */
	public int getEmbeddBitonalImage() {
		return embeddBitonalImage;
	}

	/**
	 * Sets the embedd bitonal image.
	 * 
	 * @param embeddBitonalImage
	 *            the new embedd bitonal image
	 */
	public void setEmbeddBitonalImage(int embeddBitonalImage) {
		this.embeddBitonalImage = embeddBitonalImage;
	}

	/**
	 * Gets the embedd greyscale image.
	 * 
	 * @return the embedd greyscale image
	 */
	public int getEmbeddGreyscaleImage() {
		return embeddGreyscaleImage;
	}

	/**
	 * Sets the embedd greyscale image.
	 * 
	 * @param embeddGreyscaleImage
	 *            the new embedd greyscale image
	 */
	public void setEmbeddGreyscaleImage(int embeddGreyscaleImage) {
		this.embeddGreyscaleImage = embeddGreyscaleImage;
	}

	/**
	 * Gets the embedd color image.
	 * 
	 * @return the embedd color image
	 */
	public int getEmbeddColorImage() {
		return embeddColorImage;
	}

	/**
	 * Sets the embedd color image.
	 * 
	 * @param embeddColorImage
	 *            the new embedd color image
	 */
	public void setEmbeddColorImage(int embeddColorImage) {
		this.embeddColorImage = embeddColorImage;
	}

	/**
	 * Checks if is pdfa.
	 * 
	 * @return the pdfa value
	 */
	public boolean isPdfa() {
		return pdfa;
	}

	/**
	 * Sets the pdfa.
	 * 
	 * @param pdfa
	 *            the pdfa to set
	 */
	public void setPdfa(boolean pdfa) {
		this.pdfa = pdfa;
	}

	//
	// http configuration, setter for proxy
	//

	/**
	 * Gets the httpproxyhost.
	 * 
	 * @return the httpproxyhost
	 */
	public String getHttpproxyhost() {
		return httpproxyhost;
	}

	/**
	 * Sets the httpproxyhost.
	 * 
	 * @param httpproxyhost
	 *            the httpproxyhost to set
	 */
	public void setHttpproxyhost(String httpproxyhost) {
		this.httpproxyhost = httpproxyhost;
	}

	/**
	 * Gets the httpproxyport.
	 * 
	 * @return the httpproxyport
	 */
	public String getHttpproxyport() {
		return httpproxyport;
	}

	/**
	 * Sets the httpproxyport.
	 * 
	 * @param httpproxyport
	 *            the httpproxyport to set
	 */
	public void setHttpproxyport(String httpproxyport) {
		this.httpproxyport = httpproxyport;
	}

	/**
	 * Gets the httpproxyuser.
	 * 
	 * @return the httpproxyuser
	 */
	public String getHttpproxyuser() {
		return httpproxyuser;
	}

	/**
	 * Sets the httpproxyuser.
	 * 
	 * @param httpproxyuser
	 *            the httpproxyuser to set
	 */
	public void setHttpproxyuser(String httpproxyuser) {
		this.httpproxyuser = httpproxyuser;
	}

	/**
	 * Gets the httpproxypassword.
	 * 
	 * @return the httpproxypassword
	 */
	public String getHttpproxypassword() {
		return httpproxypassword;
	}

	/**
	 * Sets the httpproxypassword.
	 * 
	 * @param httpproxypassword
	 *            the httpproxypassword to set
	 */
	public void setHttpproxypassword(String httpproxypassword) {
		this.httpproxypassword = httpproxypassword;
	}

	/**
	 * Gets the page size from a string.
	 * 
	 * @param strPageSize
	 *            the str page size
	 * 
	 * @return the page sizefrom string
	 */
	public static Integer getPageSizefromString(String strPageSize) {
		if (strPageSize.equalsIgnoreCase(PDF_A4PAGESIZE_NAME)) {
			return PDF_A4PAGESIZE;
		}
		if (strPageSize.equalsIgnoreCase(PDF_ORIGPAGESIZE_NAME)) {
			return PDF_ORIGPAGESIZE;
		}
		if (strPageSize.equalsIgnoreCase(PDF_A4PAGESIZE_BOX_NAME)) {
			return PDF_A4PAGESIZE_BOX;
		}
		return null;
	}
}
