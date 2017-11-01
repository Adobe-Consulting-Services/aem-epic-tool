package com.adobe.acs.epic.controller;

import com.adobe.acs.epic.ApplicationState;
import com.adobe.acs.epic.DataUtils;
import static com.adobe.acs.epic.DataUtils.summarizeUserDateCombo;
import com.adobe.acs.epic.EpicApp;
import com.adobe.acs.epic.util.PackageOps;
import com.adobe.acs.epic.model.CrxPackage;
import com.adobe.acs.epic.model.PackageContents;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class PackageInfoController {

    AuthHandler authHandler = null;
    
    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="tabPane"
    private TabPane tabPane;

    @FXML
    private VBox otherVersionsPane;

    @FXML
    private ListView<String> otherVersionsList;

    @FXML // fx:id="generalTab"
    private Tab generalTab; // Value injected by FXMLLoader

    @FXML // fx:id="groupLabel"
    private Label groupLabel; // Value injected by FXMLLoader

    @FXML // fx:id="packageLabel"
    private Label packageLabel; // Value injected by FXMLLoader

    @FXML // fx:id="downloadLinkLabel"
    private Label downloadLinkLabel; // Value injected by FXMLLoader

    @FXML // fx:id="sizeLabel"
    private Label sizeLabel; // Value injected by FXMLLoader

    @FXML // fx:id="versionLabel"
    private Label versionLabel; // Value injected by FXMLLoader

    @FXML // fx:id="createdLabel"
    private Label createdLabel; // Value injected by FXMLLoader

    @FXML // fx:id="modifiedLabel"
    private Label modifiedLabel; // Value injected by FXMLLoader

    @FXML // fx:id="unpackedLabel"
    private Label unpackedLabel; // Value injected by FXMLLoader

    @FXML // fx:id="analysisTab"
    private Tab analysisTab; // Value injected by FXMLLoader

    @FXML // fx:id="packageConfirmPanel"
    private BorderPane packageConfirmPanel; // Value injected by FXMLLoader

    @FXML // fx:id="downloadingPane"
    private BorderPane downloadingPane; // Value injected by FXMLLoader

    @FXML // fx:id="downloadProgressIndicator"
    private ProgressIndicator downloadProgressIndicator; // Value injected by FXMLLoader

    @FXML // fx:id="analysisPane"
    private VBox analysisPane; // Value injected by FXMLLoader

    @FXML // fx:id="downloadFileLabel"
    private Label downloadFileLabel; // Value injected by FXMLLoader

    @FXML // fx:id="folderCountLabel"
    private Label folderCountLabel; // Value injected by FXMLLoader

    @FXML // fx:id="fileCountLabel"
    private Label fileCountLabel; // Value injected by FXMLLoader

    @FXML // fx:id="rootSummaryTable"
    private TableView<Map.Entry<String, Integer>> rootSummaryTable;
    @FXML
    private TableColumn<Map.Entry<String, Integer>, String> rootSummaryCol1;
    @FXML
    private TableColumn<Map.Entry<String, Integer>, String> rootSummaryCol2;

    @FXML // fx:id="typeSummaryTable"
    private TableView<Map.Entry<String, Set<String>>> typeSummaryTable;
    @FXML
    private TableColumn<Map.Entry<String, Set<String>>, String> typeSummaryCol1;
    @FXML
    private TableColumn<Map.Entry<String, Set<String>>, String> typeSummaryCol2;

    public static long MAX_AUTO_DOWNLOAD_SIZE = 50 << 20; // 50 megabytes

    @FXML
    void browseToDownloadedFile(MouseEvent event) {

    }

    @FXML
    void packageDownloadCancel(ActionEvent event) {
        generalTab.getTabPane().selectionModelProperty().get().select(generalTab);
    }

    @FXML
    void packageDownloadOk(ActionEvent event) throws IOException {
        startDownload();
    }
    
    @FXML
    void exportRootSummaries(ActionEvent event) throws FileNotFoundException, IOException {
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Export (Save) data");
        saveChooser.setInitialFileName(pkg.getName() + "_root_path_summary.xlsx");
        File saveFile = saveChooser.showSaveDialog(null);
        if (saveFile != null) {
            FileOutputStream out = new FileOutputStream(saveFile);
            String[] headers = new String[]{
                "Root folder", "Count"
            };
            DataUtils.exportSpreadsheet(out, pkgContents.getBaseCounts().entrySet(), headers,
                    Map.Entry::getKey,
                    Map.Entry::getValue
            );
            out.close();
        }
    }
    
    @FXML
    void exportTypeSummaries(ActionEvent event) throws IOException {
        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Export (Save) data");
        saveChooser.setInitialFileName(pkg.getName() + "_type_summary.xlsx");
        File saveFile = saveChooser.showSaveDialog(null);
        if (saveFile != null) {
            FileOutputStream out = new FileOutputStream(saveFile);
            String[] headers = new String[]{
                "Type", "Count"
            };
            DataUtils.exportSpreadsheet(out, pkgContents.getFilesByType().entrySet(), headers,
                    Map.Entry::getKey,
                    e->e.getValue().size()
            );
            out.close();
        }
    }

    /**
     * Initializes the controller class.
     */
    @FXML
    void initialize() {
        assert tabPane != null : "fx:id=\"tabPane\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert generalTab != null : "fx:id=\"generalTab\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert otherVersionsPane != null : "fx:id=\"otherVersionsPane\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert otherVersionsList != null : "fx:id=\"otherVersionsList\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert groupLabel != null : "fx:id=\"groupLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert packageLabel != null : "fx:id=\"packageLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert downloadLinkLabel != null : "fx:id=\"downloadLinkLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert sizeLabel != null : "fx:id=\"sizeLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert versionLabel != null : "fx:id=\"versionLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert createdLabel != null : "fx:id=\"createdLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert modifiedLabel != null : "fx:id=\"modifiedLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert unpackedLabel != null : "fx:id=\"unpackedLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert analysisTab != null : "fx:id=\"analysisTab\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert packageConfirmPanel != null : "fx:id=\"packageConfirmPanel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert downloadingPane != null : "fx:id=\"downloadingPane\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert downloadProgressIndicator != null : "fx:id=\"downloadProgressIndicator\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert analysisPane != null : "fx:id=\"analysisPane\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert downloadFileLabel != null : "fx:id=\"downloadFileLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert folderCountLabel != null : "fx:id=\"folderCountLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert fileCountLabel != null : "fx:id=\"fileCountLabel\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert rootSummaryTable != null : "fx:id=\"rootSummaryTable\" was not injected: check your FXML file 'PackageInfo.fxml'.";
        assert typeSummaryTable != null : "fx:id=\"typeSummaryTable\" was not injected: check your FXML file 'PackageInfo.fxml'.";

        tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
            if (newTab.equals(analysisTab)) {
                analysisTabSelected();
            }
        });

        rootSummaryCol1.setCellValueFactory(cell
                -> new SimpleStringProperty(cell.getValue().getKey()));

        rootSummaryCol2.setCellValueFactory(cell
                -> new SimpleStringProperty(cell.getValue().getValue().toString()));

        typeSummaryCol1.setCellValueFactory(cell
                -> new SimpleStringProperty(cell.getValue().getKey()));

        typeSummaryCol2.setCellValueFactory(cell
                -> new SimpleStringProperty(Integer.toString(cell.getValue().getValue().size())));
    }

    private PackageType pkg;
    private PackageContents pkgContents;

    public void setPackage(PackageType pkg, List<CrxPackage> allPackages) {
        this.pkg = pkg;

        groupLabel.setText(pkg.getGroup());
        packageLabel.setText(pkg.getName());
        downloadLinkLabel.setText(PackageOps.getDownloadLink(pkg, authHandler));
        versionLabel.setText(pkg.getVersion());
        sizeLabel.setText(DataUtils.getHumanSize(pkg.getSize()) + " (" + pkg.getSize() + " bytes)");
        createdLabel.setText(summarizeUserDateCombo(pkg.getCreated(), pkg.getCreatedBy()));
        modifiedLabel.setText(summarizeUserDateCombo(pkg.getLastModified(), pkg.getLastModified()));
        unpackedLabel.setText(summarizeUserDateCombo(pkg.getLastUnpacked(), pkg.getLastUnpackedBy()));

        downloadLinkLabel.setOnMousePressed(evt -> {
            if (evt.getButton() != MouseButton.PRIMARY) {
                return;
            }
            Map<DataFormat, Object> content = new HashMap<>();
            String url = PackageOps.getDownloadLink(pkg, authHandler);
            content.put(DataFormat.URL, url);
            content.put(DataFormat.PLAIN_TEXT, url);
            Clipboard.getSystemClipboard().setContent(content);
            downloadLinkLabel.setText("Copied to clipboard");
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(PackageInfoController.class.getName()).log(Level.SEVERE, null, ex);
                }
                Platform.runLater(() -> downloadLinkLabel.setText(url));
            }).start();
        });

        CrxPackage currentPackage;
        if (pkg instanceof CrxPackage) {
            currentPackage = (CrxPackage) pkg;
        } else {
            currentPackage = allPackages.stream()
                    .filter(p -> p.getGroup().equals(pkg.getGroup())
                    && p.getName().equals(pkg.getName()))
                    .findFirst().orElse(null);
        }
        List<String> allVersions = null;
        if (currentPackage != null) {
            allVersions = currentPackage.getAllVersions().values().stream()
                    .map(PackageType::getVersion).filter(v -> !v.equals(pkg.getVersion()))
                    .collect(Collectors.toList());
        }
        if (allVersions != null && !allVersions.isEmpty()) {
            otherVersionsList.setItems(FXCollections.observableList(allVersions));
        } else {
            otherVersionsPane.setVisible(false);
        }
        otherVersionsList.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                String version = otherVersionsList.getSelectionModel().getSelectedItem();
                EpicApp.openPackageDetails(currentPackage.getAllVersions().get(version), allPackages, authHandler);
            }
        });
        otherVersionsList.setOnContextMenuRequested(evt -> {
            MenuItem diffMenuItem = new MenuItem("Diff to this version (" + pkg.getVersion() + ")");
            diffMenuItem.setOnAction(evt2 -> {
                String version = otherVersionsList.getSelectionModel().getSelectedItem();
                EpicApp.showPackageDiff(pkg, currentPackage.getAllVersions().get(version), authHandler);
            });
            ContextMenu cm = new ContextMenu(
                    diffMenuItem
            );
            cm.show(otherVersionsList, evt.getScreenX(), evt.getScreenY());
        });
    }

    private void analysisTabSelected() {
        if (pkgContents == null) {
            if (pkg.getSize() <= MAX_AUTO_DOWNLOAD_SIZE) {
                startDownload();
            } else {
                analysisPane.setVisible(false);
                downloadingPane.setVisible(false);
                packageConfirmPanel.setVisible(true);
            }
        } else {
            analysisPane.setVisible(true);
            downloadingPane.setVisible(false);
            packageConfirmPanel.setVisible(false);
        }
    }

    private void startDownload() {
        packageConfirmPanel.setVisible(false);
        analysisPane.setVisible(false);
        if (PackageOps.hasPackageContents(pkg)) {
            downloadingPane.setVisible(false);
            try {
                showPackageContents(PackageOps.getPackageContents(pkg, authHandler, null));
            } catch (IOException ex) {
                downloadingPane.setVisible(true);
                downloadingPane.setCenter(new TextArea("Error downloading package: " + ex.getMessage()));
                Logger.getLogger(PackageInfoController.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            downloadingPane.setVisible(true);
            new Thread(() -> {
                try {
                    PackageContents contents = PackageOps.getPackageContents(pkg, authHandler, downloadProgressIndicator.progressProperty());
                    Platform.runLater(() -> showPackageContents(contents));
                } catch (IOException ex) {
                    Logger.getLogger(PackageInfoController.class.getName()).log(Level.SEVERE, null, ex);
                    Platform.runLater(()
                            -> downloadingPane.setCenter(new TextArea("Error downloading package: " + ex.getMessage()))
                    );
                }
            }).start();
        }
    }

    private void showPackageContents(PackageContents packageContents) {
        packageConfirmPanel.setVisible(false);
        downloadingPane.setVisible(false);
        analysisPane.setVisible(true);
        pkgContents = packageContents;

        downloadFileLabel.setText(packageContents.getFile().getPath());
        fileCountLabel.setText(String.valueOf(packageContents.getFileCount()));
        folderCountLabel.setText(String.valueOf(packageContents.getFolderCount()));
        rootSummaryTable.setItems(FXCollections.observableArrayList(packageContents.getBaseCounts().entrySet()));
        typeSummaryTable.setItems(FXCollections.observableArrayList(packageContents.getFilesByType().entrySet()));

        downloadFileLabel.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2) {
                String path;
                if (evt.isShiftDown() || evt.isControlDown()) {
                    path = packageContents.getFile().getParent();
                } else {
                    path = packageContents.getFile().getPath();
                }
                HostServices services = ApplicationState.getInstance().getApplication().getHostServices();
                services.showDocument(path);
            }
        });
    }

    public void setAuthHandler(AuthHandler handler) {
        this.authHandler = handler;
    }
}
