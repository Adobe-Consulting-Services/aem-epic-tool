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

import com.adobe.acs.epic.ApplicationState;
import com.adobe.acs.epic.EpicApp;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class AppController {

    AtomicInteger connectionCounter = new AtomicInteger();
    PackageListController packageListController;
    Tab recentConnectionTab;
    AuthHandler recentLoginHandler;
    PackageListController recentPackageListController;

    @FXML
    LoginController loginController;

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="tabs"
    private TabPane tabs;

    @FXML // fx:id="addConnectionTab"
    private Tab addConnectionTab;

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() throws IOException {
        assert loginController != null : "fx:id=\"login\" was not injected: check your FXML file 'App.fxml'.";
        assert tabs != null : "fx:id=\"tabs\" was not injected: check your FXML file 'App.fxml'.";
        assert addConnectionTab != null : "fx:id=\"addConnectionTab\" was not injected: check your FXML file 'App.fxml'.";
        
        Platform.runLater(this::addNewConnectionTab);

    }

    public void addNewConnectionTab() {
        int index = connectionCounter.getAndIncrement();
        AuthHandler loginHandler = loginController.generateNewHandler();
        FXMLLoader loader = new FXMLLoader(EpicApp.class.getResource("/fxml/PackageList.fxml"));
        loader.setResources(ApplicationState.getInstance().getResourceBundle());
        try {
            loader.load();
            recentConnectionTab = new Tab();
            recentConnectionTab.setContent(loader.getRoot());
            PackageListController packageListController = loader.getController();
            packageListController.setIndex(index);
            ApplicationState.getInstance().setAuthHandler(loginHandler, index);
            recentConnectionTab.textProperty().bind(loginHandler.model.hostProperty());
            loginHandler.model.loginConfirmedProperty().addListener((confirmedValue, oldValue, newValue) -> this.updateConnectionTabStyle(loginHandler));
            packageListController.initAuthHandlerHooks(loginHandler);
            updateConnectionTabStyle(loginHandler);
            tabs.getTabs().add(tabs.getTabs().size() - 1, recentConnectionTab);
        } catch (IOException ex) {
            Logger.getLogger(AppController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateConnectionTabStyle(AuthHandler loginHandler) {
        if (recentConnectionTab != null) {
            recentConnectionTab.setStyle("-fx-background-color:" + (loginHandler.model.loginConfirmedProperty().getValue() ? "#8f8" : "#f88"));
        }
    }
}
