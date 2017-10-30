/*
 * Copyright 2016 Adobe Global Services.
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
package com.adobe.acs.epic;

import com.adobe.acs.epic.controller.AuthHandler;
import com.adobe.acs.epic.model.CrxPackage;
import com.adobe.acs.epic.model.PackageContents;
import com.adobe.acs.model.pkglist.PackageType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 *
 * @author brobert
 */
public class ApplicationState {

    private static ApplicationState singleton;
    private ApplicationState() {
    }
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    private ResourceBundle i18n;
    private final ArrayList<AuthHandler> authHandlers = new ArrayList<>();

    public static ApplicationState getInstance() {
        if (singleton == null) {
            singleton = new ApplicationState();
        }
        return singleton;
    }
    
    public EpicApp getApplication() {
        return EpicApp.instance;
    }

    public BooleanProperty runningProperty() {
        return isRunning;
    }

    public static String getMessage(String key) {
        if (singleton == null || singleton.i18n == null) {
            return key;
        } else {
            return singleton.i18n.getString(key);
        }
    }

    public void setResourceBundle(ResourceBundle bundle) {
        i18n = bundle;
    }

    public ResourceBundle getResourceBundle() {
        return i18n;
    }

    private ArrayList<CrxPackage> masterList;

    public void prepareMasterList(List<PackageType> rawList) {
        Map<String, CrxPackage> allPackages = new HashMap<>();
        rawList.forEach(p -> {
            String key = p.getGroup() + "~!~" + p.getName();
            if (!allPackages.containsKey(key)) {
                allPackages.put(key, new CrxPackage());
            }
            allPackages.get(key).trackVersion(p);
        });
        masterList = new ArrayList<>(allPackages.values());
        masterList.sort(PackageOps::orderPackagesByUnpacked);
    }

    public List<CrxPackage> getMasterList() {
        return masterList;
    }    

    public void setAuthHandler(AuthHandler auth, int i) {
        synchronized (authHandlers) {
            if (i >= authHandlers.size()) {
                authHandlers.add(auth);
            } else {
                authHandlers.set(i, auth);
            }
        }
    }
    
    public AuthHandler getAuthHandler(int i) {
        synchronized (authHandlers) {
            if (authHandlers.size() > i) {
                return authHandlers.get(i);
            } else {
                return null;
            }
        }
    }

    private final Map<String, PackageContents> cachedContents = new HashMap<>();
    private String getPackageContentsKey(PackageType pkg) {
        return pkg.getGroup() + "~~~" + pkg.getDownloadName() + "~~~" + pkg.getVersion() + "~~~" + pkg.getSize();
    }
    
    PackageContents getPackageContents(PackageType pkg) {
        return cachedContents.get(getPackageContentsKey(pkg));
    }

    void putPackageContents(PackageType pkg, PackageContents packageContents) {
        cachedContents.put(getPackageContentsKey(pkg), packageContents);
    }
}
