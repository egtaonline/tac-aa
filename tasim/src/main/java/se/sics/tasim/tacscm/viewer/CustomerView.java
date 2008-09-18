/**
 * TAC Supply Chain Management Simulator
 * http://www.sics.se/tac/    tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * CustomerView
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : Sat Feb 01 14:13:10 2003
 * Updated : $Date: 2008-03-07 10:04:04 -0600 (Fri, 07 Mar 2008) $
 *           $Revision: 3826 $
 */
package se.sics.tasim.tacscm.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import se.sics.isl.gui.DotDiagram;
import se.sics.isl.gui.MessageModel;
import se.sics.isl.gui.MessageRenderer;
import se.sics.isl.transport.Transportable;

public class CustomerView extends TACSCMAgentView
{
  
  private static final Logger log = Logger.getLogger(CustomerView.class.getName());
  
  private static final Color DEMAND_COLOR = new Color(0, 0, 128);
  private static final Color ORDER_COLOR = Color.blue;
  
  private JPanel dialog;
  private JList messageList;
  private MessageModel messageModel;
  
  private DotDiagram demandDiagram;
  private DotDiagram largeDemandDiagram;
  
  private int[] customerDemand = new int[30];
  private int demandPosition = 0;
  private int[] customerOrdered = new int[30];
  private int orderedPosition = 0;
  
  public CustomerView(TACSCMViewer viewer)
  {
    super(viewer);
  }
  
  protected void initializeView()
  {
    demandDiagram = new DotDiagram(2);
    demandDiagram.setDotColor(0, DEMAND_COLOR);
    demandDiagram.setDotColor(1, ORDER_COLOR);
    demandDiagram.setPreferredSize(new Dimension(70, 40));
    
    messageModel = new MessageModel(TACSCMViewer.MAX_EVENT_NO);
    
    if (getIcon() == null)
    {
      // No main image
      setBackground(new Color(0xe0e0fd));
      setBorder(BorderFactory.createTitledBorder(getName()));
      add(demandDiagram);
      
    }
    else
    {
      if ("true".equalsIgnoreCase(getConfigProperty("nameLabel")))
      {
        JLabel nameLabel = new JLabel(getName());
        layoutComponent("nameLabel", nameLabel);
      }
      layoutComponent("demandDiagram", demandDiagram);
    }
    setToolTipText("");
  }
  
  protected void addMessage(String message, int flag)
  {
    messageModel.addMessage(message, flag);
    if (dialog != null && dialog.isVisible() && messageList != null)
    {
      messageList.ensureIndexIsVisible(messageModel.getSize() - 1);
    }
  }
  
  public void dataUpdated(int type, int value)
  {
    if (type == DU_CUSTOMER_DEMAND)
    {
      int size = customerDemand.length;
      customerDemand[demandPosition] = value;
      demandPosition = (demandPosition + 1) % size;
      demandDiagram.setData(0, customerDemand, demandPosition, size);
      if (largeDemandDiagram != null)
      {
        largeDemandDiagram.setData(0, customerDemand, demandPosition, size);
      }
    }
    else if (type == DU_CUSTOMER_ORDERED)
    {
      int size = customerOrdered.length;
      customerOrdered[orderedPosition] = value;
      orderedPosition = (orderedPosition + 1) % size;
      demandDiagram.setData(1, customerOrdered, orderedPosition, size);
      if (largeDemandDiagram != null)
      {
        largeDemandDiagram.setData(1, customerOrdered, orderedPosition, size);
      }
    }
  }
  
  public void dataUpdated(int type, long value)
  {}
  
  public void dataUpdated(int type, float value)
  {}
  
  public void dataUpdated(int type, String value)
  {
    if (type == TYPE_MESSAGE)
    {
      addMessage(value, MessageModel.NONE);
    }
    else if (type == TYPE_WARNING)
    {
      addMessage(value, MessageModel.WARNING);
    }
  }
  
  public void dataUpdated(int type, Transportable value)
  {}
  
  protected JComponent getDialog()
  {
    if (dialog == null)
    {
      dialog = new JPanel(new BorderLayout());
      // Set the preferred size to avoid having a too narrow window
      // if the latest event list only contains short rows
      dialog.setPreferredSize(new Dimension(500, 400));
      dialog.setBackground(Color.white);
      
      JPanel left = new JPanel(new GridLayout(0, 1));
      left.setOpaque(false);
      dialog.add(left, BorderLayout.WEST);
      
      JLabel tmpLabel = new JLabel();
      Icon agentIcon = getIcon("customer2.jpg");
      if (agentIcon != null)
      {
        tmpLabel.setIcon(agentIcon);
      }
      tmpLabel.setBorder(BorderFactory.createTitledBorder("Customer"));
      left.add(tmpLabel);
      
      Dimension diagramSize = new Dimension(100, 100);
      largeDemandDiagram = new DotDiagram(2);
      largeDemandDiagram.setBorder(BorderFactory.createTitledBorder("Customer Demand"));
      largeDemandDiagram.setPreferredSize(diagramSize);
      largeDemandDiagram.setDotColor(0, DEMAND_COLOR);
      largeDemandDiagram.setDotColor(1, ORDER_COLOR);
      largeDemandDiagram.setData(0, customerDemand, demandPosition, customerDemand.length);
      largeDemandDiagram.setData(1, customerOrdered, orderedPosition, customerOrdered.length);
      left.add(largeDemandDiagram);
      
      // DotDiagram utilDiagram = new DotDiagram(1);
      // utilDiagram.setMinMax(0, 100);
      // utilDiagram.setDotColor(0, Color.blue);
      // utilDiagram.setBorder(BorderFactory.createTitledBorder("Utilization"));
      // utilDiagram.setPreferredSize(diagramSize);
      // left.add(utilDiagram);
      left.add(Box.createGlue());
      
      messageList = new JList(messageModel);
      messageList.setCellRenderer(new MessageRenderer(messageModel));
      dialog.add(createScrollPane(messageList, "Latest Events", true), BorderLayout.CENTER);
    }
    return dialog;
  }
  
  public String getToolTipText(MouseEvent event)
  {
    int x, y, dx, dy;
    if (((x = event.getX()) >= (dx = demandDiagram.getX()))
        && (x < (dx + demandDiagram.getWidth()))
        && ((y = event.getY()) >= (dy = demandDiagram.getY()))
        && (y < (dy + demandDiagram.getHeight())))
    {
      return "Customer Demand and Orders";
    }
    return null;
  }
  
} // CustomerView
