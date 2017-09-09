package limax.auany.plats;

import java.util.Map;

import org.w3c.dom.Element;

import com.sun.net.httpserver.HttpHandler;

import limax.auany.PlatProcess;
import limax.defines.ErrorCodes;
import limax.defines.ErrorSource;
import limax.endpoint.AuanyService.Result;
import limax.util.ElementHelper;

public final class FlowControl implements PlatProcess {

	private String password = "912349h12i3g487608asf601023g2r";

	@Override
	public void init(Element ele, Map<String, HttpHandler> httphandlers) {
		final ElementHelper eh = new ElementHelper(ele);
		password = eh.getString("password");
	}

	@Override
	public void check(String username, String token, Result result) {
		result.apply(ErrorSource.LIMAX, token.equals(password) ? ErrorCodes.SUCCEED : ErrorCodes.AUANY_BAD_TOKEN,
				username);
	}

}
