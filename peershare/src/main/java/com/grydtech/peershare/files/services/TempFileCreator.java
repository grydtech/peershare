package com.grydtech.peershare.files.services;

import java.io.File;
import java.io.IOException;

public interface TempFileCreator {

    File createTempFile(String fileName) throws IOException;
}
