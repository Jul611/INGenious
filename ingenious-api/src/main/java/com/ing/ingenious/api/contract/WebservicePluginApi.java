package com.ing.ingenious.api.contract;

import com.ing.ingenious.api.contract.CommandPluginApi;

public interface WebservicePluginApi extends CommandPluginApi  {

    /**
     * Gets the endpoint URL for HTTP/API operations.
     * @return the endpoint URL
     */
    String Endpoint();
    
    /**
     * Gets the HTTP response code.
     * @return the response code as a string
     */
    String ResponseCode();
    
    /**
     * Gets the HTTP response message.
     * @return the response message
     */
    String ResponseMessage();
    
    /**
     * Gets the HTTP response body.
     * @return the response body as a string
     */
    String ResponseBody();
    
    /**
     * Gets the HTTP connection object.
     * @return the connection object (needs to be cast to appropriate type)
     */
    Object Connection();
    
    /**
     * Gets the HTTP user agent string.
     * @return the HTTP user agent
     */
    String HttpAgent();

}
