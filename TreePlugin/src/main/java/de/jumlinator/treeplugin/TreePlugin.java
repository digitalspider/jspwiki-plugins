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

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.util.TextUtil;

/**
	A simple tree with wikilinks.
	You can define multiple trees.
	The trees are defined in separated wikipages.
	Best place for the plugin is the leftmenu page. 
	Insert "[{de.jumlinator.treeplugin.TreePlugin page='tree'}]" where 
	"tree" is the definition wikipage.
	
	In  the definition wikipage, tree items are defined like a JSPWiki list with wikilinks 
	* [page 1]
	** [page 1.1]
	*** [page 1.1.1]
	** [page 1.2] 
	* [page 2]
	
	You need to have some images located in the wiki image folder
	"folder_closed.png"
	"folder_open.png"
	
	Version 0.9 beta or better
 */
public class TreePlugin implements Plugin 
{
	// Multiple trees ca be defined
	public static HashMap<String, Tree> trees = new HashMap<String, Tree>();

	@Override
	public String execute(Context context, Map<String, String> params) throws PluginException 
	{
		String result = "";
		Engine engine = context.getEngine();
		
		String treePageName = TextUtil.replaceEntities( (String) params.get( "page" ));
		
		try  
		{
			Page page = engine.getManager(PageManager.class).getPage(treePageName);
			if (null == page)
			{
				return "Wikipage " + treePageName + " does not exist!";
			}
			
			String pageText =  engine.getManager(PageManager.class).getPureText(page);
			
			Tree tree = (Tree)trees.get(treePageName);
			if (null == tree)
			{
				tree = new Tree(treePageName);
				trees.put(treePageName, tree);
			}
			
			// Updating the tree
			Date actPageDate = engine.getManager(PageManager.class).getPage(treePageName).getLastModified();
			if (null == tree.pageDate || actPageDate.after(tree.pageDate) ) 
			{
				tree.initialize(pageText, context);
			}
			
			result = createHtml(context, context.getPage().getName(), engine, tree );
		} 
		catch (Exception e) 
		{
			result = e.toString();
			e.printStackTrace(System.err);
		}

		return result;
	}

	/**
	 * Creates the html string
	 * @param context
	 * @param currentItemName
	 * @param engine
	 * @param tree
	 * @return
	 */
	private String createHtml(Context context, String currentItemName, Engine engine, Tree tree) 
	{
		String imgFolderClosed = context.getEngine().getBaseURL()+ "images/folder_closed.png";
		String imgFolderOpen = context.getEngine().getBaseURL()+ "images/folder_open.png";

		TreeItem current = tree.get(currentItemName);
		
		// Is current page a tree page?
		if (current == null) 
		{
			current = tree.rootItem;
		}

		// Preparing TeeItems
		current.isSelected = true;
		current.isVisible = true;
		
		for (TreeItem child : current.children)
		{
			child.isVisible = true;
			child.isLeaf = true;
		}
		
		while (null != current.parent) 
		{
			current = current.parent;
			current.isVisible = true;
			current.isOpen = true; 
			for (TreeItem child : current.children)
			{
				child.isVisible = true;
				child.isLeaf = true;
			}
		}
			
		StringBuffer buffer = new StringBuffer();
		
		// Styles
		buffer.append("<style type=\'text/css\'>\n");
		buffer.append("ul.JumlinatorTree { margin-left:16px;margin-top: 2px;margin-bottom: 4px;padding-left: 3px;}\n");
		buffer.append("li.JumlinatorTreeOpen { list-style-image: url(" + imgFolderOpen + ");font-size:10pt;}\n");
		buffer.append("li.JumlinatorTreeSelected { list-style-image: url(" + imgFolderOpen + ");font-size:10pt;font-weight:bold;}\n");
		buffer.append("li.JumlinatorTree { list-style-image: url(" + imgFolderClosed + ");font-size:10pt;}\n");
		buffer.append("</style>\n");
		
		// Lines
		createTreeItemHtml(buffer, tree.rootItem, context);

		return buffer.toString();
	}

	/**
	 * Creates the html string for the children of a single tree item
	 * Recursive call!
	 * @param buffer
	 * @param parent
	 * @param context
	 */
	private void createTreeItemHtml(StringBuffer buffer, TreeItem parent, Context context) 
	{
		if (null != parent.children && parent.children.size() > 0)
		{
			// Start the list
			buffer.append("<ul class=\"JumlinatorTree\">\n");
			for (TreeItem child : parent.children)
			{
				// Default style
				String style = "JumlinatorTree"; 
				if (child.isVisible)
				{
					if (child.isOpen)
					{
						style = "JumlinatorTreeOpen";
					}
					
					if (child.isSelected)
					{
						style = "JumlinatorTreeSelected";
					}
					
					// li
					buffer.append("<li class=\"" + style + "\" title=\"" + child.text + "\">" + child.link + "</li>\n");
					
					// Lines
					createTreeItemHtml(buffer, child, context);
					
					// reset the settings of createHtml()
					child.reset();
				}
			}
			// Close the list
			buffer.append("</ul>\n");
		}
	}
}
