package com.floragunn.searchguard.sgctl.config.searchguard;

import com.floragunn.codova.documents.Document;

public interface NamedConfig<T> extends Document<T> {

    String getFileName();

}
