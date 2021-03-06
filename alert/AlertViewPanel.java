/*
 * Zed Attack Proxy (ZAP) and its related class files.
 * 
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 * 
 * Copyright 2011 The ZAP Development team
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.zaproxy.zap.extension.alert;


import java.awt.CardLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.core.scanner.Alert;
import org.parosproxy.paros.extension.AbstractPanel;
import org.parosproxy.paros.model.HistoryReference;
import org.parosproxy.paros.network.HttpMessage;
import org.zaproxy.zap.model.Vulnerabilities;
import org.zaproxy.zap.model.Vulnerability;
import org.zaproxy.zap.utils.ZapTextArea;
import org.zaproxy.zap.utils.ZapTextField;

public class AlertViewPanel extends AbstractPanel {

	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(AlertViewPanel.class);
	
	private JScrollPane defaultPane = null;
	private JScrollPane alertPane = null;
	private ZapTextArea defaultOutput = null;
	private JPanel alertDisplay = null;
	private CardLayout cardLayout = null;
	
	private JLabel alertUrl = null;
	private JLabel alertName = null;
	private JLabel alertRisk = null;
	private JLabel alertReliability = null;
	private JLabel alertParam = null;
	private JLabel alertAttack = null;
	private ZapTextArea alertDescription = null;
	private ZapTextArea alertOtherInfo = null;
	private ZapTextArea alertSolution = null;
	private ZapTextArea alertReference = null;
	
	private JComboBox<String> alertEditName = null;
	private JComboBox<String> alertEditRisk = null;
	private JComboBox<String> alertEditReliability = null;
	private JComboBox<String> alertEditParam = null;
	private ZapTextField alertEditAttack = null;
	private DefaultComboBoxModel<String> nameListModel = null;
	private DefaultComboBoxModel<String> paramListModel = null;
	
	private boolean editable = false;
	private Alert originalAlert = null;
	private List <Vulnerability> vulnerabilities = null;

	private HistoryReference historyRef = null;
	
    /**
     * Used to set the {@code HttpMessage} to the new alert when there is no
     * {@code historyRef}.
     */
	private HttpMessage httpMessage;

	/**
     * 
     */
    public AlertViewPanel() {
    	this (false);
    }
    
    public AlertViewPanel(boolean editable) {
        super();
        this.editable = editable;
 		initialize();
    }
    
	/**
	 * This method initializes this
	 */
	private void initialize() {
		cardLayout = new CardLayout();
        this.setLayout(cardLayout);
        this.setName("AlertView");

        if (! editable) {
        	this.add(getDefaultPane(), getDefaultPane().getName());
        }
        this.add(getAlertPane(), getAlertPane().getName());
			
	}
	
	private JScrollPane getAlertPane() {
		if (alertPane == null) {
			alertPane = new JScrollPane();
			alertPane.setViewportView(getAlertDisplay());
			alertPane.setName("alertPane");
			alertPane.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 11));
		}
		return alertPane;
	}
	
	private ZapTextArea createZapTextArea() {
		ZapTextArea ZapTextArea = new ZapTextArea();
		ZapTextArea = new ZapTextArea(3, 30);
		ZapTextArea.setLineWrap(true);
		ZapTextArea.setWrapStyleWord(true);
		ZapTextArea.setEditable(editable);
		return ZapTextArea;
	}
	
	private JScrollPane createJScrollPane(String name) {
		JScrollPane jScrollPane = new JScrollPane();
		jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		jScrollPane.setBorder(
				BorderFactory.createTitledBorder(
						null, name, 
						TitledBorder.DEFAULT_JUSTIFICATION, 
						javax.swing.border.TitledBorder.DEFAULT_POSITION, 
						new java.awt.Font("Dialog", java.awt.Font.PLAIN, 11), 
						java.awt.Color.black));
		return jScrollPane;
		
	}
	
	private JPanel getAlertDisplay() {
		if (alertDisplay == null) {
			alertDisplay = new JPanel();
			alertDisplay.setLayout(new GridBagLayout());
			alertDisplay.setName("alertDisplay");
			
			// Create the labels
			
			if (editable) {
				alertEditName = new JComboBox<>();
				alertEditName.setEditable(true);
				nameListModel = new DefaultComboBoxModel<>();
				
				List <String> allVulns = getAllVulnerabilityNames();
				nameListModel.addElement("");	// Default to blank
				for (String vuln : allVulns) {
					nameListModel.addElement(vuln);
				}
				
				alertEditName.setModel(nameListModel);
				alertEditName.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if ("comboBoxChanged".equals(e.getActionCommand())) {
							Vulnerability v = getVulnerability((String)alertEditName.getSelectedItem());
							if (v != null) {
								if (v.getDescription() != null && v.getDescription().length() > 0) {
									setAlertDescription(v.getDescription());
								}
								if (v.getSolution() != null && v.getSolution().length() > 0) {
									setAlertSolution(v.getSolution());
								}
								if (v.getReferences() != null) {
									StringBuilder sb = new StringBuilder();
									for (String ref : v.getReferences()) {
										sb.append(ref);
										sb.append('\n');
									}
									setAlertReference(sb.toString());
								}
							}
						}
					}
				});

				alertEditRisk = new JComboBox<>(Alert.MSG_RISK);
				alertEditReliability = new JComboBox<>(Alert.MSG_RELIABILITY);
				alertEditReliability.setSelectedItem(Alert.MSG_RELIABILITY[Alert.SUSPICIOUS]);
				alertEditAttack = new ZapTextField();
				
				paramListModel = new DefaultComboBoxModel<>();
				paramListModel.addElement("");	// Default is empty so user can type anything in
				alertEditParam = new JComboBox<>();
				alertEditParam.setModel(paramListModel);
				alertEditParam.setEditable(true);

			} else {
				alertName = new JLabel();
				Font f = alertName.getFont();
				alertName.setFont(f.deriveFont(f.getStyle() | Font.BOLD));

				alertRisk = new JLabel();
				alertReliability = new JLabel();
				alertParam = new JLabel();
				alertAttack = new JLabel();
			}
			
			alertUrl = new JLabel();
			alertDescription = createZapTextArea();
			JScrollPane descSp = createJScrollPane(Constant.messages.getString("alert.label.desc"));
			descSp.setViewportView(alertDescription);
			alertDescription.addKeyListener(new KeyAdapter() {
				// Change tab key to transfer focus to the next element
				@Override
				public void keyPressed(java.awt.event.KeyEvent evt) {
					if (evt.getKeyCode() == KeyEvent.VK_TAB) {
						alertDescription.transferFocus();
					}
				}
			});

			alertOtherInfo = createZapTextArea();
			JScrollPane otherSp = createJScrollPane(Constant.messages.getString("alert.label.other"));
			otherSp.setViewportView(alertOtherInfo);
			alertOtherInfo.addKeyListener(new KeyAdapter() {
				// Change tab key to transfer focus to the next element
				@Override
				public void keyPressed(java.awt.event.KeyEvent evt) {
					if (evt.getKeyCode() == KeyEvent.VK_TAB) {
						alertOtherInfo.transferFocus();
					}
				}
			});

			alertSolution = createZapTextArea();
			JScrollPane solutionSp = createJScrollPane(Constant.messages.getString("alert.label.solution"));
			solutionSp.setViewportView(alertSolution);
			alertSolution.addKeyListener(new KeyAdapter() {
				// Change tab key to transfer focus to the next element
				@Override
				public void keyPressed(java.awt.event.KeyEvent evt) {
					if (evt.getKeyCode() == KeyEvent.VK_TAB) {
						alertSolution.transferFocus();
					}
				}
			});

			alertReference = createZapTextArea();
			JScrollPane referenceSp = createJScrollPane(Constant.messages.getString("alert.label.ref"));
			referenceSp.setViewportView(alertReference);
			alertReference.addKeyListener(new KeyAdapter() {
				// Change tab key to transfer focus to the next element
				@Override
				public void keyPressed(java.awt.event.KeyEvent evt) {
					if (evt.getKeyCode() == KeyEvent.VK_TAB) {
						alertReference.transferFocus();
					}
				}
			});

			java.awt.GridBagConstraints gbc00 = new GridBagConstraints();
			gbc00.gridy = 0;
			gbc00.gridx = 0;
			gbc00.insets = new java.awt.Insets(1,1,1,1);
			gbc00.anchor = java.awt.GridBagConstraints.NORTHWEST;
			gbc00.fill = java.awt.GridBagConstraints.BOTH;
			//gbc00.weightx = 1.0D;
			gbc00.gridwidth = 4;

			java.awt.GridBagConstraints gbc10 = new GridBagConstraints();
			gbc10.gridy = 1;
			gbc10.gridx = 0;
			gbc10.insets = new java.awt.Insets(1,1,1,1);
			gbc10.anchor = java.awt.GridBagConstraints.WEST;
			//gbc10.weightx = 0.5D;

			java.awt.GridBagConstraints gbc11 = new GridBagConstraints();
			gbc11.gridy = 1;
			gbc11.gridx = 1;
			gbc11.insets = new java.awt.Insets(1,1,1,1);
			gbc11.anchor = java.awt.GridBagConstraints.WEST;
			//gbc11.weightx = 1.0D;

			java.awt.GridBagConstraints gbc20 = new GridBagConstraints();
			gbc20.gridy = 2;
			gbc20.gridx = 0;
			gbc20.insets = new java.awt.Insets(1,1,1,1);
			gbc20.anchor = java.awt.GridBagConstraints.WEST;
			//gbc10.weightx = 0.5D;

			java.awt.GridBagConstraints gbc21 = new GridBagConstraints();
			gbc21.gridy = 2;
			gbc21.gridx = 1;
			gbc21.insets = new java.awt.Insets(1,1,1,1);
			gbc21.anchor = java.awt.GridBagConstraints.WEST;
			//gbc11.weightx = 1.0D;

			java.awt.GridBagConstraints gbc22 = new GridBagConstraints();
			gbc22.gridy = 3;
			gbc22.gridx = 0;
			gbc22.insets = new java.awt.Insets(1,1,1,1);
			gbc22.anchor = java.awt.GridBagConstraints.WEST;
			//gbc12.weightx = 0.5D;

			java.awt.GridBagConstraints gbc23 = new GridBagConstraints();
			gbc23.gridy = 3;
			gbc23.gridx = 1;
			gbc23.insets = new java.awt.Insets(1,1,1,1);
			gbc23.anchor = java.awt.GridBagConstraints.WEST;
			//gbc13.weightx = 1.0D;

			java.awt.GridBagConstraints gbc30 = new GridBagConstraints();
			gbc30.gridy = 4;
			gbc30.gridx = 0;
			gbc30.insets = new java.awt.Insets(1,1,1,1);
			gbc30.anchor = java.awt.GridBagConstraints.WEST;
			//gbc20.weightx = 0.5D;

			java.awt.GridBagConstraints gbc31 = new GridBagConstraints();
			gbc31.gridy = 4;
			gbc31.gridx = 1;
			gbc31.insets = new java.awt.Insets(1,1,1,1);
			gbc31.anchor = java.awt.GridBagConstraints.WEST;
			//gbc21.weightx = 1.0D;

			java.awt.GridBagConstraints gbc40 = new GridBagConstraints();
			gbc40.gridy = 5;
			gbc40.gridx = 0;
			gbc40.insets = new java.awt.Insets(1,1,1,1);
			gbc40.anchor = java.awt.GridBagConstraints.WEST;
			//gbc20.weightx = 0.5D;

			java.awt.GridBagConstraints gbc41 = new GridBagConstraints();
			gbc41.gridy = 5;
			gbc41.gridx = 1;
			gbc41.insets = new java.awt.Insets(1,1,1,1);
			gbc41.anchor = java.awt.GridBagConstraints.WEST;
			gbc41.fill = java.awt.GridBagConstraints.BOTH;
			gbc41.weightx = 1.0D;

			java.awt.GridBagConstraints gbc50 = new GridBagConstraints();
			gbc50.gridy = 6;
			gbc50.gridx = 0;
			gbc50.insets = new java.awt.Insets(1,1,1,1);
			gbc50.anchor = java.awt.GridBagConstraints.WEST;
			gbc50.fill = java.awt.GridBagConstraints.BOTH;
			gbc50.weightx = 1.0D;
			gbc50.weighty = 1.0D;
			gbc50.gridwidth = 2;
			gbc50.gridheight = 1;

			java.awt.GridBagConstraints gbc60 = new GridBagConstraints();
			gbc60.gridy = 7;
			gbc60.gridx = 0;
			gbc60.insets = new java.awt.Insets(1,1,1,1);
			gbc60.anchor = java.awt.GridBagConstraints.WEST;
			gbc60.fill = java.awt.GridBagConstraints.BOTH;
			gbc60.weightx = 1.0D;
			gbc60.weighty = 1.0D;
			gbc60.gridwidth = 2;
			gbc60.gridheight = 1;

			java.awt.GridBagConstraints gbc70 = new GridBagConstraints();
			gbc70.gridy = 8;
			gbc70.gridx = 0;
			gbc70.insets = new java.awt.Insets(1,1,1,1);
			gbc70.anchor = java.awt.GridBagConstraints.WEST;
			gbc70.fill = java.awt.GridBagConstraints.BOTH;
			gbc70.weightx = 1.0D;
			gbc70.weighty = 1.0D;
			gbc70.gridwidth = 2;
			gbc70.gridheight = 1;

			java.awt.GridBagConstraints gbc80 = new GridBagConstraints();
			gbc80.gridy = 9;
			gbc80.gridx = 0;
			gbc80.insets = new java.awt.Insets(1,1,1,1);
			gbc80.anchor = java.awt.GridBagConstraints.WEST;
			gbc80.fill = java.awt.GridBagConstraints.BOTH;
			gbc80.weightx = 1.0D;
			gbc80.weighty = 1.0D;
			gbc80.gridwidth = 2;
			gbc80.gridheight = 1;

			if (editable) {
				alertDisplay.add(alertEditName, gbc00);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.url")), gbc10);
				alertDisplay.add(alertUrl, gbc11);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.risk")), gbc20);
				alertDisplay.add(alertEditRisk, gbc21);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.reliability")), gbc22);
				alertDisplay.add(alertEditReliability, gbc23);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.parameter")), gbc30);
				alertDisplay.add(alertEditParam, gbc31);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.attack")), gbc40);
				alertDisplay.add(alertEditAttack, gbc41);
			} else {
				alertDisplay.add(alertName, gbc00);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.url")), gbc10);
				alertDisplay.add(alertUrl, gbc11);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.risk")), gbc20);
				alertDisplay.add(alertRisk, gbc21);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.reliability")), gbc22);
				alertDisplay.add(alertReliability, gbc23);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.parameter")), gbc30);
				alertDisplay.add(alertParam, gbc31);
				alertDisplay.add(new JLabel(Constant.messages.getString("alert.label.attack")), gbc40);
				alertDisplay.add(alertAttack, gbc41);
			}
			
			alertDisplay.add(descSp, gbc50);
			alertDisplay.add(otherSp, gbc60);
			alertDisplay.add(solutionSp, gbc70);
			alertDisplay.add(referenceSp, gbc80);
			
		}
		return alertDisplay;
	}
	
	public void displayAlert (Alert alert) {
		this.originalAlert = alert;
		
		alertUrl.setText(alert.getUri());
		
		if (editable) {
			nameListModel.addElement(alert.getAlert());
			alertEditName.setSelectedItem(alert.getAlert());
			alertEditRisk.setSelectedItem(Alert.MSG_RISK[alert.getRisk()]);
			alertEditReliability.setSelectedItem(Alert.MSG_RELIABILITY[alert.getReliability()]);
			alertEditParam.setSelectedItem(alert.getParam());
			alertEditAttack.setText(alert.getAttack());
			alertEditAttack.discardAllEdits();
			
		} else {
			alertName.setText(alert.getAlert());
	
			alertRisk.setText(Alert.MSG_RISK[alert.getRisk()]);
			/*
	    	switch (alert.getRisk()) {
	    	case Alert.RISK_INFO:	// blue flag
				alertRisk.setIcon(new ImageIcon(Constant.INFO_FLAG_IMAGE_URL));
	    		break;
	    	case Alert.RISK_LOW:	// yellow flag
				alertRisk.setIcon(new ImageIcon(Constant.LOW_FLAG_IMAGE_URL));
	    		break;
	    	case Alert.RISK_MEDIUM:	// Orange flag
				alertRisk.setIcon(new ImageIcon(Constant.MED_FLAG_IMAGE_URL));
	    		break;
	    	case Alert.RISK_HIGH:	// Red flag
				alertRisk.setIcon(new ImageIcon(Constant.HIGH_FLAG_IMAGE_URL));
	    		break;
	    	}
	    	*/
	    	if (alert.getReliability() == Alert.FALSE_POSITIVE) {
	    		// Special case - theres no risk - use the green flag
				alertRisk.setIcon(new ImageIcon(Constant.OK_FLAG_IMAGE_URL));
	    	} else {
				alertRisk.setIcon(new ImageIcon(alert.getIconUrl()));
	    	}
			
			alertReliability.setText(Alert.MSG_RELIABILITY[alert.getReliability()]);
			alertParam.setText(alert.getParam());
			alertAttack.setText(alert.getAttack());
		}
		
		setAlertDescription(alert.getDescription());
		setAlertOtherInfo(alert.getOtherInfo());
		setAlertSolution(alert.getSolution());
		setAlertReference(alert.getReference());

		cardLayout.show(this, getAlertPane().getName());
	}
	
	public void clearAlert () {
		cardLayout.show(this, getDefaultPane().getName());
	}
	
	/**
	 * This method initializes jScrollPane	
	 * 	
	 * @return javax.swing.JScrollPane	
	 */    
	private JScrollPane getDefaultPane() {
		if (defaultPane == null) {
			defaultPane = new JScrollPane();
			defaultPane.setViewportView(getDefaultOutput());
			defaultPane.setName("defaultPane");
			defaultPane.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			defaultPane.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 11));
		}
		return defaultPane;
	}

	private ZapTextArea getDefaultOutput() {
		if (defaultOutput == null) {
			defaultOutput = new ZapTextArea();
			defaultOutput.setEditable(false);
			defaultOutput.setLineWrap(true);
			defaultOutput.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 12));
			defaultOutput.setName("");
			defaultOutput.append(Constant.messages.getString("alerts.label.defaultMessage"));
		}
		return defaultOutput;
	}
	
	public void append(final String msg) {
		if (EventQueue.isDispatchThread()) {
			getDefaultOutput().append(msg);
			return;
		}
		try {
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					getDefaultOutput().append(msg);
				}
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public void clear() {
	    getDefaultOutput().setText("");
	}

	public void setParamNames(String[] paramNames) {
		for (String param : paramNames) {
			paramListModel.addElement(param);
		}

	}

	public Alert getAlert() {
		if (! editable && originalAlert != null) {
			Alert alert = originalAlert.newInstance();
			alert.setAlertId(originalAlert.getAlertId());
			alert.setAlert((String)alertEditName.getSelectedItem());
			alert.setParam((String)alertEditParam.getSelectedItem());
			alert.setRiskReliability(alertEditRisk.getSelectedIndex(), 
					alertEditReliability.getSelectedIndex());
			alert.setDescription(alertDescription.getText());
			alert.setOtherInfo(alertOtherInfo.getText());
			alert.setSolution(alertSolution.getText());
			alert.setReference(alertReference.getText());
			alert.setHistoryRef(historyRef);
			
			return alert;
		}
		
		Alert alert = new Alert(-1, alertEditRisk.getSelectedIndex(), 
				alertEditReliability.getSelectedIndex(), (String) alertEditName.getSelectedItem());
		alert.setHistoryRef(historyRef);
		if (originalAlert != null) {
			alert.setAlertId(originalAlert.getAlertId());
		}
		
		String uri = null;
		HttpMessage msg = null;
		if (httpMessage != null) {
		    uri = httpMessage.getRequestHeader().getURI().toString();
		    msg = httpMessage;
		} else if (historyRef != null) {
			try {
				uri = historyRef.getURI().toString();
				msg = historyRef.getHttpMessage();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		} else if (originalAlert != null) {
			uri = originalAlert.getUri();
			msg = originalAlert.getMessage();
		}
		alert.setDetail(alertDescription.getText(), 
				uri, 
				(String)alertEditParam.getSelectedItem(), 
				alertEditAttack.getText(),
				alertOtherInfo.getText(), 
				alertSolution.getText(), 
				alertReference.getText(), 
				msg);
		return alert;
	}

	public Alert getOriginalAlert() {
		return this.originalAlert;
	}

	public void setHistoryRef(HistoryReference historyRef) {
		this.historyRef = historyRef;
		this.httpMessage = null;
		try {
			if (historyRef != null) {
				HttpMessage msg = historyRef.getHttpMessage();
				setParamNames(msg.getParamNames());
		        this.alertUrl.setText(msg.getRequestHeader().getURI().toString());
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

    /**
     * Sets the {@code HttpMessage} that will be set to the new alert.
     * 
     * @param httpMessage
     *            the {@code HttpMessage} that will be set to the new alert
     */
    public void setHttpMessage(HttpMessage httpMessage) {
        this.httpMessage = httpMessage;
        setParamNames(httpMessage.getParamNames());
        this.alertUrl.setText(httpMessage.getRequestHeader().getURI().toString());
        this.historyRef = null;
    }
	
	public boolean isEditable() {
		return editable;
	}
	
	private List <Vulnerability> getAllVulnerabilities() {
		if (vulnerabilities == null) {
			vulnerabilities = Vulnerabilities.getAllVulnerabilities();
		}
		return vulnerabilities;
	}
	
	private Vulnerability getVulnerability (String alert) {
		if (alert == null) {
			return null;
		}
		List <Vulnerability> vulns = this.getAllVulnerabilities();
		for (Vulnerability v : vulns) {
			if (alert.equals(v.getAlert())) {
				return v;
			}
		}
		return null;
	}

	private List<String> getAllVulnerabilityNames() {
		List <Vulnerability> vulns = this.getAllVulnerabilities();
		List <String> names = new ArrayList<>(vulns.size());
		for (Vulnerability v : vulns) {
			names.add(v.getAlert());
		}
		Collections.sort(names);
		return names;
	}

	private void setAlertDescription(String description) {
		setTextDiscardEditsAndInitCaretPosition(alertDescription, description);
	}

	private void setAlertOtherInfo(String otherInfo) {
		setTextDiscardEditsAndInitCaretPosition(alertOtherInfo, otherInfo);
	}

	private void setAlertSolution(String solution) {
		setTextDiscardEditsAndInitCaretPosition(alertSolution, solution);
	}

	private void setAlertReference(String reference) {
		setTextDiscardEditsAndInitCaretPosition(alertReference, reference);
	}

	private static void setTextDiscardEditsAndInitCaretPosition(ZapTextArea textArea, String text) {
		textArea.setText(text);
		textArea.discardAllEdits();
		textArea.setCaretPosition(0);
	}

}
