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
package de.unigoettingen.sub.commons.contentlib.exceptions;


/************************************************************************************
 * CacheException.
 * 
 * @version 15.01.2009 
 * @author Steffen Hankiewicz
 * @author Igor Toker
 * ******************************************************************************** */
public class CacheException extends Exception {
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = -8063045883493803853L;

	/**
	 * Instantiates a new cache exception.
	 */
	public CacheException() {
		super();
	}

	/**
	 * Instantiates a new cache exception.
	 * 
	 * @param inMessage the in message
	 */
	public CacheException(String inMessage) {
		super(inMessage);
	}

	/**
	 * Instantiates a new cache exception.
	 * 
	 * @param incause the incause
	 */
	public CacheException(Throwable incause) {
		super(incause);
	}

	/**
	 * Instantiates a new cache exception.
	 * 
	 * @param inMessage the in message
	 * @param incause the incause
	 */
	public CacheException(String inMessage, Throwable incause) {
		super(inMessage, incause);
	}

}
