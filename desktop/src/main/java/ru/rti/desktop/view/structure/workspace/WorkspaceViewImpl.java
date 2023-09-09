package ru.rti.desktop.view.structure.workspace;

import dagger.Lazy;
import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import lombok.extern.log4j.Log4j2;
import ru.rti.desktop.view.BaseFrame;
import ru.rti.desktop.view.structure.WorkspaceView;
import ru.rti.desktop.view.structure.workspace.profile.WorkspaceProfileView;

@Log4j2
@Singleton
public class WorkspaceViewImpl extends JPanel implements WorkspaceView {

    private final Lazy<BaseFrame> jFrame;
    private final Lazy<WorkspacePresenter> workspacePresenter;

    private final JPanel profileViewContainer = new JPanel();

    @Inject
    public WorkspaceViewImpl(Lazy<BaseFrame> jFrame,
                             @Named("workspacePresenter") Lazy<WorkspacePresenter> workspacePresenter) {
        this.jFrame = jFrame;
        this.workspacePresenter = workspacePresenter;

        this.setLayout(new BorderLayout());

        this.profileViewContainer.setLayout(new BorderLayout());
        this.profileViewContainer.setBorder(new EtchedBorder());

        this.add(profileViewContainer, BorderLayout.CENTER);
    }

    @Override
    public WorkspaceProfileView addWorkspaceProfileView(int profileId) {
        profileViewContainer.removeAll();

        WorkspaceProfileView workspaceProfileView = new WorkspaceProfileView(profileId);
        profileViewContainer.add(workspaceProfileView, BorderLayout.CENTER);

        jFrame.get().repaintJSplitPane();

        return workspaceProfileView;
    }

    @Override
    public void bindPresenter() {
        this.workspacePresenter.get();
    }
}
