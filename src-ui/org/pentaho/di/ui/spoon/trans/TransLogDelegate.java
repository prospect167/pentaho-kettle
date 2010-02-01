package org.pentaho.di.ui.spoon.trans;

import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.logging.CentralLogStore;
import org.pentaho.di.i18n.BaseMessages;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.ui.core.dialog.EnterSelectionDialog;
import org.pentaho.di.ui.core.dialog.ErrorDialog;
import org.pentaho.di.ui.core.gui.GUIResource;
import org.pentaho.di.ui.core.gui.XulHelper;
import org.pentaho.di.ui.spoon.Spoon;
import org.pentaho.di.ui.spoon.XulMessages;
import org.pentaho.di.ui.spoon.delegates.SpoonDelegate;
import org.pentaho.xul.toolbar.XulToolbar;
import org.pentaho.xul.toolbar.XulToolbarButton;

public class TransLogDelegate extends SpoonDelegate {
	private static Class<?> PKG = Spoon.class; // for i18n purposes, needed by Translator2!!   $NON-NLS-1$

	private static final String XUL_FILE_TRANS_LOG_TOOLBAR = "ui/trans-log-toolbar.xul";
	public static final String XUL_FILE_TRANS_LOG_TOOLBAR_PROPERTIES = "ui/trans-log-toolbar.properties";

	private TransGraph transGraph;

	private CTabItem transLogTab;
	
	private StyledText transLogText;
	
	private XulToolbar       toolbar;
	private Composite transLogComposite;

	private LogBrowser	logBrowser;
	
	/**
	 * @param spoon
	 */
	public TransLogDelegate(Spoon spoon, TransGraph transGraph) {
		super(spoon);
		this.transGraph = transGraph;
	}
	
	public void addTransLog() {
		// First, see if we need to add the extra view...
		//
		if (transGraph.extraViewComposite==null || transGraph.extraViewComposite.isDisposed()) {
			transGraph.addExtraView();
		} else {
			if (transLogTab!=null && !transLogTab.isDisposed()) {
				// just set this one active and get out...
				//
				transGraph.extraViewTabFolder.setSelection(transLogTab);
				return; 
			}
		}
		
		// Add a transLogTab : display the logging...
		//
		transLogTab = new CTabItem(transGraph.extraViewTabFolder, SWT.NONE);
		transLogTab.setImage(GUIResource.getInstance().getImageShowLog());
		transLogTab.setText(BaseMessages.getString(PKG, "Spoon.TransGraph.LogTab.Name"));
		
		transLogComposite = new Composite(transGraph.extraViewTabFolder, SWT.NO_BACKGROUND | SWT.NO_FOCUS);
		transLogComposite.setLayout(new FormLayout());
		
		addToolBar();
		addToolBarListeners();
		
		transLogText = new StyledText(transLogComposite, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		spoon.props.setLook(transLogText);
		FormData fdText = new FormData();
		fdText.left = new FormAttachment(0,0);
		fdText.right = new FormAttachment(100,0);
		fdText.top = new FormAttachment((Control)toolbar.getNativeObject(),0);
		fdText.bottom = new FormAttachment(100,0);
		transLogText.setLayoutData(fdText);
		
		logBrowser = new LogBrowser(transLogText, transGraph);
		logBrowser.installLogSniffer();

		// If the transformation is closed, we should dispose of all the logging information in the buffer and registry for this transformation
		//
		transGraph.addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent event) {
				if (transGraph.trans!=null) {
					CentralLogStore.discardLines(transGraph.trans.getLogChannelId(), true);
				}
			}
		});
		
		transLogTab.setControl(transLogComposite);
		
		
		
		transGraph.extraViewTabFolder.setSelection(transLogTab);
	}
	
    private void addToolBar()
	{

		try {
			toolbar = XulHelper.createToolbar(XUL_FILE_TRANS_LOG_TOOLBAR, transLogComposite, TransLogDelegate.this, new XulMessages());

			// Add a few default key listeners
			//
			ToolBar toolBar = (ToolBar) toolbar.getNativeObject();
			
			addToolBarListeners();
	        toolBar.layout(true, true);
		} catch (Throwable t ) {
			log.logError(Const.getStackTracker(t));
			new ErrorDialog(transLogComposite.getShell(), BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Title"), BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_LOG_TOOLBAR), new Exception(t));
		}
	}

	public void addToolBarListeners()
	{
		try
		{
			// first get the XML document
			URL url = XulHelper.getAndValidate(XUL_FILE_TRANS_LOG_TOOLBAR_PROPERTIES);
			Properties props = new Properties();
			props.load(url.openStream());
			String ids[] = toolbar.getMenuItemIds();
			for (int i = 0; i < ids.length; i++)
			{
				String methodName = (String) props.get(ids[i]);
				if (methodName != null)
				{
					toolbar.addMenuListener(ids[i], this, methodName);

				}
			}

		} catch (Throwable t ) {
			t.printStackTrace();
			new ErrorDialog(transLogComposite.getShell(), BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Title"), 
					BaseMessages.getString(PKG, "Spoon.Exception.ErrorReadingXULFile.Message", XUL_FILE_TRANS_LOG_TOOLBAR_PROPERTIES), new Exception(t));
		}
	}

    
    public void showLogView() {
    	
    	// What button?
    	//
    	// XulToolbarButton showLogXulButton = toolbar.getButtonById("trans-show-log");
    	// ToolItem toolBarButton = (ToolItem) showLogXulButton.getNativeObject();
    	
    	if (transLogTab==null || transLogTab.isDisposed()) {
    		addTransLog();
    	} else {
    		transLogTab.dispose();
    		
    		transGraph.checkEmptyExtraView();
    	}
    	
    	// spoon.addTransLog(transMeta);
    }
    
    public void showLogSettings() {
    	spoon.setLog();
    }
    
	public void clearLog()
	{
		if (transLogText!=null && !transLogText.isDisposed()) {
			transLogText.setText(""); //$NON-NLS-1$
		}
		Map<StepMeta, String> stepLogMap = transGraph.getStepLogMap();
		if (stepLogMap!=null) {
			stepLogMap.clear();
			transGraph.getDisplay().asyncExec(new Runnable() {  public void run() { transGraph.redraw(); }}); 
		}
	}

	public void showErrors()
	{
		String all = transLogText.getText();
		ArrayList<String> err = new ArrayList<String>();

		int i = 0;
		int startpos = 0;
		int crlen = Const.CR.length();

		while (i < all.length() - crlen)
		{
			if (all.substring(i, i + crlen).equalsIgnoreCase(Const.CR))
			{
				String line = all.substring(startpos, i);
				String uLine = line.toUpperCase();
				if (uLine.indexOf(BaseMessages.getString(PKG, "TransLog.System.ERROR")) >= 0 || //$NON-NLS-1$
						uLine.indexOf(BaseMessages.getString(PKG, "TransLog.System.EXCEPTION")) >= 0 || //$NON-NLS-1$
						uLine.indexOf("ERROR") >= 0 || // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$ 
						uLine.indexOf("EXCEPTION") >= 0 // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$
				)
				{
					err.add(line);
				}
				// New start of line
				startpos = i + crlen;
			}

			i++;
		}
		String line = all.substring(startpos);
		String uLine = line.toUpperCase();
		if (uLine.indexOf(BaseMessages.getString(PKG, "TransLog.System.ERROR2")) >= 0 || //$NON-NLS-1$
				uLine.indexOf(BaseMessages.getString(PKG, "TransLog.System.EXCEPTION2")) >= 0 || //$NON-NLS-1$
				uLine.indexOf("ERROR") >= 0 || // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$ 
				uLine.indexOf("EXCEPTION") >= 0 // i18n for compatibilty to non translated steps a.s.o. //$NON-NLS-1$
		)
		{
			err.add(line);
		}

		if (err.size() > 0)
		{
			String err_lines[] = new String[err.size()];
			for (i = 0; i < err_lines.length; i++)
				err_lines[i] = err.get(i);

			EnterSelectionDialog esd = new EnterSelectionDialog(transGraph.getShell(), err_lines, BaseMessages.getString(PKG, "TransLog.Dialog.ErrorLines.Title"), BaseMessages.getString(PKG, "TransLog.Dialog.ErrorLines.Message")); //$NON-NLS-1$ //$NON-NLS-2$
			line = esd.open();
			if (line != null)
			{
				TransMeta transMeta = transGraph.getManagedObject();
				for (i = 0; i < transMeta.nrSteps(); i++)
				{
					StepMeta stepMeta = transMeta.getStep(i);
					if (line.indexOf(stepMeta.getName()) >= 0)
					{
						spoon.editStep(transMeta, stepMeta);
					}
				}
				// System.out.println("Error line selected: "+line);
			}
		}
	}

	/**
	 * @return the transLogTab
	 */
	public CTabItem getTransLogTab() {
		return transLogTab;
	}	
	
	public String getLoggingText() {
		if (transLogText!=null && !transLogText.isDisposed()) {
			return transLogText.getText();
		} else {
			return null;
		}

	}
	
	public void pauseLog() {
		XulToolbarButton pauseContinueButton = toolbar.getButtonById("log-pause");

		if (logBrowser.isPaused()) {
			logBrowser.setPaused(false);
			if (pauseContinueButton!=null) { 
				pauseContinueButton.setImage(GUIResource.getInstance().getImagePauseLog());
			}
		} else {
			logBrowser.setPaused(true);
			if (pauseContinueButton!=null) { 
				pauseContinueButton.setImage(GUIResource.getInstance().getImageContinueLog());
			}
		}
	}
	
	public LogBrowser getLogBrowser() {
		return logBrowser;
	}
}
