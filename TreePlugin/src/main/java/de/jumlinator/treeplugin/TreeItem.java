/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
*/
package de.jumlinator.treeplugin;

import java.util.*;



/**
 * Definition of a single tree entry 
 */
public class TreeItem
{
	public TreeItem parent;
	public List<TreeItem> children = new ArrayList<TreeItem>();
	public String text;
	public String link;
	public int level;
	
	public boolean isVisible = false;
	public boolean isLeaf = false;
	public boolean isSelected = false;
	public boolean isOpen = false;
	

	/**
	 * Constructor
	 *
	 */
	public TreeItem(String link, String text, TreeItem parent, int level) 
	{
		this.link = link;
		this.text = text;
		this.parent = parent;
		this.level = level;
	}

	public void addChild(TreeItem child) 
	{
		children.add(child);
	}


	/**
	 * 
	 * @return true, if there is no parent
	 */
	public boolean isRoot() 
	{
		return null == parent;
	}
	
	public void reset()
	{
		isVisible = false;
		isLeaf = false;
		isSelected = false;
		isOpen = false;
	}
	
	
	
}
