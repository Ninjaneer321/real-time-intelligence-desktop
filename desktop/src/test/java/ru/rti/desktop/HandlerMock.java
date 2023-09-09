package ru.rti.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Lazy;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.io.TempDir;
import ru.rti.desktop.cache.impl.AppCacheImpl;
import ru.rti.desktop.config.FileConfig;
import ru.rti.desktop.config.view.ConfigurationConfig;
import ru.rti.desktop.helper.FilesHelper;
import ru.rti.desktop.helper.GUIHelper;
import ru.rti.desktop.manager.impl.ConfigurationManagerImpl;
import ru.rti.desktop.model.column.QueryColumnNames;
import ru.rti.desktop.model.config.Connection;
import ru.rti.desktop.model.config.Query;
import ru.rti.desktop.model.config.Task;
import ru.rti.desktop.model.table.JXTableCase;
import ru.rti.desktop.security.EncryptDecrypt;
import ru.rti.desktop.view.handler.connection.ConnectionButtonPanelHandler;
import ru.rti.desktop.view.handler.profile.ProfileButtonPanelHandler;
import ru.rti.desktop.view.handler.query.QueryButtonPanelHandler;
import ru.rti.desktop.view.handler.task.TaskButtonPanelHandler;
import ru.rti.desktop.view.pane.JTabbedPaneConfig;
import ru.rti.desktop.view.panel.config.ButtonPanel;
import ru.rti.desktop.view.panel.config.connection.ConnectionPanel;
import ru.rti.desktop.view.panel.config.profile.ProfilePanel;
import ru.rti.desktop.view.panel.config.query.QueryPanel;
import ru.rti.desktop.view.panel.config.task.TaskPanel;
import ru.rti.desktop.view.structure.config.ConfigViewImpl;
import ru.rti.desktop.warehouse.LocalDB;


@Log4j2
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class HandlerMock {

    @TempDir
    static File configurationDir;

    protected ObjectMapper objectMapper;

    @Inject
    protected Lazy<QueryButtonPanelHandler> queryButtonPanelHandlerLazy;

    @Inject
    protected Lazy<QueryPanel> queryPanelLazy;

    @Inject
    protected Lazy<ConfigurationManagerImpl> configurationManagerLazy;

    @Inject
    protected Lazy<FilesHelper> filesHelperLazy;
    @Inject
    protected Lazy<LocalDB> localDBLazy;

    @Inject
    protected Lazy<EncryptDecrypt> encryptDecrypt;

    @Inject
    protected Lazy<ProfileButtonPanelHandler> profileButtonPanelHandlerLazy;
    @Inject
    protected Lazy<ProfilePanel> profilePanelLazy;
    @Inject
    protected Lazy<TaskButtonPanelHandler> taskButtonPanelHandlerLazy;
    @Inject
    protected Lazy<TaskPanel> taskPanelLazy;
    @Inject
    protected Lazy<ConnectionButtonPanelHandler> connectionButtonPanelHandlerLazy;
    @Inject
    protected Lazy<ConnectionPanel> connectionPanelLazy;

    @Inject
    protected Lazy<AppCacheImpl> appCacheLazy;

    @Inject
    protected Lazy<ConfigViewImpl> configViewLazy;

    protected ButtonPanel buttonQueryPanelMock = new ButtonPanel();
    protected ButtonPanel buttonProfilePanelMock = new ButtonPanel();
    protected ButtonPanel buttonTaskPanelMock = new ButtonPanel();
    protected ButtonPanel buttonConnectionPanelMock = new ButtonPanel();

    protected JTabbedPaneConfig jTabbedPaneConfig = new JTabbedPaneConfig();

    protected JXTableCase profileCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});
    protected JXTableCase taskCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});
    protected JXTableCase connectionCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});
    protected JXTableCase queryCase = GUIHelper.getJXTableCase(5, new String[]{QueryColumnNames.ID.getColName(), QueryColumnNames.NAME.getColName()});

    @BeforeAll
    public void setUp() {
        MainComponentTest mainQueryComponentTest = DaggerMainComponentTest
                .builder()
                .configurationConfig(new ConfigurationConfig() {
                    @Override
                    public ButtonPanel getQueryButtonPanel() {return buttonQueryPanelMock;}

                    @Override
                    public JTabbedPaneConfig getJTabbedPaneConfig() {return jTabbedPaneConfig;}

                    @Override
                    public JXTableCase getProfileConfigCase() { return profileCase;}
                    @Override
                    public JXTableCase getTaskConfigCase() { return taskCase;}
                    @Override
                    public JXTableCase getConnectionConfigCase() { return connectionCase;}
                    @Override
                    public JXTableCase getQueryConfigCase() { return queryCase;}

                    @Override
                    public ButtonPanel getProfileButtonPanel() {
                        return buttonProfilePanelMock;
                    }

                    @Override
                    public ButtonPanel getTaskButtonPanel() {
                        return buttonTaskPanelMock;
                    }

                    @Override
                    public ButtonPanel getConnectionButtonPanel() {
                        return buttonConnectionPanelMock;
                    }
                })
                .fileConfig(new FileConfig() {
                    @Override
                    public FilesHelper getFilesHelper() {
                        return new FilesHelper(configurationDir.getAbsolutePath());
                    }

                    @Override
                    public Gson getGson() {
                        return new GsonBuilder()
                            .setPrettyPrinting()
                            .create();
                    }
                })
                .build();

        mainQueryComponentTest.inject(this);

        objectMapper = new ObjectMapper();
    }

    protected void createQueryTest(Query query) {
        buttonQueryPanelMock.getBtnNew().doClick();
        queryPanelLazy.get().getMainQueryPanel().getQueryName().setText(query.getName());
        queryPanelLazy.get().getMainQueryPanel().getQueryDescription().setText(query.getDescription());
        buttonQueryPanelMock.getBtnSave().doClick();
    }

    protected void createConnectionTest(@NotNull Connection connection) {
        buttonConnectionPanelMock.getBtnNew().doClick();
        connectionPanelLazy.get().getJTextFieldConnectionName().setText(connection.getName());
        connectionPanelLazy.get().getJTextFieldConnectionURL().setText(connection.getUrl());
        connectionPanelLazy.get().getJTextFieldConnectionUserName().setText(connection.getUserName());
        connectionPanelLazy.get().getJTextFieldConnectionPassword().setText(connection.getPassword());
        connectionPanelLazy.get().getJTextFieldConnectionJar().setText(connection.getJar());
        connectionPanelLazy.get().getJTextFieldConnectionDriver().setText(connection.getDriver());
        buttonConnectionPanelMock.getBtnSave().doClick();
    }

    protected void createTaskTest(@NotNull Task task) {
        buttonTaskPanelMock.getBtnNew().doClick();
        taskPanelLazy.get().getJTextFieldTask().setText(task.getName());
        taskPanelLazy.get().getJTextFieldDescription().setText(task.getDescription());
        buttonTaskPanelMock.getBtnSave().doClick();
    }

    protected String getTestData(String fileName) throws IOException {
        return Files.readString(Paths.get("src","test", "resources", "json", fileName));
    }

    protected void disposeWindows() {
        ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);

        scheduledExecutor.scheduleAtFixedRate(() -> {
            Window[] windows = Window.getWindows();
            if (windows != null && windows.length > 0) {
                log.info("Found " + windows.length + " window(s) " + windows[0].getClass().getSuperclass());

                for (int i = windows.length - 1; i >= 0; i--) {
                    try {
                        windows[i].dispose();
                    } catch (Exception e) {
                        log.catching(e);
                    }
                }
            }
        }, 2, 1, TimeUnit.SECONDS);
    }

    @AfterAll
    public void tearDown() {
        localDBLazy.get().closeBackendDb();
        filesHelperLazy.get().cleanDir();
    }
}
