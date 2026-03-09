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
    
    /**
     * Creates and executes an HTTP request with the specified request method.
     * This method handles the complete lifecycle of an HTTP request including
     * setting headers, configuring the request method, executing the request,
     * and capturing response details.
     * 
     * @param requestMethod the HTTP request method (POST, PUT, PATCH, GET, DELETE, DELETEWITHPAYLOAD)
     * @throws InterruptedException if the request is interrupted
     * @throws Exception if an error occurs during request execution
     */
    void createHttpRequest(Object requestMethod) throws InterruptedException, Exception;

}
