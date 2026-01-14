package com.floragunn.searchguard.sgctl.util.mapping.writer.realm_translation;

import com.floragunn.searchguard.sgctl.util.mapping.ir.elasticSearchYml.RealmIR;

public class FileTranslator extends RealmTranslator{
    @Override
    public NewAuthDomain translate(RealmIR ir) {
        realmNotImplementedReport(ir.getName(), ir);
        return null;
    }
}
