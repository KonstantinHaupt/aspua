package de.aspua.gui.Utils;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServlet;

import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;

@WebServlet(urlPatterns = "/*", name = "slot", asyncSupported = true, initParams = {
        @WebInitParam(name = InitParameters.I18N_PROVIDER, value = "de.aspua.gui.Utils.TranslationProvider") })
public class ApplicationServlet extends VaadinServlet {
}
