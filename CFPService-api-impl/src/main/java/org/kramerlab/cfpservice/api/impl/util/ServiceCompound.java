package org.kramerlab.cfpservice.api.impl.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.kramerlab.cfpservice.api.impl.html.ExtendedHtmlReport;
import org.mg.javalib.util.StringUtil;
import org.rendersnake.HtmlAttributesFactory;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.Renderable;

public abstract class ServiceCompound implements Renderable
{
	public enum Service
	{
		pubchem, chembl, all
	}

	public abstract String getJsonUrl(String encodedSmiles);

	protected abstract void parseJson(JSONObject obj) throws JSONException;

	protected abstract String getLinkout();

	protected abstract String getServiceName();

	protected abstract String getIdName();

	public static String get(Service service, String smiles) throws MalformedURLException, JSONException, IOException
	{
		if (service == Service.pubchem)
			return new PubChemCompound(smiles).getHTML();
		else if (service == Service.chembl)
			return new ChEMBLCompound(smiles).getHTML();
		else if (service == Service.all)
		{
			HtmlCanvas html = new HtmlCanvas();
			html.macros().stylesheet(ExtendedHtmlReport.css);
			html.body(HtmlAttributesFactory.class_("pubchem"));
			ServiceCompound p = new PubChemCompound(smiles);
			ServiceCompound c = new ChEMBLCompound(smiles);
			if (p.id != null)
				p.renderOn(html);
			if (p.id != null && c.id != null)
				html.br();
			if (c.id != null)
				c.renderOn(html);
			html._body();
			return html.toHtml();
		}
		else
			throw new IllegalArgumentException();
	}

	public static class PubChemCompound extends ServiceCompound
	{
		public PubChemCompound(String smiles)
		{
			super(smiles);
		}

		@Override
		public String getJsonUrl(String encodedSmiles)
		{
			return "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/smiles/" + encodedSmiles + "/JSON";
		}

		@Override
		protected void parseJson(JSONObject obj) throws JSONException
		{
			obj = obj.getJSONArray("PC_Compounds").getJSONObject(0);
			id = obj.getJSONObject("id").getJSONObject("id").getString("cid");
			JSONArray props = obj.getJSONArray("props");
			for (int i = 0; i < props.length(); i++)
			{
				if (props.getJSONObject(i).getJSONObject("urn").getString("label").equals("IUPAC Name"))
				{
					name = props.getJSONObject(i).getJSONObject("value").getString("sval");
					if (props.getJSONObject(i).getJSONObject("urn").getString("name").equals("Traditional"))
						break;
				}
			}
			for (int i = 0; i < props.length(); i++)
			{
				if (props.getJSONObject(i).getJSONObject("urn").getString("label").equals("Log P"))
					fields.put("LogP", StringUtil.formatDouble(
							Double.parseDouble(props.getJSONObject(i).getJSONObject("value").getString("fval")), 1));
				if (props.getJSONObject(i).getJSONObject("urn").getString("label").equals("Molecular Weight"))
					fields.put("Molecular Weight", StringUtil.formatDouble(
							Double.parseDouble(props.getJSONObject(i).getJSONObject("value").getString("fval")), 1));
			}
		}

		@Override
		protected String getLinkout()
		{
			return "https://pubchem.ncbi.nlm.nih.gov/compound/" + id;
		}

		@Override
		protected String getServiceName()
		{
			return "PubChem";
		}

		@Override
		protected String getIdName()
		{
			return "CID";
		}
	}

	public static class ChEMBLCompound extends ServiceCompound
	{

		public ChEMBLCompound(String smiles) throws MalformedURLException, JSONException, IOException
		{
			super(smiles);
		}

		@Override
		public String getJsonUrl(String encodedSmiles)
		{
			return "https://www.ebi.ac.uk/chembl/api/data/molecule/" + encodedSmiles + "?format=json";
		}

		@Override
		protected void parseJson(JSONObject obj) throws JSONException
		{
			id = obj.getString("molecule_chembl_id");
			name = obj.getString("pref_name");
		}

		@Override
		protected String getServiceName()
		{
			return "ChEMBL";
		}

		@Override
		protected String getIdName()
		{
			return "ID";
		}

		@Override
		protected String getLinkout()
		{
			return "https://www.ebi.ac.uk/chembldb/index.php/compound/inspect/" + id;
		}

	}

	protected String id;
	protected String name;
	protected HashMap<String, String> fields = new LinkedHashMap<String, String>();

	public ServiceCompound(String smiles)
	{
		System.out.println(smiles);
		try
		{
			JSONObject obj = new JSONObject(IOUtils.toString(new URL(getJsonUrl(URLEncoder.encode(smiles, "UTF-8"))),
					Charsets.UTF_8));
			parseJson(obj);
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Not found " + e.getMessage());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public String getHTML()
	{
		try
		{
			HtmlCanvas html = new HtmlCanvas();
			html.macros().stylesheet(ExtendedHtmlReport.css);
			html.body(HtmlAttributesFactory.class_("pubchem"));
			renderOn(html);
			html._body();
			return html.toHtml();
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public void renderOn(HtmlCanvas html) throws IOException
	{
		html.div();
		html.write(getServiceName() + " ");
		html.a(HtmlAttributesFactory.href(getLinkout()).target("_blank"));
		html.img(HtmlAttributesFactory.src("/img/iconExternalLink.gif"));
		html._a();
		html.br();

		html.write(getIdName() + ": ");
		html.write(id + " ");
		html.br();

		String nameStr = name != null ? name : "";
		if (nameStr.length() > 40)
			nameStr = name.substring(0, 38) + "..";
		html.write("Name: " + nameStr);
		for (String k : fields.keySet())
		{
			html.br();
			html.write(k + ": " + fields.get(k));
		}
		html._div();
	}

	public static void main(String[] args) throws MalformedURLException, JSONException, IOException
	{
		String smiles = "c1ccccc1";
		System.out.println(new PubChemCompound(smiles).getHTML());
		System.out.println(new ChEMBLCompound(smiles).getHTML());
	}
}
