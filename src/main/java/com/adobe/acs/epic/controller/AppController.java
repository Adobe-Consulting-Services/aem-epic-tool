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
import com.adobe.acs.epic.DataUtils;
import com.adobe.acs.epic.EpicApp;
import com.adobe.acs.epic.PackageOps;
import com.adobe.acs.epic.model.CrxPackage;
import com.adobe.acs.epic.model.PackageComparison;
import com.adobe.acs.model.pkglist.CrxType;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javax.xml.bind.JAXB;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

public class AppController {

    AtomicInteger connectionCounter = new AtomicInteger();
    LoginController loginController;
    PackageListController packageListController;
    Tab recentConnectionTab;
    AuthHandler recentLoginHandler;
    PackageListController recentPackageListController;

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
        assert tabs != null : "fx:id=\"tabs\" was not injected: check your FXML file 'App.fxml'.";
        assert addConnectionTab != null : "fx:id=\"addConnectionTab\" was not injected: check your FXML file 'App.fxml'.";
        
        addNewConnectionTab();

    }

    public void addNewConnectionTab() throws IOException {
        int index = connectionCounter.getAndIncrement();
        Tab connectionTab = new Tab();
        tabs.getTabs().add(tabs.getTabs().size() - 1, connectionTab);
        FXMLLoader loader = new FXMLLoader(EpicApp.class.getResource("/fxml/PackageListController.fxml"));
        loader.setResources(ApplicationState.getInstance().getResourceBundle());
        loader.load();
        PackageListController packageListController = loader.getController();
        packageListController.setIndex(index);
        AuthHandler loginHandler = loginController.generateNewHandler();
        ApplicationState.getInstance().setAuthHandler(loginHandler, index);
        loginHandler.model.loginConfirmedProperty().addListener((confirmedValue, oldValue, newValue) -> this.updateConnectionTabStyle());
        updateConnectionTabStyle();
        packageListController.initAuthHandlerHooks(loginHandler);
    }

    private void updateConnectionTabStyle() {
        if (recentConnectionTab != null) {
            recentConnectionTab.setStyle("-fx-background-color:" + (recentLoginHandler.model.loginConfirmedProperty().getValue() ? "#8f8" : "#f88"));
        }
    }
}
