/*
 * This file is part of the ContentServer project.
 * Visit the websites for more information. 
 * 		- http://gdz.sub.uni-goettingen.de 
 * 		- http://www.intranda.com 
 * 
 * Copyright 2009, Center for Retrospective Digitization, Göttingen (GDZ),
 * intranda software.
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
package org.goobi.presentation.contentServlet.controller;

import java.util.HashMap;

import javax.servlet.ServletException;

import de.unigoettingen.sub.commons.contentlib.servlet.controller.Action;
import de.unigoettingen.sub.commons.contentlib.servlet.controller.ContentServer;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;
import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;

/************************************************************************************
 * simple contentserver class for requesting images
 * 
 * @version 02.01.2009
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class GoobiContentServer extends ContentServer {
	private static final long serialVersionUID = 1L;
	private static ContentCache cc;

	/************************************************************************************
	 * default constructor for initialization
	 ************************************************************************************/
	@Override
	public void init() throws ServletException {
		super.init();
		ContentServerConfiguration config = ContentServerConfiguration.getInstance();
		try {
			/* initialize ContentCache only, if set in configuration */
			if (config.getContentCacheUse()) {
				cc = new ContentCache(config.getContentCachePath(), config.getContentCacheSize());
			}
		} catch (CacheException e) {
			throw new ServletException("ContentCache for GoobiContentServer can not be initialized", e);
		}
		actions = new HashMap<String, Class<? extends Action>>();
		actions.put("metsimage", GetMetsImageAction.class);
		actions.put("pdf", GetMetsPdfAction.class);
		actions.put("multipdf", GetPdfMultiMetsAction.class);
	}

	/*************************************************************************************
	 * Getter for ContentCache
	 *
	 * @return the cc
	 *************************************************************************************/
	public static ContentCache getContentCache() {
		return cc;
	}

}
