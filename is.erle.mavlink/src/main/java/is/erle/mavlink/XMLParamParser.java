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
		return null;
	}
	
	public HashMap<String, Short> getParamBitMask(String nodeKey,
			String vehicleType)
	{
		return null;
	}
	
	public boolean getParamRebootRequired(String nodeKey, String vehicleType)
	{
		return false;
	}
	
	public MinMaxPair<Integer> getParamRange(String nodeKey, String vehicleType)
	{
		MinMaxPair<Integer> temp = null;
		return temp;
	}
	
	
}