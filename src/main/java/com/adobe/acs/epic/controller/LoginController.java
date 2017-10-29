/* 
 * Copyright 2015 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.epic.controller;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    Map<String, StringProperty> defaults;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="passwordField"
    private PasswordField passwordField; // Value injected by FXMLLoader

    @FXML // fx:id="hostField"
    private TextField hostField; // Value injected by FXMLLoader

    @FXML // fx:id="usernameField"
    private TextField usernameField; // Value injected by FXMLLoader

    @FXML // fx:id="sslCheckbox"
    private CheckBox sslCheckbox; // Value injected by FXMLLoader

    @FXML // fx:id="connectionVerificationLabel"
    private Label connectionVerificationLabel; // Value injected by FXMLLoader

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert passwordField != null : "fx:id=\"passwordField\" was not injected: check your FXML file 'App.fxml'.";
        assert hostField != null : "fx:id=\"hostField\" was not injected: check your FXML file 'App.fxml'.";
        assert usernameField != null : "fx:id=\"usernameField\" was not injected: check your FXML file 'App.fxml'.";
        assert sslCheckbox != null : "fx:id=\"sslCheckbox\" was not injected: check your FXML file 'App.fxml'.";
        assert connectionVerificationLabel != null : "fx:id=\"connectionVerificationLabel\" was not injected: check your FXML file 'App.fxml'.";
    }

    AuthHandler generateNewHandler() {
        AuthHandler loginHandler = new AuthHandler(
                hostField.textProperty(), sslCheckbox.selectedProperty(),
                usernameField.textProperty(), passwordField.textProperty());
        connectionVerificationLabel.textProperty().bind(loginHandler.model.statusMessageProperty());
        return loginHandler;
    }
}
