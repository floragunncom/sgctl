package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SearchGuardConfigWriter {
    MigrateConfig.SgAuthc sgAuthc;
    MigrateConfig.SgAuthc sgFrontendAuthc;
    UserConfigWriter userConfig;
    ActionGroupConfigWriter actionGroupConfig;
    RoleConfigWriter roleConfig;
    RoleMappingWriter mappingWriter;


    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        var sgTranslator = new SGAuthcTranslator(ir.getElasticSearchYml());
        sgAuthc = sgTranslator.getConfig();
        sgFrontendAuthc = sgTranslator.getFrontEndConfig();
        userConfig = new UserConfigWriter(ir);
        actionGroupConfig = new ActionGroupConfigWriter();
        roleConfig = new RoleConfigWriter(ir, sgAuthc, actionGroupConfig);
        mappingWriter = new RoleMappingWriter(ir);
    }

    public void writeTo(File directory) throws IOException {
        final var writer = DocWriter.yaml();
        Files.write(new File(directory.getPath(), "sg_authc.yml"/*MigrateConfig.SgAuthc.FILE_NAME*/).toPath(), writer.writeAsString(sgAuthc).getBytes());
        Files.write(new File(directory.getPath(), "sg_frontend_authc.yml"/*MigrateConfig.SgAuthc.FILE_NAME*/).toPath(), writer.writeAsString(sgFrontendAuthc).getBytes());
        Files.write(new File(directory.getPath(), UserConfigWriter.FILE_NAME).toPath(), writer.writeAsString(userConfig).getBytes());
        Files.write(new File(directory.getPath(), RoleConfigWriter.FILE_NAME).toPath(), writer.writeAsString(roleConfig).getBytes());
        Files.write(new File(directory.getPath(), ActionGroupConfigWriter.FILE_NAME).toPath(), writer.writeAsString(actionGroupConfig).getBytes());
        Files.write(new File(directory.getPath(), RoleMappingWriter.FILE_NAME).toPath(), writer.writeAsString(mappingWriter).getBytes());
    }

    public void printFiles() {
        var writer = DocWriter.yaml();

        printHeader("sg_authc.yml"/*MigrateConfig.SgAuthc.FILE_NAME*/);
        print(writer.writeAsString(sgAuthc));
        printFooter();

        printHeader("sg_frontend_authc.yml"/*MigrateConfig.SgAuthc.FILE_NAME*/);
        print(writer.writeAsString(sgFrontendAuthc));
        printFooter();

        printHeader(UserConfigWriter.FILE_NAME);
        print(writer.writeAsString(userConfig));
        printFooter();

        printHeader(RoleConfigWriter.FILE_NAME);
        print(writer.writeAsString(roleConfig));
        printFooter();

        printHeader(ActionGroupConfigWriter.FILE_NAME);
        print(writer.writeAsString(actionGroupConfig));
        printFooter();

        printHeader(RoleMappingWriter.FILE_NAME);
        print(writer.writeAsString(mappingWriter));
        printFooter();
    }

    static private void printHeader(String filename) {
        print("\n----------------------------- " + filename + " --------------------------------------");
    }

    static private void printFooter() {
        print("--------------------------------------------------------------------------------------");
    }

    static void print(Object line) {
        System.out.println(line);
    }
}
