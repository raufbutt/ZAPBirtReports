/*
 *
 * Paros and its related class files.
 * 
 * Paros is an HTTP/HTTPS proxy for assessing web application security.
 * Copyright (C) 2003-2004 Chinotec Technologies Company
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Clarified Artistic License
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Clarified Artistic License for more details.
 * 
 * You should have received a copy of the Clarified Artistic License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
// ZAP: 2011/10/01 Fixed filename problem (issue 161)
// ZAP: 2012/01/24 Changed outer XML (issue 268) c/o Alla
// ZAP: 2012/03/15 Changed the methods getAlertXML and generate to use the class 
// StringBuilder instead of StringBuffer.
// ZAP: 2012/04/25 Added @Override annotation to all appropriate methods.
// ZAP: 2013/03/03 Issue 546: Remove all template Javadoc comments

package org.zaproxy.zap.extension.birtreports;

import edu.stanford.ejalbert.BrowserLauncher;

import java.awt.Desktop;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.db.Database;
import org.parosproxy.paros.db.RecordAlert;
import org.parosproxy.paros.db.RecordScan;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionLoader;
import org.parosproxy.paros.extension.ViewDelegate;
import org.parosproxy.paros.extension.report.ReportGenerator;
import org.parosproxy.paros.model.Model;
import org.parosproxy.paros.model.SiteMap;
import org.parosproxy.paros.model.SiteNode;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.XmlReporterExtension;
import org.zaproxy.zap.utils.XMLStringUtil;
import org.zaproxy.zap.view.ScanPanel;
//birt jars
import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.EngineConstants;
import org.eclipse.birt.report.engine.api.EngineException;
import org.eclipse.birt.report.engine.api.IReportRunnable;
import org.eclipse.birt.report.engine.api.IRunAndRenderTask;
import org.eclipse.birt.report.engine.api.PDFRenderOption;
import org.eclipse.birt.report.engine.api.impl.ReportEngine;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.Element;
import org.jdom.filter.Filter;
import org.jdom.input.SAXBuilder;
//namespaces for documentbuilder and XPath
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
//import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.Node;

public class ReportLastScan {

    private Logger logger = Logger.getLogger(ReportLastScan.class);
    private ResourceBundle messages = null;
    private static String fileNameLogo="";
    private StringBuilder sbXML;
    private int totalCount = 0;
    
    public ReportLastScan() {
    }

    private String getAlertXML(Database db, RecordScan recordScan) throws SQLException {

        Connection conn = null;
        PreparedStatement psAlert = null;
        StringBuilder sb = new StringBuilder();

        // prepare table connection
        try {
            conn = db.getDatabaseServer().getNewConnection();
            conn.setReadOnly(true);
            // ZAP: Changed to read all alerts and order by risk
            psAlert = conn.prepareStatement("SELECT ALERT.ALERTID FROM ALERT ORDER BY RISK, PLUGINID");
            //psAlert = conn.prepareStatement("SELECT ALERT.ALERTID FROM ALERT JOIN SCAN ON ALERT.SCANID = SCAN.SCANID WHERE SCAN.SCANID = ? ORDER BY PLUGINID");
            //psAlert.setInt(1, recordScan.getScanId());
            psAlert.executeQuery();
            ResultSet rs = psAlert.getResultSet();

            if(rs == null)
            	return "";
            
            RecordAlert recordAlert = null;
            Alert alert = null;
            Alert lastAlert = null;

            StringBuilder sbURLs = new StringBuilder(100);
            String s = null;

            // get each alert from table
            while (rs.next()) {
                int alertId = rs.getInt(1);
                recordAlert = db.getTableAlert().read(alertId);
                alert = new Alert(recordAlert);

                // ZAP: Ignore false positives
                if (alert.getReliability() == Alert.FALSE_POSITIVE) {
                    continue;
                }

                if (lastAlert != null
                        && (alert.getPluginId() != lastAlert.getPluginId()
                        || alert.getRisk() != lastAlert.getRisk())) {
                    s = lastAlert.toPluginXML(sbURLs.toString());
                    sb.append(s);
                    sbURLs.setLength(0);
                }

                s = alert.getUrlParamXML();
                sbURLs.append(s);

                lastAlert = alert;

            }
            rs.close();

            if (lastAlert != null) {
                sb.append(lastAlert.toPluginXML(sbURLs.toString()));
            }



        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.close();
            }

        }

        //exit
        return sb.toString();
    }
    
    public void uploadLogo (ViewDelegate view)
    {
        try {
            JFileChooser chooser = new JFileChooser(Model.getSingleton().getOptionsParam().getUserDirectory());
            chooser.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    } else if (file.isFile()
                            && file.getName().toLowerCase().endsWith(".jpg")) {
                        return true;
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return ".jpg";
                }
            });

            File file = null;
            
            int rc = chooser.showSaveDialog(View.getSingleton().getMainFrame());
            if (rc == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                if (file != null) {
                    Model.getSingleton().getOptionsParam().setUserDirectory(chooser.getCurrentDirectory());
                    fileNameLogo = file.getAbsolutePath().toLowerCase();
                    if (!fileNameLogo.endsWith(".jpg")) {
                        file = new File(file.getAbsolutePath() + ".jpg"); 
                        fileNameLogo = file.getAbsolutePath();
                    } // select the file and close the Save dialog box
                    
                    //Save the image with the name logo.jpg
                    BufferedImage image = null;
                    try {
             
                        image = ImageIO.read(new File(fileNameLogo));
                        File logo = new File("reportdesignfiles/logo.jpg");
                        ImageIO.write(image, "jpg", logo);
                        fileNameLogo = logo.getAbsolutePath().substring(0,logo.getAbsolutePath().lastIndexOf(File.separator));
             
                    } catch (IOException e) {
                    	e.printStackTrace();
                    	view.showWarningDialog("Error: Unable to upload the selected logo image.");
                    }
                }
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            view.showWarningDialog("There is some problem in choosen logo image. Please try again.");
        }
    }
    
    public File generate(String fileName, Model model, String xslFile) throws Exception {

    	StringBuilder sb = new StringBuilder(500);
        // ZAP: Dont require scan to have been run

        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<OWASPZAPReport version=\"").append(Constant.PROGRAM_VERSION).append("\" generated=\"").append(ReportGenerator.getCurrentDateTimeString()).append("\">\r\n");
        //sbXML = sb.append(getAlertXML(model.getDb(), null));
        sbXML = siteXML();
        sb.append(sbXML);
        sb.append("</OWASPZAPReport>");
        // To call another function to filter xml records
        //sbXML = filterXML(sb);
         
        File report = ReportGenerator.stringToHtml(sb.toString(), xslFile, fileName);

        return report;
    }

    public int setCount(int count)
    {
    	return totalCount = count;
    }
    
    private StringBuilder filterXML(StringBuilder xmlsb) throws JDOMException, IOException
    {
    	// convert String into InputStream
    	
    	StringBuilder sb = new StringBuilder (500);
    	
    	InputStream is = new ByteArrayInputStream(xmlsb.toString().getBytes());
    	// load in xml file and get a handle on the root element
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document doc = builder.build(is);
        Element rootElement = doc.getRootElement();
        
        // filter to get all immediate element nodes called 'colour'
        // for a specific, the null represent the default namespace
        Filter elementFilter = new ElementFilter( "alerts", null );
    	
        // gets all immediate nodes under the rootElement
        List allNodes = (List) rootElement.getContent();

        // gets all element nodes under the rootElement
        List elements = (List) rootElement.getContent( elementFilter );
     // cycle through all immediate elements under the rootElement
        for( Iterator it = elements.iterator(); it.hasNext(); ) {
          // note that this is a downcast because we
          // have used the element filter.  This would
          // not be the case for a getContents() on the element
          Element anElement = (Element) it.next();
          System.out.println( anElement );
        }
    	
    	/*
        String xmlString = xmlsb.toString();
        try {
        DocumentBuilderFactory builderFactory =
                DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(false); // never forget this!
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        
        Document xmlDocument = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));
       // Document xmlDocument = builder.parse(
                //new FileInputStream("reportdesignfiles/xmloutput/xmloutputzap.xml"));
 
        // get the first element
        Element element = xmlDocument.getElementById("alertitem");

        // get all child nodes
        NodeList nodes = element.getChildNodes();

        // print the text content of each child
        for (int i = 0; i < nodes.getLength(); i++) {
           System.out.println("" + nodes.item(i).getTextContent());
        }
        /*
        XPath xPath =  XPathFactory.newInstance().newXPath();
        String expression = "alertitem";
        
      //read a string value
      
	  //String email = xPath.compile(expression).evaluate(xmlDocument);       
      //read an xml node using xpath
      Node node = (Node) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODE);
 //     if(node!=null)
 //     System.out.println("Node: " + node.toString());
      
      //read a nodelist using xpath
      NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
      System.out.println("Node List: " + nodeList.toString());
      for (int i = 0; i < nodeList.getLength(); i++) {
    	    System.out.println(nodeList.item(i).getFirstChild().getNodeValue()); 
    	}*/
        /*
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        } 
        /*catch (XPathExpressionException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}*/
      return sb;
    }
    
    private StringBuilder siteXML() {
        StringBuilder report = new StringBuilder();
        SiteMap siteMap = Model.getSingleton().getSession().getSiteTree();
        SiteNode root = (SiteNode) siteMap.getRoot();
        int siteNumber = root.getChildCount();
        for (int i = 0; i < siteNumber; i++) {
            SiteNode site = (SiteNode) root.getChildAt(i);
            String siteName = ScanPanel.cleanSiteName(site, true);
            String[] hostAndPort = siteName.split(":");
            boolean isSSL = (site.getNodeName().startsWith("https"));
            String siteStart = "<site name=\"" + XMLStringUtil.escapeControlChrs(site.getNodeName()) + "\"" +
                    " host=\"" + XMLStringUtil.escapeControlChrs(hostAndPort[0])+ "\""+
                    " port=\"" + XMLStringUtil.escapeControlChrs(hostAndPort[1])+ "\""+
                    " ssl=\"" + String.valueOf(isSSL) + "\"" +
                    ">";
            StringBuilder extensionsXML = getExtensionsXML(site);
            String siteEnd = "</site>";
            report.append(siteStart);
            report.append(extensionsXML);
            report.append(siteEnd);
        }
        return report;
    }
    
    public StringBuilder getExtensionsXML(SiteNode site) {
        StringBuilder extensionXml = new StringBuilder();
        ExtensionLoader loader = Control.getSingleton().getExtensionLoader();
        int extensionCount = loader.getExtensionCount();
        for(int i=0; i<extensionCount; i++) {
            Extension extension = loader.getExtension(i);
            if(extension instanceof XmlReporterExtension) {
                extensionXml.append(((XmlReporterExtension)extension).getXml(site));
                //extensionXml.append(((XmlReporterExtension)extension).getXmlgroup(site, totalCount));
            }
        }
        return extensionXml;
    }


    public void generateXml(ViewDelegate view, Model model) {

        // ZAP: Allow scan report file name to be specified
        try {
            JFileChooser chooser = new JFileChooser(Model.getSingleton().getOptionsParam().getUserDirectory());
            chooser.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    } else if (file.isFile()
                            && file.getName().toLowerCase().endsWith(".xml")) {
                        return true;
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return Constant.messages.getString("file.format.xml");
                }
            });

            File file = null;
            int rc = chooser.showSaveDialog(View.getSingleton().getMainFrame());
            if (rc == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
                if (file != null) {
                    Model.getSingleton().getOptionsParam().setUserDirectory(chooser.getCurrentDirectory());
                    String fileNameLc = file.getAbsolutePath().toLowerCase();
                    if (!fileNameLc.endsWith(".xml")) {
                        file = new File(file.getAbsolutePath() + ".xml");
                    }
                }

                if (!file.getParentFile().canWrite()) {
                    view.showMessageDialog(
                            MessageFormat.format(Constant.messages.getString("report.write.error"),
                            new Object[]{file.getAbsolutePath()}));
                    return;
                }

                File report = generate(file.getAbsolutePath(), model, "xml/report.xml.xsl");
                if (report == null) {
                    view.showMessageDialog(
                            MessageFormat.format(Constant.messages.getString("report.unknown.error"),
                            new Object[]{file.getAbsolutePath()}));
                    return;
                }

                try {
                    BrowserLauncher bl = new BrowserLauncher();
                    bl.openURLinBrowser("file://" + report.getAbsolutePath());
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                    view.showMessageDialog(
                            MessageFormat.format(Constant.messages.getString("report.complete.warning"),
                            new Object[]{report.getAbsolutePath()}));
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            view.showWarningDialog(Constant.messages.getString("report.unexpected.warning"));
        }
    }

    public void generateXmlforBirtPdf(ViewDelegate view, Model model)
    {
    	try
    	{
    		//generate xml file
    		File birtfile = new File("reportdesignfiles/xmloutput/xmloutputzap.xml");
    		File report = generate(birtfile.getAbsolutePath(), model, "xml/report.xml.xsl");
    		 if (report == null) {
                 view.showMessageDialog(
                         MessageFormat.format(Constant.messages.getString("report.unknown.error"),
                         new Object[]{birtfile.getAbsolutePath()}));
                 return;                 
               
             }
   	         if(sbXML.length()==0)
    	       	view.showWarningDialog("You are about to generate an empty report.");

    		}
    		catch(Exception e)
    		{
    		 logger.error(e.getMessage(), e);
             //view.showWarningDialog(Constant.messages.getString("report.unexpected.warning"));
    		
    		 }   	
    }
    
    public void executeBirtScriptReport(ViewDelegate view,String reportDesign, String title)
 	{
 		try {
 			
 			AlertReport report = new AlertReport();
 			report.getAlertsReport();
 			
 			//user chooses where to save PDF report
 			JFileChooser chooser = new JFileChooser(Model.getSingleton().getOptionsParam().getUserDirectory());
             chooser.setFileFilter(new FileFilter() {

                 @Override
                 public boolean accept(File file) {
                     if (file.isDirectory()) {
                         return true;
                     } else if (file.isFile()
                             && file.getName().toLowerCase().endsWith(".pdf")) {
                         return true;
                     }
                     return false;
                 }

                 @Override
                 public String getDescription() {
                     return Constant.messages.getString("file.format.pdf");
                     //TODO: define message on package Messages.Properties own file
                 	//return messages.getString("file.format.pdf");
                 }
             });

             File file = null;
             int rc = chooser.showSaveDialog(View.getSingleton().getMainFrame());
             if (rc == JFileChooser.APPROVE_OPTION) {
                 file = chooser.getSelectedFile();
             }
                 if (file != null) {
                     Model.getSingleton().getOptionsParam().setUserDirectory(chooser.getCurrentDirectory());
                     String fileNameLc = file.getAbsolutePath().toLowerCase();
                     // if a user forgets to specify .pdf at the end of the filename 
                     // then append it with the file name
                     if (!fileNameLc.endsWith(".pdf")) {
                         file = new File(file.getAbsolutePath() + ".pdf"); 
                         fileNameLc = file.getAbsolutePath();
                     } // select the file and close the Save dialog box
                         
                         //BIRT engine code
                         EngineConfig config = new EngineConfig();
                         // set the resource path to the folder where logo image is placed 
                         config.setResourcePath(fileNameLogo);  
             			Platform.startup(config);
             			
             			ReportEngine engine = new ReportEngine(config);
             		
             			IReportRunnable reportRunnable = engine.openReportDesign(reportDesign);
             			IRunAndRenderTask runAndRender = engine.createRunAndRenderTask(reportRunnable);
             			
             			//Get Current Report Title
             			System.out.println(reportRunnable.getDesignHandle().getProperty("title"));  // or IReportRunnable.TITLE
             			
             			//Set New Report Title
             			reportRunnable.getDesignHandle().setProperty("title", title);
             			
             			//Scripted source related code 
             			HashMap contextMap = new HashMap();  
             			List<Alert> sortedList = report.sortAndGroupAlerts(this.totalCount);
             			contextMap.put("Alerts", sortedList);
             			runAndRender.setAppContext(contextMap);  
             			
             			PDFRenderOption option = new PDFRenderOption();
                         option.setOutputFileName(fileNameLc); // takes old file name but now I did some modification
                        
             			option.setOutputFormat("PDF");
             			runAndRender.setRenderOption(option);            			
             			runAndRender.run();            			
             			runAndRender.close();
             			// open the PDF
             			boolean isOpen = openPDF(new File(fileNameLc));
             			if(!isOpen)
             				view.showWarningDialog("Error: Unable to open PDF from location: " + fileNameLc);
             			//engine.destroy();
             			//Platform.shutdown();
                     
                 //}
 //
                			
             }
 				}catch (EngineException e) {
 					e.printStackTrace();
 					} catch (BirtException e) {
 						view.showWarningDialog("Error with BIRT API: " + e.toString());
 						e.printStackTrace();
 						}
 		
 		//
 	
 			}
     //end
    
    public void executeBirtPdfReport(ViewDelegate view,String reportDesign, String title)
	{
		try {
						
			//user chooses where to save PDF report
			JFileChooser chooser = new JFileChooser(Model.getSingleton().getOptionsParam().getUserDirectory());
            chooser.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File file) {
                    if (file.isDirectory()) {
                        return true;
                    } else if (file.isFile()
                            && file.getName().toLowerCase().endsWith(".pdf")) {
                        return true;
                    }
                    return false;
                }

                @Override
                public String getDescription() {
                    return Constant.messages.getString("file.format.pdf");
                    //TODO: define message on package Messages.Properties own file
                	//return messages.getString("file.format.pdf");
                }
            });

            File file = null;
            int rc = chooser.showSaveDialog(View.getSingleton().getMainFrame());
            if (rc == JFileChooser.APPROVE_OPTION) {
                file = chooser.getSelectedFile();
            }
                if (file != null) {
                    Model.getSingleton().getOptionsParam().setUserDirectory(chooser.getCurrentDirectory());
                    String fileNameLc = file.getAbsolutePath().toLowerCase();
                    // if a user forgets to specify .pdf at the end of the filename 
                    // then append it with the file name
                    if (!fileNameLc.endsWith(".pdf")) {
                        file = new File(file.getAbsolutePath() + ".pdf"); 
                        fileNameLc = file.getAbsolutePath();
                    } // select the file and close the Save dialog box
                        
                        //BIRT engine code
                        EngineConfig config = new EngineConfig();
                        // set the resource path to the folder where logo image is placed 
                        config.setResourcePath(fileNameLogo);  
            			Platform.startup(config);
            			
            			ReportEngine engine = new ReportEngine(config);
            		
            			IReportRunnable reportRunnable = engine.openReportDesign(reportDesign);
            			IRunAndRenderTask runAndRender = engine.createRunAndRenderTask(reportRunnable);
            			
            			//Get Current Report Title
            			System.out.println(reportRunnable.getDesignHandle().getProperty("title"));  // or IReportRunnable.TITLE
            			
            			//Set New Report Title
            			reportRunnable.getDesignHandle().setProperty("title", title);
            			//reportRunnable.getDesignHandle()
            			
            			PDFRenderOption option = new PDFRenderOption();
                        option.setOutputFileName(fileNameLc); // takes old file name but now I did some modification
                       
            			option.setOutputFormat("PDF");
            			runAndRender.setRenderOption(option);            			
            			runAndRender.run();            			
            			runAndRender.close();
            			// open the PDF
            			boolean isOpen = openPDF(new File(fileNameLc));
            			if(!isOpen)
            				view.showWarningDialog("Error: Unable to open PDF from location: " + fileNameLc);
            			//engine.destroy();
            			//Platform.shutdown();
                    
                //}
//
               			
            }
				}catch (EngineException e) {
					e.printStackTrace();
					} catch (BirtException e) {
						view.showWarningDialog("Error with BIRT API: " + e.toString());
						e.printStackTrace();
						}
		
		//
	
			}
    //end
    public boolean openPDF(File file)
    {
/*        try
        {
            if (OSDetector.isWindows())
            {
                Runtime.getRuntime().exec(new String[]
                {"rundll32 url.dll,FileProtocolHandler",
                 file.getAbsolutePath()});
                return true;
            } else if (OSDetector.isLinux() || OSDetector.isMac())
            {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/open",
                                                       file.getAbsolutePath()});
                return true;
            } else
            {
                // Unknown OS, try with desktop
                if (Desktop.isDesktopSupported())
                {
                    Desktop.getDesktop().open(file);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace(System.err);
            return false;
        }*/
    	
    	if (Desktop.isDesktopSupported()) {
    	    try {
    	        //File myFile = new File("/path/to/file.pdf");
    	        Desktop.getDesktop().open(file);
    	    } catch (IOException ex) {
    	        // no application registered for PDFs
    	    	return false;
    	    }
    	}
    	return true;
    }
    
    
    public static class OSDetector
    {
        private static boolean isWindows = false;
        private static boolean isLinux = false;
        private static boolean isMac = false;

        static
        {
            String os = System.getProperty("os.name").toLowerCase();
            isWindows = os.contains("win");
            isLinux = os.contains("nux") || os.contains("nix");
            isMac = os.contains("mac");
        }

        public static boolean isWindows() { return isWindows; }
        public static boolean isLinux() { return isLinux; }
        public static boolean isMac() { return isMac; };

    }
    
}


