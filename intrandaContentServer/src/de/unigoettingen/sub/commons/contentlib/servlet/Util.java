package de.unigoettingen.sub.commons.contentlib.servlet;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.log4j.helpers.Loader;

public class Util {

	/************************************************************************************
	 * get {@link File} from application root path in file system
	 * 
	 * @return {@link File} for root path
	 ************************************************************************************/
	public static File getBaseFolderAsFile() {
		File basefolder;
		// TODO: GDZ: Do we really need to depend on Log4J here? I don't think so...
		URL url = Loader.getResource("");
		
		if (!url.getProtocol().startsWith("file"))
			return new File(".");
		
		try {
			basefolder = new File(url.toURI());
		} catch (URISyntaxException ue) {
			basefolder = new File(url.getPath());
		}
		return basefolder;
	}

}
