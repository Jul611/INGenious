package com.ing.ingenious.api.contract;

/**
 * Interface for browser general API contract.
 */
public interface GeneralBrApi extends CommandApi {
    // Signature methods
    Boolean checkIfDriverIsAlive();
    Boolean elementPresent();
    Boolean elementSelected();
    Boolean elementDisplayed();
    Boolean elementEnabled();
    boolean isHScrollBarPresent();
    boolean isvScrollBarPresent();
    
}
