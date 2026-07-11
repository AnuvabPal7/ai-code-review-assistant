package com.codereview.app.service;

import com.codereview.app.dto.StaticFinding;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class PmdService {

    public List<StaticFinding> analyze(File javaFile) {
        List<StaticFinding> findings = new ArrayList<>();

        PMDConfiguration config = new PMDConfiguration();
        config.setInputPathList(List.of(javaFile.toPath()));
        config.addRuleSet("category/java/bestpractices.xml");
        config.addRuleSet("category/java/errorprone.xml");

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            Report report = pmd.performAnalysisAndCollectReport();
            for (RuleViolation violation : report.getViolations()) {
                findings.add(new StaticFinding(
                        violation.getRule().getPriority().toString(),
                        violation.getDescription(),
                        "PMD",
                        violation.getBeginLine()
                ));
            }
        } catch (Exception e) {
            findings.add(new StaticFinding("LOW", "PMD analysis failed: " + e.getMessage(), "PMD", 0));
        }

        return findings;
    }
}