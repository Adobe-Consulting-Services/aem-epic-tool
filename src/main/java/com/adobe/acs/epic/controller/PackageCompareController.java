/**
 * Sample Skeleton for 'PackageCompare.fxml' Controller Class
 */
package com.adobe.acs.epic.controller;

import com.adobe.acs.epic.util.PackageOps;
import com.adobe.acs.epic.model.PackageComparison;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.VBox;

public class PackageCompareController {

    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="compareLabel"
    private Label compareLabel; // Value injected by FXMLLoader

    @FXML // fx:id="diffViewPane"
    private Accordion diffViewPane;
    
    @FXML
    private ProgressIndicator downloadIndicator;

    @FXML
    private VBox downloadPane;

    @FXML // fx:id="uniqueToLeftPane"
    private TitledPane uniqueToLeftPane; // Value injected by FXMLLoader

    @FXML // fx:id="uniqueLeftList"
    private ListView<String> uniqueLeftList; // Value injected by FXMLLoader

    @FXML // fx:id="uniqueToRightPane"
    private TitledPane uniqueToRightPane; // Value injected by FXMLLoader

    @FXML // fx:id="uniqueRightList"
    private ListView<String> uniqueRightList; // Value injected by FXMLLoader

    @FXML // fx:id="overlapPane"
    private TitledPane overlapPane; // Value injected by FXMLLoader

    @FXML // fx:id="overlapList"
    private ListView<String> overlapList; // Value injected by FXMLLoader

    @FXML // fx:id="commonPane"
    private TitledPane commonPane; // Value injected by FXMLLoader

    @FXML // fx:id="commonList"
    private ListView<String> commonList; // Value injected by FXMLLoader

    @FXML
    void copyAction(ActionEvent event) {
        Map<DataFormat, Object> content = new HashMap<>();
        content.put(DataFormat.PLAIN_TEXT, buildDiffReport());
        Clipboard.getSystemClipboard().setContent(content);
    }

    @FXML
    void exportAction(ActionEvent event) {

    }

    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert compareLabel != null : "fx:id=\"compareLabel\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert diffViewPane != null : "fx:id=\"diffViewPane\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert downloadPane != null : "fx:id=\"downloadPane\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert downloadIndicator != null : "fx:id=\"downloadIndicator\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert uniqueToLeftPane != null : "fx:id=\"uniqueToLeftPane\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert uniqueLeftList != null : "fx:id=\"uniqueLeftList\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert uniqueToRightPane != null : "fx:id=\"uniqueToRightPane\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert uniqueRightList != null : "fx:id=\"uniqueRightList\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert overlapPane != null : "fx:id=\"overlapPane\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert overlapList != null : "fx:id=\"overlapList\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert commonPane != null : "fx:id=\"commonPane\" was not injected: check your FXML file 'PackageCompare.fxml'.";
        assert commonList != null : "fx:id=\"commonList\" was not injected: check your FXML file 'PackageCompare.fxml'.";
    }

    PackageComparison diff;
    PackageType left, right;

    public void initDiffView(PackageType left, PackageType right, AuthHandler handler) {
        this.left = left;
        this.right = right;
        diff = new PackageComparison();
        SimpleDoubleProperty leftDownload = new SimpleDoubleProperty(0);
        SimpleDoubleProperty rightDownload = new SimpleDoubleProperty(0);
        downloadIndicator.progressProperty().bind(leftDownload.add(rightDownload).multiply(0.5));

        new Thread(() -> {
            try {
                diff.observe(PackageOps.getPackageContents(left, handler, leftDownload));
                diff.observe(PackageOps.getPackageContents(right, handler, rightDownload));
                downloadPane.setVisible(false);
                diffViewPane.setVisible(true);
                Platform.runLater(this::initListViews);
            } catch (IOException ex) {
                Logger.getLogger(PackageCompareController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    private void initListViews() {
        uniqueLeftList.setItems(FXCollections.observableArrayList(diff.getFilesUniqueForPackage(left)));
        uniqueRightList.setItems(FXCollections.observableArrayList(diff.getFilesUniqueForPackage(right)));
        overlapList.setItems(FXCollections.observableArrayList(diff.getOverlaps(true).keySet()));
        commonList.setItems(FXCollections.observableArrayList(diff.getCommonFiles()));

        uniqueToLeftPane.setText(uniqueLeftList.getItems().size() + " unique to " + left.getName() + " : " + left.getVersion());
        uniqueToRightPane.setText(uniqueRightList.getItems().size() + " unique to " + right.getName() + " : " + right.getVersion());
        overlapPane.setText(overlapList.getItems().size() + " " + overlapPane.getText());
        commonPane.setText(commonList.getItems().size() + " " + commonPane.getText());
    }

    private String buildDiffReport() {
        StringBuilder report = new StringBuilder();
        Collection<String> col = diff.getFilesUniqueForPackage(left);
        report.append(col.size()).append(" files unique to ").append(left.getVersion()).append("\n");
        col.forEach(f -> report.append(f).append("\n"));
        col = diff.getFilesUniqueForPackage(right);
        report.append("\nFiles unique to ").append(right.getVersion()).append("\n");
        col.forEach(f -> report.append(f).append("\n"));
        col = diff.getOverlaps(true).keySet();
        report.append("\n").append(col.size()).append(" files changed between ").append(left.getVersion()).append(" and ").append(right.getVersion()).append("\n");
        col.forEach(f -> report.append(f).append("\n"));
        col = diff.getCommonFiles();
        report.append("\n").append(col.size()).append(" common files (unchanged)\n");
        col.forEach(f -> report.append(f).append("\n"));
        return report.toString();
    }
}
