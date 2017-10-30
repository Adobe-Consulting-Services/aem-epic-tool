package com.adobe.acs.epic;

import com.adobe.acs.epic.controller.AppController;
import com.adobe.acs.epic.controller.AuthHandler;
import com.adobe.acs.epic.controller.PackageCompareController;
import com.adobe.acs.epic.controller.PackageInfoController;
import com.adobe.acs.model.pkglist.PackageType;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EpicApp extends Application {

    static EpicApp instance;
    private static Stage applicationWindow;

    private AppController appController;

    public static void openPackageDetails(PackageType pkg) {
        try {
            FXMLLoader loader = new FXMLLoader(EpicApp.class.getResource("/fxml/PackageInfo.fxml"));
            loader.setResources(ApplicationState.getInstance().getResourceBundle());
            loader.load();
            PackageInfoController runnerActivityController = loader.getController();

            Stage popup = new Stage();
            popup.setTitle(pkg.getName() + " (" + pkg.getVersion() + ")");
            popup.setScene(new Scene(loader.getRoot()));
            popup.initModality(Modality.NONE);
            popup.initOwner(applicationWindow);

            runnerActivityController.setPackage(pkg);

            popup.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(EpicApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void showPackageDiff(PackageType left, PackageType right, AuthHandler handler) {
        try {
            FXMLLoader loader = new FXMLLoader(EpicApp.class.getResource("/fxml/PackageCompare.fxml"));
            loader.setResources(ApplicationState.getInstance().getResourceBundle());
            loader.load();
            PackageCompareController runnerActivityController = loader.getController();

            Stage popup = new Stage();
            popup.setTitle("Package Comparison");
            popup.setScene(new Scene(loader.getRoot()));
            popup.initModality(Modality.NONE);
            popup.initOwner(applicationWindow);

            runnerActivityController.initDiffView(left, right, handler);

            popup.showAndWait();
        } catch (IOException ex) {
            Logger.getLogger(EpicApp.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    

    @Override
    public void start(Stage stage) throws Exception {
        instance = this;
        applicationWindow = stage;
        Locale locale = Locale.US;
        ApplicationState.getInstance().setResourceBundle(ResourceBundle.getBundle("Bundle", locale));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/App.fxml"));
        loader.setResources(ApplicationState.getInstance().getResourceBundle());
        loader.load();
        Parent root = loader.getRoot();
        appController = loader.getController();

        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");

        stage.setTitle("AEM External Package Inspect and Compare Tool");
        stage.setScene(scene);
        stage.show();
        applicationWindow.setOnCloseRequest(evt -> {
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
