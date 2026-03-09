package com.ing.engine.commands.webservice;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.ingenious.api.contract.WebservicePluginApi;

public class GeneralWebservice extends Command implements WebservicePluginApi {

    public GeneralWebservice(CommandControl cc) {
        super(cc);
    }

}
