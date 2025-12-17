package com.floragunn.searchguard.sgctl.util.mapping.writer;

import com.floragunn.codova.documents.DocWriter;
import com.floragunn.searchguard.sgctl.commands.MigrateConfig;
import com.floragunn.searchguard.sgctl.util.mapping.ir.IntermediateRepresentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class SearchGuardConfigWriter {
    MigrateConfig.SgAuthc sg_authc;
    UserConfigWriter userConfig;
    ActionGroupConfigWriter actionGroupConfig;
    RoleConfigWriter roleConfig;
    RoleMappingWriter mappingWriter;


    public SearchGuardConfigWriter(IntermediateRepresentation ir) {
        sg_authc = new MigrateConfig.SgAuthc();
        userConfig = new UserConfigWriter(ir);
        actionGroupConfig = new ActionGroupConfigWriter(ir);
        roleConfig = new RoleConfigWriter(ir, sg_authc, actionGroupConfig);
        mappingWriter = new RoleMappingWriter(ir);
    }

    public void writeTo(File directory) throws IOException {
        final var writer = DocWriter.yaml();
//        Files.write(new File(directory.getPath(), MigrateConfig.SgAuthc.FILE_NAME).toPath(), writer.writeAsString(sg_authc).getBytes());
        Files.write(new File(directory.getPath(), UserConfigWriter.FILE_NAME).toPath(), writer.writeAsString(userConfig).getBytes());
        Files.write(new File(directory.getPath(), RoleConfigWriter.FILE_NAME).toPath(), writer.writeAsString(roleConfig).getBytes());
//        Files.write(new File(directory.getPath(), ActionGroupConfigWriter.FILE_NAME).toPath(), writer.writeAsString(actionGroupConfig).getBytes());
        Files.write(new File(directory.getPath(), RoleMappingWriter.FILE_NAME).toPath(), writer.writeAsString(mappingWriter).getBytes());
    }

    public void printFiles() {
        var writer = DocWriter.yaml();

//        printHeader(MigrateConfig.SgAuthc.FILE_NAME);
//        print(writer.writeAsString(sg_authc));

        printHeader(UserConfigWriter.FILE_NAME);
        print(writer.writeAsString(userConfig));

        printHeader(RoleConfigWriter.FILE_NAME);
        print(writer.writeAsString(roleConfig));

//        printHeader(ActionGroupConfigWriter.FILE_NAME);
//        print(writer.writeAsString(actionGroupConfig));

        printHeader(RoleMappingWriter.FILE_NAME);
        print(writer.writeAsString(mappingWriter));
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
