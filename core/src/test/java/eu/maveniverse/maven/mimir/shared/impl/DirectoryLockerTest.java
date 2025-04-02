package eu.maveniverse.maven.mimir.shared.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DirectoryLockerTest {
    @Test
    void shared(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = new DirectoryLocker();

        try {
            locker.lockDirectory(basedir, false);
            locker.lockDirectory(basedir, false);
        } finally {
            locker.unlockDirectory(basedir);
            locker.unlockDirectory(basedir);
        }
    }

    @Test
    void exclusive(@TempDir Path basedir) throws IOException {
        DirectoryLocker locker = new DirectoryLocker();

        try {
            locker.lockDirectory(basedir, true);
            assertThrows(IOException.class, () -> locker.lockDirectory(basedir, false));
        } finally {
            locker.unlockDirectory(basedir);
        }
    }
}
