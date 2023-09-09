package ru.rti.desktop.view.handler.task;

import static ru.rti.desktop.model.view.handler.LifeCycleStatus.COPY;
import static ru.rti.desktop.model.view.handler.LifeCycleStatus.EDIT;
import static ru.rti.desktop.model.view.handler.LifeCycleStatus.NEW;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import lombok.extern.log4j.Log4j2;
import ru.rti.desktop.exception.EmptyNameException;
import ru.rti.desktop.exception.NotFoundException;
import ru.rti.desktop.exception.NotSelectedRowException;
import ru.rti.desktop.manager.ProfileManager;
import ru.rti.desktop.manager.TemplateManager;
import ru.rti.desktop.model.config.Query;
import ru.rti.desktop.model.info.*;
import ru.rti.desktop.model.table.JXTableCase;
import ru.rti.desktop.model.view.handler.LifeCycleStatus;
import ru.rti.desktop.prompt.Internationalization;
import ru.rti.desktop.router.event.EventListener;
import ru.rti.desktop.view.pane.JTabbedPaneConfig;
import ru.rti.desktop.view.panel.config.ButtonPanel;
import ru.rti.desktop.view.panel.config.task.MultiSelectQueryPanel;
import ru.rti.desktop.view.panel.config.task.TaskPanel;

@Log4j2
@Singleton
public class TaskButtonPanelHandler implements ActionListener {

    private final ProfileManager profileManager;
    private final TemplateManager templateManager;
    private final EventListener eventListener;

    private final JXTableCase profileCase;
    private final JXTableCase taskCase;
    private final JXTableCase connectionCase;
    private final JXTableCase queryCase;
    private final TaskPanel taskPanel;
    private final MultiSelectQueryPanel multiSelectQueryPanel;
    private final ButtonPanel taskButtonPanel;
    private final JTabbedPaneConfig jTabbedPaneConfig;
    private final JCheckBox checkboxConfig;

    private LifeCycleStatus status;
    private final ResourceBundle bundleDefault;

    @Inject
    public TaskButtonPanelHandler(@Named("profileManager") ProfileManager profileManager,
                                  @Named("templateManager") TemplateManager templateManager,
                                  @Named("eventListener") EventListener eventListener,
                                  @Named("profileConfigCase") JXTableCase profileCase,
                                  @Named("taskConfigCase") JXTableCase taskCase,
                                  @Named("connectionConfigCase") JXTableCase connectionCase,
                                  @Named("queryConfigCase") JXTableCase queryCase,
                                  @Named("taskConfigPanel") TaskPanel taskPanel,
                                  @Named("multiSelectQueryPanel") MultiSelectQueryPanel multiSelectQueryPanel,
                                  @Named("taskButtonPanel") ButtonPanel taskButtonPanel,
                                  @Named("jTabbedPaneConfig") JTabbedPaneConfig jTabbedPaneConfig,
                                  @Named("checkboxConfig") JCheckBox checkboxConfig) {
        this.profileManager = profileManager;
        this.templateManager = templateManager;

        this.bundleDefault = Internationalization.getInternationalizationBundle();

        this.profileCase = profileCase;
        this.taskCase = taskCase;
        this.connectionCase = connectionCase;
        this.queryCase = queryCase;

        this.taskPanel = taskPanel;
        this.multiSelectQueryPanel = multiSelectQueryPanel;
        this.taskButtonPanel = taskButtonPanel;
        this.jTabbedPaneConfig = jTabbedPaneConfig;
        this.checkboxConfig = checkboxConfig;
        this.taskButtonPanel.getBtnNew().addActionListener(this);
        this.taskButtonPanel.getBtnCopy().addActionListener(this);
        this.taskButtonPanel.getBtnDel().addActionListener(this);
        this.taskButtonPanel.getBtnEdit().addActionListener(this);
        this.taskButtonPanel.getBtnSave().addActionListener(this);
        this.taskButtonPanel.getBtnCancel().addActionListener(this);

        this.taskButtonPanel.getBtnDel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        this.taskButtonPanel.getBtnDel().getActionMap().put("delete", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                taskButtonPanel.getBtnDel().doClick();
            }
        });

        this.taskButtonPanel.getBtnCancel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        this.taskButtonPanel.getBtnCancel().getActionMap().put("cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                taskButtonPanel.getBtnCancel().doClick();
            }
        });

        this.taskPanel.getTaskConnectionComboBox().addItemListener(e -> {
            if (NEW.equals(status) || COPY.equals(status) || EDIT.equals(status)) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    fillAvailableQueryList();
                }
            }
        });

        this.status = LifeCycleStatus.NONE;

        this.eventListener = eventListener;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == taskButtonPanel.getBtnNew()) {
            status = NEW;
            List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
            if (!connectionAll.isEmpty()) {
                setPanelView(false);
                List<List<?>> connectionDataList = new LinkedList<>();
                connectionAll.forEach(connection -> connectionDataList.add(
                        new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                connection.getUrl(), connection.getJar(), connection.getDriver()))));

                newEmptyPanel();

                taskPanel.getTaskConnectionComboBox().setTableData(connectionDataList);

                multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel().getDataVector().removeAllElements();
                multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel().fireTableDataChanged();

                fillAvailableQueryList();
            } else {
                JOptionPane.showMessageDialog(null, "No connections were created",
                        "General Error", JOptionPane.ERROR_MESSAGE);
            }

        } else if (e.getSource() == taskButtonPanel.getBtnCopy()) {
            status = COPY;
            if (taskCase.getJxTable().getSelectedRow() == -1) {
                throw new NotSelectedRowException("The task to copy is not selected. Please select and try again!");
            } else {
                int taskId = getSelectedTaskId();
                TaskInfo task = profileManager.getTaskInfoById(taskId);
                if (Objects.isNull(task)) {
                    throw new NotFoundException("Not found task: " + taskId);
                }
                setPanelView(false);

                taskPanel.getJTextFieldTask().setText(task.getName() + "_copy");
                taskPanel.getJTextFieldDescription().setText(task.getDescription() + "_copy");
                fillAvailableQueryList();
            }

        } else if (e.getSource() == taskButtonPanel.getBtnDel()) {
            if (taskCase.getJxTable().getSelectedRow() == -1) {
                JOptionPane.showMessageDialog(null, "Not selected task. Please select and try again!",
                        "General Error", JOptionPane.ERROR_MESSAGE);
            } else {
                int taskId = getSelectedTaskId();
                int input = JOptionPane.showConfirmDialog(new JDialog(),// 0=yes, 1=no, 2=cancel
                        "Do you want to delete configuration: " + taskCase.getDefaultTableModel()
                                .getValueAt(taskCase.getJxTable().getSelectedRow(), 1) + "?");
                if (isUsedOnTask(taskId)) {
                if (input == 0) {
                    TaskInfo task = profileManager.getTaskInfoById(taskId);
                    if (Objects.isNull(task)) {
                        throw new NotFoundException("Not found task: " + taskId);
                    }

                    profileManager.deleteTask(task.getId(), task.getName());
                    clearTaskCase();

                    profileManager.getTaskInfoList().forEach(taskInfo -> taskCase.getDefaultTableModel()
                            .addRow(new Object[]{taskInfo.getId(), taskInfo.getName()}));

                    if (taskCase.getJxTable().getSelectedRow() > 0) {
                        taskCase.getJxTable().setRowSelectionInterval(0, 0);
                    }
                }
                } else {
                    JOptionPane.showMessageDialog(null, "Cannot delete this task it is used in the profile",
                            "General Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (e.getSource() == taskButtonPanel.getBtnEdit()) {
            if (taskCase.getJxTable().getSelectedRow() == -1) {
                throw new NotSelectedRowException("Not selected task. Please select and try again!");
            }
            status = EDIT;
            setPanelView(false);

        } else if (e.getSource() == taskButtonPanel.getBtnSave()) {

            if (NEW.equals(status) || COPY.equals(status)) {

                AtomicInteger taskIdNext = new AtomicInteger();

                profileManager.getTaskInfoList().stream()
                        .max(Comparator.comparing(TaskInfo::getId))
                        .ifPresentOrElse(task -> taskIdNext.set(task.getId()),
                                () -> {
                                    log.info("Not found Task");
                                    taskIdNext.set(0);
                                });
                if (!taskPanel.getJTextFieldTask().getText().trim().isEmpty()) {
                    int taskId = taskIdNext.incrementAndGet();
                    String newTaskName = taskPanel.getJTextFieldTask().getText();
                    checkTaskNameIsBusy(taskId, newTaskName);

                    TaskInfo saveTask = getTaskInfo(taskIdNext.incrementAndGet());

                    profileManager.addTask(saveTask);

                    clearTaskCase();

                    int selection = 0;
                    int index = 0;
                    for (TaskInfo task : profileManager.getTaskInfoList()) {
                        taskCase.getDefaultTableModel()
                                .addRow(new Object[]{task.getId(), task.getName()});

                        if (task.getId() == saveTask.getId()) {
                            index++;
                            selection = index;
                        }
                        index++;
                    }
                    setPanelView(true);
                    taskCase.getJxTable().setRowSelectionInterval(selection - 1, selection - 1);
                    multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
                } else {
                    throw new EmptyNameException("The name field is empty");
                }
            } else if (EDIT.equals(status)) {
                int selectedIndex = taskCase.getJxTable().getSelectedRow();
                int taskId = getSelectedTaskId();

                if (!taskPanel.getJTextFieldTask().getText().trim().isEmpty()) {
                    String newTaskName = taskPanel.getJTextFieldTask().getText();
                    checkTaskNameIsBusy(taskId, newTaskName);

                    TaskInfo oldTask = profileManager.getTaskInfoById(taskId);

                    TaskInfo editTask = getTaskInfo(taskId);

                    if (!oldTask.getName().equals(newTaskName)) {
                        deleteTaskById(taskId);
                        profileManager.addTask(editTask);
                    } else {
                        profileManager.updateTask(editTask);
                    }

                    clearTaskCase();

                    profileManager.getTaskInfoList().forEach(taskInfo -> taskCase.getDefaultTableModel()
                            .addRow(new Object[]{taskInfo.getId(), taskInfo.getName()}));

                    setPanelView(true);
                    taskCase.getJxTable().setRowSelectionInterval(selectedIndex, selectedIndex);
                    multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);
                } else {
                    throw new EmptyNameException("The name field is empty");
                }
            }
        } else if (e.getSource() == taskButtonPanel.getBtnCancel()) {

            if (taskCase.getJxTable().getSelectedRowCount() > 0) {
                int taskId = taskCase.getJxTable().getSelectedRow();
                taskCase.getJxTable().setRowSelectionInterval(0, 0);
                setPanelView(true);
                taskCase.getJxTable().setRowSelectionInterval(taskId, taskId);

                int selectedId = getSelectedTaskId();
                TaskInfo task = profileManager.getTaskInfoById(selectedId);
                if (Objects.isNull(task)) {
                    throw new NotFoundException("Not found task: " + taskId);
                }
                taskPanel.getJTextFieldTask().setText(task.getName());
                taskPanel.getJTextFieldDescription().setText(task.getDescription());

                String selectedConnection = (String) taskPanel.getTaskConnectionComboBox().getSelectedItem();

                int connectionId = profileManager.getConnectionInfoList()
                        .stream()
                        .filter(f -> f.getName().equalsIgnoreCase(selectedConnection))
                        .findAny()
                        .orElseThrow(() -> new NotFoundException("Not found connection: " + selectedConnection))
                        .getId();
                List<ConnectionInfo> connectionAll = profileManager.getConnectionInfoList();
                List<List<?>> connectionData = new ArrayList<>();
                connectionData.add(0, new ArrayList<>(Arrays.asList(connectionAll.get(connectionId - 1).getName(),
                        connectionAll.get(connectionId - 1).getUserName(),
                        connectionAll.get(connectionId - 1).getUrl(), connectionAll.get(connectionId - 1).getJar(),
                        connectionAll.get(connectionId - 1).getDriver())));
                for (ConnectionInfo connection : connectionAll) {
                    if (connection.getId() != connectionId) {
                        connectionData.add(
                                new ArrayList<>(Arrays.asList(connection.getName(), connection.getUserName(),
                                        connection.getUrl(), connection.getJar(), connection.getDriver())));
                    }
                }
                taskPanel.getTaskConnectionComboBox().setTableData(connectionData);

                fillAvailableQueryList();
                multiSelectQueryPanel.getJTabbedPaneQuery().setSelectedIndex(0);

            } else {
                setPanelView(true);
                newEmptyPanel();
                fillAvailableQueryList();
            }
        }

    }

    private void newEmptyPanel() {
        taskPanel.getJTextFieldTask().setText("");
        taskPanel.getJTextFieldTask().setPrompt(bundleDefault.getString("tName"));
        taskPanel.getJTextFieldDescription().setText("");
        taskPanel.getJTextFieldDescription().setPrompt(bundleDefault.getString("tDesc"));
    }

    public void checkTaskNameIsBusy(int id, String newTaskName) {
        List<TaskInfo> taskList = profileManager.getTaskInfoList();
        for (TaskInfo task : taskList) {
            if (task.getName().equals(newTaskName) && task.getId() != id) {
                throw new NotFoundException("Name " + newTaskName
                        + " already exists, please enter another one.");
            }
        }
    }

    public void deleteTaskById(int id) {
        TaskInfo taskDel = profileManager.getTaskInfoById(id);
        if (Objects.isNull(taskDel)) {
            throw new NotFoundException("Not found task by id: " + id);
        }
        profileManager.deleteTask(taskDel.getId(), taskDel.getName());
    }

    private void fillAvailableQueryList() {
        String connName = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0).toString();
        String connDriver = taskPanel.getTaskConnectionComboBox().getSelectedRow().get(4).toString();

        log.info("Connection's " + connName + " driver: " + connDriver);

        List<String> listSelectedQueryForExclude = getSelectedQueryNameList();

        // Configuration
        multiSelectQueryPanel.getQueryListCase().getDefaultTableModel().getDataVector().removeAllElements();
        multiSelectQueryPanel.getQueryListCase().getDefaultTableModel().fireTableDataChanged();

        List<String> connectionDrivers = getConnectionDriverAll();
        connectionDrivers.removeIf(q -> connDriver.contains(q));

        List<QueryInfo> queryListOfUnsuitableConnDriver = connectionDrivers.stream()
                .flatMap(driver -> profileManager.getQueryInfoListByConnDriver(driver).stream())
                .collect(Collectors.toList());

        List<QueryInfo> queryListWithoutConnDriver = profileManager.getQueryInfoList().stream()
                .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
                .filter(q -> !queryListOfUnsuitableConnDriver.contains(q))
                .collect(Collectors.toList());


        for (QueryInfo q : queryListWithoutConnDriver) {
            multiSelectQueryPanel.getQueryListCase().getDefaultTableModel()
                    .addRow(new Object[]{q.getId(), q.getName(), q.getDescription(), q.getText()});
        }

        // Template
        multiSelectQueryPanel.getTemplateListQueryCase().getDefaultTableModel().getDataVector().removeAllElements();
        multiSelectQueryPanel.getTemplateListQueryCase().getDefaultTableModel().fireTableDataChanged();

        templateManager.getQueryListByConnDriver(connDriver).stream()
                .filter(q -> !listSelectedQueryForExclude.contains(q.getName()))
                .forEach(q -> {

                    QueryInfo queryInfo = new QueryInfo();
                    queryInfo.setId(q.getId());
                    queryInfo.setName(q.getName());
                    queryInfo.setText(q.getText());
                    queryInfo.setDescription(q.getDescription());

                    multiSelectQueryPanel.getTemplateListQueryCase().getDefaultTableModel()
                            .addRow(new Object[]{queryInfo.getId(), queryInfo.getName(),
                                    queryInfo.getDescription(), queryInfo.getText()});
                });

        //Selected
        multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel().getDataVector().removeAllElements();
        multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel().fireTableDataChanged();

        profileManager.getQueryInfoListByConnDriver(connDriver).stream()
                .filter(q -> listSelectedQueryForExclude.contains(q.getName()))
                .forEach(q -> multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel()
                        .addRow(new Object[]{q.getId(), q.getName(), q.getDescription(), q.getText()}));

    }


    public int setRadioButtonPullTimeout() {
        int pullTimeout = 0;
        if (taskPanel.getRadioButtonPanel().getJRadioButton1().isSelected()) pullTimeout = 1;
        if (taskPanel.getRadioButtonPanel().getJRadioButton3().isSelected()) pullTimeout = 3;
        if (taskPanel.getRadioButtonPanel().getJRadioButton5().isSelected()) pullTimeout = 5;
        if (taskPanel.getRadioButtonPanel().getJRadioButton10().isSelected()) pullTimeout = 10;
        if (taskPanel.getRadioButtonPanel().getJRadioButton30().isSelected()) pullTimeout = 30;
        return pullTimeout;
    }

    private int getSelectedTaskId() {
        return (Integer) taskCase.getDefaultTableModel()
                .getValueAt(taskCase.getJxTable().getSelectedRow(), 0);
    }


    private void clearTaskCase() {
        taskCase.getDefaultTableModel().getDataVector().removeAllElements();
        taskCase.getDefaultTableModel().fireTableDataChanged();
    }

    private TaskInfo getTaskInfo(int taskId) {
        TaskInfo task = new TaskInfo();
        task.setId(taskId);
        task.setName(taskPanel.getJTextFieldTask().getText());
        task.setDescription(taskPanel.getJTextFieldDescription().getText());
        task.setPullTimeout(setRadioButtonPullTimeout());
        String selectedConnection = (String) taskPanel.getTaskConnectionComboBox().getSelectedRow().get(0);

        int connectionId = profileManager.getConnectionInfoList()
                .stream()
                .filter(f -> f.getName().equalsIgnoreCase(selectedConnection))
                .findAny()
                .orElseThrow(() -> new NotFoundException("Not found connection: " + selectedConnection))
                .getId();

        task.setConnectionId(connectionId);

        DefaultTableModel tableModel = multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel();

        if (tableModel.getRowCount() > 0) {
            List<Integer> queryListId = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                Integer selectedDataQueryId = (Integer) tableModel.getValueAt(i, 0);
                String selectedQueryName = tableModel.getValueAt(i, 1).toString();

                AtomicInteger queryIdNext = new AtomicInteger();

                profileManager.getQueryInfoList().stream()
                        .max(Comparator.comparing(QueryInfo::getId))
                        .ifPresentOrElse(query -> queryIdNext.set(query.getId()),
                                () -> log.info("Not found Query"));
                int newId = queryIdNext.incrementAndGet();

                if (!isExistQueryName(selectedQueryName)) {
                    List<Query> queryList = templateManager.getConfigList(Query.class);
                    Query saveQuery = queryList.stream()
                            .filter(s -> s.getId() == selectedDataQueryId)
                            .findAny()
                            .orElseThrow(() -> new NotFoundException("Not found query by id: " + selectedDataQueryId));

                    QueryInfo queryInfo = new QueryInfo();
                    queryInfo.setId(newId);
                    queryInfo.setName(saveQuery.getName());
                    queryInfo.setText(saveQuery.getText());
                    queryInfo.setDescription(saveQuery.getDescription());
                    queryInfo.setGatherDataSql(saveQuery.getGatherDataSql());
                    queryInfo.setLoadDataMode(saveQuery.getLoadDataMode());
                    queryInfo.setMetricList(saveQuery.getMetricList());
                    profileManager.addQuery(queryInfo);
                    TableInfo tableInfo = new TableInfo();
                    tableInfo.setTableName(saveQuery.getName());
                    profileManager.addTable(tableInfo);

                } else {
                    QueryInfo query = profileManager.getQueryInfoList().stream()
                            .filter(s -> s.getName().equals(selectedQueryName))
                            .findAny()
                            .orElseThrow(() -> new NotFoundException("Not found query by id: " + selectedDataQueryId));
                    newId = query.getId();
                }

                queryListId.add(newId);
            }
            task.setQueryInfoList(queryListId);
        } else {
            task.setQueryInfoList(Collections.emptyList());
        }

        return task;
    }

    private void setPanelView(Boolean isSelected) {
        taskButtonPanel.setButtonView(isSelected);
        taskPanel.getJTextFieldTask().setEditable(!isSelected);
        taskPanel.getJTextFieldDescription().setEditable(!isSelected);
        taskPanel.getTaskConnectionComboBox().setEnabled(!isSelected);
        multiSelectQueryPanel.getUnPickBtn().setEnabled(!isSelected);
        multiSelectQueryPanel.getPickBtn().setEnabled(!isSelected);
        multiSelectQueryPanel.getPickAllBtn().setEnabled(!isSelected);
        multiSelectQueryPanel.getUnPickAllBtn().setEnabled(!isSelected);
        if (isSelected) {
            taskPanel.getRadioButtonPanel().setButtonNotView();
        } else {
            taskPanel.getRadioButtonPanel().setButtonView();
        }
        jTabbedPaneConfig.setEnabledAt(0, isSelected);
        jTabbedPaneConfig.setEnabledAt(2, isSelected);
        jTabbedPaneConfig.setEnabledAt(3, isSelected);

        profileCase.getJxTable().setEnabled(isSelected);
        taskCase.getJxTable().setEnabled(isSelected);
        connectionCase.getJxTable().setEnabled(isSelected);
        queryCase.getJxTable().setEnabled(isSelected);
        checkboxConfig.setEnabled(isSelected);
    }

    private boolean isExistQueryName(String selectedName) {
        boolean isExist = false;
        List<QueryInfo> queryList = profileManager.getQueryInfoList();
        for (QueryInfo query : queryList) {
            if (query.getName().equals(selectedName)) {
                isExist = true;
            }
        }
        return isExist;
    }

    private List<String> getSelectedQueryNameList() {
        List<String> queryListId = new ArrayList<>();

        DefaultTableModel tableModel = multiSelectQueryPanel.getSelectedQueryCase().getDefaultTableModel();
        if (tableModel.getRowCount() > 0) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String selectedQueryName = tableModel.getValueAt(i, 1).toString();
                queryListId.add(selectedQueryName);
            }
        }

        return queryListId;
    }

    private List<String> getConnectionDriverAll() {
        List<String> connAllDriver = Collections.emptyList();
        List<ConnectionInfo> connectionInfoList = profileManager.getConnectionInfoList();
        connAllDriver = connectionInfoList.stream()
                .map(ConnectionInfo::getDriver)
                .collect(Collectors.toList());

        return connAllDriver.stream().distinct().collect(Collectors.toList());
    }

    private boolean isUsedOnTask(int taskId) {
        return !profileManager.getProfileInfoList().stream()
                .anyMatch(profile -> profile.getTaskInfoList().contains(taskId));
    }

}
