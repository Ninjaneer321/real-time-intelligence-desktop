package ru.rti.desktop.config.prototype.profile;

import dagger.Subcomponent;
import ru.rti.desktop.config.prototype.WorkspaceProfileScope;
import ru.rti.desktop.config.prototype.task.WorkspaceTaskComponent;
import ru.rti.desktop.config.prototype.task.WorkspaceTaskModule;
import ru.rti.desktop.view.structure.workspace.profile.WorkspaceProfileView;

@WorkspaceProfileScope
@Subcomponent(modules = WorkspaceProfileModule.class)
public interface WorkspaceProfileComponent {
  void inject(WorkspaceProfileView workspaceProfileView);
  WorkspaceTaskComponent initTask(WorkspaceTaskModule workspaceTaskModule);
}