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

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.render.RenderingManager;


/**
 * A JSPWiki tree
 * @author Jumlinator
 */
public class Tree 
{
	// All items, unique by their names
	public HashMap<String, TreeItem> treeItems = new HashMap<String, TreeItem>();
	public Date pageDate = null;
	public boolean isInitialized;
	
	// The unvisible root item on level 0
	public TreeItem rootItem = new TreeItem("root",  "",  null, 0);
	// The name of rhe tree and of the wikipage
	public String name; 
	
	/**
	 * Constructor
	 * @param pPage The wiki-page with tree informations
	 */
	public Tree(String pPage) 
	{
		name = pPage; 
	}

	/**
	 * Searches an item the the HashMap 
	 * @param key
	 * @return null or TreeItem
	 */
	public TreeItem get(String key) 
	{
		return (TreeItem) treeItems.get(key);
	}

	/**
	 * Initializes the tree
	 * @param pagetext
	 * @param context
	 */
	protected void initialize(String pagetext, Context context) 
	{
		treeItems.clear();
		
		if (StringUtils.isNotEmpty(pagetext)) 
		{
			StringTokenizer st = new StringTokenizer(pagetext, System.getProperty("line.separator"));
			int level = 0;
			
			TreeItem last = rootItem;
			TreeItem parent = rootItem;
			
			String lastLine = null;
			
			while (st.hasMoreTokens()) 
			{
				lastLine = st.nextToken().trim();
				String line = lastLine;
				int index = 0;
				while (line.startsWith("*"))
				{
					line = line.substring(1);
					index++;
				}
				
				// wrong number of *?
				if (index > level + 1)
				{
					index = level + 1;
				}
				
				
				if (index != 0)
				{
					if (index > level)
					{
						level++;
						parent = last;
					}
					else if (index < level)
					{
						parent = getParentOnLevel(last, index - 1);
						level = index;
					}
				
					line = line.trim();
                                         
					String link = context.getEngine().getManager(RenderingManager.class).textToHTML(context, line);
					String text = line.replace("[", "");
					text = text.replace("]", "");
					int pipe = text.indexOf("|");
					if (pipe > 0)
					{
						text = text.substring(pipe + 1);
					}
					last = new TreeItem(link, text, parent, index);
					parent.addChild(last);
					treeItems.put(last.text, last);
				}
			}
		}
		isInitialized = true;
		pageDate = new Date();
	}
	
	/**
	 * Searches the parent on the given level
	 * @param item entry item
	 * @param level 
	 * @return null or the item
	 */
	protected TreeItem getParentOnLevel(TreeItem item, int level)
	{
		boolean found = false;
		TreeItem result = item;
		do
		{
			if (result.level == level)
			{
				found = true;
			}
			else
			{
				result = result.parent;
			}
		}
		while (null != result && !found);		
		
		return result;
	}

}
	
	
