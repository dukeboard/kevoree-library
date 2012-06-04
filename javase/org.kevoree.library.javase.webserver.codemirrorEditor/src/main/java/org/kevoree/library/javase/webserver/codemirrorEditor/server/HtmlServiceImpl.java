package org.kevoree.library.javase.webserver.codemirrorEditor.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.kevoree.library.javase.webserver.codemirrorEditor.client.SendContent;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;




public class HtmlServiceImpl extends RemoteServiceServlet implements SendContent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	HtmlEditor wrappee;
	public HtmlServiceImpl(HtmlEditor wrappee  ) {
		this.wrappee = wrappee;
	}
	public HtmlServiceImpl() {
	
	}
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		super.doGet(req, resp);
		//System.err.println("toto");
	}
	@Override
	public void sendHtmlContent(String s) {
        System.err.println("toto " + s);
		wrappee.sendHtmlContent(s) ;
	}

	


}
