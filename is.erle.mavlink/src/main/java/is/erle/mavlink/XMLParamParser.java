package is.erle.mavlink;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.collect.Multiset.Entry;

public class XMLParamParser
{
	private File xmlFile;
	private Document xmlDocument;
	
    public final String PARAMDELIMETER = "@";
    public final String PATHDELIMETER = ",";
    public final String PARAM = "Param";
    public final String GROUP = "Group";
    public final String PATH = "Path";
    
    public final String DISPLAYNAME = "DisplayName";
    public final String DESCRIPTION = "Description";
    public final String UNITS = "Units";
    public final String RANGE = "Range";
    public final String VALUES = "Values";
    public final String INCREMENT = "Increment";
    public final String USER = "User";
    public final String REBOOTREQUIRED = "RebootRequired";
    public final String BITMASK = "Bitmask";
    
    public final String ADVANCED = "Advanced";
    public final String STANDARD = "Standard";
    
	public File getFile()
	{
		return xmlFile;
	}
	
	public Document getDocument()
	{
		return xmlDocument;
	}
	
	public void setFile(File xmlFile)
	{
		this.xmlFile = xmlFile;
	}
	
	public void setFile(Document xmlDocument)
	{
		this.xmlDocument = xmlDocument;
	}
	
	public XMLParamParser(File xmlFile) throws SAXException, IOException, ParserConfigurationException
	{
		this.xmlFile = xmlFile ;
		xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this.xmlFile);
	}
	
	public XMLParamParser(Document xmlDocument)
	{
		this.xmlDocument = xmlDocument ;
	}
	
	public String getParamDataXml(String nodeKey, String metaKey,
			String vehicleType)
	{
		if (xmlDocument.getDocumentElement().getNodeName().equals("Params"))
		{
			String value = null;
			Node vehicleNode = null;
			try
			{
				vehicleNode = xmlDocument.getElementsByTagName(vehicleType)
						.item(0);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			Element vehicle = null;
			if (vehicleNode.getNodeType() == Node.ELEMENT_NODE)
			{
				vehicle = (Element) vehicleNode;
			}
			NodeList nodeKeyList = null;
			try
			{
				nodeKeyList = vehicle.getElementsByTagName(nodeKey);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			for (int i = 0; i < nodeKeyList.getLength(); i++)
			{
				Node nodeMetaKey = nodeKeyList.item(i);
				Element elementMetaKey = null;
				if (nodeMetaKey.getNodeType() == Node.ELEMENT_NODE)
				{
					elementMetaKey = (Element) nodeMetaKey;
				}
				try
				{
					value = elementMetaKey.getElementsByTagName(metaKey)
							.item(0).getTextContent();
				}
				catch (DOMException e2)
				{
					e2.printStackTrace();
				}
				return value;
			}
		}
		return null;
	}
	
	public HashMap<String, Short> getParamOptions(String nodeKey,
			String vehicleType)
	{
		HashMap<String, Short> valueMap = new HashMap<String, Short>();
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, VALUES, vehicleType);
			if (!(rawData.isEmpty()))
			{
				String[] values = rawData.split(",");
				for (int i = 0; i < values.length; i++)
				{
					try
					{
						String[] valuePart = values[i].split(":");
						valueMap.put(valuePart[1],
								Short.parseShort(valuePart[0].trim()));
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return valueMap;
	}
	
	public HashMap<String, Short> getParamBitMask(String nodeKey,
			String vehicleType)
	{
		HashMap<String, Short> valueMap = new HashMap<String, Short>();
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, BITMASK, vehicleType);
			if (!(rawData.isEmpty()))
			{
				String[] values = rawData.split(",");
				for (int i = 0; i < values.length; i++)
				{
					try
					{
						String[] valuePart = values[i].split(":");
						valueMap.put(valuePart[1],
								Short.parseShort(valuePart[0].trim()));
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return valueMap;
	}
	
	public boolean getParamRebootRequired(String nodeKey, String vehicleType)
	{
		boolean answer = false;
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, REBOOTREQUIRED, vehicleType);
			if (!(rawData.isEmpty()))
			{
				try
				{
					answer = Boolean.parseBoolean(rawData);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return answer;
	}
	
	public MinMaxPair<Float> getParamRange(String nodeKey, String vehicleType)
	{
		MinMaxPair<Float> pair = null;
		if (!(xmlDocument == null))
		{
			String rawData = getParamDataXml(nodeKey, RANGE, vehicleType);
			if (!(rawData.isEmpty()))
			{
				String[] values = rawData.split(" ");
				if (values.length == 2)
				{
					try
					{
						float min = Float.parseFloat(values[0].trim());
						float max = Float.parseFloat(values[1].trim());
						pair = new MinMaxPair<Float>(min, max);
					}
					catch (NumberFormatException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		return pair;
	}
	
	
}