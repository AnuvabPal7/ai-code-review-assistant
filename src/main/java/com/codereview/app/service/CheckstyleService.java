package com.codereview.app.service;

import com.codereview.app.dto.StaticFinding;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class CheckstyleService {

    public List<StaticFinding> analyze(File javaFile) {
        List<StaticFinding> findings = new ArrayList<>();

        try {
            Configuration config = ConfigurationLoader.loadConfiguration(
                    "checkstyle-rules.xml",
                    new PropertiesExpander(new Properties())
            );

            Checker checker = new Checker();
            checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
            checker.configure(config);

            checker.addListener(new AuditListener() {
                @Override public void auditStarted(AuditEvent event) {}
                @Override public void auditFinished(AuditEvent event) {}
                @Override public void fileStarted(AuditEvent event) {}
                @Override public void fileFinished(AuditEvent event) {}

                @Override
                public void addError(AuditEvent event) {
                    findings.add(new StaticFinding("MEDIUM", event.getMessage(), "CHECKSTYLE", event.getLine()));
                }

                @Override
                public void addException(AuditEvent event, Throwable throwable) {}
            });

            checker.process(List.of(javaFile));
            checker.destroy();

        } catch (Exception e) {
            findings.add(new StaticFinding("LOW", "Checkstyle analysis failed: " + e.getMessage(), "CHECKSTYLE", 0));
        }

        return findings;
    }
}