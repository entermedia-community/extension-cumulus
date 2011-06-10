/*
 * Created on Nov 27, 2006
 */
package com.openedit.archive.cumulus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openedit.entermedia.Category;
import org.openedit.entermedia.ConvertStatus;
import org.openedit.entermedia.MediaArchive;

import com.canto.cumulus.CatalogCollection;
import com.canto.cumulus.Categories;
import com.canto.cumulus.Field;
import com.canto.cumulus.Fields;
import com.canto.cumulus.Record;
import com.canto.cumulus.ServerCatalog;
import com.openedit.config.Configuration;
import com.openedit.users.Group;
import com.openedit.users.User;
import com.openedit.users.UserManager;

public class CumulusUserConverter extends BaseCumulusConvert
{
	protected UserManager fieldUserManager;
	private static final Log log = LogFactory.getLog(CumulusUserConverter.class);

	public void importAssets(MediaArchive archive, ConvertStatus inErrorLog) throws Exception
	{
		Configuration config = getSettings(archive);
		String imUsers = config.getChildValue("importusers");
		if (Boolean.parseBoolean(imUsers))
		{
			ServerCatalog cat = getServerCatalogs().getServerCatalog("$Users");
			parseUsers(cat, archive, inErrorLog);
		}
		String imUsersNames = config.getChildValue("importusernames");
		if (Boolean.parseBoolean(imUsersNames))
		{
			ServerCatalog cat = getServerCatalogs().getServerCatalog("$Users");
			parseUserNames(cat, archive);
		}
	}

	/*
	 * Loop over all users
	 * Find Category with parent of $Roles
	 * Add user to that group
	 * 
	 * then loop over all the $Roles and update the group accordingly
	 * 
	 * 
	 */
	public void parseUsers(ServerCatalog inUserCat, MediaArchive inArchive, ConvertStatus inErrorLog) throws Exception
	{
		CatalogCollection col = inUserCat.open();
		
		for (int i = 0; i < col.getRecords().countRecords(); i++) 
		{
				Record record = col.getRecords().getRecord(i);
				Fields fields = record.getFields();
				String username = getValue(fields.getField("Unique User ID"));
				String email = getValue(fields.getField("E-Mail Address"));
				User oeUser = getUserManager().getUser(username);
				if (oeUser == null)
				{
					oeUser = getUserManager().createUser(username, "password"); //Nice default!!
					inErrorLog.add("Creating : " + username);
					Group guest = getUserManager().getGroup("users");
					if (guest != null)
					{
						oeUser.addGroup(guest);
					}					
					oeUser.setEmail(email);
					//parsePermissions( inArchive, oeUser,col);
					log.info("Saving user: " + oeUser.getUserName());
					getUserManager().saveUser(oeUser);
				}
				else
				{
					if(oeUser.getEmail() == null && email != null)
					{
						oeUser.setEmail(email);
						log.info("Adding E-Mail: "+oeUser.getEmail()+ " For User:" + oeUser.getUserName());
						getUserManager().saveUser(oeUser);
					}
				}
		}
		col.close();
	}

	public void parseUserNames(ServerCatalog inUserCat, MediaArchive inArchive) throws Exception
	{
		CatalogCollection col = inUserCat.open();
		//loop over each record found
		com.canto.cumulus.Category roles = col.getCategories().getCategory("$Roles");
		Categories categories = roles.getSubCategories();
		for (int i = 0; i < categories.countCategories(); i++)
		{
			com.canto.cumulus.Category user = categories.getCategory(i);
			String username = user.getName();

			User oeUser = getUserManager().getUser(username);
			if (oeUser == null)
			{
				oeUser = getUserManager().createUser(username, "password");
				Group guest = getUserManager().getGroup("users");
				if (guest != null)
				{
					oeUser.addGroup(guest);
				}
				log.info("Saving user: " + oeUser.getUserName());
				getUserManager().saveUser(oeUser);
			}
		}

		col.close();

	}
	
	
	protected void parsePermissions(MediaArchive inArchive, User oeUser, CatalogCollection col) throws Exception
	{
		//loop over each record found
		com.canto.cumulus.Category roles = col.getCategories().getCategory("$Roles");
		Categories categories = roles.getSubCategories();
		for (int i = 0; i < categories.countCategories(); i++)
		{
			com.canto.cumulus.Category user = categories.getCategory(i);

			List includeCatalogList = new ArrayList();
			List showCatalogList = new ArrayList();
			//dump("User",user);
			com.canto.cumulus.Category catalogs = user.getSubCategories().getCategory("$Catalogs");

			for (int j = 0; j < catalogs.getSubCategories().countCategories(); j++)
			{
				com.canto.cumulus.Category cat = catalogs.getSubCategories().getCategory(j);

				//hide records
				Field recordFilterField = cat.getFields().getField("Live Filtering Record Query");
				if (recordFilterField.hasValue())
				{
					String recordFilter = String.valueOf(recordFilterField.getValue());
					String hideRecords[] = recordFilter.split("\nor");
					for (int k = 0; k < hideRecords.length; k++)
					{
						String hide = hideRecords[k].trim();
						if (hide.length() > 0)
						{
							String includeCatalog = hide.substring(hide.indexOf("\tis") + 3).trim();
							includeCatalogList.add(includeCatalog);
						}
					}
				}

				//hide catalog
				Field categoryFilterField = cat.getFields().getField(
					"Live Filtering Category Query");
				if (categoryFilterField.hasValue())
				{
					String categoryFilter = String.valueOf(categoryFilterField.getValue());
					String hideCatalogs[] = categoryFilter.split("\nor");
					for (int k = 0; k < hideCatalogs.length; k++)
					{
						String hide = hideCatalogs[k].trim();
						if (hide.length() > 0)
						{
							String showCatalog = hide.substring(hide.indexOf("\tis") + 3).trim();
							showCatalogList.add(showCatalog);
						}
					}
				}

				if (includeCatalogList.size() > 0)
				{
					Category photo = inArchive.getCategoryArchive().getCategoryByName(
						cat.getName());
					for (Iterator iterator = photo.getChildren().iterator(); iterator.hasNext();)
					{
						Category childCat = (Category) iterator.next();
						if (!includeCatalogList.contains(childCat.getName()))
						{
							oeUser.put("excludecatalog:" + childCat.getId(), "true");
						}
					}
				}

				if (showCatalogList.size() > 0)
				{
					Category photo = inArchive.getCategoryArchive().getCategoryByName(
						cat.getName());
					for (Iterator iterator = photo.getChildren().iterator(); iterator.hasNext();)
					{
						Category childCat = (Category) iterator.next();
						if (!showCatalogList.contains(childCat.getName()))
						{
							oeUser.put("hidecatalog:" + childCat.getId(), "true");
						}
					}
				}
			}
		}

	}
	
	public UserManager getUserManager()
	{
		return fieldUserManager;
	}

	public void setUserManager(UserManager inUserManager)
	{
		fieldUserManager = inUserManager;
	}

}
