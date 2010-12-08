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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.goobi.presentation.contentServlet.controller.ContentCache;

import de.unigoettingen.sub.commons.contentlib.exceptions.CacheException;
import de.unigoettingen.sub.commons.contentlib.servlet.model.ContentServerConfiguration;

/************************************************************************************
 * simple contentserver class for requesting images
 * 
 * @version 02.01.2009 
 * @author Steffen Hankiewicz
 ************************************************************************************/
public class ContentServer extends HttpServlet {
	private static final Logger logger = Logger.getLogger(ContentServer.class);
	protected Map<String, Class<? extends Action>> actions = null;
	private static ContentCache cc;
	private static ContentCache thumbnailcache;
	
	private static final long serialVersionUID = 1L;

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
			if (config.getThumbnailCacheUse()) {
				thumbnailcache = new ContentCache(config.getThumbnailCachePath(), config.getThumbnailCacheSize());
			}
		} catch (CacheException e) {
			throw new ServletException("ContentCache for GoobiContentServer can not be initialized", e);
		}
		actions = new HashMap<String, Class<? extends Action>>();
		actions.put("image", GetImageAction.class);
		actions.put("pdf", GetPdfAction.class);
	}

	/************************************************************************************
	 * get method for executing contentserver requests
	 ************************************************************************************/
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("Contentserver start");

		/*
		 * -------------------------------- check action-Parameter if empty
		 * execute echo-action --------------------------------
		 */
		String actionString = request.getParameter("action");
		if (actionString == null || actionString.equals("")) {
			actionString = "echo";
		} else {
			actionString = actionString.toLowerCase().trim();
		}
		logger.debug("actionString is:" + actionString);

		/*-------------------------------- 
		 * prepare appropriate action method
		 * --------------------------------*/
		
		Action action = null;
		logger.debug("Implementation class for action " + actionString + " is " + this.actions.get(actionString).getName());
		try {
			action = this.actions.get(actionString).newInstance();
		} catch (InstantiationException e1) {
			logger.error("Can't intantiate Action class for " + actionString, e1);
		} catch (IllegalAccessException e1) {
			logger.error("Illegal Access to Action class for " + actionString, e1);
		}
		/*
		if (actionString.equals("image")) {
			action = new GetImageAction();
		} else if (actionString.equals("pdf")) {
			action = new GetPdfAction();
		} else {
			action = new JspOnlyAction(actionString);
		}
		*/
		if (action == null) {
			action = new JspOnlyAction(actionString);
		}
		logger.debug("action is:" + action.getClass().getName());

		/*-------------------------------- 
		 * execute action method
		 * --------------------------------*/
		try {
			/* run the action */
			action.run(getServletContext(), request, response);
		} catch (Exception e) {
			/* if an error occurs log stacktrace and forward error message */
			logger.error("An error occured", e);
			/*
			 * depending on error reporting parameter show jsp oder image for
			 * errors
			 */
			Action errorAction = new JspOnlyAction("echo");
			if (request.getParameterMap().containsKey("errorReport") && request.getParameter("errorReport").equals("image")) {
				errorAction = new GetErrorReportAction();
			}
			request.setAttribute("error", e.getClass().getSimpleName() + ": " + e.getMessage());
			try {
				/* execute error report action */
				errorAction.run(getServletContext(), request, response);
			} catch (Exception e2) {
				logger.error("An error occured", e2);
			}
		}
		logger.debug("Contentserver end");
	}

	/************************************************************************************
	 * post-method for contentserver requests, simply forwards request to get
	 * method
	 ************************************************************************************/
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doGet(req, resp);
	}
	
	/*************************************************************************************
	 * Getter for ContentCache
	 *
	 * @return the cc
	 *************************************************************************************/
	public static ContentCache getContentCache() {
		return cc;
	}

	/**
	 * @return the thumbnailcache
	 */
	public static ContentCache getThumbnailCache() {
		return thumbnailcache;
	}

}
