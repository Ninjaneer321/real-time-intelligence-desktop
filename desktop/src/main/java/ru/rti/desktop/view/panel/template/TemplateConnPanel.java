package ru.rti.desktop.view.panel.template;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jdesktop.swingx.JXTextField;
import org.painlessgridbag.PainlessGridBag;
import ru.rti.desktop.helper.GUIHelper;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class TemplateConnPanel extends JPanel {

    private final JLabel labelConnectionName;
    private final JLabel labelConnectionURL;
    private final JLabel labelConnectionUserName;
    private final JLabel labelConnectionJar;
    private final JLabel labelConnectionDriver;

    private final JXTextField connectionName;
    private final JXTextField connectionURL;
    private final JXTextField connectionUserName;
    private final JXTextField connectionJar;
    private final JXTextField connectionDriver;

    @Inject
    public TemplateConnPanel() {
        this.labelConnectionName = new JLabel("Name");
        this.labelConnectionURL = new JLabel("URL");
        this.labelConnectionUserName = new JLabel("Username");
        this.labelConnectionJar = new JLabel("Jar");
        this.labelConnectionDriver = new JLabel("Driver");

        this.connectionName = new JXTextField();
        this.connectionName.setPrompt("Connection name...");
        this.connectionName.setEditable(false);

        this.connectionURL = new JXTextField();
        this.connectionURL.setPrompt("URL...");
        this.connectionURL.setEditable(false);

        this.connectionUserName = new JXTextField();
        this.connectionUserName.setPrompt("User name...");
        this.connectionUserName.setEditable(false);

        this.connectionJar = new JXTextField();
        this.connectionJar.setPrompt("Jar...");
        this.connectionJar.setEditable(false);

        this.connectionDriver = new JXTextField();
        this.connectionDriver.setPrompt("Driver...");
        this.connectionDriver.setEditable(false);

        Border finalBorder = GUIHelper.getBorder();
        this.connectionName.setBorder(finalBorder);
        this.connectionURL.setBorder(finalBorder);
        this.connectionUserName.setBorder(finalBorder);
        this.connectionJar.setBorder(finalBorder);
        this.connectionDriver.setBorder(finalBorder);

        JPanel connectionPanel = new JPanel();
        PainlessGridBag gblCon = new PainlessGridBag(connectionPanel, GUIHelper.getPainlessGridbagConfigurationNull(), false);

        gblCon.row()
                .cell(labelConnectionURL).cell(connectionURL).fillX();
        gblCon.row()
                .cell(labelConnectionUserName).cell(connectionUserName).fillX();
        gblCon.row()
                .cell(labelConnectionJar).cell(connectionJar).fillX();
        gblCon.row()
                .cell(labelConnectionDriver).cell(connectionDriver).fillX();
        gblCon.row()
                .cell(new JLabel()).cell(new JLabel()).fillX();

        gblCon.done();

        PainlessGridBag gbl = new PainlessGridBag(this, GUIHelper.getPainlessGridbagConfiguration(), false);
        gbl.row().cellXRemainder(connectionPanel).fillX();;
        gbl.done();
    }
}
